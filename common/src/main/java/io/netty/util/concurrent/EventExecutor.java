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
package io.netty.util.concurrent;

/**
 * The {@link EventExecutor} is a special {@link EventExecutorGroup} which comes
 * with some handy methods to see if a {@link Thread} is executed in a event loop.
 * Besides this, it also extends the {@link EventExecutorGroup} to allow for a generic
 * way to access methods.
 *
 */
public interface EventExecutor extends EventExecutorGroup {

    /**
     * Returns a reference to itself.
     */
    @Override
    EventExecutor next();

    /**
     * EventLoop 所属 EventLoopGroup
     */
    EventExecutorGroup parent();

    /**
     * 当前线程是否在 EventLoop 中
     */
    boolean inEventLoop();

    /**
     * 指定线程是否在 EventLoop 中
     */
    boolean inEventLoop(Thread thread);

    /**
     * 创建 Promise
     */
    <V> Promise<V> newPromise();

    /**
     * 创建 ProgressivePromise
     */
    <V> ProgressivePromise<V> newProgressivePromise();

    /**
     * 创建结果成功的 Future，isSuccess() 返回 true，通知所有添加到该 future 的 listener
     */
    <V> Future<V> newSucceededFuture(V result);

    /**
     * 创建结果成功的 Future，isSuccess() 返回 false，通知所有添加到该 future 的 listener
     */
    <V> Future<V> newFailedFuture(Throwable cause);
}
