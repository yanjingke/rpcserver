package cn.itcast.rpc.server;


import cn.itcast.rpc.common.RpcDecoder;
import cn.itcast.rpc.common.RpcEncoder;
import cn.itcast.rpc.common.RpcRequest;
import cn.itcast.rpc.common.RpcResponse;
import cn.itcast.rpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class RpcServer implements ApplicationContextAware, InitializingBean  {
    private static final org.slf4j.Logger LOGGER=LoggerFactory.getLogger(RpcServer.class);
    private String severAddress;
    private Map<String,Object> handlerMap=new HashMap<String,Object>();
    private ServiceRegistry serviceRegistry;
    public RpcServer(String severAddress) {
        this.severAddress = severAddress;
    }


    public RpcServer(String severAddress, ServiceRegistry serviceRegistry) {
        this.severAddress = severAddress;
        this.serviceRegistry = serviceRegistry;
    }


    public void afterPropertiesSet() throws Exception {
        EventLoopGroup bossGroups=new NioEventLoopGroup();
        EventLoopGroup workerGrop=new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap=new ServerBootstrap();
            bootstrap.group(bossGroups,workerGrop)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    .addLast(new RpcHandler(handlerMap));

                        }
                    }).option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            String[]arry=severAddress.split(":");
            String host=arry[0];
            int port=Integer.parseInt(arry[1]);
            ChannelFuture future = bootstrap.bind(host, port).sync();
            LOGGER.debug("server started on port {}", port);
            if(serviceRegistry!=null){
                serviceRegistry.register(severAddress);
            }
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            workerGrop.shutdownGracefully();
            bossGroups.shutdownGracefully();
        }

    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String,Object>serviceBeanMap=applicationContext.getBeansWithAnnotation(RpcService.class);
        if(MapUtils.isNotEmpty(serviceBeanMap)){
            for(Object serviceBean:serviceBeanMap.values()){
                String interfaceName= serviceBean.getClass()
                        .getAnnotation(RpcService.class).value().getName();
                handlerMap.put(interfaceName,serviceBean);

            }
        }

    }



}
