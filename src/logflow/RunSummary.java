package logflow;

import java.util.LinkedHashMap;
import java.util.Map;

public class RunSummary {

    private long inputCount;
    private long outputCount;
    private long deadLetterCount;
    private final Map<String, Long> droppedByStage = new LinkedHashMap<>();
    private long startTime;
    private long endTime;

    public void start() {
        startTime = System.nanoTime();
    }

    public void finish() {
        endTime = System.nanoTime();
    }

    public void incrementInput() {
        inputCount++;
    }

    public void incrementOutput() {
        outputCount++;
    }

    public void incrementDeadLetter() {
        deadLetterCount++;
    }

    public void incrementDropped(String stageName) {
        droppedByStage.merge(stageName, 1L, Long::sum);
    }

    public long getInputCount() {
        return inputCount;
    }

    public long getOutputCount() {
        return outputCount;
    }

    public long getDeadLetterCount() {
        return deadLetterCount;
    }

    public long getElapsedMillis() {
        return (endTime - startTime) / 1_000_000;
    }

    public void print() {
        System.out.println();
        System.out.println("=== Run Summary ===");
        System.out.println("Input records: " + inputCount);
        System.out.println("Output records: " + outputCount);
        System.out.println("Dead-letter records: " + deadLetterCount);

        for (Map.Entry<String, Long> entry : droppedByStage.entrySet()) {
            System.out.println(
                    "Dropped at " + entry.getKey() + ": " + entry.getValue()
            );
        }

        System.out.println("Elapsed time: " + getElapsedMillis() + " ms");
    }
}