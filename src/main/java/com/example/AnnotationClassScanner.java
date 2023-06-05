package com.example;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class AnnotationClassScanner {
    public static List<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationClass)
            throws IOException, ClassNotFoundException {
        List<Class<?>> annotatedClasses = new ArrayList<>();

        String packageName = annotationClass.getPackage().getName();
        String packagePath = packageName.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packagePath);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());

            if (directory.exists()) {
                String[] files = directory.list();

                if (files != null) {
                    for (String file : files) {
                        String className = packageName + '.' + file.substring(0, file.length() - 6); // .class extension removed
                        Class<?> loadedClass = Class.forName(className);

                        if (loadedClass.isAnnotationPresent(annotationClass)) {
                            annotatedClasses.add(loadedClass);
                        }
                    }
                }
            }
        }

        return annotatedClasses;
    }
}

