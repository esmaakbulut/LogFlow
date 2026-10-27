package logflow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeadLetterSink implements Sink<FailedRecord> {

    private final Path path;
    private BufferedWriter writer;

    public DeadLetterSink(Path path) {
        this.path = path;
    }

    @Override
    public void open() throws IOException {
        writer = Files.newBufferedWriter(path);
    }

    @Override
    public void consume(FailedRecord record) {
        try {
            writer.write(
                    "time=" + record.timestamp()
                            + " | stage=" + record.stageName()
                            + " | error=" + record.exception()
                            + " | raw=" + record.rawInput()
            );
            writer.newLine();
        } catch (IOException e) {
            throw new PipelineException(
                    "Failed to write dead-letter record",
                    e
            );
        }
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}