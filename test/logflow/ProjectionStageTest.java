package logflow;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectionStageTest {

    @Test
    void projectsSelectedFieldsInOrder() throws StageException {

        ProjectionStage stage =
                new ProjectionStage("clientIp", "status", "path");

        CollectingEmitter<String> emitter =
                new CollectingEmitter<>();

        LogRecord record = new LogRecord(
                Instant.parse("2026-10-10T11:00:01Z"),
                "10.0.0.8",
                "GET",
                "/admin",
                403,
                256,
                "Safari",
                Map.of(),
                "raw"
        );

        stage.process(record, emitter);

        assertEquals(1, emitter.getItems().size());
        assertEquals(
                "10.0.0.8 403 /admin",
                emitter.getItems().get(0)
        );
    }
}