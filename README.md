# LogFlow

LogFlow is a simple pipeline-based log processing application developed for the Software Architecture course.

## Project Structure

- `Source`: Produces data.
- `Emitter`: Sends data to the next component.
- `Stage`: Processes incoming data.
- `Sink`: Consumes final output.
- `Pipeline`: Connects source, stages, and sink.

## How to Compile

Version 4 uses SnakeYAML to read YAML configuration files.

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

- Tests: 19
- Successful tests: 19
- Failed tests: 0

Earlier coverage measurement for the parser was performed using JaCoCo.

## Version 3 Features

Version 3 added configurable pipeline composition in Java.

### New Stages

- `PredicateFilterStage`: filters `LogRecord` objects using predicates.
- `ProjectionStage`: selects specific fields from a record.
- `FileSink`: writes pipeline output to a file.

### Pipeline Builder

Pipelines can be created using a fluent builder:

```java
PipelineBuilder.from(new FileLineSource(input))
    .via(new ParserStage())
    .via(new PredicateFilterStage(r -> r.status() >= 400))
    .via(new ProjectionStage("clientIp", "status", "path"))
    .to(new FileSink(output));
```

## Version 4 - Configuration-Driven Pipeline

Version 4 allows LogFlow pipelines to be defined using YAML configuration files.

The pipeline structure no longer needs to be written directly in `Main.java`.

The application now uses:

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

Available configurations:

- `configs/error-report.yaml`
- `configs/api-traffic.yaml`
- `configs/bandwidth-by-client.yaml`

### Error Report

```bash
java -cp "out:lib/snakeyaml.jar" logflow.Main --config configs/error-report.yaml
```

Creates:

```text
out/errors.txt
```

### API Traffic Extract

```bash
java -cp "out:lib/snakeyaml.jar" logflow.Main --config configs/api-traffic.yaml
```

Creates:

```text
out/api-traffic.txt
```

### Bandwidth by Client Extract

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

`PipelineLoaderTest` verifies the following cases:

- Valid configuration
- Unknown stage type
- Missing parameter
- Wrong parameter type
- Empty pipeline

All loader tests are currently successful.