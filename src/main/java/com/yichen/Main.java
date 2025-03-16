package com.yichen;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import picocli.CommandLine;
public class Main {
    public static void main(String[] args) {
        CliOptions options = new CliOptions();
        new CommandLine(options).parseArgs(args);
        Vertx vertx = Vertx.vertx(new VertxOptions()
                .setBlockedThreadCheckInterval(5000));
        new SpriteGenerator(vertx, options).generate();
    }
}
