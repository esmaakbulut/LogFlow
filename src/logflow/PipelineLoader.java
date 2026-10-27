package logflow;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PipelineLoader {

    private final StageRegistry registry;

    public PipelineLoader(StageRegistry registry) {
        this.registry = registry;
    }

    @SuppressWarnings("unchecked")
    public Pipeline load(Path configPath) throws IOException {

        Yaml yaml = new Yaml();

        try (InputStream input = Files.newInputStream(configPath)) {

            Map<String, Object> root = yaml.load(input);

            if (root == null || !root.containsKey("pipeline")) {
                throw new IllegalArgumentException(
                        "Missing required section: pipeline"
                );
            }

            Map<String, Object> pipeline =
                    (Map<String, Object>) root.get("pipeline");

            if (!pipeline.containsKey("source")) {
                throw new IllegalArgumentException(
                        "Missing required section: source"
                );
            }

            if (!pipeline.containsKey("stages")) {
                throw new IllegalArgumentException(
                        "Missing required section: stages"
                );
            }

            if (!pipeline.containsKey("sink")) {
                throw new IllegalArgumentException(
                        "Missing required section: sink"
                );
            }

            return buildPipeline(pipeline);
        }
    }

    @SuppressWarnings("unchecked")
    private Pipeline buildPipeline(Map<String, Object> config) {

        Map<String, Object> sourceConfig =
                (Map<String, Object>) config.get("source");

        Map<String, Object> sourceParams =
                (Map<String, Object>) sourceConfig.get("params");

        if (sourceParams == null
                || !(sourceParams.get("path") instanceof String)) {

            throw new IllegalArgumentException(
                    "Source parameter 'path' is required and must be a string"
            );
        }

        Source<?> source =
                new FileLineSource(
                        Path.of((String) sourceParams.get("path"))
                );

        List<Map<String, Object>> stageConfigs =
                (List<Map<String, Object>>) config.get("stages");

        if (stageConfigs == null || stageConfigs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Pipeline must contain at least one stage"
            );
        }

        List<Stage<?, ?>> stages = new ArrayList<>();

        for (Map<String, Object> stageConfig : stageConfigs) {

            Object typeObject = stageConfig.get("type");

            if (!(typeObject instanceof String)) {
                throw new IllegalArgumentException(
                        "Stage type is required and must be a string"
                );
            }

            String type = (String) typeObject;

            Map<String, Object> params =
                    (Map<String, Object>) stageConfig.get("params");

            if (params == null) {
                params = Map.of();
            }

            StageFactory factory = registry.getFactory(type);
            stages.add(factory.create(params));
        }

        Map<String, Object> sinkConfig =
                (Map<String, Object>) config.get("sink");

        Map<String, Object> sinkParams =
                (Map<String, Object>) sinkConfig.get("params");

        if (sinkParams == null
                || !(sinkParams.get("path") instanceof String)) {

            throw new IllegalArgumentException(
                    "Sink parameter 'path' is required and must be a string"
            );
        }

        Sink<?> sink =
                new FileSink(
                        Path.of((String) sinkParams.get("path"))
                );

        return new Pipeline(source, stages, sink);
    }
}