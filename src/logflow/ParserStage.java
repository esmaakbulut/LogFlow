package logflow;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserStage implements Stage<String, LogRecord> {

    private int invalidCount = 0;

    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\S+)\\s+\\S+\\s+\\S+\\s+" +
        "\\[([^\\]]+)\\]\\s+" +
        "\"(\\S+)\\s+(\\S+)(?:\\s+HTTP/[^\\\"]+)?\"\\s+" +
        "(\\d{3})\\s+" +
        "(\\d+|-)\\s*" +
        "(?:\"([^\"]*)\")?\\s*$"
    );

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern(
            "dd/MMM/yyyy:HH:mm:ss Z",
            Locale.ENGLISH
        );

    @Override
    public void process(String input, Emitter<LogRecord> out)
            throws StageException {

        if (input == null || input.isBlank()) {
            invalidCount++;
            return;
        }

        try {
            Matcher matcher = LOG_PATTERN.matcher(input.trim());

            if (!matcher.matches()) {
                invalidCount++;
                return;
            }

            String clientIp = matcher.group(1);

            Instant timestamp =
                ZonedDateTime.parse(
                    matcher.group(2),
                    TIME_FORMAT
                ).toInstant();

            String method = matcher.group(3);
            String path = matcher.group(4);

            int status = Integer.parseInt(
                matcher.group(5)
            );

            long bytes =
                matcher.group(6).equals("-")
                    ? 0
                    : Long.parseLong(matcher.group(6));

            String userAgent =
                matcher.group(7) == null
                    ? ""
                    : matcher.group(7);

            LogRecord record = new LogRecord(
                timestamp,
                clientIp,
                method,
                path,
                status,
                bytes,
                userAgent,
                new HashMap<>(),
                input
            );

            out.emit(record);

        } catch (Exception e) {
            invalidCount++;
        }
    }

    public int getInvalidCount() {
        return invalidCount;
    }
}