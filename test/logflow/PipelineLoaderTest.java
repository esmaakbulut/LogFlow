package logflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PipelineLoaderTest {

    @TempDir
    Path tempDir;

    private PipelineLoader createLoader() {
        return new PipelineLoader(new StageRegistry());
    }

    private Path createConfig(String content) throws Exception {
        Path config = tempDir.resolve("config.yaml");
        Files.writeString(config, content);
        return config;
    }

    @Test
    void loadsValidConfiguration() throws Exception {

        String yaml = """
                pipeline:
                  name: valid-test

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
                          - clientIp
                          - status

                  sink:
                    type: file
                    params:
                      path: out/test-loader.txt
                """;

        Path config = createConfig(yaml);

        assertDoesNotThrow(
                () -> createLoader().load(config)
        );
    }

    @Test
    void rejectsUnknownStageType() throws Exception {

        String yaml = """
                pipeline:
                  source:
                    type: file
                    params:
                      path: data/access-small.log

                  stages:
                    - type: unknown-stage

                  sink:
                    type: file
                    params:
                      path: out/test.txt
                """;

        Path config = createConfig(yaml);

        assertThrows(
                IllegalArgumentException.class,
                () -> createLoader().load(config)
        );
    }

    @Test
    void rejectsMissingParameter() throws Exception {

        String yaml = """
                pipeline:
                  source:
                    type: file
                    params:
                      path: data/access-small.log

                  stages:
                    - type: filter
                      params:
                        field: status
                        op: ">="

                  sink:
                    type: file
                    params:
                      path: out/test.txt
                """;

        Path config = createConfig(yaml);

        assertThrows(
                IllegalArgumentException.class,
                () -> createLoader().load(config)
        );
    }

    @Test
    void rejectsWrongParameterType() throws Exception {

        String yaml = """
                pipeline:
                  source:
                    type: file
                    params:
                      path: data/access-small.log

                  stages:
                    - type: filter
                      params:
                        field: status
                        op: ">="
                        value: wrong

                  sink:
                    type: file
                    params:
                      path: out/test.txt
                """;

        Path config = createConfig(yaml);

        assertThrows(
                IllegalArgumentException.class,
                () -> createLoader().load(config)
        );
    }

    @Test
    void rejectsEmptyPipeline() throws Exception {

        String yaml = """
                pipeline:
                  source:
                    type: file
                    params:
                      path: data/access-small.log

                  stages: []

                  sink:
                    type: file
                    params:
                      path: out/test.txt
                """;

        Path config = createConfig(yaml);

        assertThrows(
                IllegalArgumentException.class,
                () -> createLoader().load(config)
        );
    }
}