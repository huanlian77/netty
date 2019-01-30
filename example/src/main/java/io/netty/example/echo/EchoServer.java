/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.ConcurrentMap;

/**
 * Netty 服务器，回写收到的消息
 */
public final class EchoServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {

        // 配置 ssl
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }


        // 创建两个 EventLoopGroup 线程组
        // 创建 bossGroup 线程组用于服务器接受客户端连接，线程组中线程数为1，表示只需要1个线程
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 创建 workerGroup 线程组用于 SocketChannel 的数据读写
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // 自定义的 ChannelHanlder
        final EchoServerHandler serverHandler = new EchoServerHandler();
        try {
            // 服务器启动服务类
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup) // 设置线程池
             .channel(NioServerSocketChannel.class) // 设置 Channel 类型是 NioServerSocketChannel
             .option(ChannelOption.SO_BACKLOG, 100) // 设置 HTTP 协议中 backlog 大小为 100
             .handler(new LoggingHandler(LogLevel.INFO)) // 添加 ChannelHandler
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 // 服务器为每一个客户端连接分配 Channel，在 initChannel() 方法中为该 Channel 添加 ChannerHandler
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     // 获取 ChannelPipeline
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc()));
                     }
                     // 向 ChannelPipeline 最后添加自定义的 ChannelHander
                     p.addLast(serverHandler);
                 }
             });

            // 绑定端口，对外提供服务
            ChannelFuture f = b.bind(PORT).sync();

            // 等待所有的连接关闭
            f.channel().closeFuture().sync();
        } finally {
            // 释放连接池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
