package logflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileSinkTest {

    @Test
    void writesLinesToFile() throws Exception {

        Path tempFile = Files.createTempFile("logflow-test", ".txt");

        FileSink sink = new FileSink(tempFile);

        sink.open();
        sink.consume("first line");
        sink.consume("second line");
        sink.close();

        List<String> lines = Files.readAllLines(tempFile);

        assertEquals(2, lines.size());
        assertEquals("first line", lines.get(0));
        assertEquals("second line", lines.get(1));

        Files.deleteIfExists(tempFile);
    }
}