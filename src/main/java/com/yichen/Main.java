package com.yichen;

import io.vertx.core.Vertx;
import picocli.CommandLine;


public class Main {
    public static void main(String[] args) {
        CliOptions options = new CliOptions();
        new CommandLine(options).parseArgs(args);
        
        Vertx vertx = Vertx.vertx();
        new SpriteGenerator(vertx, options).generate();
    }
}
