# LogFlow Architecture

## Overview

LogFlow uses a simple pipeline architecture.

The system is divided into two main parts:

1. Source side
2. Sink side

## Two-Box Diagram

```text
+-------------------+        +-------------------+
|   FileLineSource  | -----> |    ConsoleSink    |
|      Source       |        |       Sink        |
+-------------------+        +-------------------+
