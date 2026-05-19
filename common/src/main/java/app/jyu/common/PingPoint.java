package app.jyu.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record PingPoint(
        UUID id,
        Vec3 pos,
        String owner,
        int color,
        byte sound,
        long createdAtMillis,
        PingType type,
        UUID entityUuid
) {
    public enum PingType {
        LOCATION,
        ENTITY
    }

    public static PingPoint location(Vec3 pos, String owner, int color, byte sound) {
        return new PingPoint(UUID.randomUUID(), pos, owner, color, sound, System.currentTimeMillis(), PingType.LOCATION, null);
    }

    public static PingPoint entity(Vec3 pos, String owner, int color, byte sound, UUID entityUuid) {
        return new PingPoint(UUID.randomUUID(), pos, owner, color, sound, System.currentTimeMillis(), PingType.ENTITY, entityUuid);
    }

    public boolean shouldVanish(long secondsToVanish) {
        if (secondsToVanish == 0) {
            return false;
        }
        return System.currentTimeMillis() - createdAtMillis > secondsToVanish * 1000L;
    }

    public boolean isCorrupt() {
        if (id == null || pos == null || owner == null || type == null) {
            return true;
        }
        return type == PingType.ENTITY && entityUuid == null;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeUtf(owner, 64);
        buf.writeInt(color);
        buf.writeByte(sound);
        buf.writeLong(createdAtMillis);
        buf.writeEnum(type);
        buf.writeBoolean(entityUuid != null);
        if (entityUuid != null) {
            buf.writeUUID(entityUuid);
        }
    }

    public static PingPoint read(FriendlyByteBuf buf) {
        try {
            UUID id = buf.readUUID();
            Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            String owner = buf.readUtf(64);
            int color = buf.readInt();
            byte sound = buf.readByte();
            long createdAtMillis = buf.readLong();
            PingType type = buf.readEnum(PingType.class);
            UUID entityUuid = buf.readBoolean() ? buf.readUUID() : null;
            PingPoint point = new PingPoint(id, pos, owner, color, sound, createdAtMillis, type, entityUuid);
            return point.isCorrupt() ? null : point;
        } catch (RuntimeException e) {
            Constants.LOGGER.warn("Failed to read ping packet", e);
            return null;
        } finally {
            if (buf.readableBytes() > 0) {
                buf.readerIndex(buf.readerIndex() + buf.readableBytes());
            }
        }
    }
}
