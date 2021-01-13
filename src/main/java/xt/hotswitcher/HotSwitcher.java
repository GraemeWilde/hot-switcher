package xt.hotswitcher;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.command.Commands;
import net.minecraft.command.impl.BanCommand;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

@Mod("hotswitcher")
@Mod.EventBusSubscriber//(Dist.CLIENT)
//@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HotSwitcher {
    public static final String MOD_ID = "hotswitcher";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final KeyBindingMap HASH;
    private static final Method GET_BINDING_METHOD;

    public static final KeyBinding cycle_hotbar = new KeyBinding("key.hotswitcher.swap_hotbar", KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.hotswitcher");
    public static final KeyBinding cycle_hotbar_reverse = new KeyBinding("key.hotswitcher.swap_hotbar_reverse", KeyConflictContext.UNIVERSAL, KeyModifier.ALT, InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.hotswitcher");

    public static final KeyBinding cycle_slot = new KeyBinding("key.hotswitcher.swap_slot", KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.hotswitcher");
    public static final KeyBinding cycle_slot_reverse = new KeyBinding("key.hotswitcher.swap_slot_reverse", KeyConflictContext.UNIVERSAL, KeyModifier.ALT, InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.hotswitcher");

    public KeyBinding[] hotbarKeybindings = new KeyBinding[]{};

    Consumer<InputEvent.KeyInputEvent> keyInputEventHandler;
    Consumer<GuiScreenEvent.MouseScrollEvent.Post> guiScrollEventHandler;

    static {
        // Reflection to get access to the keybindings map and its getBinding function
        // This is used to find the keybinding that has precedence, and then if it is one of mine, it allows me to
        // watch for the binding when the chest screen is open (Normally chest screen blocks bindings).
        Field keyBindingHashField;
        try {
            keyBindingHashField = KeyBinding.class.getDeclaredField("HASH");
            keyBindingHashField.setAccessible(true);
            HASH = (KeyBindingMap)keyBindingHashField.get(null);

            GET_BINDING_METHOD = KeyBindingMap.class.getDeclaredMethod("getBinding", InputMappings.Input.class, KeyModifier.class);
            GET_BINDING_METHOD.setAccessible(true);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public HotSwitcher() {
        // Attach clientSetup initializer function to the initializer event bus
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        // Loads configuration
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.config);
        Config.loadConfig(Config.config, FMLPaths.CONFIGDIR.get().resolve(HotSwitcher.MOD_ID + "-config.toml").toString());
        HotSwitcher.LOGGER.info("Register Config");

        // Register my config screen to forge
        ModLoadingContext.get().registerExtensionPoint(
                ExtensionPoint.CONFIGGUIFACTORY,
                () -> (mc, screen) -> new ConfigScreen(screen)
        );

        keyInputEventHandler = this::onInputEvent;
        guiScrollEventHandler = this::guiMouseScrollEvent;

        /*try {
            Field keybindDescriptionMapField = KeyBinding.class.getDeclaredField("KEYBIND_ARRAY");
            keybindDescriptionMapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, KeyBinding> keybindDescriptionMap = (Map<String, KeyBinding>)keybindDescriptionMapField.get(null);
            keybindDescriptionMap.get("");

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }*/
    }



    private void clientSetup(final FMLClientSetupEvent event) {
        // Register keybindings. They will now show up on the controls screen allowing users to change them
        ClientRegistry.registerKeyBinding(cycle_hotbar);
        ClientRegistry.registerKeyBinding(cycle_hotbar_reverse);
        ClientRegistry.registerKeyBinding(cycle_slot);
        ClientRegistry.registerKeyBinding(cycle_slot_reverse);

        MinecraftForge.EVENT_BUS.addListener(this::clientPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::clientPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    public void clientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
        HotSwitcher.LOGGER.info("Client Logged In");
        MinecraftForge.EVENT_BUS.addListener(keyInputEventHandler);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, guiScrollEventHandler);
    }

    public void clientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        HotSwitcher.LOGGER.info("Client Logged Out");
        MinecraftForge.EVENT_BUS.unregister(keyInputEventHandler);
        MinecraftForge.EVENT_BUS.unregister(guiScrollEventHandler);
    }

    public void guiMouseScrollEvent(GuiScreenEvent.MouseScrollEvent.Post event) {
        if (ConfigSettings.getConfigEnableHotbarInContainers()) {

            // Allow scrolling the hotbar when container is open
            if (Minecraft.getInstance().currentScreen instanceof ContainerScreen) {
                Objects.requireNonNull(Minecraft.getInstance().player).inventory.changeCurrentItem((int) event.getScrollDelta());
                event.setCanceled(true);
            }
        }
    }

    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hotswitcher")
        .executes(
                context -> { Minecraft.getInstance().displayGuiScreen(new ConfigScreen(null)); return 0; }
                ));
    }

    public void onInputEvent(InputEvent.KeyInputEvent event) {
        // Translate event mod keys from GLFW_MOD_ to Set<> of Forge KeyModifier type
        EnumSet<KeyModifier> mods = EnumSet.noneOf(KeyModifier.class);
        if ((event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0)
            mods.add(KeyModifier.SHIFT);
        if ((event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0)
            mods.add(KeyModifier.CONTROL);
        if ((event.getModifiers() & GLFW.GLFW_MOD_ALT) != 0)
            mods.add(KeyModifier.ALT);


        if (event.getAction() == GLFW.GLFW_PRESS) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.currentScreen == null || minecraft.currentScreen.passEvents
                    || minecraft.currentScreen instanceof ContainerScreen) {

                // For each active mod key, search keybindings for modkey + keycode (Uses reflection because of private
                // fields), then add to list of keybindings that should be activated
                ArrayList<KeyBinding> keyBinds = new ArrayList<>();
                mods.forEach(keyMod -> {
                    try {
                        KeyBinding bind = (KeyBinding) GET_BINDING_METHOD
                                .invoke(HASH, InputMappings.getInputByCode(event.getKey(), event.getScanCode()), keyMod);
                        if (bind != null) {
                            keyBinds.add(bind);
                            HotSwitcher.LOGGER.info("Keybind Description: " + bind.getKeyDescription());
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });

                // If no keybinding was found with the active mod keys, search for a fall through one with no mods
                // ex. player may be holding shift/control to crouch/sprint, but also trying to use our keybinding
                if (keyBinds.isEmpty()) {
                    try {
                        KeyBinding bind = (KeyBinding) GET_BINDING_METHOD
                                .invoke(HASH, InputMappings.getInputByCode(event.getKey(), event.getScanCode()), KeyModifier.NONE);
                        if (bind != null) {
                            keyBinds.add(bind);
                            HotSwitcher.LOGGER.info("Keybind Description: " + bind.getKeyDescription());
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }

                PlayerController playerController = Objects.requireNonNull(minecraft.playerController);
                ClientPlayerEntity player = Objects.requireNonNull(minecraft.player);

                // Container screens have different slotIds and windowId
                int invBottomLeftSlot = -1;
                int container;
                if (minecraft.currentScreen instanceof ContainerScreen) {
                    container = player.openContainer.windowId;
                    List<Slot> inv = ((ContainerScreen<?>) minecraft.currentScreen).getContainer().inventorySlots;
                    for (Slot slot : inv) {
                        if (slot.inventory == player.inventory) {
                            if (slot.getSlotIndex() == 27) {
                                HotSwitcher.LOGGER.info("Slot number: " + slot.slotNumber);
                                invBottomLeftSlot = slot.slotNumber;
                                break;
                            }
                        }
                    }
                } else {
                    invBottomLeftSlot = 27;
                    container = player.container.windowId;
                }
                if (invBottomLeftSlot == -1) {
                    HotSwitcher.LOGGER.info("Error finding inventory slot 27");
                    return;
                // Creative Inventory Screen hack
                } else if (invBottomLeftSlot == 0 && minecraft.currentScreen instanceof CreativeScreen) {
                    invBottomLeftSlot = 27;
                }

                if (keyBinds.contains(cycle_hotbar)) {
                    // Swap hotbar with highest configured inventory row, repeat with each lower inventory row,
                    // end result should be rotating all configured rows down
                    for (int j = ConfigSettings.getConfigSwapBarCount() - 1; j >= 0; j--) {
                        for (int i = 0; i < 9; i++) {
                            playerController.windowClick(container, invBottomLeftSlot - j * 9 + i, i, ClickType.SWAP, player);
                        }
                    }
                }

                if (keyBinds.contains(cycle_hotbar_reverse)) {
                    // Swap hotbar with lowest inventory row, repeat with each higher configured inventory row,
                    // end result should be rotating all configured rows down
                    for (int j = 0; j <= ConfigSettings.getConfigSwapBarCount() - 1; j++) {
                        for (int i = 0; i < 9; i++) {
                            playerController.windowClick(container, invBottomLeftSlot - j * 9 + i, i, ClickType.SWAP, player);
                        }
                    }
                }

                if (keyBinds.contains(cycle_slot)) {
                    // Swap active slot with highest configured inventory active slot in same column, repeat with each
                    // lower inventory slot, end result should be rotating all configured rows down
                    for (int j = ConfigSettings.getConfigSwapSlotCount() - 1; j >= 0; j--) {
                        playerController.windowClick(container, invBottomLeftSlot - j * 9 + player.inventory.currentItem, player.inventory.currentItem, ClickType.SWAP, player);
                    }
                }
                if (keyBinds.contains(cycle_slot_reverse)) {
                    // Swap active slot with lowest inventory active slot in same column, repeat with each higher
                    // configured inventory slot, end result should be rotating all configured slots up
                    for (int j = 0; j <= ConfigSettings.getConfigSwapSlotCount() - 1; j++) {
                        playerController.windowClick(container, invBottomLeftSlot - j * 9 + player.inventory.currentItem, player.inventory.currentItem, ClickType.SWAP, player);
                    }
                }

                if (ConfigSettings.getConfigEnableHotbarInContainers()) {
                    if (minecraft.currentScreen instanceof ContainerScreen) {
                        for (int i = 0; i < 9; i++) {
                            //this.player.inventory.currentItem = i;
                            if (keyBinds.contains(minecraft.gameSettings.keyBindsHotbar[i])) {
                                player.inventory.currentItem = i;
                            }
                        }
                    }
                }
            }
        }
    }
}
