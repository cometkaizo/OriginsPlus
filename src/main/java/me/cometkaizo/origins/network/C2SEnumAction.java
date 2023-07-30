package me.cometkaizo.origins.network;

import me.cometkaizo.origins.origin.CapabilityOrigin;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class C2SEnumAction {
    public static final Logger LOGGER = LogManager.getLogger();
    protected final String typeName;
    protected final int actionOrdinal;

    public <T extends Enum<T>> C2SEnumAction(T action) {
        this.typeName = action.getClass().getName();
        this.actionOrdinal = action.ordinal();
    }

    public C2SEnumAction(PacketBuffer buffer) {
        this.typeName = buffer.readString();
        this.actionOrdinal = buffer.readVarInt();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeString(typeName);
        buffer.writeVarInt(actionOrdinal);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender == null) {
                LOGGER.warn("No sender found in Direction: {}; {}", ctx.getDirection(), this);
                return;
            }

            sender.getCapability(CapabilityOrigin.ORIGIN_CAPABILITY).ifPresent(o -> o.onEvent(getAction()));
        });
        return true;
    }

    private Object getAction() {
        Class<?> type = getType();
        return type == null ? null : type.getEnumConstants()[actionOrdinal];
    }

    public Class<?> getType() {
        try {
            Class<?> type = Class.forName(typeName);
            if (!Enum.class.isAssignableFrom(type)) LOGGER.error("Invalid packet: type '{}' is not an enum; {}", typeName, this);
            else return type;
        } catch (ClassNotFoundException e) {
            LOGGER.error("Invalid packet: no enum type '{}'; {}", typeName, this);
        }
        return null;
    }

    @Override
    public String toString() {
        return "C2SEnumAction{" +
                "typeName='" + typeName + '\'' +
                ", actionOrdinal=" + actionOrdinal +
                '}';
    }
}
