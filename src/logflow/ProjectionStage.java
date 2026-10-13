package logflow;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectionStage implements Stage<LogRecord, String> {

    private final List<String> fields;

    public ProjectionStage(String... fields) {
        this.fields = Arrays.asList(fields);
    }

    @Override
    public void process(LogRecord r, Emitter<String> out)
            throws StageException {

        String result = fields.stream()
                .map(field -> switch (field) {
                    case "timestamp" -> r.timestamp().toString();
                    case "clientIp" -> r.clientIp();
                    case "method" -> r.method();
                    case "path" -> r.path();
                    case "status" -> String.valueOf(r.status());
                    case "bytes" -> String.valueOf(r.bytes());
                    case "userAgent" -> r.userAgent();
                    default -> "";
                })
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(" "));

        out.emit(result);
    }
}