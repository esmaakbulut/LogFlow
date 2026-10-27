package logflow;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
                getParams(sourceConfig);

        String sourcePath =
                requireString(sourceParams, "path");

        Source<?> source =
                new FileLineSource(Path.of(sourcePath));

        List<Map<String, Object>> stageConfigs =
                (List<Map<String, Object>>) config.get("stages");

        if (stageConfigs == null || stageConfigs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Pipeline must contain at least one stage"
            );
        }

        List<Stage<?, ?>> stages =
                new ArrayList<>();

        Map<Integer, ErrorPolicy> policies =
                new HashMap<>();

        for (int i = 0; i < stageConfigs.size(); i++) {

            Map<String, Object> stageConfig =
                    stageConfigs.get(i);

            String type =
                    requireString(stageConfig, "type");

            Map<String, Object> params =
                    getParams(stageConfig);

            StageFactory factory =
                    registry.getFactory(type);

            Stage<?, ?> stage =
                    factory.create(params);

            stages.add(stage);

            String policyText =
                    getOptionalString(
                            stageConfig,
                            "onError",
                            "FAIL_FAST"
                    );

            ErrorPolicy policy =
                    ErrorPolicy.parse(policyText);

            policies.put(i, policy);
        }

        Map<String, Object> sinkConfig =
                (Map<String, Object>) config.get("sink");

        Map<String, Object> sinkParams =
                getParams(sinkConfig);

        String sinkPath =
                requireString(sinkParams, "path");

        Sink<?> sink =
                new FileSink(Path.of(sinkPath));

        DeadLetterSink deadLetterSink =
                createDeadLetterSink(
                        config,
                        policies
                );

        RunSummary summary =
                new RunSummary();

        return new Pipeline(
                source,
                stages,
                sink,
                policies,
                deadLetterSink,
                summary
        );
    }

    @SuppressWarnings("unchecked")
    private DeadLetterSink createDeadLetterSink(
            Map<String, Object> config,
            Map<Integer, ErrorPolicy> policies) {

        boolean needsDeadLetter =
                policies.values()
                        .stream()
                        .anyMatch(
                                policy ->
                                        policy.getType()
                                                == ErrorPolicy.Type.DEAD_LETTER
                        );

        if (!needsDeadLetter) {
            return null;
        }

        Object deadLetterObject =
                config.get("deadLetter");

        if (!(deadLetterObject instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                    "deadLetter configuration is required "
                            + "when DEAD_LETTER policy is used"
            );
        }

        Map<String, Object> deadLetterConfig =
                (Map<String, Object>) deadLetterObject;

        Map<String, Object> params =
                getParams(deadLetterConfig);

        String path =
                requireString(params, "path");

        return new DeadLetterSink(
                Path.of(path)
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getParams(
            Map<String, Object> config) {

        Object paramsObject =
                config.get("params");

        if (paramsObject == null) {
            return Map.of();
        }

        if (!(paramsObject instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                    "params must be a map"
            );
        }

        return (Map<String, Object>) paramsObject;
    }

    private String requireString(
            Map<String, Object> map,
            String key) {

        Object value =
                map.get(key);

        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    "Parameter '" + key
                            + "' is required and must be a string"
            );
        }

        return (String) value;
    }

    private String getOptionalString(
            Map<String, Object> map,
            String key,
            String defaultValue) {

        Object value =
                map.get(key);

        if (value == null) {
            return defaultValue;
        }

        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    "Parameter '" + key
                            + "' must be a string"
            );
        }

        return (String) value;
    }
}