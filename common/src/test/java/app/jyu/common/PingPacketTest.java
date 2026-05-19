package app.jyu.common;

import app.jyu.common.network.PingLocationC2SPacket;
import app.jyu.common.network.PingLocationS2CPacket;
import app.jyu.common.network.UpdateChannelC2SPacket;
import app.jyu.common.core.PingType;
import app.jyu.common.core.PingWheelMouseCapture;
import app.jyu.common.core.PingWheelController;
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
		var original = new PingLocationC2SPacket("alpha", new Vec3(1.25, 64.0, -2.5), entity, 7, 3, PingType.ATTACK);
		var buf = new FriendlyByteBuf(Unpooled.buffer());

		original.write(buf);
		var decoded = PingLocationC2SPacket.readSafe(buf);

		assertNotNull(decoded);
		assertEquals(original.channel(), decoded.channel());
		assertEquals(original.pos(), decoded.pos());
		assertEquals(original.entity(), decoded.entity());
		assertEquals(original.sequence(), decoded.sequence());
		assertEquals(original.dimension(), decoded.dimension());
		assertEquals(original.type(), decoded.type());
	}

	@Test
	void roundTripsServerPingPacket() {
		var author = UUID.randomUUID();
		var original = new PingLocationS2CPacket("", new Vec3(3, 4, 5), null, 11, -1, author, PingType.HELP);
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
		assertEquals(original.type(), decoded.type());
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

	@Test
	void unknownPingTypeIsCorrupt() {
		var buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeUtf("alpha");
		buf.writeDouble(1);
		buf.writeDouble(2);
		buf.writeDouble(3);
		buf.writeBoolean(false);
		buf.writeInt(1);
		buf.writeInt(0);
		buf.writeVarInt(999);

		var decoded = PingLocationC2SPacket.readSafe(buf);

		assertNotNull(decoded);
		assertTrue(decoded.isCorrupt());
	}

	@Test
	void radialSelectionUsesEightSectors() {
		assertEquals(PingType.LOCATION, PingWheelController.selectType(220, 100, 200, 200, 12));
		assertEquals(PingType.DANGER, PingWheelController.selectType(100, 220, 200, 200, 12));
		assertEquals(PingType.GATHER, PingWheelController.selectType(-20, 100, 200, 200, 12));
		assertEquals(PingType.CONFIRM, PingWheelController.selectType(185, 15, 200, 200, 12));
	}

	@Test
	void radialDeadZoneClearsSelection() {
		assertNull(PingWheelController.selectType(104, 103, 200, 200, 12));
	}

	@Test
	void holdThresholdControlsWheelOpenTiming() {
		assertFalse(PingWheelController.isHoldElapsed(179, 180));
		assertTrue(PingWheelController.isHoldElapsed(180, 180));
	}

	@Test
	void wheelOpensOnHoldOrOutsideDeadZone() {
		assertFalse(PingWheelController.shouldOpenWheel(179, 180, false));
		assertTrue(PingWheelController.shouldOpenWheel(180, 180, false));
		assertTrue(PingWheelController.shouldOpenWheel(1, 180, true));
	}

	@Test
	void wheelMouseRegrabRequiresWheelReleaseAndSafeClientState() {
		assertFalse(PingWheelMouseCapture.shouldRegrabMouse(false, true, true, true));
		assertFalse(PingWheelMouseCapture.shouldRegrabMouse(true, false, true, true));
		assertFalse(PingWheelMouseCapture.shouldRegrabMouse(true, true, false, true));
		assertFalse(PingWheelMouseCapture.shouldRegrabMouse(true, true, true, false));
		assertTrue(PingWheelMouseCapture.shouldRegrabMouse(true, true, true, true));
	}

	@Test
	void pingTypesUsePingSystemUnicodeIcons() {
		assertEquals("◆", PingType.LOCATION.getIcon());
		assertEquals("⚔", PingType.ATTACK.getIcon());
		assertEquals("⚠", PingType.DANGER.getIcon());
		assertEquals("♥", PingType.HELP.getIcon());
		assertEquals("⚑", PingType.GATHER.getIcon());
		assertEquals("■", PingType.DEFEND.getIcon());
		assertEquals("★", PingType.LOOT.getIcon());
		assertEquals("✓", PingType.CONFIRM.getIcon());
	}
}
