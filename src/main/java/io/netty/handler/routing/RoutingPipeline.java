/*
 * This file is part of the netty-router package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package io.netty.handler.routing;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerInvoker;
import io.netty.channel.ChannelHandlerInvokerUtil;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutorGroup;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
class RoutingPipeline implements ChannelPipeline {

    private final AnchorChannelHandler start;

    private final AnchorChannelHandler end;

    private final ChannelPipeline parentPipeline;

    private final String pipelineName;

    private final String handlerNamePrefix;

    public String getPipelineName() {
        return pipelineName;
    }

    /**
     * Constructor for RoutingPipeline
     *
     * @param routerCtx the parent router handler context.
     * @param name The name of this pipeline, mostly is proposed as the routing
     * name.
     */
    public RoutingPipeline(final ChannelHandlerContext routerCtx, final String name) {
        this.handlerNamePrefix = UUID.randomUUID().toString();
        this.pipelineName = name;
        final ChannelPipeline self_pipeline = this;
        this.start = new AnchorChannelHandler(this) {

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                ChannelHandlerInvokerUtil.invokeWriteNow(routerCtx, msg, promise);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) throws Exception {
                ChannelHandlerInvokerUtil.invokeExceptionCaughtNow(routerCtx, new RoutingException() {

                    @Override
                    public String getRoutingName() {
                        return name;
                    }

                    @Override
                    public ChannelPipeline getRoutingPipeline() {
                        return self_pipeline;
                    }

                    @Override
                    public Throwable unwrapException() {
                        return cause;
                    }
                });
            }

        };
        this.end = new AnchorChannelHandler(this) {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            }

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            }

        };
        this.parentPipeline = routerCtx.pipeline();
    }

    public AnchorChannelHandler getStart() {
        return start;
    }

    public AnchorChannelHandler getEnd() {
        return end;
    }

    private String handlerNameFormatter(String handlerName) {
        return this.handlerNamePrefix + "-" + handlerName;
    }

    @Override
    public ChannelPipeline addFirst(String name, ChannelHandler handler) {
        this.parentPipeline.addAfter(this.start.getAnchorName(), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler) {
        this.parentPipeline.addAfter(group, this.start.getAnchorName(), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(ChannelHandlerInvoker invoker, String name, ChannelHandler handler) {
        this.parentPipeline.addAfter(invoker, this.start.getAnchorName(), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addLast(String name, ChannelHandler handler) {
        this.parentPipeline.addBefore(this.end.getAnchorName(), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
        this.parentPipeline.addBefore(group, this.end.getAnchorName(), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandlerInvoker invoker, String name, ChannelHandler handler) {
        this.parentPipeline.addBefore(invoker, this.end.getAnchorName(), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler) {
        this.parentPipeline.addBefore(this.handlerNameFormatter(baseName), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler) {
        this.parentPipeline.addBefore(group, this.handlerNameFormatter(baseName), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addBefore(ChannelHandlerInvoker invoker, String baseName, String name, ChannelHandler handler) {
        this.parentPipeline.addBefore(invoker, this.handlerNameFormatter(baseName), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler) {
        this.parentPipeline.addAfter(this.handlerNameFormatter(baseName), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler) {
        this.parentPipeline.addAfter(group, this.handlerNameFormatter(baseName), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addAfter(ChannelHandlerInvoker invoker, String baseName, String name, ChannelHandler handler) {
        this.parentPipeline.addAfter(invoker, this.handlerNameFormatter(baseName), this.handlerNameFormatter(name), handler);
        return this;
    }

    @Override
    public ChannelPipeline addFirst(ChannelHandler... handlers) {
        for (int i = handlers.length - 1; i > 0; i--) {
            this.parentPipeline.addAfter(this.getStart().getAnchorName(), null, handlers[i]);
        }
        return this;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers) {
        for (int i = handlers.length - 1; i > 0; i--) {
            this.parentPipeline.addAfter(group, this.getStart().getAnchorName(), null, handlers[i]);
        }
        return this;
    }

    @Override
    public ChannelPipeline addFirst(ChannelHandlerInvoker invoker, ChannelHandler... handlers) {
        for (int i = handlers.length - 1; i > 0; i--) {
            this.parentPipeline.addAfter(invoker, this.getStart().getAnchorName(), null, handlers[i]);
        }
        return this;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandler... handlers) {
        for (ChannelHandler handler : handlers) {
            this.parentPipeline.addBefore(this.getEnd().getAnchorName(), null, handler);
        }
        return this;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers) {
        for (ChannelHandler handler : handlers) {
            this.parentPipeline.addBefore(group, this.getEnd().getAnchorName(), null, handler);
        }
        return this;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandlerInvoker invoker, ChannelHandler... handlers) {
        for (ChannelHandler handler : handlers) {
            this.parentPipeline.addBefore(invoker, this.getEnd().getAnchorName(), null, handler);
        }
        return this;
    }

    @Override
    public ChannelPipeline remove(ChannelHandler handler) {
        this.parentPipeline.remove(handler);
        return this;
    }

    @Override
    public ChannelHandler remove(String name) {
        return this.parentPipeline.remove(this.handlerNameFormatter(name));
    }

    @Override
    public <T extends ChannelHandler> T remove(Class<T> handlerType) {
        return this.parentPipeline.remove(handlerType);
    }

    @Override
    public ChannelHandler removeFirst() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelHandler removeLast() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler) {
        this.parentPipeline.replace(oldHandler, this.handlerNameFormatter(newName), newHandler);
        return this;
    }

    @Override
    public ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler) {
        return this.parentPipeline.replace(this.handlerNameFormatter(oldName), this.handlerNameFormatter(newName), newHandler);
    }

    @Override
    public <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName, ChannelHandler newHandler) {
        return this.parentPipeline.replace(oldHandlerType, this.handlerNameFormatter(newName), newHandler);
    }

    @Override
    public ChannelHandler first() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelHandlerContext firstContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelHandler last() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelHandlerContext lastContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelHandler get(String name) {
        return this.parentPipeline.get(this.handlerNameFormatter(name));
    }

    @Override
    public <T extends ChannelHandler> T get(Class<T> handlerType) {
        return this.parentPipeline.get(handlerType);
    }

    @Override
    public ChannelHandlerContext context(ChannelHandler handler) {
        return this.parentPipeline.context(handler);
    }

    @Override
    public ChannelHandlerContext context(String name) {
        return this.parentPipeline.context(this.handlerNameFormatter(name));
    }

    @Override
    public ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType) {
        return this.parentPipeline.context(handlerType);
    }

    @Override
    public Channel channel() {
        return this.parentPipeline.channel();
    }

    @Override
    public List<String> names() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, ChannelHandler> toMap() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline fireChannelRegistered() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline fireChannelUnregistered() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline fireChannelActive() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline fireChannelInactive() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline fireExceptionCaught(Throwable cause) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline fireUserEventTriggered(Object event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline fireChannelRead(Object msg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline fireChannelReadComplete() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline fireChannelWritabilityChanged() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture disconnect() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture deregister() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline read() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture write(Object msg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelPipeline flush() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<Map.Entry<String, ChannelHandler>> iterator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}