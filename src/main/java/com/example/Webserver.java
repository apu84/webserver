package com.example;

import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Signal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Webserver {
    private static final Logger logger = LogManager.getLogger(Webserver.class);

    public static void main(String[] args) throws Exception {
        var httpServer = new MyServer(2000);
        logger.info("Starting Server ... ");
        httpServer.get().start();
        logger.info("Started Server ... ");
        var terminationHandler = new TerminationHandler();
    }
}

class MyServer {
    private final HttpServer httpServer;

    public MyServer(int port) throws IOException, ClassNotFoundException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        var context = httpServer.createContext("/");
        context.setHandler(new RequestHandler());
    }

    public HttpServer get() {
        return httpServer;
    }
}

class TerminationHandler {
    Logger logger = LogManager.getLogger(TerminationHandler.class);
    TerminationHandler() {
        final long start = System.nanoTime();
        Signal.handle(new Signal("INT"), sig -> {
            logger.info(String.format("\nProgram execution took %f seconds\n", (System.nanoTime() - start) / 1e9f));
            System.exit(0);
        });
    }
}
