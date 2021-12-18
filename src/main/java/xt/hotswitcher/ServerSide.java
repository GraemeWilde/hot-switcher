package xt.hotswitcher;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ServerSide {

    public void registerEvents() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverSetup);
    }

    public void serverSetup(final FMLDedicatedServerSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    public void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(Commands.literal("hotswitcher2")
                .executes(
                        context -> {
                            context.getSource().sendFailure(new TextComponent("This command should not go through. Please check that you have HotSwitcher installed."));
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
}
