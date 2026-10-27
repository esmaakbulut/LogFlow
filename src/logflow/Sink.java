package logflow;

public interface Sink<T> {

    default void open() throws Exception {
        // Varsayilan olarak bir sey yapmaz.
    }

    void consume(T item);

    default void close() throws Exception {
        // Varsayilan olarak bir sey yapmaz.
    }
}