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

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Netty 服务器自定义 ChannelHandler
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

	/**
	 * 每当从客户端读取数据会触发该方法
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		// 当读取数据时，回写给客户端，但是没有立即响应给客户端，调用 flush() 后才响应给客户端
		ctx.write(msg);
	}

	/**
	 * 当读取完客户端发送过来的数据，会触发该方法
	 */
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		// 把 ChannelHandlerContext 中回写给客户端的数据立即响应
		ctx.flush();
	}

	/**
	 * 出现异常时，会触发该方法
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		// 释放资源
		ctx.close();
	}
}
