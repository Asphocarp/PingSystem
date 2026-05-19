package app.jyu.common;

import app.jyu.common.network.PingLocationC2SPacket;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.network.UpdateChannelC2SPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PingPacketTest {
	@Test
	void roundTripsClientPingPacket() {
		var entity = UUID.randomUUID();
		var original = new PingLocationC2SPacket("alpha", new Vec3(1.25, 64.0, -2.5), entity, 7, 3);
		var buf = new FriendlyByteBuf(Unpooled.buffer());

		original.write(buf);
		var decoded = PingLocationC2SPacket.readSafe(buf);

		assertNotNull(decoded);
		assertEquals(original.channel(), decoded.channel());
		assertEquals(original.pos(), decoded.pos());
		assertEquals(original.entity(), decoded.entity());
		assertEquals(original.sequence(), decoded.sequence());
		assertEquals(original.dimension(), decoded.dimension());
	}

	@Test
	void roundTripsServerPingPacket() {
		var author = UUID.randomUUID();
		var original = new PingLocationS2CPacket("", new Vec3(3, 4, 5), null, 11, -1, author);
		var buf = new FriendlyByteBuf(Unpooled.buffer());

		original.write(buf);
		var decoded = PingLocationS2CPacket.readSafe(buf);

		assertNotNull(decoded);
		assertEquals(original.channel(), decoded.channel());
		assertEquals(original.pos(), decoded.pos());
		assertNull(decoded.entity());
		assertEquals(original.sequence(), decoded.sequence());
		assertEquals(original.dimension(), decoded.dimension());
		assertEquals(author, decoded.author());
	}

	@Test
	void roundTripsChannelUpdatePacket() {
		var original = new UpdateChannelC2SPacket("party");
		var buf = new FriendlyByteBuf(Unpooled.buffer());

		original.write(buf);
		var decoded = UpdateChannelC2SPacket.readSafe(buf);

		assertNotNull(decoded);
		assertEquals("party", decoded.channel());
	}

	@Test
	void corruptBufferReturnsNull() {
		var buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeInt(42);

		var decoded = PingLocationC2SPacket.readSafe(buf);

		assertNotNull(decoded);
		assertTrue(decoded.isCorrupt());
	}
}
