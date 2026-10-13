# Change Log

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