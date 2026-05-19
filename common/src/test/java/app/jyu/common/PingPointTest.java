package app.jyu.common;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PingPointTest {
    @Test
    void roundTripsLocationPacket() {
        PingPoint original = PingPoint.location(new Vec3(1.25, 64.0, -2.5), "Player", 0xFF112233, (byte) 2);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        original.write(buf);
        PingPoint decoded = PingPoint.read(buf);

        assertNotNull(decoded);
        assertEquals(original.id(), decoded.id());
        assertEquals(original.pos(), decoded.pos());
        assertEquals(original.owner(), decoded.owner());
        assertEquals(original.color(), decoded.color());
        assertEquals(original.sound(), decoded.sound());
        assertEquals(PingPoint.PingType.LOCATION, decoded.type());
        assertNull(decoded.entityUuid());
    }

    @Test
    void roundTripsEntityPacket() {
        UUID entity = UUID.randomUUID();
        PingPoint original = PingPoint.entity(new Vec3(3, 4, 5), "Player", 0xFFABCDEF, (byte) 1, entity);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        original.write(buf);
        PingPoint decoded = PingPoint.read(buf);

        assertNotNull(decoded);
        assertEquals(PingPoint.PingType.ENTITY, decoded.type());
        assertEquals(entity, decoded.entityUuid());
    }

    @Test
    void detectsExpiredPings() {
        PingPoint oldPing = new PingPoint(UUID.randomUUID(), Vec3.ZERO, "Player", 0, (byte) 0, 0L, PingPoint.PingType.LOCATION, null);

        assertTrue(oldPing.shouldVanish(1));
        assertFalse(oldPing.shouldVanish(0));
    }

    @Test
    void corruptBufferReturnsNull() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(42);

        assertNull(PingPoint.read(buf));
        assertEquals(0, buf.readableBytes());
    }
}
