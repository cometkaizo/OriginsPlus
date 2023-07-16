package me.cometkaizo.origins.origin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.C2SChooseOrigin;
import me.cometkaizo.origins.network.Packets;
import me.cometkaizo.origins.origin.Origin;
import me.cometkaizo.origins.origin.OriginType;
import me.cometkaizo.origins.origin.OriginTypes;
import me.cometkaizo.origins.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ChooseOriginScreen extends Screen {

    public static final ResourceLocation PANEL_TEXTURES = new ResourceLocation(Main.MOD_ID, "textures/gui/choose_origin.png");
    public static final int CHOOSE_COOLDOWN = 20;
    private int halfWidth, halfHeight;
    public int confirmButtonWidth = 100;
    public int confirmButtonHeight = 20;
    public int arrowButtonWidth = 20;
    public int arrowButtonHeight = 20;
    public int panelWidth = 176;
    public int panelHeight = 182;
    public final int panelPadding = 10;
    public int panelMargin = 5;
    public int panelTop, panelLeft, panelRight, panelBottom;
    public int panelTitleY, panelTitleX;
    public int panelIconY, panelIconX;
    public int panelDescTop, panelDescLeft;
    public int panelDescTitleLineHeight = 18;
    public int panelDescLineHeight = 13;
    public int panelDescMargin = 15, panelDescMarginRight = 25;
    public int panelDescWidth;
    public int panelScrollbarHeight, panelScrollbarWidth;
    public int panelScrollbarLeft, panelScrollbarRight;
    public final int maxScrollbarOffset = 141;
    public TextFormatting[] panelDescFormatting = {TextFormatting.GRAY};
    public TextFormatting[] panelDescTitleFormatting = {TextFormatting.WHITE, TextFormatting.BOLD};
    public TextFormatting[] panelDescSummaryFormatting = {TextFormatting.WHITE, TextFormatting.ITALIC};
    private final List<OriginType> types;
    private List<DescriptionList> typeDescriptions;
    private ItemStack currentTypeIcon;
    private int selectionIndex;
    private final AtomicBoolean chosen = new AtomicBoolean(false);
    private final AtomicInteger chooseCooldown = new AtomicInteger(0);
    private Button confirmButton;
    public boolean scrolling;
    private int scrollDragStart;
    private double mouseDragStart;

    protected ChooseOriginScreen() {
        super(new TranslationTextComponent(Main.MOD_ID + ".screen.choose_origin"));
        this.types = new ArrayList<>(OriginTypes.ORIGINS_REGISTRY.get().getValues());
        if (this.types.remove(OriginTypes.HUMAN.get())) {
            this.types.add(0, OriginTypes.HUMAN.get());
        }
        if (types.isEmpty()) throw new IllegalStateException("No origin options");
    }

    @Override
    public void tick() {
        super.tick();
        trySyncToServer();
    }

    protected void trySyncToServer() {
        if (chosen.get() && chooseCooldown.decrementAndGet() <= 0) {
            Packets.sendToServer(new C2SChooseOrigin(getCurrentType()));
            chooseCooldown.set(CHOOSE_COOLDOWN);
        }
    }

    @Override
    protected void init() {
        super.init();
        setSelectionIndex(0);
        halfWidth = width / 2;
        halfHeight = height / 2;
        panelTop = (height - panelHeight) / 2;
        panelLeft = (width - panelWidth) / 2;
        panelRight = panelLeft + panelWidth;
        panelBottom = panelTop + panelHeight;
        panelTitleY = panelTop + 19;
        panelTitleX = panelLeft + 40;
        panelIconY = panelTop + 15;
        panelIconX = panelLeft + 15;
        panelDescTop = panelTop + 42;
        panelDescLeft = panelLeft + panelDescMargin;
        panelDescWidth = panelWidth - panelDescMargin - panelDescMarginRight;
        panelScrollbarWidth = 6;
        panelScrollbarHeight = 27;
        panelScrollbarLeft = panelLeft + 156;
        panelScrollbarRight = panelScrollbarLeft + panelScrollbarWidth;
        this.typeDescriptions = types.stream().map(t -> new DescriptionList(Minecraft.getInstance(), t)).collect(Collectors.toList());

        confirmButton = new Button(halfWidth - confirmButtonWidth / 2, panelBottom + panelMargin,
                confirmButtonWidth, confirmButtonHeight, new StringTextComponent("Confirm"), this::onConfirm);

        addButton(confirmButton);

        if (types.size() > 1) {
            addButton(new Button(panelRight + panelMargin, halfHeight - arrowButtonHeight / 2,
                    arrowButtonWidth, arrowButtonHeight, new StringTextComponent(">"), b -> increaseSelectionIndex()));
            addButton(new Button(panelLeft - panelMargin - arrowButtonWidth, halfHeight - arrowButtonHeight / 2,
                    arrowButtonWidth, arrowButtonHeight, new StringTextComponent("<"), b -> decreaseSelectionIndex()));
        }
    }

    private void onConfirm(Button button) {
        if (!isOriginChosen()) {
            chooseCurrentOrigin();
            confirmButton.active = false;
        }
    }

    @Override
    public void render(@Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        renderDirtBackground(0);
        renderPanel(stack, mouseX, mouseY, partialTicks);
        super.render(stack, mouseX, mouseY, partialTicks);
        drawCenteredString(stack, font, title, halfWidth, 15, 16777215);
        renderScrollbar(stack, mouseX, mouseY);
    }

    private void renderPanel(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        if (minecraft == null) return;
        renderPanelFrame(stack, mouseX, mouseY, partialTicks);
        renderPanelDescription(stack, mouseX, mouseY, partialTicks);
        minecraft.getTextureManager().bindTexture(PANEL_TEXTURES);
        this.blit(stack, panelLeft, panelTop, 0, 0, panelWidth, panelHeight);
        renderPanelTitle(stack);
    }

    private void renderPanelTitle(MatrixStack stack) {
        drawString(stack, font, getCurrentName(), panelTitleX, panelTitleY, 16777215);
        itemRenderer.renderItemAndEffectIntoGUI(getCurrentIcon(), panelIconX, panelIconY);
    }

    private void renderPanelFrame(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        if (minecraft == null) return;
        int offsetYStart = 16;
        int offsetYEnd = 0;
        int border = 13;
        int endX = panelLeft + panelWidth - border;
        int endY = panelTop + panelHeight - border;
        minecraft.getTextureManager().bindTexture(PANEL_TEXTURES);
        for (int x = panelLeft; x < endX; x += 16) {
            for (int y = panelTop + offsetYStart; y < endY + offsetYEnd; y += 16) {
                this.blit(stack, x, y, panelWidth, 0, Math.max(16, endX - x), Math.max(16, endY + offsetYEnd - y));
            }
        }
    }

    private void renderPanelDescription(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        getCurrentDescription().render(stack, mouseX, mouseY, partialTicks);
    }

    private void renderScrollbar(MatrixStack stack, int mouseX, int mouseY) {
        if (!getCurrentDescription().canScroll() || minecraft == null) {
            return;
        }
        minecraft.getTextureManager().bindTexture(PANEL_TEXTURES);
        this.blit(stack, panelLeft + 155, panelTop + 35, 188, 24, 8, 134);
        int scrollbarY = 36;
        int u = 176;
        float part = (float) (getCurrentDescription().getScrollAmount() / getCurrentDescription().getMaxScroll());
        scrollbarY += (maxScrollbarOffset - scrollbarY) * part;

        if (this.scrolling) {
            u += 6;
        } else if (mouseX >= panelScrollbarLeft && mouseX < panelScrollbarRight) {
            if (mouseY >= panelTop + scrollbarY && mouseY < panelTop + scrollbarY + panelScrollbarHeight) {
                u += 6;
            }
        }
        this.blit(stack, panelScrollbarLeft, panelTop + scrollbarY, u, 24, panelScrollbarWidth, panelScrollbarHeight);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getCurrentDescription().canScroll()) {
            scrolling = false;
            int scrollbarY = 36;
            float part = (float) (getCurrentDescription().getScrollAmount() / getCurrentDescription().getMaxScroll());
            scrollbarY += (maxScrollbarOffset - scrollbarY) * part;
            if (mouseX >= panelScrollbarLeft && mouseX < panelScrollbarRight) {
                if (mouseY >= panelTop + scrollbarY && mouseY < panelTop + scrollbarY + panelScrollbarHeight) {
                    scrolling = true;
                    scrollDragStart = scrollbarY;
                    mouseDragStart = mouseY;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrolling) {
            int delta = (int) (mouseY - mouseDragStart);
            int newScrollPos = Math.max(36, Math.min(141, scrollDragStart + delta));
            float part = (newScrollPos - 36) / (float) (141 - 36);
            getCurrentDescription().setScrollAmount(part * getCurrentDescription().getMaxScroll());
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void increaseSelectionIndex() {
        setSelectionIndex((selectionIndex + 1) % (types.size()));
    }

    private void decreaseSelectionIndex() {
        int max = types.size();
        setSelectionIndex((selectionIndex - 1 + max) % max);
    }

    private void setSelectionIndex(int index) {
        this.selectionIndex = index;
        currentTypeIcon = getCurrentType().getIcon();
    }

    private DescriptionList getCurrentDescription() {
        return typeDescriptions.get(selectionIndex);
    }

    private String getCurrentName() {
        return getCurrentType().getName();
    }

    private OriginType getCurrentType() {
        return types.get(selectionIndex);
    }

    private ItemStack getCurrentIcon() {
        return currentTypeIcon;
    }

    public boolean isOriginChosen() {
        return chosen.get();
    }

    public void chooseCurrentOrigin() {
        chosen.compareAndSet(false, true);
    }

    public void setServerSynced() {
        chosen.compareAndSet(true, false);
        if (minecraft == null) return;
        minecraft.displayGuiScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return getCurrentDescription().mouseScrolled(mouseX, mouseY, delta);
    }

    public class DescriptionList extends ExtendedList<DescriptionList.Entry> {
        private final List<IFormattableTextComponent> summaryLines;

        public DescriptionList(Minecraft mc, OriginType type) {
            super(mc, panelDescWidth, panelHeight - panelPadding * 2, panelDescTop, panelBottom - panelPadding, panelDescLineHeight);
            setLeftPos(panelLeft + panelPadding);
            setRenderTextOverlay(false);
            Origin.Description description = type.getDescription();
            description.entries.stream().map(Entry::new).forEach(this::addEntry);

            summaryLines = createSummaryLines(description);
        }

        private List<IFormattableTextComponent> createSummaryLines(Origin.Description description) {
            return StringUtils.createLines(description.summary.getString(), font, width).stream()
                    .map(StringTextComponent::new)
                    .map(t -> t.mergeStyle(panelDescSummaryFormatting))
                    .collect(Collectors.toList());
        }

        @Override
        protected void renderList(@Nonnull MatrixStack stack, int x, int y, int mouseX, int mouseY, float partialTicks) {
            if (getSummaryBottom() >= getTop() && getSummaryTop() <= getBottom())
                renderSummary(stack, getSummaryTop());

            int itemCount = this.getItemCount();

            for (int index = 0; index < itemCount; ++index) {
                Entry entry = getEntry(index);
                int entryTop = getRowTop(index);
                int entryBottom = entryTop + entry.getHeight();
                if (entryBottom >= getTop() && entryTop <= getBottom()) {
                    int height = entry.getHeight();
                    int width = getRowWidth();
                    int entryLeft = getRowLeft();

                    entry.render(stack, index, entryTop, entryLeft, width, height, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && Objects.equals(this.getEntryAtPosition(mouseX, mouseY), entry), partialTicks);
                }
            }
        }

        private void renderSummary(MatrixStack stack, int top) {
            for (int lineIndex = 0; lineIndex < summaryLines.size(); lineIndex ++) {
                int lineTop = top + lineIndex * panelDescLineHeight;
                if (!isInPanel(lineTop)) continue;
                IFormattableTextComponent line = summaryLines.get(lineIndex);
                AbstractGui.drawString(stack, font, line, panelDescLeft, lineTop, 16777215);
            }
        }

        public int getSummaryTop() {
            return getTop() - (int)this.getScrollAmount() + this.headerHeight;
        }

        public int getSummaryBottom() {
            return getSummaryTop() + getSummaryHeight();
        }

        public int getSummaryHeight() {
            return summaryLines.isEmpty() ? 0 : (summaryLines.size() - 1) * panelDescLineHeight + panelDescTitleLineHeight;
        }

        @Override
        protected int getScrollbarPosition() {
            return 999999;
        }

        public boolean canScroll() {
            return getMaxScroll() > 0;
        }

        @Override
        protected int getMaxPosition() {
            int totalLineHeight = 0;
            List<Entry> entries = getEntries();
            for (Entry entry : entries) totalLineHeight += entry.getHeight();
            return totalLineHeight + getSummaryHeight() + headerHeight;
        }

        @Override
        protected int getRowTop(int index) {
            int totalLineHeight = 0;
            List<Entry> entries = getEntries();
            for (int entryIndex = 0; entryIndex < index; entryIndex++) {
                Entry entry = entries.get(entryIndex);
                totalLineHeight += entry.getHeight();
            }
            return getTop() - (int)this.getScrollAmount() + totalLineHeight + this.headerHeight + getSummaryHeight();
        }

        private List<Entry> getEntries() {
            return getEventListeners(); // this actually returns the entries, not event listeners (not sure why it is named that)
        }

        private void setRenderTextOverlay(boolean render) {
            func_244605_b(render);
            func_244606_c(render);
        }

        @Override
        public int getRowWidth() {
            return panelDescWidth;
        }

        private boolean isInPanel(int lineTop) {
            return lineTop > panelTop && lineTop + 8 < panelBottom;
        }

        public class Entry extends ExtendedList.AbstractListEntry<Entry> {
            private final IFormattableTextComponent title;
            private final List<IFormattableTextComponent> descriptionLines;

            public Entry(Origin.Description.Entry entry) {
                this.title = entry.title.mergeStyle(panelDescTitleFormatting);
                descriptionLines = createDescriptionLines(entry);
            }

            private List<IFormattableTextComponent> createDescriptionLines(Origin.Description.Entry entry) {
                return StringUtils.createLines(entry.description.getString(), font, width).stream()
                        .map(StringTextComponent::new)
                        .map(t -> t.mergeStyle(panelDescFormatting))
                        .collect(Collectors.toList());
            }

            @Override
            public void render(@Nonnull MatrixStack stack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                if (top > panelTop && top + 12 < panelBottom) AbstractGui.drawString(stack, font, title, panelDescLeft, top + panelDescTitleLineHeight/4, 16777215);

                for (int lineIndex = 0; lineIndex < descriptionLines.size(); lineIndex ++) {
                    int lineTop = top + panelDescTitleLineHeight + lineIndex * panelDescLineHeight;
                    if (!isInPanel(lineTop)) continue;
                    IFormattableTextComponent line = descriptionLines.get(lineIndex);
                    AbstractGui.drawString(stack, font, line, panelDescLeft, lineTop, 16777215);
                }
            }

            public int getHeight() {
                return (descriptionLines.size() - 1) * panelDescLineHeight + panelDescTitleLineHeight * 2;
            }
        }
    }
}
