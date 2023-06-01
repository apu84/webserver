package com.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

public class Webserver {
    public static void main(String[] args) throws Exception {
        var httpServer = new MyServer(2000);
        System.out.println("Starting Server ... ");
        httpServer.get().start();
        System.out.println("Started Server ... ");
        var terminationHandler = new TerminationHandler();
    }
}

class RequestHandler implements HttpHandler {
    PrimeGenerator primeGenerator;

    public RequestHandler() throws IOException {
        primeGenerator = new PrimeGenerator(
                new AppConfiguration()
        );
    }

    static <T> String toString(List<T> list) {
        StringBuilder builder = new StringBuilder();
        for (T t: list) {
            builder.append(t);
            builder.append(",");
        }
        return builder.toString();
    }
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        System.out.println(httpExchange.getRequestURI());
        var requestUri = httpExchange.getRequestURI();
        var primesPath = "/primes";
        if(requestUri.getPath().contains("/primes/")) {
            var countString = requestUri.getPath().substring(primesPath.length() + 1);
            try {
                List<Integer> primes = primeGenerator.calculatePrime(Integer.parseInt(countString));
                var primesString = toString(primes);
                httpExchange.sendResponseHeaders(200, primesString.length());
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(primesString.getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                var error = e.toString();
                httpExchange.sendResponseHeaders(400, error.length());
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(error.getBytes());
                outputStream.flush();
                outputStream.close();
            }

        }
        var response = "Hello from Filix";
        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}

class MyServer {
    private final HttpServer httpServer;

    public MyServer(int port) throws IOException {
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
    TerminationHandler() {
        final long start = System.nanoTime();
        Signal.handle(new Signal("INT"), sig -> {
            System.out.format("\nProgram execution took %f seconds\n", (System.nanoTime() - start) / 1e9f);
            System.exit(0);
        });
    }
}
