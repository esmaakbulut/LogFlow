package logflow;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Kullanim: java logflow.Main <dosya_yolu>");
            return;
        }

        Path filePath = Path.of(args[0]);

        Source<String> source = new FileLineSource(filePath);
        ParserStage parser = new ParserStage();
        Sink<LogRecord> sink = new ConsoleSink();

        Pipeline pipeline = new Pipeline(source, parser, sink);

        pipeline.run();
    }
}