package logflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PipelineIntegrationTest {

    @Test
    void pipelineFiltersProjectsAndWrites() throws Exception {

        Path input = Path.of("test/logflow/fixture.log");
        Path output = Files.createTempFile("logflow-output", ".txt");

        FileSink sink = new FileSink(output);

        Pipeline pipeline = PipelineBuilder
                .from(new FileLineSource(input))
                .via(new ParserStage())
                .via(new PredicateFilterStage(r -> r.status() >= 400))
                .via(new ProjectionStage("clientIp", "status", "path"))
                .to(sink);

        sink.open();

        try {
            pipeline.run();
        } finally {
            sink.close();
        }

        List<String> lines = Files.readAllLines(output);

        assertEquals(1, lines.size());
        assertEquals("10.0.0.8 403 /admin", lines.get(0));

        Files.deleteIfExists(output);
    }
}