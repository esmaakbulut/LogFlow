# LogFlow Architecture

## Overview

LogFlow uses a pipeline-based architecture.

The system processes log records through three main architectural parts:

1. Source
2. Processing stages
3. Sink

Starting with Version 5, the architecture also contains a secondary error channel for failed records.

## Basic Pipeline Diagram

```text
+------------------+
|  FileLineSource  |
|      Source      |
+--------+---------+
         |
         v
+------------------+
| Processing Stage |
+--------+---------+
         |
         v
+------------------+
|     FileSink     |
|       Sink       |
+------------------+
```

The source reads input records.

Stages transform, filter, parse, validate or project records.

The sink writes successfully processed records to the final output.

## Stage Ordering Experiment

Pipeline stages can be composed in different orders.

Two equivalent filter orders were compared:

1. `ParserStage -> status filter -> path filter -> ProjectionStage`
2. `ParserStage -> path filter -> status filter -> ProjectionStage`

Both orders produce the same final records because both filters must be satisfied.

However, performance can differ because the filter that removes more records earlier reduces the amount of work performed by later stages.

`ParserStage` must always run before filters that use `LogRecord` fields because the raw input line must first be converted into a `LogRecord`.

Filtering before `ProjectionStage` is also preferable because unnecessary records are removed before projection work is performed.

### Experiment Result

The two filter orders produced the same output.

Example measured values from one test run:

- Order 1: 1,876,177 ns
- Order 2: 1,894,808 ns
- Same result: true

The exact timing values vary between test runs because the input is small and JVM execution times can fluctuate.

The experiment demonstrates that stage order can affect performance while preserving the same logical result.

# Version 4 - Configuration-Driven Pipeline

Version 4 separates pipeline definition from pipeline implementation.

Instead of building the complete pipeline directly in Java code, LogFlow can load the pipeline structure from a YAML configuration file.

## Configuration Schema

A configuration can contain:

- `pipeline`: root pipeline definition
- `name`: pipeline name
- `source`: input source configuration
- `stages`: ordered processing stages
- `sink`: output destination

Example:

```yaml
pipeline:
  name: error-report

  source:
    type: file
    params:
      path: data/access-small.log

  stages:
    - type: parse
      params:
        format: clf

    - type: filter
      params:
        field: status
        op: ">="
        value: 400

    - type: project
      params:
        fields:
          - timestamp
          - clientIp
          - status
          - path

  sink:
    type: file
    params:
      path: out/errors.txt
```

## Stage Registry

`StageRegistry` maps stage names from the YAML configuration to Java stage implementations.

For example:

```text
parse    -> ParserStage
filter   -> PredicateFilterStage
project  -> ProjectionStage
validate -> ValidationStage
```

This allows pipeline configuration to remain separate from Java implementation details.

## Pipeline Loader

`PipelineLoader` reads the YAML configuration and creates the corresponding:

- source
- ordered stages
- error policies
- sink
- dead-letter sink

The loader also checks required parameters and reports invalid configuration values.

# Version 5 - Error Management, Validation and Dead-Letter Channel

Version 5 introduces explicit error handling as part of the architecture.

Previously, malformed input could be silently ignored.

In Version 5, processing errors are represented explicitly and handled according to a configured policy.

The goal is to allow the pipeline to continue processing valid records without silently losing invalid records.

## Error Model

LogFlow distinguishes between two main error types.

### StageException

`StageException` represents an error while processing a single record.

Examples:

- malformed log line
- invalid timestamp
- negative byte count
- invalid IP address
- record that is too large
- invalid encoded characters

These errors are considered record-level errors.

Depending on the configured error policy, the pipeline can skip the record, send it to the dead-letter channel, retry the operation or stop processing.

### PipelineException

`PipelineException` represents a failure that prevents normal pipeline execution.

Examples include:

- source cannot be read
- output cannot be written
- dead-letter output cannot be written
- required pipeline resource is unavailable
- stage continues to fail after all configured retries
- pipeline output cannot be closed correctly

Unlike `StageException`, a `PipelineException` represents a pipeline-level failure rather than a problem with one individual record.

## Error Policies

Each stage may define an `onError` policy in the YAML configuration.

Supported policies are:

### FAIL_FAST

The pipeline stops immediately when the stage throws a `StageException`.

Example:

```yaml
onError: FAIL_FAST
```

### SKIP

The invalid record is dropped.

The pipeline continues with following records and the dropped record is counted in `RunSummary`.

Example:

```yaml
onError: SKIP
```

### DEAD_LETTER

The invalid record is routed to the secondary dead-letter output.

The main pipeline continues processing other records.

Example:

```yaml
onError: DEAD_LETTER
```

### RETRY(n)

The stage is retried up to `n` additional times.

Example:

```yaml
onError: RETRY(3)
```

If the original execution fails, the stage can be attempted three more times.

If all retry attempts fail, LogFlow throws a `PipelineException`.

This policy is especially appropriate for temporary I/O-related failures.

## Main and Dead-Letter Paths

The dead-letter channel is a second output path independent of the normal successful output.

```text
                         +-------------------+
                         |    Main Sink      |
                         |  Valid Records    |
                         +---------^---------+
                                   |
+--------+     +---------+     +---+---------+
| Source | --> | Parser  | --> | Validation  |
+--------+     +----+----+     +------+------+ 
                   |                 |
                   | StageException  | StageException
                   |                 |
                   v                 v
              +---------------------------+
              |     Error Policy          |
              +-------------+-------------+
                            |
                            | DEAD_LETTER
                            v
              +---------------------------+
              |     DeadLetterSink        |
              |      FailedRecord         |
              +---------------------------+
```

The important architectural point is that invalid records are not simply hidden inside local `try/catch` blocks.

The error policy determines where the failed record goes.

Therefore, error handling is part of the pipeline topology.

## FailedRecord

A failed record contains contextual information about the failure.

The `FailedRecord` structure contains:

```text
rawInput
stageName
exception
timestamp
```

This information allows a failed record to be inspected later without mixing it with valid output.

Example dead-letter entry:

```text
time=... | stage=ValidationStage | error=Bytes value out of range: -45 | raw=...
```

## ValidationStage

`ValidationStage` performs declarative validation on parsed `LogRecord` objects.

The current implementation validates:

- required fields
- minimum byte value
- maximum byte value
- minimum timestamp
- maximum timestamp
- IPv4 format
- maximum input-line length
- non-ASCII user-agent characters when configured

Example configuration:

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

## RunSummary

Every pipeline run creates and prints a `RunSummary`.

It contains:

- input record count
- output record count
- dead-letter record count
- dropped record count per stage
- actual elapsed execution time

Example:

```text
=== Run Summary ===

Input records: 11
Output records: 3
Dead-letter records: 8
Elapsed time: 98 ms
```

For the dirty fixture used in Version 5:

```text
11 input records
= 3 valid output records
+ 8 dead-letter records
```

The first line of the fixture is an explanatory comment.

LogFlow treats this comment as an invalid log record, so it is also routed to the dead-letter channel.

This is why the current run contains eight dead-letter records instead of seven corrupted data records.

## Dirty Fixture Error Mapping

The following table documents how each deliberate corruption in `data/access-dirty.log` is handled.

| Input problem | Stage detecting the problem | Policy | Result |
|---|---|---|---|
| Fixture comment/header line | `ParserStage` | `DEAD_LETTER` | Routed to dead-letter |
| Truncated timestamp record | `ParserStage` | `DEAD_LETTER` | Routed to dead-letter |
| Truncated record with missing fields | `ParserStage` | `DEAD_LETTER` | Routed to dead-letter |
| Negative byte count `-45` | `ValidationStage` | `DEAD_LETTER` | Routed to dead-letter |
| Timestamp from 1970 | `ValidationStage` | `DEAD_LETTER` | Routed to dead-letter |
| Timestamp from 2099 | `ValidationStage` | `DEAD_LETTER` | Routed to dead-letter |
| Invalid/non-ASCII user-agent data | `ValidationStage` | `DEAD_LETTER` | Routed to dead-letter |
| Approximately 100 KiB single line | `ValidationStage` | `DEAD_LETTER` | Routed to dead-letter |
| Valid access-log records | All stages | No error | Written to main sink |
| Empty input file | Source | No error | Clean run with zero records |

The three valid records are written to the normal output.

All detected invalid records are accounted for by the configured error policy.

## Empty File Behaviour

`data/access-empty.log` contains zero records.

Running the pipeline with this source produces:

```text
Input records: 0
Output records: 0
Dead-letter records: 0
```

The pipeline terminates successfully without crashing.

## Version 5 Test Result

The Version 5 implementation is covered by unit and integration tests.

Current result:

```text
28 tests found
28 tests successful
0 tests failed
```

The tests include:

- parser behaviour
- validation rules
- stage ordering
- configuration loading
- sink behaviour
- pipeline integration
- `SKIP` error policy
- `DEAD_LETTER` error policy
- `RETRY(n)` error policy

This confirms that the new error-management behaviour does not break the existing pipeline functionality.