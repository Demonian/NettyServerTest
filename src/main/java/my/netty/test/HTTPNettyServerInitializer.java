package my.netty.test;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import my.netty.test.codec.HTTPNettyServerHandler;


/**
 * Created by Dmytro on 02.08.2015.
 */
public class HTTPNettyServerInitializer extends ChannelInitializer<SocketChannel> {
//    Initializing my pipeline with handlers
    @Override
    protected void initChannel(SocketChannel sc) throws Exception {
        ChannelPipeline p = sc.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HTTPNettyServerHandler());
    }
}
