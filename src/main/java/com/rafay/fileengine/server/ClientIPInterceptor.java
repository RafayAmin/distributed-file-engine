package com.rafay.fileengine.server;

import io.grpc.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ClientIPInterceptor implements ServerInterceptor {
    // This key allows us to store and retrieve the IP address anywhere in our code
    public static final Context.Key<String> CLIENT_IP = Context.key("client-ip");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        Attributes attrs = call.getAttributes();
        SocketAddress remoteAddr = attrs.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        String ip = "unknown";
        
        if (remoteAddr instanceof InetSocketAddress) {
            ip = ((InetSocketAddress) remoteAddr).getAddress().getHostAddress();
        }
        
        // Store the IP in the context so FileServer can read it
        Context ctx = Context.current().withValue(CLIENT_IP, ip);
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
