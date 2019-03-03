package top.ljming.nettyLearn.handler;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * 描述类的功能.
 *
 * @author ljming
 */
public class DiscardNettyServerHandler extends ChannelInboundHandlerAdapter {

    // 数据被接收的时候调用
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            ByteBuf byteBuf = (ByteBuf) msg;
            while (byteBuf.isReadable()) {
                System.out.println((char) byteBuf.readByte());
                System.out.flush();
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
