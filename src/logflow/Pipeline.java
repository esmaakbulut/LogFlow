package logflow;

import java.util.List;

public class Pipeline {

    private final Source<?> source;
    private final List<Stage<?, ?>> stages;
    private final Sink<?> sink;

    public Pipeline(
            Source<?> source,
            List<Stage<?, ?>> stages,
            Sink<?> sink) {

        this.source = source;
        this.stages = stages;
        this.sink = sink;
    }

    public void run() {

        try {
            sink.open();

            source.produce(
                    item -> processStage(0, item)
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Pipeline execution failed",
                    e
            );

        } finally {

            try {
                sink.close();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to close sink",
                        e
                );
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processStage(
            int index,
            Object item) {

        try {

            if (index >= stages.size()) {
                ((Sink) sink).consume(item);
                return;
            }

            Stage stage = stages.get(index);

            stage.process(
                    item,
                    next -> processStage(
                            index + 1,
                            next
                    )
            );

        } catch (StageException e) {

            throw new RuntimeException(
                    "Pipeline stage failed",
                    e
            );
        }
    }
}