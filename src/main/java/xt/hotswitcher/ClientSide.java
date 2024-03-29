package xt.hotswitcher;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public class ClientSide {

    private static final EnumMap<KeyModifier, Map<InputConstants.Key, Collection<KeyMapping>>> MAP;

    public static final KeyMapping cycle_hotbar = new KeyMapping("key.hotswitcher.swap_hotbar", KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputConstants.Type.KEYSYM, InputConstants.KEY_R, "key.categories.hotswitcher");
    public static final KeyMapping cycle_hotbar_reverse = new KeyMapping("key.hotswitcher.swap_hotbar_reverse", KeyConflictContext.UNIVERSAL, KeyModifier.ALT, InputConstants.Type.KEYSYM, InputConstants.KEY_R, "key.categories.hotswitcher");

    public static final KeyMapping cycle_slot = new KeyMapping("key.hotswitcher.swap_slot", KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputConstants.Type.KEYSYM, InputConstants.KEY_G, "key.categories.hotswitcher");
    public static final KeyMapping cycle_slot_reverse = new KeyMapping("key.hotswitcher.swap_slot_reverse", KeyConflictContext.UNIVERSAL, KeyModifier.ALT, InputConstants.Type.KEYSYM, InputConstants.KEY_G, "key.categories.hotswitcher");

    public static final KeyMapping config = new KeyMapping("key.hotswitcher.config", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.UNKNOWN, "key.categories.hotswitcher");

    //public KeyMapping[] hotbarKeybindings = new KeyMapping[]{};

    Consumer<InputEvent.KeyInputEvent> keyInputEventHandler;
    Consumer<ScreenEvent.MouseScrollEvent.Post> guiScrollEventHandler;

    Consumer<TickEvent.ClientTickEvent> clientTickEventHandler;
    public boolean openScreen = false;

    static {
        // Reflection to get access to the keybindings map and its getBinding function
        // This is used to find the keybinding that has precedence, and then if it is one of mine, it allows me to
        // watch for the binding when the chest screen is open (Normally chest screen blocks bindings).
        //Field keyMappingHashField;
        Field keyBindingMapMapField;
        try {

//            Field[] fields = KeyBindingMap.class.getDeclaredFields();
//
//            HotSwitcher.LOGGER.info("Fields--------------");
//            for (int i = 0; i < fields.length; i++) {
//                HotSwitcher.LOGGER.info("Field " + i + " named: " + fields[i].getName() + " type: " + fields[i].getType().getName());
//            }

            try {
                keyBindingMapMapField = KeyBindingMap.class.getDeclaredField("map");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            keyBindingMapMapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            EnumMap<KeyModifier, Map<InputConstants.Key, Collection<KeyMapping>>> map_temp = (EnumMap<KeyModifier, Map<InputConstants.Key, Collection<KeyMapping>>>) keyBindingMapMapField.get(null);

            MAP = map_temp;

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public ClientSide() {
        // Event handler variables, used to add and remove these events
        keyInputEventHandler = this::onInputEvent;
        guiScrollEventHandler = this::guiMouseScrollEvent;
        clientTickEventHandler = this::clientTickEvent;
    }

    public void registerEvents() {
        // Attach clientSetup initializer function to the initializer event bus
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        // Loads configuration
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.config);
        Config.loadConfig(Config.config, FMLPaths.CONFIGDIR.get().resolve(HotSwitcher.MOD_ID + "-config.toml").toString());
        HotSwitcher.LOGGER.info("Register Config");

        // Register my config screen to forge
        ModLoadingContext.get().registerExtensionPoint(
                ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((mc, screen) -> new ConfigScreen(screen))
                //() -> (mc, screen) -> new ConfigScreen(screen)
        );

    }



    public void clientSetup(final FMLClientSetupEvent event) {
        // Register keybindings. They will now show up on the controls screen allowing users to change them
        ClientRegistry.registerKeyBinding(cycle_hotbar);
        ClientRegistry.registerKeyBinding(cycle_hotbar_reverse);
        ClientRegistry.registerKeyBinding(cycle_slot);
        ClientRegistry.registerKeyBinding(cycle_slot_reverse);

        ClientRegistry.registerKeyBinding(config);


        //if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.isClientSide) {
        HotSwitcher.LOGGER.info("ClientSide?");
        MinecraftForge.EVENT_BUS.addListener(this::clientPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::clientPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        //}

        MinecraftForge.EVENT_BUS.addListener(this::clientChatEvent);

//            HotSwitcher.LOGGER.info("CLIENT.");
//        } else {
//            HotSwitcher.LOGGER.info("Not CLIENT!?");
//        }
    }


    public void clientChatEvent(ClientChatEvent event) {
        HotSwitcher.LOGGER.info("ChatEvent: " + event.getOriginalMessage());
        if (event.getOriginalMessage().equals("/hotswitcher")) {
            //Minecraft.getInstance().setScreen(new ConfigScreen(null));
            openScreen = true;
            MinecraftForge.EVENT_BUS.addListener(clientTickEventHandler);
            event.setCanceled(true);
        }
    }

    public void clientTickEvent(TickEvent.ClientTickEvent event) {
        HotSwitcher.LOGGER.info("clientTickEvent: " + event.phase + " - " + event.side);
        if (openScreen) {
            openScreen = false;
            Minecraft.getInstance().setScreen(new ConfigScreen(null));
            MinecraftForge.EVENT_BUS.unregister(clientTickEventHandler);
        }
    }


    public void clientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
        HotSwitcher.LOGGER.info("Client Logged In");
        MinecraftForge.EVENT_BUS.addListener(keyInputEventHandler);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, guiScrollEventHandler);

        if (event.getPlayer() != null && ConfigSettings.getConfigShowInitMessage())
            event.getPlayer().displayClientMessage(new TranslatableComponent("hotswitcher.load_message").withStyle(ChatFormatting.GRAY), false);
    }

    public void clientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        HotSwitcher.LOGGER.info("Client Logged Out");
        MinecraftForge.EVENT_BUS.unregister(keyInputEventHandler);
        MinecraftForge.EVENT_BUS.unregister(guiScrollEventHandler);
    }

    public void guiMouseScrollEvent(ScreenEvent.MouseScrollEvent.Post event) {
        if (ConfigSettings.getConfigEnableHotbarInContainers()) {

            // Allow scrolling the hotbar when container is open
            if (Minecraft.getInstance().screen instanceof AbstractContainerScreen) {
                Objects.requireNonNull(Minecraft.getInstance().player).getInventory().swapPaint((int) event.getScrollDelta());
                event.setCanceled(true);
            }
        }
    }

    public void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(Commands.literal("hotswitcher")
                .executes(
                        context -> {
//                            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.isClientSide) {
//                                //DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> {
//                                    HotSwitcher.LOGGER.info("Client.");
//                                    Minecraft.getInstance().setScreen(new ConfigScreen(null));
//                                    //return null;
//                                //});
//                            } else {
//                                HotSwitcher.LOGGER.info("Not CLIENT!?");
//                            }
//                            //Minecraft.getInstance().setScreen(null);
                            return 0;
                        }
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
            if (minecraft.screen == null || minecraft.screen.passEvents
                    || minecraft.screen instanceof AbstractContainerScreen) {

                InputConstants.Key keyCode = InputConstants.getKey(event.getKey(), event.getScanCode());

                ArrayList<KeyMapping> keyBinds = new ArrayList<>();

                // For each active mod key, search keybindings for modkey + keycode. Check if each found keybind is
                // active, if it is, add to keybinds list
                mods.forEach(keyMod -> {
                    Collection<KeyMapping> bindings = MAP.get(keyMod).get(keyCode);
                    if (bindings != null) {
                        for (KeyMapping binding : bindings) {
                            if (binding.isActiveAndMatches(keyCode)) {
                                keyBinds.add(binding);
                            }
                        }
                    }
                });

                // If no keybinds were found above, search for keybinds with the same keycode but no mods
                if (keyBinds.size() == 0) {
                    Collection<KeyMapping> bindings = MAP.get(KeyModifier.NONE).get(keyCode);
                    if (bindings != null) {
                        for (KeyMapping binding : bindings) {
                            if (binding.isActiveAndMatches(keyCode)) {
                                keyBinds.add(binding);
                            }
                        }
                    }
                }

                if (keyBinds.size() != 0) {
                    MultiPlayerGameMode playerController = Objects.requireNonNull(minecraft.gameMode);
                    LocalPlayer player = Objects.requireNonNull(minecraft.player);

                    // Container screens have different slotIds and windowId
                    int invBottomLeftSlot = -1;
                    int container;

                    if (minecraft.screen instanceof AbstractContainerScreen) {
                        container = player.containerMenu.containerId;
                        List<Slot> inv = ((AbstractContainerScreen<?>) minecraft.screen).getMenu().slots;
                        if (inv.get(inv.size() - 18).getSlotIndex() == 27) {
                            invBottomLeftSlot = inv.size() - 18;
                        } else {
                            for (Slot slot : inv) {
                                if (slot.container == player.getInventory()) {
                                    if (slot.getSlotIndex() == 27) {
                                        HotSwitcher.LOGGER.debug("Slot number: " + slot.index + " - " + slot.getItem().toString());
                                        invBottomLeftSlot = slot.index;
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        invBottomLeftSlot = 27;
                        container = player.containerMenu.containerId;
                    }

                    if (invBottomLeftSlot == -1) {
                        HotSwitcher.LOGGER.debug("Error finding inventory slot 27");
                        return;
                    }
                    // Creative Inventory Screen hack
                    else if (invBottomLeftSlot == 0 && minecraft.screen instanceof CreativeModeInventoryScreen) {
                        invBottomLeftSlot = 27;
                    }


                    if (keyBinds.contains(cycle_hotbar)) {
                        // Swap hotbar with highest configured inventory row, repeat with each lower inventory row,
                        // end result should be rotating all configured rows down
                        for (int j = ConfigSettings.getConfigSwapBarCount() - 1; j >= 0; j--) {
                            for (int i = 0; i < 9; i++) {
                                playerController.handleInventoryMouseClick(container, invBottomLeftSlot - j * 9 + i, i, ClickType.SWAP, player);
                            }
                        }
                    } else if (keyBinds.contains(cycle_hotbar_reverse)) {
                        // Swap hotbar with lowest inventory row, repeat with each higher configured inventory row,
                        // end result should be rotating all configured rows down
                        for (int j = 0; j <= ConfigSettings.getConfigSwapBarCount() - 1; j++) {
                            for (int i = 0; i < 9; i++) {
                                playerController.handleInventoryMouseClick(container, invBottomLeftSlot - j * 9 + i, i, ClickType.SWAP, player);
                            }
                        }
                    }


                    if (keyBinds.contains(cycle_slot)) {
                        // Swap active slot with highest configured inventory active slot in same column, repeat with each
                        // lower inventory slot, end result should be rotating all configured rows down
                        for (int j = ConfigSettings.getConfigSwapSlotCount() - 1; j >= 0; j--) {
                            playerController.handleInventoryMouseClick(container, invBottomLeftSlot - j * 9 + player.getInventory().selected, player.getInventory().selected, ClickType.SWAP, player);
                        }
                    } else if (keyBinds.contains(cycle_slot_reverse)) {
                        // Swap active slot with lowest inventory active slot in same column, repeat with each higher
                        // configured inventory slot, end result should be rotating all configured slots up
                        for (int j = 0; j <= ConfigSettings.getConfigSwapSlotCount() - 1; j++) {
                            playerController.handleInventoryMouseClick(container, invBottomLeftSlot - j * 9 + player.getInventory().selected, player.getInventory().selected, ClickType.SWAP, player);
                        }
                    }

                    // Open Config
                    if (keyBinds.contains(config)) {
                        Minecraft.getInstance().setScreen(new ConfigScreen(null));
                    }

                    if (ConfigSettings.getConfigEnableHotbarInContainers()) {
                        if (minecraft.screen instanceof AbstractContainerScreen) {
                            for (int i = 0; i < 9; i++) {
                                if (keyBinds.contains(minecraft.options.keyHotbarSlots[i])) {
                                    player.getInventory().selected = i;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
