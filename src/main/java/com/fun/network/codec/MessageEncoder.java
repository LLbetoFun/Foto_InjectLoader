package com.fun.network.codec;

import com.fun.network.model.DataModel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<DataModel> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, DataModel dataModel, ByteBuf byteBuf) throws Exception {
        dataModel.writeToByteBuffer(byteBuf);
    }
}
