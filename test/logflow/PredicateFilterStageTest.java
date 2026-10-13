package logflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PredicateFilterStageTest {

    @Test
    void passesMatchingRecord() throws StageException {
        PredicateFilterStage stage =
                new PredicateFilterStage(r -> r.status() >= 400);

        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        LogRecord record = new LogRecord(
                java.time.Instant.now(),
                "10.0.0.8",
                "GET",
                "/admin",
                403,
                100,
                "Safari",
                java.util.Map.of(),
                "raw"
        );

        stage.process(record, emitter);

        assertEquals(1, emitter.getItems().size());
    }

    @Test
    void filtersNonMatchingRecord() throws StageException {
        PredicateFilterStage stage =
                new PredicateFilterStage(r -> r.status() >= 400);

        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        LogRecord record = new LogRecord(
                java.time.Instant.now(),
                "127.0.0.1",
                "GET",
                "/index",
                200,
                100,
                "Safari",
                java.util.Map.of(),
                "raw"
        );

        stage.process(record, emitter);

        assertEquals(0, emitter.getItems().size());
    }
}