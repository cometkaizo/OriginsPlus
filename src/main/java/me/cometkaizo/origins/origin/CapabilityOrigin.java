package me.cometkaizo.origins.origin;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
public class CapabilityOrigin {
    @CapabilityInject(Origin.class)
    public static Capability<Origin> ORIGIN_CAPABILITY = null;

    public static void register()
    {
        CapabilityManager.INSTANCE.register(Origin.class, new Capability.IStorage<Origin>()
        {
            @Override
            public INBT writeNBT(Capability<Origin> capability, Origin instance, Direction side)
            {
                return instance.serializeNBT();
            }

            @Override
            public void readNBT(Capability<Origin> capability, Origin instance, Direction side, INBT base)
            {
                instance.deserializeNBT(base);
            }
        }, () -> new Origin(null, null));
    }

}
