# LogFlow

LogFlow is a simple pipeline-based log processing application developed for the Software Architecture course.

## Project Structure

- `Source`: Produces data.
- `Emitter`: Sends data to the next component.
- `Stage`: Processes incoming data.
- `Sink`: Consumes final output.
- `Pipeline`: Connects source, stages, and sink.
- `DeadLetterSink`: Stores failed records separately.
- `RunSummary`: Reports execution statistics.

## How to Compile

LogFlow uses SnakeYAML to read YAML configuration files.

Download SnakeYAML:

```bash
curl -L -o lib/snakeyaml.jar https://repo1.maven.org/maven2/org/yaml/snakeyaml/2.7/snakeyaml-2.7.jar
```

Compile the source files:

```bash
javac -cp "lib/snakeyaml.jar" -d out src/logflow/*.java
```

## Tests

JUnit 5 is used for automated testing.

Compile the tests:

```bash
javac -cp "out:lib/snakeyaml.jar:lib/junit-platform-console-standalone.jar" -d out test/logflow/*.java
```

Run all tests:

```bash
java -jar lib/junit-platform-console-standalone.jar \
execute \
--class-path "out:lib/snakeyaml.jar" \
--scan-class-path
```

Current test result:

- Tests: 28
- Successful tests: 28
- Failed tests: 0

Earlier coverage measurement for the parser was performed using JaCoCo.

# Version 3 Features

Version 3 added configurable pipeline composition in Java.

## New Stages

- `PredicateFilterStage`: filters `LogRecord` objects using predicates.
- `ProjectionStage`: selects specific fields from a record.
- `FileSink`: writes pipeline output to a file.

## Pipeline Builder

Pipelines can be created using a fluent builder:

```java
PipelineBuilder.from(new FileLineSource(input))
    .via(new ParserStage())
    .via(new PredicateFilterStage(r -> r.status() >= 400))
    .via(new ProjectionStage("clientIp", "status", "path"))
    .to(new FileSink(output));
```

# Version 4 - Configuration-Driven Pipeline

Version 4 allows LogFlow pipelines to be defined using YAML configuration files.

The pipeline structure no longer needs to be written directly in `Main.java`.

The application uses:

- `StageFactory`
- `StageRegistry`
- `PipelineLoader`
- YAML configuration files

The loader validates the configuration before log data is processed.

## Running with a Configuration File

Example:

```bash
java -cp "out:lib/snakeyaml.jar" logflow.Main --config configs/error-report.yaml
```

Available Version 4 configurations:

- `configs/error-report.yaml`
- `configs/api-traffic.yaml`
- `configs/bandwidth-by-client.yaml`

## Error Report

```bash
java -cp "out:lib/snakeyaml.jar" logflow.Main --config configs/error-report.yaml
```

Creates:

```text
out/errors.txt
```

## API Traffic Extract

```bash
java -cp "out:lib/snakeyaml.jar" logflow.Main --config configs/api-traffic.yaml
```

Creates:

```text
out/api-traffic.txt
```

## Bandwidth by Client Extract

```bash
java -cp "out:lib/snakeyaml.jar" logflow.Main --config configs/bandwidth-by-client.yaml
```

Creates:

```text
out/bandwidth-by-client.txt
```

## Configuration Error Examples

The configuration is validated before data processing begins.

### Unknown Stage Type

```text
Configuration error: Unknown stage type: unknown-stage
```

### Missing Required Parameter

```text
Configuration error: Filter parameter 'value' is required
```

### Wrong Parameter Type

```text
Configuration error: Filter 'value' must be an integer for status
```

### Empty Pipeline

```text
Configuration error: Pipeline must contain at least one stage
```

## Version 4 Loader Tests

`PipelineLoaderTest` verifies:

- valid configuration
- unknown stage type
- missing parameter
- wrong parameter type
- empty pipeline

All loader tests are successful.

# Version 5 - Error Management, Validation and Dead-Letter Channel

Version 5 adds explicit error handling to the pipeline architecture.

Malformed records are no longer silently ignored.

Instead, record-level errors are represented by `StageException` and handled according to a configurable error policy.

Pipeline-level failures are represented by `PipelineException`.

## Error Policies

LogFlow supports the following policies:

- `FAIL_FAST`
- `SKIP`
- `DEAD_LETTER`
- `RETRY(n)`

### FAIL_FAST

Stops the pipeline immediately after a stage error.

```yaml
onError: FAIL_FAST
```

### SKIP

Drops the invalid record and continues processing following records.

Dropped records are counted in `RunSummary`.

```yaml
onError: SKIP
```

### DEAD_LETTER

Routes the failed record to a separate dead-letter output.

```yaml
onError: DEAD_LETTER
```

### RETRY(n)

Retries the failing stage up to `n` additional times.

```yaml
onError: RETRY(3)
```

If all retry attempts fail, the pipeline throws a `PipelineException`.

## ValidationStage

Version 5 adds `ValidationStage`.

It can validate:

- required fields
- minimum and maximum byte values
- timestamp range
- IPv4 format
- maximum line length
- non-ASCII user-agent characters

Example:

```yaml
- type: validate
  onError: DEAD_LETTER
  params:
    requiredFields:
      - clientIp
      - path
    minBytes: 0
    maxBytes: 10000000
    minTimestamp: "2000-01-01T00:00:00Z"
    maxTimestamp: "2030-12-31T23:59:59Z"
    maxLineLength: 10000
    asciiUserAgentOnly: true
```

## FailedRecord

Records sent to the dead-letter channel are stored as `FailedRecord`.

A failed record contains:

- original input
- stage name
- error message
- timestamp

Example dead-letter output:

```text
time=... | stage=ValidationStage | error=Bytes value out of range: -45 | raw=...
```

## RunSummary

Every pipeline execution prints a summary.

The summary contains:

- input record count
- output record count
- dead-letter record count
- dropped record count per stage
- elapsed execution time

Example:

```text
=== Run Summary ===
Input records: 11
Output records: 3
Dead-letter records: 8
Elapsed time: 98 ms
```

## Dirty Input Test

The Version 5 chaos test uses:

```text
data/access-dirty.log
```

The file contains valid and deliberately corrupted access-log records.

Run the test configuration with:

```bash
java -cp "out:lib/snakeyaml.jar" logflow.Main --config configs/dirty-chaos.yaml
```

The expected result is:

```text
Input records: 11
Output records: 3
Dead-letter records: 8
```

The main output is written to:

```text
out/dirty-valid.txt
```

The dead-letter output is written to:

```text
out/dead-letter.txt
```

The current valid output contains exactly three records:

```text
2026-10-15T10:15:32Z 192.0.2.10 GET /index.html 200 1234
2026-10-15T10:16:05Z 198.51.100.22 POST /api/login 401 287
2026-10-15T10:17:41Z 203.0.113.7 GET /images/logo.png 304 0
```

The dirty fixture currently produces eight dead-letter records because the explanatory comment line at the beginning of the fixture is also treated as invalid input.

Therefore:

```text
11 input records
= 3 valid output records
+ 8 dead-letter records
```

## Empty File Test

The empty-input test uses:

```text
data/access-empty.log
```

Run:

```bash
java -cp "out:lib/snakeyaml.jar" logflow.Main --config configs/empty-test.yaml
```

Expected result:

```text
=== Run Summary ===
Input records: 0
Output records: 0
Dead-letter records: 0
```

The pipeline completes successfully without crashing.

## Version 5 Tests

Version 5 adds tests for:

- valid records
- negative byte values
- invalid timestamps
- invalid IPv4 addresses
- non-ASCII user-agent data
- oversized log lines
- `SKIP`
- `DEAD_LETTER`
- `RETRY(n)`
- existing parser behaviour
- existing pipeline integration
- configuration loading

Current result:

```text
28 tests found
28 tests successful
0 tests failed
```

This confirms that the Version 5 error-management features work without breaking the existing pipeline behaviour.