package com.yichen;

import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(
    name = "sprite-generator",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "CSS Sprite Generator with Vert.x"
)
public class CliOptions {
    
    @CommandLine.Option(
        names = {"-i", "--input"},
        description = "输入目录（默认：./avatar）"
    )
    private String inputDir = "./avatar";

    @CommandLine.Option(
        names = {"-o", "--output"},
        description = "输出目录（默认：当前目录）"
    )
    private String outputDir = "out";

    // Getters
    public Path getInputPath() {
        return Paths.get(inputDir).toAbsolutePath();
    }
    
    public Path getOutputPath() {
        return Paths.get(outputDir).toAbsolutePath();
    }
}
