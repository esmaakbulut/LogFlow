package logflow;

public class Pipeline {

    private final Source<String> source;
    private final ParserStage parser;
    private final Sink<LogRecord> sink;

    public Pipeline(
        Source<String> source,
        ParserStage parser,
        Sink<LogRecord> sink
    ) {
        this.source = source;
        this.parser = parser;
        this.sink = sink;
    }

    public void run() {
        source.produce(line -> {
            try {
                parser.process(line, sink::consume);
            } catch (StageException e) {
                throw new RuntimeException("Pipeline hatasi", e);
            }
        });

        System.out.println(
            "Hatali satir sayisi: " + parser.getInvalidCount()
        );
    }
}