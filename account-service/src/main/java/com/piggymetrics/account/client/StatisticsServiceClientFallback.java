package com.piggymetrics.account.client;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cdov
 *
 * Replacement of the former Hystrix {@code fallback} bean: a MicroProfile Fault Tolerance
 * {@link FallbackHandler} bound to {@code StatisticsServiceClient#updateStatistics}.
 * Behaviour is unchanged - the failure is swallowed and logged.
 */
public class StatisticsServiceClientFallback implements FallbackHandler<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsServiceClientFallback.class);

    @Override
    public Void handle(ExecutionContext context) {
        Object[] parameters = context.getParameters();
        Object accountName = (parameters != null && parameters.length > 0) ? parameters[0] : null;
        LOGGER.error("Error during update statistics for account: {}", accountName);
        return null;
    }
}
