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
        this.renderBackground(matrixStack);
        this.optionsRowList.render(matrixStack, mouseX, mouseY, partialTicks);
        this.font.draw(matrixStack, this.title.getString(),
                (float)this.width / 2 - ((float)this.font.width(this.title.getString()) / 2),
                TITLE_HEIGHT,
                0xFFFFFF);
        super.render(matrixStack, mouseX, mouseY, partialTicks);



        List<FormattedCharSequence> list = OptionsSubScreen.tooltipAt(this.optionsRowList, mouseX, mouseY);
        if (list != null) {
            this.renderTooltip(matrixStack, list, mouseX, mouseY);
        }
    }

    @Override
    protected void init() {
        //super.init();

        HotSwitcher.LOGGER.info("Init Start Config Screen");
        settings = ConfigSettings.currentSettings();

        // Create an options list group to put our options into
        this.optionsRowList = new OptionsList(
                Objects.requireNonNull(this.minecraft), this.width, this.height,
                OPTIONS_LIST_TOP_HEIGHT,
                this.height - OPTIONS_LIST_BOTTOM_OFFSET,
                OPTIONS_LIST_ITEM_HEIGHT
        );

        // Create swap bar count option
        this.optionsRowList.addBig(
                CycleOption.create(
                        // Title
                        "hotswitcher.configGui.swapBarCount.title",

                        // Values
                        new Integer[] {1, 2, 3},

                        // Displayed value
                        (value) -> new TranslatableComponent(value.toString()),

                        // Getter to get value
                        (o) -> settings.getSwapBarCount(),

                        // Setter to set value
                        (o1, o2, sbc) -> settings.setSwapBarCount(sbc)

                ).setTooltip(m -> (value) -> m.font.split(new TranslatableComponent("hotswitcher.configGui.swapBarCount.tooltip"), 200))
        );

        // Create swap slot count option
        this.optionsRowList.addBig(
                CycleOption.create(
                        // Title
                        "hotswitcher.configGui.swapSlotCount.title",

                        // Values
                        new Integer[] {1, 2, 3},

                        // Displayed value
                        (v) -> new TranslatableComponent(v.toString()),

                        // Getter to get value
                        (o) -> settings.getSwapSlotCount(),

                        // Setter to set value
                        (o1, o2, ssc) -> settings.setSwapSlotCount(ssc)

                ).setTooltip(m -> (value) -> m.font.split(new TranslatableComponent("hotswitcher.configGui.swapSlotCount.tooltip"), 200))
        );

        // Create enable hotswitcher in containers option
        this.optionsRowList.addBig(
             CycleOption.createOnOff(
                     // Title
                     "hotswitcher.configGui.enableHotbarInContainer.title",

                     // Getter to get value
                     (o) -> settings.getEnableHotbarInContainers(),

                     // Setter to set value
                     (o1, o2, b) -> settings.setEnableHotbarInContainers(b)
             ).setTooltip(m -> (value) -> m.font.split(new TranslatableComponent("hotswitcher.configGui.enableHotbarInContainer.tooltip"), 200))
        );


        // Add the options list group to the screen
        this.addWidget(this.optionsRowList);

        // Add a cancel button
        this.addRenderableWidget(new Button(
                (this.width - BUTTON_WIDTH * 2) / 3,
                this.height - DONE_BUTTON_TOP_OFFSET,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslatableComponent("hotswitcher.configGui.cancel"),
                button -> this.cancel()
        ));

        // Add a done button
        this.addRenderableWidget(new Button(
                (this.width - BUTTON_WIDTH * 2) / 3 * 2 + BUTTON_WIDTH,
                this.height - DONE_BUTTON_TOP_OFFSET,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslatableComponent("hotswitcher.configGui.done"),
                button -> this.done()
        ));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // If escape is pressed, cancel
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.cancel();
            return true;
        } else
            return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cancel() {
        // Go back to previous screen (or close screen if there was no previous)
        Objects.requireNonNull(this.minecraft).setScreen(parentScreen);
    }

    private void done() {
        // Save the current settings
        this.settings.saveSettings();

        // Go back to previous screen (or close screen if there was no previous)
        Objects.requireNonNull(this.minecraft).setScreen(parentScreen);
    }
}
