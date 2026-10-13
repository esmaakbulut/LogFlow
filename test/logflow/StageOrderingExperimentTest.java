package logflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StageOrderingExperimentTest {

    @Test
    void compareFilterOrders() throws Exception {

        Path input = Path.of("data/access-small.log");

        Path output1 = Files.createTempFile("order1", ".txt");
        Path output2 = Files.createTempFile("order2", ".txt");

        long start1 = System.nanoTime();
        runFirstOrder(input, output1);
        long time1 = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        runSecondOrder(input, output2);
        long time2 = System.nanoTime() - start2;

        List<String> result1 = Files.readAllLines(output1);
        List<String> result2 = Files.readAllLines(output2);

        assertEquals(result1, result2);

        System.out.println("Order 1 time: " + time1 + " ns");
        System.out.println("Order 2 time: " + time2 + " ns");
        System.out.println("Same result: " + result1.equals(result2));

        Files.deleteIfExists(output1);
        Files.deleteIfExists(output2);
    }

    private void runFirstOrder(Path input, Path output) throws Exception {

        FileSink sink = new FileSink(output);

        Pipeline pipeline = PipelineBuilder
                .from(new FileLineSource(input))
                .via(new ParserStage())
                .via(new PredicateFilterStage(r -> r.status() >= 400))
                .via(new PredicateFilterStage(r -> r.path().startsWith("/admin")))
                .via(new ProjectionStage("clientIp", "status", "path"))
                .to(sink);

        sink.open();
        try {
            pipeline.run();
        } finally {
            sink.close();
        }
    }

    private void runSecondOrder(Path input, Path output) throws Exception {

        FileSink sink = new FileSink(output);

        Pipeline pipeline = PipelineBuilder
                .from(new FileLineSource(input))
                .via(new ParserStage())
                .via(new PredicateFilterStage(r -> r.path().startsWith("/admin")))
                .via(new PredicateFilterStage(r -> r.status() >= 400))
                .via(new ProjectionStage("clientIp", "status", "path"))
                .to(sink);

        sink.open();
        try {
            pipeline.run();
        } finally {
            sink.close();
        }
    }
}