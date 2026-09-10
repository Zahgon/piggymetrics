package com.piggymetrics.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * was: org.springframework.cloud.config.environment.Environment
 *
 * Wire-compatible representation of the Spring Cloud Config Server response body:
 * {"name":..,"profiles":[..],"label":..,"version":null,"state":null,"propertySources":[..]}
 */
@JsonPropertyOrder({ "name", "profiles", "label", "version", "state", "propertySources" })
public record Environment(
        String name,
        List<String> profiles,
        String label,
        String version,
        String state,
        List<PropertySource> propertySources) {

    public Environment(String name, List<String> profiles, String label, List<PropertySource> propertySources) {
        this(name, profiles, label, null, null, propertySources);
    }
}
