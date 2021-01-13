package xt.hotswitcher;

import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigSettings {
    private static final int MIN_SWAP_COUNT = 0;
    private static final int MAX_SWAP_COUNT = 3;
    private static ForgeConfigSpec.IntValue configSwapBarCount;
    private static ForgeConfigSpec.IntValue configSwapSlotCount;
    private static ForgeConfigSpec.BooleanValue configEnableHotbarInContainers;

    private int swapBarCount;
    private int swapSlotCount;
    private boolean enableHotbarInContainers;


    public static ConfigSettings currentSettings() {
        ConfigSettings settings = new ConfigSettings();
        settings.getSettings();

        return settings;
    }

    public static void init(ForgeConfigSpec.Builder config) {
        configSwapBarCount = config.comment("Swap Bar Count")
                .defineInRange("swap_bar_count", 1, MIN_SWAP_COUNT, MAX_SWAP_COUNT);
        configSwapSlotCount = config.comment("Swap Slot Count")
                .defineInRange("swap_slot_count", 1, MIN_SWAP_COUNT, MAX_SWAP_COUNT);
        configEnableHotbarInContainers = config.comment("Enable Hotbar in Containers")
                .define("hotbar_in_containers", true);
    }

    public static int getConfigSwapBarCount() {
        return configSwapBarCount.get();
    }
    public static int getConfigSwapSlotCount() {
        return configSwapSlotCount.get();
    }
    public static boolean getConfigEnableHotbarInContainers() {
        return configEnableHotbarInContainers.get();
    }


    public int getSwapBarCount() {
        return this.swapBarCount;
    }
    public int getSwapSlotCount() {
        return this.swapSlotCount;
    }
    public boolean getEnableHotbarInContainers() { return this.enableHotbarInContainers; }

    public void setSwapBarCount(int count) {
        this.swapBarCount = (count - MIN_SWAP_COUNT) % (MAX_SWAP_COUNT - MIN_SWAP_COUNT) + MIN_SWAP_COUNT;
    }

    public void setSwapSlotCount(int count) {
        this.swapSlotCount = (count - MIN_SWAP_COUNT) % (MAX_SWAP_COUNT - MIN_SWAP_COUNT) + MIN_SWAP_COUNT;
    }

    public void setEnableHotbarInContainers(boolean enable) {
        this.enableHotbarInContainers = enable;
    }

    public void incrementSwapBarCount(int increment) {
        this.swapBarCount += increment;

        if (this.swapBarCount > MAX_SWAP_COUNT) {
            this.swapBarCount = (this.swapBarCount - MIN_SWAP_COUNT) % (MAX_SWAP_COUNT - MIN_SWAP_COUNT) + MIN_SWAP_COUNT;
        }
    }

    public void incrementSwapSlotCount(int increment) {
        this.swapSlotCount += increment;

        if (this.swapSlotCount > MAX_SWAP_COUNT) {
            this.swapSlotCount = (this.swapSlotCount - MIN_SWAP_COUNT) % (MAX_SWAP_COUNT - MIN_SWAP_COUNT) + MIN_SWAP_COUNT;
        }
    }

    public void saveSettings() {
        configSwapBarCount.set(this.swapBarCount);
        configSwapSlotCount.set(this.swapSlotCount);
        configEnableHotbarInContainers.set(this.enableHotbarInContainers);
    }

    public void getSettings() {
        this.swapBarCount = configSwapBarCount.get();
        this.swapSlotCount = configSwapSlotCount.get();
        this.enableHotbarInContainers = configEnableHotbarInContainers.get();
    }
}
