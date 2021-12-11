package xt.hotswitcher;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.io.File;

public class Config {
    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec config;

    static {
        ConfigSettings.init(builder);
        config = builder.build();
    }

    public static void loadConfig(ForgeConfigSpec config, String path) {
        HotSwitcher.LOGGER.info("Loading config: " + path);
        final CommentedFileConfig file = CommentedFileConfig.builder(new File(path))
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE).build();
        HotSwitcher.LOGGER.info("Build config: " + path);
        file.load();
        HotSwitcher.LOGGER.info("Loaded config: " + path);
        config.setConfig(file);
    }
}
