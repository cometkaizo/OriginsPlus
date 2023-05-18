package me.cometkaizo.origins.origin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

public class OriginCapProvider implements ICapabilityProvider, INBTSerializable<CompoundNBT> {

    private final PlayerEntity player;
    private Origin origin;
    private final LazyOptional<Origin> opt = LazyOptional.of(this::getOrCreateOrigin);

    public OriginCapProvider(PlayerEntity player) {
        this.player = player;
    }

    private Origin getOrCreateOrigin() {
        if (origin == null) origin = new Origin(OriginTypes.HUMAN.get(), player);
        return origin;
    }


    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, Direction side) {
        if (cap == CapabilityOrigin.ORIGIN_CAPABILITY) {
            return opt.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        return getOrCreateOrigin().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        getOrCreateOrigin().deserializeNBT(nbt);
    }
}
