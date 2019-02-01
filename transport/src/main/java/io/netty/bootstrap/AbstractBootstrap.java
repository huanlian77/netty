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

package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.util.internal.SocketUtils;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link AbstractBootstrap} is a helper class that makes it easy to bootstrap a {@link Channel}. It support
 * method-chaining to provide an easy way to configure the {@link AbstractBootstrap}.
 *
 * <p>When not used in a {@link ServerBootstrap} context, the {@link #bind()} methods are useful for connectionless
 * transports such as datagram (UDP).</p>
 *
 * 实现 Cloneable 接口，AbstractBootstrap 两个子类（ServerBootstrap、Bootstrap）中实现 clone()
 * 然后 new ServerBootstrap(this)/new Bootstrap(this) 进行克隆
 * 其中 group、channelFactory、handler、localAddress 浅克隆
 * options、attrs 深克隆
 *
 */
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {

	/**
	 * EventLoopGroup 对象
	 */
	volatile EventLoopGroup group;
	/**
	 * Channel 工厂，用于创建 Channel 对象
	 */
	@SuppressWarnings("deprecation")
	private volatile ChannelFactory<? extends C> channelFactory;
	/**
	 * 本地地址
	 */
	private volatile SocketAddress localAddress;
	/**
	 * 可选项集合
	 */
	private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();
	/**
	 * 属性集合
	 */
	private final Map<AttributeKey<?>, Object> attrs = new LinkedHashMap<AttributeKey<?>, Object>();
	/**
	 * ChannelHandler
	 */
	private volatile ChannelHandler handler;

	AbstractBootstrap() {
		// Disallow extending from a different package.
	}

	AbstractBootstrap(AbstractBootstrap<B, C> bootstrap) {
		group = bootstrap.group;
		channelFactory = bootstrap.channelFactory;
		handler = bootstrap.handler;
		localAddress = bootstrap.localAddress;
		synchronized (bootstrap.options) { // 使用 synchronized 关键字，options 可能在其他线程修改
			options.putAll(bootstrap.options);
		}
		synchronized (bootstrap.attrs) { // 使用 synchronized 关键字，attrs 可能在其他线程修改
			attrs.putAll(bootstrap.attrs);
		}
	}

	/**
	 * 设置用于 EventLoopGroup, EventLoopGroup 接受客户端连接创建 Channel，还可以处理 Channel 事件
	 */
	public B group(EventLoopGroup group) {
		if (group == null) {
			throw new NullPointerException("group");
		}
		if (this.group != null) {
			throw new IllegalStateException("group set already");
		}
		this.group = group;
		return self();
	}

	/**
	 * 返回自己，所以可以用链式调用
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private B self() {
		return (B) this;
	}

	/**
	 *
	 * 参数是 Class 类型，通过反射获取参数的默认构造器保存在 ChannelFactory 中
	 * 后面通过 ChannelFactory.newChannel() 创建 channel
	 *
	 */
	public B channel(Class<? extends C> channelClass) {
		if (channelClass == null) {
			throw new NullPointerException("channelClass");
		}
		return channelFactory(new ReflectiveChannelFactory<C>(channelClass));
	}

	/**
	 * @deprecated Use {@link #channelFactory(io.netty.channel.ChannelFactory)} instead.
	 */
	@Deprecated
	public B channelFactory(ChannelFactory<? extends C> channelFactory) {
		if (channelFactory == null) {
			throw new NullPointerException("channelFactory");
		}
		if (this.channelFactory != null) {
			throw new IllegalStateException("channelFactory set already");
		}

		this.channelFactory = channelFactory;
		return self();
	}

	/**
	 * {@link io.netty.channel.ChannelFactory} which is used to create {@link Channel} instances from
	 * when calling {@link #bind()}. This method is usually only used if {@link #channel(Class)}
	 * is not working for you because of some more complex needs. If your {@link Channel} implementation
	 * has a no-args constructor, its highly recommend to just use {@link #channel(Class)} to
	 * simplify your code.
	 */
	@SuppressWarnings({"unchecked", "deprecation"})
	public B channelFactory(io.netty.channel.ChannelFactory<? extends C> channelFactory) {
		// io.netty.channel.ChannelFactory<? extends C>  向上转型为  io.netty.bootstrap.ChannelFactory<? extends C>
		return channelFactory((ChannelFactory<C>) channelFactory);
	}

	/**
	 * The {@link SocketAddress} which is used to bind the local "end" to.
	 */
	public B localAddress(SocketAddress localAddress) {
		this.localAddress = localAddress;
		return self();
	}

	/**
	 * @see #localAddress(SocketAddress)
	 */
	public B localAddress(int inetPort) {
		return localAddress(new InetSocketAddress(inetPort));
	}

	/**
	 * @see #localAddress(SocketAddress)
	 */
	public B localAddress(String inetHost, int inetPort) {
		return localAddress(SocketUtils.socketAddress(inetHost, inetPort));
	}

	/**
	 * @see #localAddress(SocketAddress)
	 */
	public B localAddress(InetAddress inetHost, int inetPort) {
		return localAddress(new InetSocketAddress(inetHost, inetPort));
	}

	/**
	 * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they got
	 * created. Use a value of {@code null} to remove a previous set {@link ChannelOption}.
	 */
	public <T> B option(ChannelOption<T> option, T value) {
		if (option == null) {
			throw new NullPointerException("option");
		}
		if (value == null) {
			synchronized (options) {
				options.remove(option);
			}
		} else {
			synchronized (options) {
				options.put(option, value);
			}
		}
		return self();
	}

	/**
	 * Allow to specify an initial attribute of the newly created {@link Channel}.  If the {@code value} is
	 * {@code null}, the attribute of the specified {@code key} is removed.
	 */
	public <T> B attr(AttributeKey<T> key, T value) {
		if (key == null) {
			throw new NullPointerException("key");
		}
		if (value == null) {
			synchronized (attrs) {
				attrs.remove(key);
			}
		} else {
			synchronized (attrs) {
				attrs.put(key, value);
			}
		}
		return self();
	}

	/**
	 * 调用 bind() 时会校验 EventLoopGroup 和 ChannelFactory 是否为 null
	 */
	public B validate() {
		if (group == null) {
			throw new IllegalStateException("group not set");
		}
		if (channelFactory == null) {
			throw new IllegalStateException("channel or channelFactory not set");
		}
		return self();
	}

	/**
	 * Returns a deep clone of this bootstrap which has the identical configuration.  This method is useful when making
	 * multiple {@link Channel}s with similar settings.  Please note that this method does not clone the
	 * {@link EventLoopGroup} deeply but shallowly, making the group a shared resource.
	 */
	@Override
	@SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
	public abstract B clone();

	/**
	 * Create a new {@link Channel} and register it with an {@link EventLoop}.
	 */
	public ChannelFuture register() {
		validate();
		return initAndRegister();
	}

	/**
	 * Create a new {@link Channel} and bind it.
	 */
	public ChannelFuture bind() {
		// 校验服务启动需要的必要参数
		validate();
		SocketAddress localAddress = this.localAddress;
		if (localAddress == null) {
			throw new IllegalStateException("localAddress not set");
		}
		// 绑定本地地址
		return doBind(localAddress);
	}

	/**
	 * Create a new {@link Channel} and bind it.
	 */
	public ChannelFuture bind(int inetPort) {
		return bind(new InetSocketAddress(inetPort));
	}

	/**
	 * Create a new {@link Channel} and bind it.
	 */
	public ChannelFuture bind(String inetHost, int inetPort) {
		return bind(SocketUtils.socketAddress(inetHost, inetPort));
	}

	/**
	 * Create a new {@link Channel} and bind it.
	 */
	public ChannelFuture bind(InetAddress inetHost, int inetPort) {
		return bind(new InetSocketAddress(inetHost, inetPort));
	}

	/**
	 * Create a new {@link Channel} and bind it.
	 */
	public ChannelFuture bind(SocketAddress localAddress) {
		validate();
		if (localAddress == null) {
			throw new NullPointerException("localAddress");
		}
		return doBind(localAddress);
	}

	private ChannelFuture doBind(final SocketAddress localAddress) {
		// 初始化并注册一个 Channel 到 SelectionKey 中，因为注册是异步的过程，所以返回一个 ChannelFuture 对象
		final ChannelFuture regFuture = initAndRegister();
		final Channel channel = regFuture.channel();
		if (regFuture.cause() != null) { // 若发送异常，直接进行返回
			return regFuture;
		}

		// isDone() 操作执行结束，完成/异常/取消都返回 true
		if (regFuture.isDone()) {
			// 注册成功
			ChannelPromise promise = channel.newPromise();
			doBind0(regFuture, channel, localAddress, promise);
			return promise;
		} else {
			// 如果注册未执行结束，需要添加监听器，在注册完成后，进行回调执行 doBind0()
			final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
			regFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					Throwable cause = future.cause();
					if (cause != null) {
						// Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
						// IllegalStateException once we try to access the EventLoop of the Channel.
						promise.setFailure(cause);
					} else {
						// Registration was successful, so set the correct executor to use.
						// See https://github.com/netty/netty/issues/2586
						promise.registered();

						doBind0(regFuture, channel, localAddress, promise);
					}
				}
			});
			return promise;
		}
	}

	final ChannelFuture initAndRegister() {
		Channel channel = null;
		try {
			// 通过 Channel 工厂创建 Channel
			// 创建 Channel 会设置 Channel 为非阻塞，监听的事件，设置channelId，初始化 Unsafe，ChannelPipeline，ChannelHandlerContext(ChannelHandlerContext链表结构)
			channel = channelFactory.newChannel();
			// 初始化 Channel 配置
			init(channel);
		} catch (Throwable t) {
			if (channel != null) {
				// 强制关闭 Channel
				channel.unsafe().closeForcibly();
				// as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
				return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
			}
			// as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
			return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
		}
		// 调用 group() 得到 EventLoopGroup，分配一个 EventLoop ，将 Channel 注册到其上
		ChannelFuture regFuture = config().group().register(channel);
		if (regFuture.cause() != null) {
			if (channel.isRegistered()) {
				channel.close();
			} else {
				channel.unsafe().closeForcibly();
			}
		}

		// If we are here and the promise is not failed, it's one of the following cases:
		// 1) If we attempted registration from the event loop, the registration has been completed at this point.
		//    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
		// 2) If we attempted registration from the other thread, the registration request has been successfully
		//    added to the event loop's task queue for later execution.
		//    i.e. It's safe to attempt bind() or connect() now:
		//         because bind() or connect() will be executed *after* the scheduled registration task is executed
		//         because register(), bind(), and connect() are all bound to the same thread.

		return regFuture;
	}

	abstract void init(Channel channel) throws Exception;

	private static void doBind0(
			final ChannelFuture regFuture, final Channel channel,
			final SocketAddress localAddress, final ChannelPromise promise) {

		// This method is invoked before channelRegistered() is triggered.  Give user handlers a chance to set up
		// the pipeline in its channelRegistered() implementation.
		channel.eventLoop().execute(new Runnable() {
			@Override
			public void run() {
				// 注册成功，绑定端口
				if (regFuture.isSuccess()) {
					// bind() 最后会调用 JDK 的 bind()
					channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
				// 注册失败，回调通知 promise 异常
				} else {
					promise.setFailure(regFuture.cause());
				}
			}
		});
	}

	/**
	 * the {@link ChannelHandler} to use for serving the requests.
	 */
	public B handler(ChannelHandler handler) {
		if (handler == null) {
			throw new NullPointerException("handler");
		}
		this.handler = handler;
		return self();
	}

	/**
	 * Returns the configured {@link EventLoopGroup} or {@code null} if non is configured yet.
	 *
	 * @deprecated Use {@link #config()} instead.
	 */
	@Deprecated
	public final EventLoopGroup group() {
		return group;
	}

	/**
	 * 返回 AbstractBootstrapConfig（引导类配置项） 对象，该对象中有一个 AbstractBootstrapConfig 对象
	 */
	public abstract AbstractBootstrapConfig<B, C> config();

	static <K, V> Map<K, V> copiedMap(Map<K, V> map) {
		final Map<K, V> copied;
		synchronized (map) {
			if (map.isEmpty()) {
				return Collections.emptyMap();
			}
			copied = new LinkedHashMap<K, V>(map);
		}
		return Collections.unmodifiableMap(copied);
	}

	final Map<ChannelOption<?>, Object> options0() {
		return options;
	}

	final Map<AttributeKey<?>, Object> attrs0() {
		return attrs;
	}

	final SocketAddress localAddress() {
		return localAddress;
	}

	@SuppressWarnings("deprecation")
	final ChannelFactory<? extends C> channelFactory() {
		return channelFactory;
	}

	final ChannelHandler handler() {
		return handler;
	}

	final Map<ChannelOption<?>, Object> options() {
		return copiedMap(options);
	}

	final Map<AttributeKey<?>, Object> attrs() {
		return copiedMap(attrs);
	}

	/**
	 * 调用 setChannelOption() 为 Channel 设置可选项
	 */
	static void setChannelOptions(
			Channel channel, Map<ChannelOption<?>, Object> options, InternalLogger logger) {
		for (Map.Entry<ChannelOption<?>, Object> e : options.entrySet()) {
			setChannelOption(channel, e.getKey(), e.getValue(), logger);
		}
	}

	static void setChannelOptions(
			Channel channel, Map.Entry<ChannelOption<?>, Object>[] options, InternalLogger logger) {
		for (Map.Entry<ChannelOption<?>, Object> e : options) {
			setChannelOption(channel, e.getKey(), e.getValue(), logger);
		}
	}

	/**
	 * 通过 config() 获取 Channel 配置项对象，为 Channel 设置可选项，
	 */
	@SuppressWarnings("unchecked")
	private static void setChannelOption(
			Channel channel, ChannelOption<?> option, Object value, InternalLogger logger) {
		try {
			if (!channel.config().setOption((ChannelOption<Object>) option, value)) {
				logger.warn("Unknown channel option '{}' for channel '{}'", option, channel);
			}
		} catch (Throwable t) {
			logger.warn(
					"Failed to set channel option '{}' with value '{}' for channel '{}'", option, value, channel, t);
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder()
				.append(StringUtil.simpleClassName(this))
				.append('(').append(config()).append(')');
		return buf.toString();
	}

	static final class PendingRegistrationPromise extends DefaultChannelPromise {

		// Is set to the correct EventExecutor once the registration was successful. Otherwise it will
		// stay null and so the GlobalEventExecutor.INSTANCE will be used for notifications.
		private volatile boolean registered;

		PendingRegistrationPromise(Channel channel) {
			super(channel);
		}

		void registered() {
			registered = true;
		}

		@Override
		protected EventExecutor executor() {
			if (registered) {
				// If the registration was a success executor is set.
				//
				// See https://github.com/netty/netty/issues/2586
				return super.executor();
			}
			// The registration failed so we can only use the GlobalEventExecutor as last resort to notify.
			return GlobalEventExecutor.INSTANCE;
		}
	}
}
