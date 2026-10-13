package logflow;

import java.util.function.Predicate;

public class PredicateFilterStage implements Stage<LogRecord, LogRecord> {

    private final Predicate<LogRecord> predicate;

    public PredicateFilterStage(Predicate<LogRecord> predicate) {
        this.predicate = predicate;
    }

    @Override
    public void process(LogRecord input, Emitter<LogRecord> out)
            throws StageException {

        if (predicate.test(input)) {
            out.emit(input);
        }
    }
}