package logflow;

import java.util.ArrayList;
import java.util.List;

public class CollectingEmitter<T> implements Emitter<T> {

    private final List<T> items = new ArrayList<>();

    @Override
    public void emit(T item) {
        items.add(item);
    }

    public List<T> getItems() {
        return items;
    }
}