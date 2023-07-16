package me.cometkaizo.origins.common;

import me.cometkaizo.origins.Main;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolType;

import java.util.HashMap;
import java.util.Map;

public class OriginTags {

    public static class Blocks {

        public static final Tags.IOptionalNamedTag<Block> ENDERIAN_NORMAL_DROP_PICKAXE = createTag("enderian_normal_drop_pickaxe");
        public static final Tags.IOptionalNamedTag<Block> ENDERIAN_NORMAL_DROP_AXE = createTag("enderian_normal_drop_axe");
        public static final Tags.IOptionalNamedTag<Block> ENDERIAN_NORMAL_DROP_SHOVEL = createTag("enderian_normal_drop_shovel");
        public static final Tags.IOptionalNamedTag<Block> ENDERIAN_NORMAL_DROP_HOE = createTag("enderian_normal_drop_hoe");
        public static final Map<ToolType, Tags.IOptionalNamedTag<Block>> ENDERIAN_NORMAL_DROP_TAGS = new HashMap<>(4);
        public static final Tags.IOptionalNamedTag<Block> FOX_EXTRA_SPEED = createTag("fox_extra_speed");

        static {
            ENDERIAN_NORMAL_DROP_TAGS.put(ToolType.PICKAXE, ENDERIAN_NORMAL_DROP_PICKAXE);
            ENDERIAN_NORMAL_DROP_TAGS.put(ToolType.AXE, ENDERIAN_NORMAL_DROP_AXE);
            ENDERIAN_NORMAL_DROP_TAGS.put(ToolType.SHOVEL, ENDERIAN_NORMAL_DROP_SHOVEL);
            ENDERIAN_NORMAL_DROP_TAGS.put(ToolType.HOE, ENDERIAN_NORMAL_DROP_HOE);
        }

        private static Tags.IOptionalNamedTag<Block> createTag(String namespace) {
            return BlockTags.createOptional(new ResourceLocation(Main.MOD_ID, namespace));
        }
    }

    public static class Items {

        public static final Tags.IOptionalNamedTag<Item> SEAFOOD = createTag("seafood");

        private static Tags.IOptionalNamedTag<Item> createTag(String namespace) {
            return ItemTags.createOptional(new ResourceLocation(Main.MOD_ID, namespace));
        }
    }

    private OriginTags() {
        throw new AssertionError("No OriginTags instances for you!");
    }
}
