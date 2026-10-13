package logflow;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println(
                "Kullanim: java logflow.Main <giris> <cikis> " +
                "[--min-status 400] [--path-prefix /api] " +
                "[--fields timestamp,clientIp,status]"
            );
            return;
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);

        Integer minStatus = null;
        String pathPrefix = null;
        String[] fields = {
            "timestamp", "clientIp", "method", "path", "status"
        };

        for (int i = 2; i < args.length; i++) {

            switch (args[i]) {

                case "--min-status":
                    minStatus = Integer.parseInt(args[++i]);
                    break;

                case "--path-prefix":
                    pathPrefix = args[++i];
                    break;

                case "--fields":
                    fields = args[++i].split(",");
                    break;

                default:
                    System.out.println("Bilinmeyen arguman: " + args[i]);
                    return;
            }
        }

        PipelineBuilder builder =
                PipelineBuilder.from(new FileLineSource(input))
                        .via(new ParserStage());

        if (minStatus != null) {
            int threshold = minStatus;

            builder.via(
                new PredicateFilterStage(
                    record -> record.status() >= threshold
                )
            );
        }

        if (pathPrefix != null) {
            String prefix = pathPrefix;

            builder.via(
                new PredicateFilterStage(
                    record -> record.path().startsWith(prefix)
                )
            );
        }

        builder.via(new ProjectionStage(fields));

        FileSink sink = new FileSink(output);

        Pipeline pipeline = builder.to(sink);

        sink.open();

        try {
            pipeline.run();
        } finally {
            sink.close();
        }

        System.out.println("Cikti dosyasi: " + output);
    }
}