# Change Log

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