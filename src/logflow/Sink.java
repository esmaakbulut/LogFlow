package logflow;

public interface Sink<I> {
    void consume(I item);
}