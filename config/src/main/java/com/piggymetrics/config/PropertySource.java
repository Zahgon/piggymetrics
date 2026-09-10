package com.piggymetrics.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * was: org.springframework.cloud.config.environment.PropertySource
 *
 * One flattened YAML document, e.g.
 * {"name":"classpath:/shared/account-service.yml","source":{"server.port":6000,...}}
 */
@JsonPropertyOrder({ "name", "source" })
public record PropertySource(String name, Map<String, Object> source) {
}
