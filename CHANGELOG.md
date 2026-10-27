# Change Log

## v5 - Error Management, Validation and Dead-Letter Channel

### Added

- `src/logflow/PipelineException.java`
- `src/logflow/ErrorPolicy.java`
- `src/logflow/FailedRecord.java`
- `src/logflow/DeadLetterSink.java`
- `src/logflow/RunSummary.java`
- `src/logflow/ValidationStage.java`
- `configs/dirty-chaos.yaml`
- `configs/empty-test.yaml`
- `data/access-dirty.log`
- `data/access-empty.log`
- `test/logflow/ValidationStageTest.java`
- `test/logflow/ErrorHandlingTest.java`

### Modified

- `src/logflow/ParserStage.java`
- `src/logflow/StageRegistry.java`
- `src/logflow/Pipeline.java`
- `src/logflow/PipelineLoader.java`
- `test/logflow/ParserStageTest.java`
- `test/logflow/StageOrderingExperimentTest.java`
- `ARCHITECTURE.md`
- `README.md`

### Changes

- Added explicit record-level error handling using `StageException`.
- Added pipeline-level failure handling using `PipelineException`.
- Added configurable error policies:
  - `FAIL_FAST`
  - `SKIP`
  - `DEAD_LETTER`
  - `RETRY(n)`
- Added `DeadLetterSink` as a secondary output path.
- Added `FailedRecord` for storing failed record context.
- Added `ValidationStage`.
- Added validation for:
  - required fields
  - byte ranges
  - timestamp ranges
  - IPv4 format
  - maximum line length
  - non-ASCII user-agent data
- Added `RunSummary`.
- Added input, output and dead-letter counters.
- Added dropped-record counting per stage.
- Added actual elapsed-time measurement.
- Added chaos testing using `data/access-dirty.log`.
- Added empty-file testing using `data/access-empty.log`.
- Dirty fixture result:
  - Input records: 11
  - Output records: 3
  - Dead-letter records: 8
- Empty fixture result:
  - Input records: 0
  - Output records: 0
  - Dead-letter records: 0
- Added automated tests for validation and error policies.
- Added `RETRY(n)` behaviour test.
- Total tests: 28
- Successful tests: 28
- Failed tests: 0

## v4 - Configuration-Driven Pipeline

### Added

- `configs/error-report.yaml`
- `configs/api-traffic.yaml`
- `configs/bandwidth-by-client.yaml`
- `src/logflow/StageFactory.java`
- `src/logflow/StageRegistry.java`
- `src/logflow/PipelineLoader.java`
- `test/logflow/PipelineLoaderTest.java`

### Modified

- `src/logflow/Main.java`
- `src/logflow/Pipeline.java`
- `src/logflow/Sink.java`
- `data/access-small.log`
- `ARCHITECTURE.md`
- `README.md`

### Changes

- Added YAML-based pipeline configuration.
- Added stage registry and factory system.
- Added configuration loader.
- Added configuration validation and fail-fast error handling.
- Added three different pipeline configurations.
- Added loader tests for valid and invalid configurations.
- Total tests: 19
- Successful tests: 19
- Failed tests: 0

## v3 - Filtering, Projection and Pipeline Builder

### Added

- `src/logflow/PredicateFilterStage.java`
- `src/logflow/ProjectionStage.java`
- `src/logflow/FileSink.java`
- `src/logflow/PipelineBuilder.java`
- `test/logflow/PredicateFilterStageTest.java`
- `test/logflow/ProjectionStageTest.java`
- `test/logflow/FileSinkTest.java`
- `test/logflow/PipelineIntegrationTest.java`
- `test/logflow/StageOrderingExperimentTest.java`
- `test/logflow/fixture.log`

### Modified

- `src/logflow/Pipeline.java`
- `src/logflow/Main.java`
- `ARCHITECTURE.md`
- `README.md`

### Changes

- Added predicate-based filtering.
- Added field projection.
- Added file output using `FileSink`.
- Added fluent `PipelineBuilder`.
- Added command line options:
  - `--min-status`
  - `--path-prefix`
  - `--fields`
- Added integration testing.
- Added stage ordering experiment.
- Total tests: 14
- Successful tests: 14
- Failed tests: 0

## v2 - Typed Records and ParserStage

### Added

- `src/logflow/LogRecord.java`
- `src/logflow/ParserStage.java`
- `test/logflow/CollectingEmitter.java`
- `test/logflow/ParserStageTest.java`

### Modified

- `src/logflow/ConsoleSink.java`
- `src/logflow/Pipeline.java`
- `src/logflow/Main.java`
- `data/access-small.log`
- `README.md`

### Changes

- Added typed `LogRecord` objects.
- Added `ParserStage` for parsing log lines.
- Invalid log lines are skipped and counted.
- `ConsoleSink` now prints structured records.
- Added 8 JUnit 5 tests for `ParserStage`.
- Added JaCoCo line coverage reporting.