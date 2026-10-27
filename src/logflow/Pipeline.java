package logflow;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class Pipeline {

    private final Source<?> source;
    private final List<Stage<?, ?>> stages;
    private final Sink<?> sink;

    private final Map<Integer, ErrorPolicy> errorPolicies;
    private final DeadLetterSink deadLetterSink;
    private final RunSummary summary;

    public Pipeline(
            Source<?> source,
            List<Stage<?, ?>> stages,
            Sink<?> sink) {

        this(
                source,
                stages,
                sink,
                Map.of(),
                null,
                new RunSummary()
        );
    }

    public Pipeline(
            Source<?> source,
            List<Stage<?, ?>> stages,
            Sink<?> sink,
            Map<Integer, ErrorPolicy> errorPolicies,
            DeadLetterSink deadLetterSink,
            RunSummary summary) {

        this.source = source;
        this.stages = stages;
        this.sink = sink;
        this.errorPolicies = errorPolicies;
        this.deadLetterSink = deadLetterSink;
        this.summary = summary;
    }

    public RunSummary getSummary() {
        return summary;
    }

    public void run() {

        summary.start();

        try {

            sink.open();

            if (deadLetterSink != null) {
                deadLetterSink.open();
            }

            source.produce(item -> {

                summary.incrementInput();

                String rawInput =
                        String.valueOf(item);

                processStage(
                        0,
                        item,
                        rawInput
                );
            });

        } catch (PipelineException e) {

            throw e;

        } catch (Exception e) {

            throw new PipelineException(
                    "Pipeline execution failed",
                    e
            );

        } finally {

            try {

                sink.close();

                if (deadLetterSink != null) {
                    deadLetterSink.close();
                }

            } catch (Exception e) {

                throw new PipelineException(
                        "Failed to close pipeline output",
                        e
                );

            } finally {

                summary.finish();
                summary.print();
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processStage(
            int index,
            Object item,
            String rawInput) {

        if (index >= stages.size()) {

            try {

                ((Sink) sink).consume(item);
                summary.incrementOutput();

            } catch (Exception e) {

                throw new PipelineException(
                        "Failed to write output",
                        e
                );
            }

            return;
        }

        Stage stage =
                stages.get(index);

        ErrorPolicy policy =
                errorPolicies.getOrDefault(
                        index,
                        ErrorPolicy.FAIL_FAST
                );

        if (policy.getType()
                == ErrorPolicy.Type.RETRY) {

            processWithRetry(
                    index,
                    stage,
                    item,
                    rawInput,
                    policy
            );

            return;
        }

        try {

            stage.process(
                    item,
                    next -> processStage(
                            index + 1,
                            next,
                            rawInput
                    )
            );

        } catch (StageException e) {

            handleStageError(
                    index,
                    stage,
                    rawInput,
                    e,
                    policy
            );
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processWithRetry(
            int index,
            Stage stage,
            Object item,
            String rawInput,
            ErrorPolicy policy) {

        int retryCount =
                policy.getRetryCount();

        int attempt = 0;

        while (true) {

            try {

                stage.process(
                        item,
                        next -> processStage(
                                index + 1,
                                next,
                                rawInput
                        )
                );

                return;

            } catch (StageException e) {

                if (attempt >= retryCount) {

                    throw new PipelineException(
                            "Stage failed after "
                                    + retryCount
                                    + " retries: "
                                    + stage.getClass()
                                            .getSimpleName(),
                            e
                    );
                }

                attempt++;
            }
        }
    }

    private void handleStageError(
            int index,
            Stage<?, ?> stage,
            String rawInput,
            StageException exception,
            ErrorPolicy policy) {

        String stageName =
                stage.getClass().getSimpleName();

        switch (policy.getType()) {

            case FAIL_FAST -> throw new PipelineException(
                    "Stage failed: " + stageName,
                    exception
            );

            case SKIP ->
                    summary.incrementDropped(stageName);

            case DEAD_LETTER -> {

                if (deadLetterSink == null) {

                    throw new PipelineException(
                            "Dead-letter policy is configured "
                                    + "but no dead-letter sink exists"
                    );
                }

                FailedRecord failedRecord =
                        new FailedRecord(
                                rawInput,
                                stageName,
                                exception.getMessage(),
                                Instant.now()
                        );

                deadLetterSink.consume(
                        failedRecord
                );

                summary.incrementDeadLetter();
            }

            case RETRY -> throw new PipelineException(
                    "Unexpected retry policy state"
            );
        }
    }
}