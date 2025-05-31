package com.fun.network.model;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
@AllArgsConstructor
public class DataModel {
    public DataModel() {

    }
    public static enum Type {
        INFO,WARN,ERROR,DEBUG
    }
    public static final short MAGIC_NUMBER = 0xCB;
    public Type type;
    public String msg;
    public void readFromByteBuffer(ByteBuf buffer) {
        if (buffer == null || buffer.readableBytes() < 8) {
            throw new IllegalArgumentException("Invalid buffer");
        }
        short magicNumber = buffer.readShort();
        if(magicNumber != MAGIC_NUMBER) return;
        int index = buffer.readInt();
        int length = buffer.readInt();
        if(buffer.readableBytes() < length){
            length = buffer.readableBytes();
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        type = Type.values()[index];
        msg = new String(bytes, StandardCharsets.UTF_8);
    }
    public void writeToByteBuffer(ByteBuf buffer) {
        buffer.writeShort(MAGIC_NUMBER);
        buffer.writeInt(type.ordinal());
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
    }
}
