# LogFlow

LogFlow is a simple pipeline-based log processing application developed for the Software Architecture course.

## Project Structure

- `Source`: Produces data.
- `Emitter`: Sends data to the next component.
- `Stage`: Processes incoming data.
- `Sink`: Consumes final output.
- `Pipeline`: Connects source, stages, and sink.

## How to Compile

```bash
javac -d out src/logflow/*.java

## Test Coverage

JUnit 5 tests were created for `ParserStage`.

- Tests: 8
- Successful tests: 8
- Failed tests: 0
- Line coverage: 67%

Coverage was measured using JaCoCo.