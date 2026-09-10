package com.piggymetrics.account.client;

import com.piggymetrics.account.domain.Account;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author cdov
 *
 * The statistics-service REST client points at an unreachable URL in the test profile, so
 * every call fails and the MicroProfile Fault Tolerance {@code @Fallback} kicks in - the
 * same observable behaviour as the former Hystrix fallback: no exception is propagated and
 * the failure is logged.
 */
@QuarkusTest
class StatisticsServiceClientFallbackTest {

    @Inject
    @RestClient
    StatisticsServiceClient statisticsServiceClient;

    private final List<LogRecord> records = new CopyOnWriteArrayList<>();

    private final Handler outputCapture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void setup() {
        records.clear();
        Logger.getLogger("").addHandler(outputCapture);
    }

    @AfterEach
    void tearDown() {
        Logger.getLogger("").removeHandler(outputCapture);
    }

    @Test
    void testUpdateStatisticsWithFailFallback() {
        statisticsServiceClient.updateStatistics("test", new Account());

        assertTrue(records.stream().map(StatisticsServiceClientFallbackTest::render)
                        .anyMatch(m -> m.contains("Error during update statistics for account: test")),
                () -> "Expected fallback log message, captured: "
                        + records.stream().map(StatisticsServiceClientFallbackTest::render).toList());
    }

    private static String render(LogRecord record) {
        String message = record.getMessage();
        if (message == null) {
            return "";
        }
        Object[] parameters = record.getParameters();
        if (parameters != null) {
            for (Object parameter : parameters) {
                message = message.replaceFirst("\\{}|\\{\\d+}", String.valueOf(parameter));
            }
        }
        return message;
    }
}
