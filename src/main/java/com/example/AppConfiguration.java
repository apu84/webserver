package com.example;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.Map;

public class AppConfiguration {
    private final Map<String, Object> yamlData;

    public AppConfiguration() throws IOException {
        try(var inputStream =  getClass().getResourceAsStream("/application.yaml")) {
            final Yaml yaml = new Yaml();
            yamlData = yaml.load(inputStream);
            if (yamlData == null) {
                throw new IllegalArgumentException("Failed to load properties from application.yaml");
            }
        }
    }

    public Object getProperty(String propertyKey) {
        String[] keys = propertyKey.split("\\.");
        Object value = yamlData;
        for (String key : keys) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(key);
            } else {
                throw new NoSuchFieldError(String.format("No property named %s found", propertyKey));
            }
        }
        return value;
    }
}
