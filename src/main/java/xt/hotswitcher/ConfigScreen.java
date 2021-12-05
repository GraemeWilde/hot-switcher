package xt.hotswitcher;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CycleOption;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

public final class ConfigScreen extends Screen {
    private static final int TITLE_HEIGHT = 8;

    private static final int OPTIONS_LIST_TOP_HEIGHT = 24;
    private static final int OPTIONS_LIST_BOTTOM_OFFSET = 32;
    private static final int OPTIONS_LIST_ITEM_HEIGHT = 25;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int DONE_BUTTON_TOP_OFFSET = 26;

    private final Screen parentScreen;

    private OptionsList optionsRowList;

    private ConfigSettings settings;

    public ConfigScreen(Screen parentScreen) {
        super(new TranslatableComponent("hotswitcher.configGui.title"));
        this.parentScreen = parentScreen;
        HotSwitcher.LOGGER.info("Constructor End Config Screen");
    }



    @Override
    @ParametersAreNonnullByDefault
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        //HotSwitcher.LOGGER.info("Render Start Config Screen");
        this.renderBackground(matrixStack);
        this.optionsRowList.render(matrixStack, mouseX, mouseY, partialTicks);
        this.font.draw(matrixStack, this.title.getString(),
                (float)this.width / 2 - ((float)this.font.width(this.title.getString()) / 2),
                TITLE_HEIGHT,
                0xFFFFFF);
        //HotSwitcher.LOGGER.info("Render End Config Screen");
        super.render(matrixStack, mouseX, mouseY, partialTicks);



//        List<FormattedCharSequence> list = OptionsSubScreen.tooltipAt(this.optionsRowList, mouseX, mouseY);
//        if (list != null) {
//            this.renderTooltip(matrixStack, list, mouseX, mouseY);
//        }
    }

    @Override
    protected void init() {
        //super.init();

        HotSwitcher.LOGGER.info("Init Start Config Screen");
        settings = ConfigSettings.currentSettings();

        this.optionsRowList = new OptionsList(
                Objects.requireNonNull(this.minecraft), this.width, this.height,
                OPTIONS_LIST_TOP_HEIGHT,
                this.height - OPTIONS_LIST_BOTTOM_OFFSET,
                OPTIONS_LIST_ITEM_HEIGHT
        );
        HotSwitcher.LOGGER.info("Init Config Screen");

//        Integer[] vals = {1, 2, 3};
//
//
//        CycleOption<Integer> swapBarCount = CycleOption.create(
//                "hotswitcher.configGui.swapBarCount.title",
//                new Integer[] {1, 2, 3},
//                (v) -> new TranslatableComponent("Test"),
//                (o) -> settings.getSwapBarCount(),
//                (o1, o2, sbc) -> settings.incrementSwapBarCount(1)
//        );

        this.optionsRowList.addBig(
                CycleOption.create(
                        "hotswitcher.configGui.swapBarCount.title",
                        new Integer[] {1, 2, 3},
                        (v) -> {
                            HotSwitcher.LOGGER.info("SwapBarCount: " + v);
                            return new TranslatableComponent(v.toString());
                        },
                        (o) -> {
                            return settings.getSwapBarCount();
                        },
                        (o1, o2, sbc) -> {
                            HotSwitcher.LOGGER.info("SwapBarCount Setter: " + sbc);
                            settings.setSwapBarCount(sbc);
                        }
                )
        );

        this.optionsRowList.addBig(
                CycleOption.create(
                        "hotswitcher.configGui.swapSlotCount.title",
                        new Integer[] {1, 2, 3},
                        (v) -> {
                            HotSwitcher.LOGGER.info("SwapSlotCount: " + v);
                            return new TranslatableComponent(v.toString());
                        },
                        (o) -> {
                            return settings.getSwapSlotCount();
                        },
                        (o1, o2, ssc) -> {
                            HotSwitcher.LOGGER.info("SwapSlotCount Setter: " + ssc);
                            settings.setSwapSlotCount(ssc);
                        }
                )
        );
//                new CycleOption(
//                "hotswitcher.configGui.swapSlotCount.title",
//                (unused, newValue) -> settings.incrementSwapSlotCount(newValue),
//                (gameSettings, option) ->
//                        new TranslatableComponent("hotswitcher.configGui.swapSlotCount.title")
//                                .append(new TextComponent(": " + settings.getSwapSlotCount()))
//        ));

        this.optionsRowList.addBig(
             CycleOption.createOnOff(
                     "hotswitcher.configGui.enableHotbarInContainer.title",
                     (o) -> settings.getEnableHotbarInContainers(),
                     (o1, o2, b) -> {
                         settings.setEnableHotbarInContainers(b);
                     }
             )
        );

//                new BooleanOption(
//                "hotswitcher.configGui.enableHotbarInContainer.title",
//                new TranslatableComponent("hotswitcher.configGui.enableHotbarInContainer.tooltip"),
//                unused -> settings.getEnableHotbarInContainers(),
//                (unused, newValue) -> settings.setEnableHotbarInContainers(newValue)
//        ));

        this.addWidget(this.optionsRowList);

        this.addRenderableWidget(new Button(
                (this.width - BUTTON_WIDTH * 2) / 3,
                this.height - DONE_BUTTON_TOP_OFFSET,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslatableComponent("hotswitcher.configGui.cancel"),
                button -> this.cancel()
        ));

        this.addRenderableWidget(new Button(
                (this.width - BUTTON_WIDTH * 2) / 3 * 2 + BUTTON_WIDTH,
                this.height - DONE_BUTTON_TOP_OFFSET,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslatableComponent("hotswitcher.configGui.done"),
                button -> this.done()
        ));

        //HotSwitcher.LOGGER.info("Init End Config Screen");
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.cancel();
            return true;
        } else
            return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cancel() {
        Objects.requireNonNull(this.minecraft).setScreen(parentScreen);
    }

    private void done() {
        this.settings.saveSettings();
        Objects.requireNonNull(this.minecraft).setScreen(parentScreen);
    }

    @Override
    public void onClose() {
        HotSwitcher.LOGGER.info("ConfigScreen onClose()");
        super.onClose();
    }
}
