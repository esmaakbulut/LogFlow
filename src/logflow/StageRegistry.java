package logflow;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StageRegistry {

    private final Map<String, StageFactory> factories = new HashMap<>();

    public StageRegistry() {

        register("parse", params -> new ParserStage());

        register("filter", params -> {
            String field = requireString(params, "field");
            String op = requireString(params, "op");
            Object value = params.get("value");

            if (value == null) {
                throw new IllegalArgumentException(
                        "Filter parameter 'value' is required"
                );
            }

            if ("status".equals(field) && ">=".equals(op)) {

                if (!(value instanceof Integer)) {
                    throw new IllegalArgumentException(
                            "Filter 'value' must be an integer for status"
                    );
                }

                int threshold = (Integer) value;

                return new PredicateFilterStage(
                        record -> record.status() >= threshold
                );
            }

            if ("path".equals(field) && "startsWith".equals(op)) {

                if (!(value instanceof String)) {
                    throw new IllegalArgumentException(
                            "Filter 'value' must be a string for path"
                    );
                }

                String prefix = (String) value;

                return new PredicateFilterStage(
                        record -> record.path().startsWith(prefix)
                );
            }

            throw new IllegalArgumentException(
                    "Unsupported filter: " + field + " " + op
            );
        });

        register("project", params -> {

            Object fieldsObject = params.get("fields");

            if (!(fieldsObject instanceof List<?> fields)) {
                throw new IllegalArgumentException(
                        "Project parameter 'fields' is required and must be a list"
                );
            }

            String[] fieldNames = fields.stream()
                    .map(Object::toString)
                    .toArray(String[]::new);

            return new ProjectionStage(fieldNames);
        });

        register("validate", params -> {

            Set<String> requiredFields = new HashSet<>();

            Object requiredObject = params.get("requiredFields");

            if (requiredObject instanceof List<?> list) {
                for (Object item : list) {
                    requiredFields.add(item.toString());
                }
            }

            long minBytes = requireLong(params, "minBytes");
            long maxBytes = requireLong(params, "maxBytes");

            String minTimestampText =
                    requireString(params, "minTimestamp");

            String maxTimestampText =
                    requireString(params, "maxTimestamp");

            int maxLineLength =
                    requireInt(params, "maxLineLength");

            boolean asciiUserAgentOnly =
                    requireBoolean(params, "asciiUserAgentOnly");

            Instant minTimestamp =
                    Instant.parse(minTimestampText);

            Instant maxTimestamp =
                    Instant.parse(maxTimestampText);

            return new ValidationStage(
                    requiredFields,
                    minBytes,
                    maxBytes,
                    minTimestamp,
                    maxTimestamp,
                    maxLineLength,
                    asciiUserAgentOnly
            );
        });
    }

    public void register(String type, StageFactory factory) {
        factories.put(type, factory);
    }

    public StageFactory getFactory(String type) {

        StageFactory factory = factories.get(type);

        if (factory == null) {
            throw new IllegalArgumentException(
                    "Unknown stage type: " + type
            );
        }

        return factory;
    }

    public boolean contains(String type) {
        return factories.containsKey(type);
    }

    private String requireString(
            Map<String, Object> params,
            String key) {

        Object value = params.get(key);

        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    "Parameter '" + key
                            + "' is required and must be a string"
            );
        }

        return (String) value;
    }

    private long requireLong(
            Map<String, Object> params,
            String key) {

        Object value = params.get(key);

        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "Parameter '" + key
                            + "' is required and must be a number"
            );
        }

        return ((Number) value).longValue();
    }

    private int requireInt(
            Map<String, Object> params,
            String key) {

        Object value = params.get(key);

        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "Parameter '" + key
                            + "' is required and must be a number"
            );
        }

        return ((Number) value).intValue();
    }

    private boolean requireBoolean(
            Map<String, Object> params,
            String key) {

        Object value = params.get(key);

        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(
                    "Parameter '" + key
                            + "' is required and must be true or false"
            );
        }

        return (Boolean) value;
    }
}