package me.cometkaizo.origins.origin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.cometkaizo.origins.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class OriginBarOverlayGui extends AbstractGui {

    public static final ResourceLocation BAR_TEXTURES = new ResourceLocation(Main.MOD_ID, "textures/gui/resource_bar.png");
    public int barHeight = 5;
    public int barWidth = 71;
    public int barMargin = 2;
    public int barYOffset = 2;
    public int barUVInterval = 10;
    public int iconHeight = 8;
    public int iconWidth = 8;
    protected final Minecraft minecraft;
    protected int width;
    protected int height;
    protected int guiX;
    protected int guiY;
    protected int barIndex;
    protected double barPercent;
    protected boolean visible = true;
    protected Predicate<OriginBarOverlayGui> visibilityRule;
    protected boolean running;

    public OriginBarOverlayGui(Minecraft minecraft, int barIndex, Predicate<OriginBarOverlayGui> visibilityRule) {
        this.minecraft = minecraft;
        this.visibilityRule = visibilityRule;
        setBarIndex(barIndex);
    }

    protected void render(MatrixStack stack) {
        if (!running) return;
        visible = visibilityRule.test(this);
        if (!visible) return;
        width = minecraft.getMainWindow().getScaledWidth();
        height = minecraft.getMainWindow().getScaledHeight();
        guiX = getGuiX();
        guiY = getGuiY();

        minecraft.getTextureManager().bindTexture(BAR_TEXTURES);
        renderIcon(stack);
        if (barPercent < 1) renderBackgroundBar(stack);
        if (barPercent > 0) renderActiveBar(stack);
    }

    protected int getGuiX() {
        return width / 2 + 10;
    }

    protected int getGuiY() {
        ClientPlayerEntity player = minecraft.player;
        return player != null && player.getAir() < player.getMaxAir() ? height - 49 - 10 : height - 49;
    }

    protected void renderBackgroundBar(MatrixStack stack) {
        int x = guiX + iconWidth + barMargin;
        int y = guiY + barYOffset;
        blit(stack, x, y, 0, 0, barWidth, barHeight);
    }

    protected void renderActiveBar(MatrixStack stack) {
        int barVOffset = (barIndex + 1) * barUVInterval;
        int activeBarWidth = (int) (barWidth * barPercent);
        int x = guiX + iconWidth + barMargin;
        int y = guiY + barYOffset;
        blit(stack, x, y, 0, barVOffset, activeBarWidth, barHeight);
    }

    protected void renderIcon(MatrixStack stack) {
        int iconVOffset = (barIndex + 1) * barUVInterval - barYOffset;
        blit(stack, guiX, guiY, barWidth + barMargin, iconVOffset, iconWidth, iconHeight);
    }

    @SubscribeEvent
    public void renderOverlay(RenderGameOverlayEvent event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;
        render(event.getMatrixStack());
    }

    public void setBar(Supplier<Integer> bar) {
        setBarIndex(bar.get());
    }
    public void setBarIndex(int index) {
        this.barIndex = index;
    }
    public void setBarPercent(double percent) {
        this.barPercent = MathHelper.clamp(percent, 0, 1);
    }
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void start() {
        running = true;
        MinecraftForge.EVENT_BUS.register(this);
    }
    public void stop() {
        running = false;
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    public enum Bar implements Supplier<Integer> {
        WINGS(4),
        ENDER_PEARL(6),
        FLAME(7),
        POISON(9),
        TRIDENT_ACTIVE(10),
        TRIDENT_INACTIVE(11),
        TRIDENT_FULL(12);
        public final int index;
        Bar(int index) {
            this.index = index;
        }

        @Override
        public Integer get() {
            return index;
        }
    }

    public static class Builder {
        private final int barIndex;
        private Predicate<OriginBarOverlayGui> visibilityRule = p -> true;

        public Builder(int barIndex) {
            this.barIndex = barIndex;
        }
        public Builder(Supplier<Integer> indexSup) {
            this.barIndex = indexSup.get();
        }

        public Builder disappearWhenEmpty() {
            visibilityRule = visibilityRule.and(overlay -> overlay.barPercent > 0);
            return this;
        }

        public Builder disappearWhenFull() {
            visibilityRule = visibilityRule.and(overlay -> overlay.barPercent < 1);
            return this;
        }

        public Builder withVisibilityRule(Predicate<OriginBarOverlayGui> visibilityRule) {
            this.visibilityRule = this.visibilityRule.and(visibilityRule);
            return this;
        }

        public OriginBarOverlayGui build() {
            return new OriginBarOverlayGui(Minecraft.getInstance(), barIndex, visibilityRule);
        }
    }

}
