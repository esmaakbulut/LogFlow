package logflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ErrorHandlingTest {

    @Test
    void deadLetterPolicyRoutesFailedRecord() throws Exception {

        Path output =
                Files.createTempFile("main-output", ".txt");

        Path deadLetter =
                Files.createTempFile("dead-letter", ".txt");

        FileSink sink =
                new FileSink(output);

        DeadLetterSink deadLetterSink =
                new DeadLetterSink(deadLetter);

        RunSummary summary =
                new RunSummary();

        Pipeline pipeline =
                new Pipeline(
                        emitter ->
                                emitter.emit("invalid line"),
                        List.of(
                                new ParserStage()
                        ),
                        sink,
                        Map.of(
                                0,
                                ErrorPolicy.DEAD_LETTER
                        ),
                        deadLetterSink,
                        summary
                );

        pipeline.run();

        assertEquals(
                1,
                summary.getInputCount()
        );

        assertEquals(
                0,
                summary.getOutputCount()
        );

        assertEquals(
                1,
                summary.getDeadLetterCount()
        );

        List<String> deadLetters =
                Files.readAllLines(deadLetter);

        assertEquals(
                1,
                deadLetters.size()
        );

        assertTrue(
                deadLetters.get(0)
                        .contains("ParserStage")
        );

        Files.deleteIfExists(output);
        Files.deleteIfExists(deadLetter);
    }

    @Test
    void skipPolicyDropsFailedRecord() throws Exception {

        Path output =
                Files.createTempFile("skip-output", ".txt");

        FileSink sink =
                new FileSink(output);

        RunSummary summary =
                new RunSummary();

        Pipeline pipeline =
                new Pipeline(
                        emitter ->
                                emitter.emit("invalid line"),
                        List.of(
                                new ParserStage()
                        ),
                        sink,
                        Map.of(
                                0,
                                ErrorPolicy.SKIP
                        ),
                        null,
                        summary
                );

        pipeline.run();

        assertEquals(
                1,
                summary.getInputCount()
        );

        assertEquals(
                0,
                summary.getOutputCount()
        );

        assertEquals(
                0,
                summary.getDeadLetterCount()
        );

        Files.deleteIfExists(output);
    }

    @Test
    void retryPolicyRetriesUntilSuccess() throws Exception {

        Path output =
                Files.createTempFile("retry-output", ".txt");

        FileSink sink =
                new FileSink(output);

        RunSummary summary =
                new RunSummary();

        int[] attempts = {0};

        Stage<String, String> retryStage =
                (input, emitter) -> {

                    attempts[0]++;

                    if (attempts[0] < 3) {
                        throw new StageException(
                                "Temporary failure"
                        );
                    }

                    emitter.emit(input);
                };

        Pipeline pipeline =
                new Pipeline(
                        emitter ->
                                emitter.emit("retry-test"),
                        List.of(
                                retryStage
                        ),
                        sink,
                        Map.of(
                                0,
                                ErrorPolicy.retry(2)
                        ),
                        null,
                        summary
                );

        pipeline.run();

        assertEquals(
                3,
                attempts[0]
        );

        assertEquals(
                1,
                summary.getInputCount()
        );

        assertEquals(
                1,
                summary.getOutputCount()
        );

        List<String> lines =
                Files.readAllLines(output);

        assertEquals(
                1,
                lines.size()
        );

        Files.deleteIfExists(output);
    }
}