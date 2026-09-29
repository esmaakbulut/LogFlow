package logflow;

public interface Emitter<T> {
    void emit(T item);
}