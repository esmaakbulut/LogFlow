package logflow;

public class ErrorPolicy {

    public enum Type {
        FAIL_FAST,
        SKIP,
        DEAD_LETTER,
        RETRY
    }

    public static final ErrorPolicy FAIL_FAST =
            new ErrorPolicy(Type.FAIL_FAST, 0);

    public static final ErrorPolicy SKIP =
            new ErrorPolicy(Type.SKIP, 0);

    public static final ErrorPolicy DEAD_LETTER =
            new ErrorPolicy(Type.DEAD_LETTER, 0);

    private final Type type;
    private final int retryCount;

    private ErrorPolicy(Type type, int retryCount) {
        this.type = type;
        this.retryCount = retryCount;
    }

    public static ErrorPolicy retry(int count) {

        if (count < 1) {
            throw new IllegalArgumentException(
                    "Retry count must be at least 1"
            );
        }

        return new ErrorPolicy(
                Type.RETRY,
                count
        );
    }

    public Type getType() {
        return type;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public static ErrorPolicy parse(String text) {

        if (text == null || text.isBlank()) {
            return FAIL_FAST;
        }

        String value = text.trim().toUpperCase();

        return switch (value) {

            case "FAIL_FAST" -> FAIL_FAST;

            case "SKIP" -> SKIP;

            case "DEAD_LETTER" -> DEAD_LETTER;

            default -> {

                if (value.startsWith("RETRY(")
                        && value.endsWith(")")) {

                    String numberText =
                            value.substring(
                                    6,
                                    value.length() - 1
                            );

                    try {

                        int count =
                                Integer.parseInt(numberText);

                        yield retry(count);

                    } catch (NumberFormatException e) {

                        throw new IllegalArgumentException(
                                "Invalid RETRY policy: " + text
                        );
                    }
                }

                throw new IllegalArgumentException(
                        "Unknown error policy: " + text
                );
            }
        };
    }
}