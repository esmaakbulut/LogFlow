package logflow;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationStageTest {

    private ValidationStage createStage() {
        return new ValidationStage(
                Set.of("clientIp", "path"),
                0,
                10_000_000,
                Instant.parse("2000-01-01T00:00:00Z"),
                Instant.parse("2030-12-31T23:59:59Z"),
                10_000,
                true
        );
    }

    @Test
    void acceptsValidRecord() {

        LogRecord record = new LogRecord(
                Instant.parse("2026-10-15T10:15:32Z"),
                "192.0.2.10",
                "GET",
                "/index.html",
                200,
                1234,
                "Mozilla/5.0",
                new HashMap<>(),
                "valid line"
        );

        ValidationStage stage = createStage();

        assertDoesNotThrow(
                () -> stage.process(
                        record,
                        item -> {
                        }
                )
        );
    }

    @Test
    void rejectsNegativeBytes() {

        LogRecord record = new LogRecord(
                Instant.parse("2026-10-15T10:15:32Z"),
                "192.0.2.10",
                "GET",
                "/download",
                200,
                -45,
                "test-agent",
                new HashMap<>(),
                "negative bytes"
        );

        assertThrows(
                StageException.class,
                () -> createStage().process(
                        record,
                        item -> {
                        }
                )
        );
    }

    @Test
    void rejectsOldTimestamp() {

        LogRecord record = new LogRecord(
                Instant.parse("1970-01-01T00:00:00Z"),
                "192.0.2.10",
                "GET",
                "/old",
                200,
                10,
                "test-agent",
                new HashMap<>(),
                "old timestamp"
        );

        assertThrows(
                StageException.class,
                () -> createStage().process(
                        record,
                        item -> {
                        }
                )
        );
    }

    @Test
    void rejectsInvalidIp() {

        LogRecord record = new LogRecord(
                Instant.parse("2026-10-15T10:15:32Z"),
                "999.999.999.999",
                "GET",
                "/index.html",
                200,
                100,
                "test-agent",
                new HashMap<>(),
                "invalid ip"
        );

        assertThrows(
                StageException.class,
                () -> createStage().process(
                        record,
                        item -> {
                        }
                )
        );
    }

    @Test
    void rejectsNonAsciiUserAgent() {

        LogRecord record = new LogRecord(
                Instant.parse("2026-10-15T10:15:32Z"),
                "192.0.2.10",
                "GET",
                "/bad-encoding",
                200,
                50,
                "ÿþú",
                new HashMap<>(),
                "bad encoding"
        );

        assertThrows(
                StageException.class,
                () -> createStage().process(
                        record,
                        item -> {
                        }
                )
        );
    }

    @Test
    void rejectsVeryLongLine() {

        String longLine = "A".repeat(20_000);

        LogRecord record = new LogRecord(
                Instant.parse("2026-10-15T10:15:32Z"),
                "192.0.2.10",
                "GET",
                "/huge",
                200,
                1,
                "test-agent",
                new HashMap<>(),
                longLine
        );

        assertThrows(
                StageException.class,
                () -> createStage().process(
                        record,
                        item -> {
                        }
                )
        );
    }
}