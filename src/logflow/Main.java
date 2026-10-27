package logflow;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {

        if (args.length != 2 || !"--config".equals(args[0])) {
            System.out.println(
                    "Usage: java logflow.Main --config <config-file>"
            );
            return;
        }

        Path configPath = Path.of(args[1]);

        try {

            StageRegistry registry = new StageRegistry();

            PipelineLoader loader =
                    new PipelineLoader(registry);

            Pipeline pipeline =
                    loader.load(configPath);

            pipeline.run();

            System.out.println(
                    "Pipeline completed successfully."
            );

        } catch (Exception e) {

            System.err.println(
                    "Configuration error: "
                            + e.getMessage()
            );
        }
    }
}