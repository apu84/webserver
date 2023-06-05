package com.example;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotatedMethodFinder {
    public static Object[] getAnnotatedParameterValues(Method method,
                                                       Class<? extends Annotation> annotationClass,
                                                       String value)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (method.isAnnotationPresent(annotationClass)) {
            Method valueMethod = annotationClass.getDeclaredMethod("value");
            Annotation annotation = method.getAnnotation(annotationClass);
            Object annotationValue = valueMethod.invoke(annotation);
            List<String> pathVaribeList = pathVariables(annotationValue.toString());
            var pattern = annotationValueBuilder(method.getParameters(), pathVaribeList, annotationValue.toString());

            Pattern regexPattern = Pattern.compile(pattern);
            Matcher matcher = regexPattern.matcher(value);

            // List to store the extracted values
            List<Object> extractedValues = new ArrayList<>();
            while (matcher.find()) {
                int groupCount = matcher.groupCount();
                for (int i = 1; i <= groupCount; i++) {
                    String extractedValue = matcher.group(i);
                    extractedValues.add(convertToActualType(extractedValue));
                }
            }
            return extractedValues.toArray();
        }

        throw new IllegalArgumentException("No Annotated parameter found");
    }

    private static Object convertToActualType(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignore) {}
        return value;
    }

    public static Method findAnnotatedMethod(Class<?> clazz,
                                             Class<? extends Annotation> annotationClass,
                                             String matchValue)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(annotationClass)) {
                Method valueMethod = annotationClass.getDeclaredMethod("value");
                Annotation annotation = method.getAnnotation(annotationClass);
                Object value = valueMethod.invoke(annotation);
                if (regexMatcher(method.getParameters(), value.toString(), matchValue)) {
                    return method;
                }
            }
        }
        throw new NoSuchMethodException(String.format("No request handler found for %s", matchValue));
    }

    private static boolean matches(String pattern, String value) {
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(value);
        return matcher.matches();
    }

    private static boolean regexMatcher(Parameter[] parameters,
                                        String annotationValue,
                                        String matchValue) {
        final List<String> pathVariables = pathVariables(annotationValue);
        var regexPattern = annotationValueBuilder(parameters, pathVariables, annotationValue);
        return matches(regexPattern, matchValue);
    }

    private static String annotationValueBuilder(Parameter[] parameters,
                                                 List<String> pathVariables,
                                                 String annotationValue) {
        for (String pathVariable : pathVariables) {
            var parameter = findParameter(parameters, pathVariable);
            var parameterName = parameter.getAnnotation(PathVariable.class).value();
            var pattern = "\\s+";
            if (parameter.getType().getName().equals("int")) {
                pattern = "\\d+";
            }
            annotationValue = annotationValue
                    .replace("{" + parameterName + "}", "(" + pattern + ")");
        }
        return annotationValue.replace("/", "\\/");
    }

    private static Parameter findParameter(Parameter[] parameters, String name) {
        return Arrays.stream(parameters)
                .filter(parameter -> parameter.getAnnotation(PathVariable.class).value().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> pathVariables(String annotationValue) {
        String pattern = "\\{([^}]+)}";
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(annotationValue);
        List<String> extractedValues = new ArrayList<>();
        while (matcher.find()) {
            String extractedValue = matcher.group(1);
            extractedValues.add(extractedValue);
        }
        return extractedValues;
    }
}
