package logflow;

import java.util.ArrayList;
import java.util.List;

public class Pipeline {

    private final Source<String> source;
    private final Sink<String> sink;
    private final List<Stage<String, String>> stages = new ArrayList<>();

    public Pipeline(Source<String> source, Sink<String> sink) {
        this.source = source;
        this.sink = sink;
    }

    public void addStage(Stage<String, String> stage) {
        stages.add(stage);
    }

    public void run() {
        source.produce(item -> processStage(0, item));
    }

    private void processStage(int index, String item) {

        if (index >= stages.size()) {
            sink.consume(item);
            return;
        }

        Stage<String, String> stage = stages.get(index);

        try {
            stage.process(
                item,
                output -> processStage(index + 1, output)
            );
        } catch (StageException e) {
            throw new RuntimeException("Pipeline hatasi", e);
        }
    }
}