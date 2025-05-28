package app.jyu;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.io.*;
import java.util.UUID;

public class AnswerPacket implements Serializable {
    public UUID pingUUID;
    public int answerIndex; // 0-3 for options 1-4
    public String playerName;
    
    public AnswerPacket(UUID pingUUID, int answerIndex, String playerName) {
        this.pingUUID = pingUUID;
        this.answerIndex = answerIndex;
        this.playerName = playerName;
    }
    
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(this);
        oos.flush();

        byte[] serializedData = bos.toByteArray();

        oos.close();
        bos.close();

        return serializedData;
    }

    public PacketByteBuf toPacketByteBuf() throws IOException {
        var buf = PacketByteBufs.create();
        buf.writeBytes(this.toByteArray());
        return buf;
    }

    public static AnswerPacket fromPacketByteBuf(PacketByteBuf buf) throws IOException, ClassNotFoundException {
        var serializedData = buf.getWrittenBytes();

        ByteArrayInputStream bis = new ByteArrayInputStream(serializedData);
        ObjectInputStream ois = new ObjectInputStream(bis);

        AnswerPacket packet = (AnswerPacket) ois.readObject();

        ois.close();
        bis.close();

        return packet;
    }
} 