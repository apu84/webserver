package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    private InstanceMethod findRestControllerMethod(String value, String requestType) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        InstanceMethod instanceMethod;
        switch (requestType) {
            case "Get":
                instanceMethod = findMethod(GetMapping.class, value);
                break;
            case "Post":
                instanceMethod = findMethod(PostMapping.class, value);
                break;
            default:
                throw new NoSuchMethodException(String.format("No request handler for %s", requestType));
        }
        return instanceMethod;
    }

    private record InstanceMethod(Object instance, Method method) {
    }

    private InstanceMethod findMethod(Class<? extends Annotation> annotation, String value) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        for (Class<?> controller : restControllers) {
            var instance = controller.getDeclaredConstructor().newInstance();
            Method method = findAnnotatedMethod(controller, annotation, value);
            return new InstanceMethod(instance, method);
        }
        throw new NoSuchMethodException(String.format("No request handler for %s", value));
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        logger.info(httpExchange.getRequestURI());
        var requestUri = httpExchange.getRequestURI();
        try {
            InstanceMethod instanceMethod = findRestControllerMethod(requestUri.toString(), "Get");
            Object[] parameters = getAnnotatedParameterValues(instanceMethod.method(), GetMapping.class, requestUri.toString());
            Object response = instanceMethod.method().invoke(instanceMethod.instance, parameters);
            String json = objectMapper.writeValueAsString(response);
            httpExchange.sendResponseHeaders(HttpCode.OK, json.length());
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(json.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            logger.error(e);
            sendError(e.toString(), HttpCode.NOT_FOUND, httpExchange);
        } catch (InstantiationException ie) {
            logger.error(ie);
            sendError(ie.toString(), HttpCode.CLIENT_ERROR, httpExchange);
        } catch (Exception ex) {
            logger.error(ex);
            sendError(ex.toString(), HttpCode.SERVER_ERROR, httpExchange);
        }
    }

    private void sendError(String error,
                           int errorCode,
                           HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(errorCode, error.length());
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(error.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
