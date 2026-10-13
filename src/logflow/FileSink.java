package logflow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSink implements Sink<String> {

    private final Path path;
    private BufferedWriter writer;

    public FileSink(Path path) {
        this.path = path;
    }

    public void open() throws IOException {
        writer = Files.newBufferedWriter(path);
    }

    @Override
    public void consume(String item) {
        try {
            writer.write(item);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Dosyaya yazma hatasi", e);
        }
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}