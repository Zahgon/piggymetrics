package com.piggymetrics.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * was: spring-cloud-config-server, `native` profile with
 * spring.cloud.config.server.native.search-locations=classpath:/shared
 *
 * Loads YAML documents from the classpath directory {@code shared/} and flattens them
 * into the dotted-key {@code source} maps that the Config Server REST contract exposes.
 */
@ApplicationScoped
public class SharedConfigRepository {

    private static final String LOCATION = "shared/";
    private static final String CLASSPATH_PREFIX = "classpath:/shared/";
    private static final String DEFAULT_APPLICATION = "application";

    /** Guards against classpath traversal via the {application} path parameter. */
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._-]+");

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    /**
     * Builds the property sources for an application, reproducing Spring's first-wins
     * precedence: the application-specific document comes first, {@code application.yml}
     * last. Empty (0-byte) documents contribute no property source at all.
     */
    public List<PropertySource> propertySourcesFor(String application) {
        List<PropertySource> sources = new ArrayList<>(2);
        if (application != null && !DEFAULT_APPLICATION.equals(application)) {
            PropertySource specific = load(application + ".yml");
            if (specific != null) {
                sources.add(specific);
            }
        }
        PropertySource shared = load(DEFAULT_APPLICATION + ".yml");
        if (shared != null) {
            sources.add(shared);
        }
        return sources;
    }

    private PropertySource load(String fileName) {
        if (!SAFE_NAME.matcher(fileName).matches()) {
            return null;
        }
        Map<String, Object> parsed = read(fileName);
        if (parsed == null || parsed.isEmpty()) {
            return null;
        }
        Map<String, Object> flattened = new LinkedHashMap<>();
        flatten("", parsed, flattened);
        if (flattened.isEmpty()) {
            return null;
        }
        return new PropertySource(CLASSPATH_PREFIX + fileName, flattened);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> read(String fileName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = SharedConfigRepository.class.getClassLoader();
        }
        try (InputStream in = loader.getResourceAsStream(LOCATION + fileName)) {
            if (in == null) {
                return null;
            }
            byte[] content = in.readAllBytes();
            // A 0-byte / whitespace-only document yields no property source at all.
            if (new String(content, StandardCharsets.UTF_8).isBlank()) {
                return null;
            }
            return yaml.readValue(content, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + CLASSPATH_PREFIX + fileName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> node, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> map) {
                if (map.isEmpty()) {
                    target.put(key, new LinkedHashMap<>());
                } else {
                    flatten(key, (Map<String, Object>) map, target);
                }
            } else if (value instanceof List<?> list) {
                flattenList(key, list, target);
            } else {
                target.put(key, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void flattenList(String key, List<?> list, Map<String, Object> target) {
        if (list.isEmpty()) {
            target.put(key, new ArrayList<>());
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            String indexed = key + "[" + i + "]";
            Object value = list.get(i);
            if (value instanceof Map<?, ?> map) {
                flatten(indexed, (Map<String, Object>) map, target);
            } else if (value instanceof List<?> nested) {
                flattenList(indexed, nested, target);
            } else {
                target.put(indexed, value);
            }
        }
    }
}
