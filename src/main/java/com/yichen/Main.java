package com.yichen;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import picocli.CommandLine;
public class Main {
    public static void main(String[] args) {
        CliOptions options = new CliOptions();
        try {
            CommandLine cmd = new CommandLine(options);
            cmd.parseArgs(args);
            if (cmd.isUsageHelpRequested()) {
                cmd.usage(System.out);
                return;
            } else if (cmd.isVersionHelpRequested()) {
                cmd.printVersionHelp(System.out);
                return;
            }
        } catch (CommandLine.ParameterException e) {
            System.err.println("[错误] " + e.getMessage());
            return;
        }

        Vertx vertx = Vertx.vertx(new VertxOptions()
                .setBlockedThreadCheckInterval(5000));
        new SpriteGenerator(vertx, options).generate();
    }
}
