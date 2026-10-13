package logflow;

import java.util.ArrayList;
import java.util.List;

public class PipelineBuilder {

    private final Source<?> source;
    private final List<Stage<?, ?>> stages = new ArrayList<>();

    private PipelineBuilder(Source<?> source) {
        this.source = source;
    }

    public static PipelineBuilder from(Source<?> source) {
        return new PipelineBuilder(source);
    }

    public PipelineBuilder via(Stage<?, ?> stage) {
        stages.add(stage);
        return this;
    }

    public Pipeline to(Sink<?> sink) {
        return new Pipeline(source, stages, sink);
    }
}