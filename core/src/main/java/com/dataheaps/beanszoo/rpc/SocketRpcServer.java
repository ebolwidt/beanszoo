package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import com.dataheaps.beanszoo.sd.ServiceDirectory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by admin on 29/5/16.
 */


public class SocketRpcServer extends AbstractRpcServer<Socket> {

    ServerSocket serverSocket;
    Thread acceptor;
    Map<Socket, SocketRpcController> controllers = new ConcurrentHashMap<>();

    public SocketRpcServer(SocketRpcServerAddress bindings, RPCRequestCodec codec, ServiceDirectory sd) {
        super(bindings, codec, sd);
    }

    @Override
    public void start() throws Exception {

        serverSocket = new ServerSocket(((SocketRpcServerAddress)bindings).getPort());
        serverSocket.setReuseAddress(true);

        acceptor = new Thread() {
            @Override
            public void run() {
                while (!interrupted()) {
                    try {
                        Socket session = serverSocket.accept();
                        SocketRpcController c = new SocketRpcController(session, new SocketRpcController.MessageHandler() {
                            @Override
                            public void handleMessage(Socket socket, byte[] buffer) {
                                handleRequest(socket, buffer);
                            }

                            @Override
                            public void socketError(Socket socket) {
                                controllers.remove(socket);
                            }
                        });
                        controllers.put(session, c);
                        c.start();
                    } catch (Exception e) {

                    }
                }
            }
        };
        acceptor.setDaemon(true);
        acceptor.start();
    }

    @Override
    public void stop() throws Exception {
        acceptor.interrupt();
        serverSocket.close();
    }

    @Override
    void sendMessage(Socket endpoint, byte[] buffer) throws Exception {
        SocketRpcController c = controllers.get(endpoint);
        if (c != null) c.sendMessage(buffer);
    }
}
