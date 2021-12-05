package xt.hotswitcher;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.client.settings.IteratableOption;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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

    private OptionsRowList optionsRowList;

    private ConfigSettings settings;

    public ConfigScreen(Screen parentScreen) {
        super(new TranslationTextComponent("hotswitcher.configGui.title"));
        this.parentScreen = parentScreen;
    }



    @Override
    @ParametersAreNonnullByDefault
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        this.optionsRowList.render(matrixStack, mouseX, mouseY, partialTicks);
        this.font.draw(matrixStack, this.title.getString(),
                (float)this.width / 2 - ((float)this.font.width(this.title.getString()) / 2),
                TITLE_HEIGHT,
                0xFFFFFF);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        List<IReorderingProcessor> list = SettingsScreen.tooltipAt(this.optionsRowList, mouseX, mouseY);
        if (list != null) {
            this.renderTooltip(matrixStack, list, mouseX, mouseY);
        }
    }

    @Override
    protected void init() {
        settings = ConfigSettings.currentSettings();

        this.optionsRowList = new OptionsRowList(
                Objects.requireNonNull(this.minecraft), this.width, this.height,
                OPTIONS_LIST_TOP_HEIGHT,
                this.height - OPTIONS_LIST_BOTTOM_OFFSET,
                OPTIONS_LIST_ITEM_HEIGHT
        );
        HotSwitcher.LOGGER.info("Init Config Screen");

        this.optionsRowList.addBig(new IteratableOption(
                "hotswitcher.configGui.swapBarCount.title",
                (unused, newValue) -> settings.incrementSwapBarCount(newValue),
                (gameSettings, option) ->
                        new TranslationTextComponent("hotswitcher.configGui.swapBarCount.title")
                                .append(new StringTextComponent(": " + settings.getSwapBarCount()))
        ));

        this.optionsRowList.addBig(new IteratableOption(
                "hotswitcher.configGui.swapSlotCount.title",
                (unused, newValue) -> settings.incrementSwapSlotCount(newValue),
                (gameSettings, option) ->
                        new TranslationTextComponent("hotswitcher.configGui.swapSlotCount.title")
                                .append(new StringTextComponent(": " + settings.getSwapSlotCount()))
        ));

        this.optionsRowList.addBig(new BooleanOption(
                "hotswitcher.configGui.enableHotbarInContainer.title",
                new TranslationTextComponent("hotswitcher.configGui.enableHotbarInContainer.tooltip"),
                unused -> settings.getEnableHotbarInContainers(),
                (unused, newValue) -> settings.setEnableHotbarInContainers(newValue)
        ));

        this.children.add(this.optionsRowList);

        this.addButton(new Button(
                (this.width - BUTTON_WIDTH * 2) / 3,
                this.height - DONE_BUTTON_TOP_OFFSET,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslationTextComponent("hotswitcher.configGui.cancel"),
                button -> this.cancel()
        ));

        this.addButton(new Button(
                (this.width - BUTTON_WIDTH * 2) / 3 * 2 + BUTTON_WIDTH,
                this.height - DONE_BUTTON_TOP_OFFSET,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslationTextComponent("hotswitcher.configGui.done"),
                button -> this.done()
        ));

        super.init();
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
