package app.jyu;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class PingPoint implements Serializable {
    public enum PingType {
        LOCATION, ENTITY
    }

    public UUID id;
    public Vec3d pos;
    public String owner;
    // the highlight color
    public Color color;
    // the sound index
    public byte sound;
    public LocalDateTime createTime;
    // New fields
    public PingType type;
    public UUID entityUUID; // Nullable: only set for ENTITY type
    // set for quiz question and options
    // {
    //     "question": "n. 环境",
    //     "options": [
    //       "environment",
    //       "instrument",
    //       "argument",
    //       "entertainment"
    //     ],
    //     "answer": 1
    // },
    public Quiz quiz; 
    
    // Client-side state, not serialized
    public transient boolean clientSideIsCurrentlyGlowing = false;

    // Constructor for location pings
    public PingPoint(Vec3d pos, String owner, Color color, byte soundIdx) {
        this(pos, owner, color, soundIdx, PingType.LOCATION, null);
    }

    // Constructor for entity pings
    public PingPoint(Vec3d pos, String owner, Color color, byte soundIdx, UUID entityUUID) {
        this(pos, owner, color, soundIdx, PingType.ENTITY, entityUUID);
    }
    
    // Main constructor
    public PingPoint(Vec3d pos, String owner, Color color, byte soundIdx, PingType type, UUID entityUUID) {
        this.id = UUID.randomUUID();
        this.pos = pos;
        this.owner = owner;
        this.color = color;
        this.sound = soundIdx;
        this.createTime = LocalDateTime.now();
        this.type = type;
        // Ensure entityUUID is only set for ENTITY type
        this.entityUUID = (type == PingType.ENTITY) ? entityUUID : null; 
        this.quiz = Book.getRandomQuiz(ServerConfig.getCurrentBookId());

        // // log quiz content here
        // if (this.quiz != null) {
        //     QuizCraft.LOGGER.debug("PingPoint created with quiz: question='{}', options={}, answer={}, uuid={}",
        //         this.quiz.question, java.util.Arrays.toString(this.quiz.options), this.quiz.answer, this.quiz.uuid);
        // } else { // should not happen
        //     QuizCraft.LOGGER.error("PingPoint created without quiz");
        // }
    }

    // Deprecated constructor, adapt or remove if not needed elsewhere
    @Deprecated
    public PingPoint(Vec3d pos, String owner, int color, byte soundIdx) {
        this(pos, owner, new Color(color), soundIdx, PingType.LOCATION, null); // Assume LOCATION if type not specified
    }

    public boolean shouldVanish(long SecondsToVanish) {
        if (SecondsToVanish == 0) {
            return false;
        }
        return LocalDateTime.now().minusSeconds(SecondsToVanish).isAfter(createTime);
    }

    // for Vec3d is not serializable
    @Serial
    private void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(id);
        stream.writeDouble(pos.x);
        stream.writeDouble(pos.y);
        stream.writeDouble(pos.z);
        stream.writeObject(owner);
        stream.writeObject(color);
        stream.writeByte(sound);
        stream.writeObject(createTime);
        // Serialize new fields
        stream.writeObject(type);
        stream.writeObject(entityUUID); // Can be null
        stream.writeObject(quiz);
    }

    // for Vec3d is not serializable
    @Serial
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        id = (UUID) stream.readObject();
        double x = stream.readDouble();
        double y = stream.readDouble();
        double z = stream.readDouble();
        pos = new Vec3d(x, y, z);
        owner = (String) stream.readObject();
        color = (Color) stream.readObject();
        sound = stream.readByte();
        createTime = (LocalDateTime) stream.readObject();
        // Deserialize new fields
        type = (PingType) stream.readObject();
        entityUUID = (UUID) stream.readObject(); // Can be null
        quiz = (Quiz) stream.readObject();
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(this);
        oos.flush();

        // get byte array
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

    public static PingPoint fromPacketByteBuf(PacketByteBuf buf) throws IOException, ClassNotFoundException {
        var serializedData = buf.getWrittenBytes();

        ByteArrayInputStream bis = new ByteArrayInputStream(serializedData);
        ObjectInputStream ois = new ObjectInputStream(bis);

        // deserialize
        PingPoint p = (PingPoint) ois.readObject();

        ois.close();
        bis.close();

        return p;
    }
}