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

    @CommandLine.Option(
        names = {"--all-images", "-all"},
        description = "读取输入目录下的所有图片，不使用正则表达式过滤"
    )
    private boolean allImages = false;

    // Getters
    public Path getInputPath() {
        return Paths.get(inputDir).toAbsolutePath();
    }
    
    public Path getOutputPath() {
        return Paths.get(outputDir).toAbsolutePath();
    }

    public boolean isAllImages() {
        return allImages;
    }
}
