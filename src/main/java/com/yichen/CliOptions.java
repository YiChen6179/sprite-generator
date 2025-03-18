package com.yichen;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(
    name = "sprite-generator",
    mixinStandardHelpOptions = true,
    version = "1.4",
    description = "基于Vert.x异步IO的Sprite生成器"
)
public class CliOptions {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;
    // 参数校验方法
    
    @CommandLine.Option(
        names = {"-i", "--input"},
        description = "输入目录（默认：./avatar）"
    )
    public void setInputDir(String inputDir) {
        Path path = Paths.get(inputDir).toAbsolutePath();
        if (!Files.exists(path)) {
            throw new CommandLine.ParameterException(spec.commandLine(), "输入目录不存在: " + path);
        }
        this.inputDir = path.toString();
    }
    private String inputDir = "./avatar";

    @CommandLine.Option(
        names = {"-o", "--output"},
        description = "输出目录（默认：当前目录）"
    )
    private String outputDir = "out";

    @CommandLine.Option(
        names = {"-a","--all-images"},
        description = "读取输入目录下的所有图片，不使用正则表达式过滤"
    )
    private boolean allImages = false;

    @CommandLine.Option(
            names = {"-w", "--max-width"},
            description = "精灵图最大宽度（默认：4096）",
            // 参数校验：最小 64px，最大 16384px
            paramLabel = "[64-16384]"
    )
    public void setMaxWidth(int maxWidth) {
        if (maxWidth < 64 || maxWidth > 16384) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    "最大宽度必须为 64-16384 之间的整数"
            );
        }
        this.maxWidth = maxWidth;
    }
    private int maxWidth = 4096; // 默认值

    @CommandLine.Option(
            names = {"-q", "--quality"},
            description = "生成webp格式图片质量（默认：90）",
            paramLabel = "[0-100]"
    )
    public void setQuality(int quality) {
        if (quality < 0 || quality > 100) {
            throw new CommandLine.ParameterException(
                    spec.commandLine(),
                    "质量必须为 0-100 之间的整数"
            );
        }
        this.quality = quality;
    }
    private int quality = 80;

    public int getQuality() {
        return quality;
    }
    public int getMaxWidth() {
        return maxWidth;
    }
    public Path getInputPath() {
        return Paths.get(inputDir);
    }
    public Path getOutputPath() {
        return Paths.get(outputDir).toAbsolutePath();
    }
    public boolean isAllImages() {
        return allImages;
    }
}
