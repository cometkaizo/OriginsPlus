package me.cometkaizo.origins.network;

import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.client.ClientUtils;
import me.cometkaizo.origins.origin.Origin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class S2CEnumAction {
    public static final Logger LOGGER = LogManager.getLogger();
    protected final int targetId;
    protected final String typeName;
    protected final int actionOrdinal;

    public <T extends Enum<T>> S2CEnumAction(Entity target, T action) {
        this.targetId = target.getEntityId();
        this.typeName = action.getClass().getName();
        this.actionOrdinal = action.ordinal();
    }

    public S2CEnumAction(PacketBuffer buffer) {
        this.targetId = buffer.readInt();
        this.typeName = buffer.readString();
        this.actionOrdinal = buffer.readVarInt();
    }

    public void toBytes(PacketBuffer buffer) {
        buffer.writeInt(targetId);
        buffer.writeString(typeName);
        buffer.writeVarInt(actionOrdinal);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> {
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getClientLevel);
            if (world == null) return;

            Entity entity = world.getEntityByID(targetId);
            if (entity == null) Main.LOGGER.error("Invalid packet: no entity with id {}; {}", targetId, this);
            Origin origin = Origin.getOrigin(entity);

            if (origin != null) {
                origin.onEvent(getAction());
            } else {
                Main.LOGGER.error("Invalid packet: {} does not have origin capability; {}",
                        entity instanceof PlayerEntity ? ((PlayerEntity) entity).getGameProfile().getName() : entity,
                        this);
            }
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
        return "S2CEnumAction{" +
                "targetId=" + targetId +
                ", typeName='" + typeName + '\'' +
                ", actionOrdinal=" + actionOrdinal +
                '}';
    }
}
