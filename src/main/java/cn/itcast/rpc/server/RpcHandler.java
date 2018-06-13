package cn.itcast.rpc.server;

import cn.itcast.rpc.common.RpcRequest;
import cn.itcast.rpc.common.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;


public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(RpcHandler.class);

    private final Map<String, Object> handlerMap;

    public RpcHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;

    }

    public void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) {
      //  System.out.println(handlerMap.isEmpty()+"DASADS");
        RpcResponse response=new RpcResponse();
        response.setRequestId(rpcRequest.getRequesteId());
       // System.out.println(  rpcRequest.getMethodName()+"sdsaasddsad");

        try {
            Object result=handle(rpcRequest);
            response.setResult(result);
        } catch (Exception e) {
            e.printStackTrace();
            response.setError(e);
        }
        //写入 outbundle（即RpcEncoder）进行下一步处理（即编码）后发送到channel中给客户端
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private Object handle(RpcRequest request) throws Exception{

        String className = request.getClassName();
        Object serviceBen = handlerMap.get(className);//获取实现类
        //拿取方法名，参数类型，参数值
        String methodName = request.getMethodName();

        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[]parameters=request.getParameters();
        Class<?>forName=Class.forName(className);
        Method method=forName.getMethod(methodName,parameterTypes);
        return method.invoke(serviceBen,parameters);

    }
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("server caught exception", cause);
        ctx.close();
    }
}
