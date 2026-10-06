package logflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserStageTest {

    @Test
    void validLine() throws StageException {
        ParserStage parser = new ParserStage();
        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        String line =
            "127.0.0.1 - - [10/Oct/2026:13:55:36 +0300] " +
            "\"GET /index.html HTTP/1.1\" 200 1234 \"Mozilla Firefox\"";

        parser.process(line, emitter);

        assertEquals(1, emitter.getItems().size());

        LogRecord record = emitter.getItems().get(0);

        assertEquals("127.0.0.1", record.clientIp());
        assertEquals("GET", record.method());
        assertEquals("/index.html", record.path());
        assertEquals(200, record.status());
    }

    @Test
    void missingField() throws StageException {
        ParserStage parser = new ParserStage();
        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        parser.process(
            "127.0.0.1 - - [10/Oct/2026:13:55:36 +0300]",
            emitter
        );

        assertEquals(0, emitter.getItems().size());
        assertEquals(1, parser.getInvalidCount());
    }

    @Test
    void invalidTimestamp() throws StageException {
        ParserStage parser = new ParserStage();
        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        String line =
            "127.0.0.1 - - [HATALI-TARIH] " +
            "\"GET /index.html HTTP/1.1\" 200 1234 \"Safari\"";

        parser.process(line, emitter);

        assertEquals(0, emitter.getItems().size());
        assertEquals(1, parser.getInvalidCount());
    }

    @Test
    void invalidStatusCode() throws StageException {
        ParserStage parser = new ParserStage();
        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        String line =
            "127.0.0.1 - - [10/Oct/2026:13:55:36 +0300] " +
            "\"GET /index.html HTTP/1.1\" ABC 1234 \"Safari\"";

        parser.process(line, emitter);

        assertEquals(0, emitter.getItems().size());
        assertEquals(1, parser.getInvalidCount());
    }

    @Test
    void emptyLine() throws StageException {
        ParserStage parser = new ParserStage();
        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        parser.process("", emitter);

        assertEquals(0, emitter.getItems().size());
        assertEquals(1, parser.getInvalidCount());
    }

    @Test
    void extraSpaces() throws StageException {
        ParserStage parser = new ParserStage();
        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        String line =
            "127.0.0.1    -    -    [10/Oct/2026:13:55:36 +0300]    " +
            "\"GET /index.html HTTP/1.1\"    200    1234    \"Safari\"";

        parser.process(line, emitter);

        assertEquals(1, emitter.getItems().size());
    }

    @Test
    void userAgentWithSpaces() throws StageException {
        ParserStage parser = new ParserStage();
        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        String line =
            "127.0.0.1 - - [10/Oct/2026:13:55:36 +0300] " +
            "\"GET /index.html HTTP/1.1\" 200 1234 " +
            "\"Mozilla Firefox Browser\"";

        parser.process(line, emitter);

        assertEquals(1, emitter.getItems().size());

        assertEquals(
            "Mozilla Firefox Browser",
            emitter.getItems().get(0).userAgent()
        );
    }

    @Test
    void queryString() throws StageException {
        ParserStage parser = new ParserStage();
        CollectingEmitter<LogRecord> emitter = new CollectingEmitter<>();

        String line =
            "127.0.0.1 - - [10/Oct/2026:13:55:36 +0300] " +
            "\"GET /products?id=5 HTTP/1.1\" 200 2048 \"Chrome\"";

        parser.process(line, emitter);

        assertEquals(1, emitter.getItems().size());

        assertEquals(
            "/products?id=5",
            emitter.getItems().get(0).path()
        );
    }
}