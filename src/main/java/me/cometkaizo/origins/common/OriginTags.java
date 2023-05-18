package me.cometkaizo.origins.common;

import me.cometkaizo.origins.Main;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

public class OriginTags {

    public static class Blocks {

        private static Tags.IOptionalNamedTag<Block> createTag(String namespace) {
            return BlockTags.createOptional(new ResourceLocation(Main.MOD_ID, namespace));
        }
        private static Tags.IOptionalNamedTag<Block> createForgeTag(String namespace) {
            return BlockTags.createOptional(new ResourceLocation("forge", namespace));
        }
    }

    public static class Items {

        public static final Tags.IOptionalNamedTag<Item> ARMOR = createTag("armor");

        private static Tags.IOptionalNamedTag<Item> createTag(String namespace) {
            return ItemTags.createOptional(new ResourceLocation(Main.MOD_ID, namespace));
        }
        private static Tags.IOptionalNamedTag<Item> createForgeTag(String namespace) {
            return ItemTags.createOptional(new ResourceLocation("forge", namespace));
        }
    }

    private OriginTags() {
        throw new AssertionError("No OriginTags instances for you!");
    }
}
