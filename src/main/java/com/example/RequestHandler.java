package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static com.example.AnnotatedMethodFinder.findAnnotatedMethod;
import static com.example.AnnotatedMethodFinder.getAnnotatedParameterValues;

class RequestHandler implements HttpHandler {
    Logger logger = LogManager.getLogger(RequestHandler.class);
    final List<Class<?>> restControllers;
    ObjectMapper objectMapper = new ObjectMapper();

    public RequestHandler() throws IOException, ClassNotFoundException {
        restControllers = AnnotationClassScanner.findAnnotatedClasses(RestController.class);
    }

    private InstanceMethod findRestControllerMethod(String value, String requestType) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        InstanceMethod method;
        switch (requestType) {
            case "Get":
                method = findMethod(GetMapping.class, value);
                break;
            case "Post":
                method = findMethod(PostMapping.class, value);
                break;
            default:
                throw new IllegalArgumentException(String.format("No request handler for %s", requestType));
        }
        return method;
    }

    private record InstanceMethod(Object instance, Method method) {}

    private InstanceMethod findMethod(Class<? extends Annotation> annotation, String value) {
        Method method;
        for (Class<?> controller : restControllers) {
            try {
                var instance = controller.getDeclaredConstructor().newInstance();
                method = findAnnotatedMethod(controller, annotation, value);
                return new InstanceMethod(instance, method);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignore) {
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        logger.info(httpExchange.getRequestURI());
        var requestUri = httpExchange.getRequestURI();

        try {
            InstanceMethod instanceMethod = findRestControllerMethod(requestUri.toString(), "Get");
            if (instanceMethod != null) {
                Object[] parameters = getAnnotatedParameterValues(instanceMethod.method(), GetMapping.class, requestUri.toString());
                Object response = instanceMethod.method().invoke(instanceMethod.instance, parameters);
                String json = null;
                try {
                    json = objectMapper.writeValueAsString(response);
                    logger.info("Serialized response: " + json);
                    httpExchange.sendResponseHeaders(200, json.length());
                    OutputStream outputStream = httpExchange.getResponseBody();
                    outputStream.write(json.getBytes());
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
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
        }
//        var primesPath = "/primes/";
//        var nthPrimePath = "/nth-prime/";
//        var primesString = "";
//        if (requestUri.getPath().contains(primesPath)) {
//            var countString = requestUri.getPath().substring(primesPath.length());
//            List<Integer> result = primeGenerator.calculatePrime(Integer.parseInt(countString));
//            primesString = toString(result);
//        } else if (requestUri.getPath().contains(nthPrimePath)) {
//            var countString = requestUri.getPath().substring(primesPath.length() + 1);
//            int result = primeGenerator.nthPrime(Integer.parseInt(countString));
//            primesString = Integer.toString(result);
//        }
    }
}
