package logflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileLineSource implements Source<String> {

    private final Path filePath;

    public FileLineSource(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void produce(Emitter<String> out) {
        try {
            Files.lines(filePath).forEach(out::emit);
        } catch (IOException e) {
            throw new RuntimeException("Dosya okunamadi: " + filePath, e);
        }
    }
}