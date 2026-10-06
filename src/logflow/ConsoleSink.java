package logflow;

public class ConsoleSink implements Sink<LogRecord> {

    @Override
    public void consume(LogRecord item) {
        System.out.println(
            item.clientIp() + " " +
            item.method() + " " +
            item.path() + " " +
            item.status()
        );
    }
}