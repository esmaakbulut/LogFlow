# LogFlow Architecture

## Overview

LogFlow uses a simple pipeline architecture.

The system is divided into two main parts:

1. Source side
2. Sink side

## Two-Box Diagram

```text
+------------------+      +------------------+
|  FileLineSource  | ---> |   ConsoleSink    |
|      Source      |      |       Sink       |
+------------------+      +------------------+
## Stage Ordering Experiment

The pipeline stages can be composed in different orders.

Two equivalent filter orders were compared:

1. `ParserStage -> status filter -> path filter -> ProjectionStage`
2. `ParserStage -> path filter -> status filter -> ProjectionStage`

Both orders produce the same final records because both filters must be satisfied.

However, performance can differ because the filter that removes more records earlier reduces the amount of work done by the following stages.

`ParserStage` must always run before filters that use `LogRecord` fields, because the raw log line must first be converted into a `LogRecord`.

Filtering before `ProjectionStage` is also preferable because unnecessary records are removed before projection work is performed.

### Experiment Result

The two filter orders produced the same output.

Measured in one test run:

- Order 1: 2,769,038 ns
- Order 2: 4,183,060 ns
- Same result: true

In this run, Order 1 was faster.

These timing values may vary between runs, but the experiment shows that stage order can affect performance while producing the same final result.