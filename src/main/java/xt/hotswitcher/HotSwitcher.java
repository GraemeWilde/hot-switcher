package xt.hotswitcher;


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("hotswitcher")
//@Mod.EventBusSubscriber(Dist.CLIENT)//(Dist.CLIENT)
//@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HotSwitcher {
    public static final String MOD_ID = "hotswitcher";
    public static final Logger LOGGER = LogManager.getLogger();


    public HotSwitcher() {

        // Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        final ClientSide clientSide = new ClientSide();
        final ServerSide serverSide = new ServerSide();

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> clientSide::registerEvents);

        DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> serverSide::registerEvents);

    }

}
