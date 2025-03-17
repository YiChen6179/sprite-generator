package com.yichen;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SpriteGenerator {

    private final Vertx vertx;
    private final FileSystem fs;
    private final Path inputDir;
    private final Path outputDir;
    private final Integer maxWidth;
    private final Boolean allowAllImages;
    private final ProgressBar progressBar;
    private final List<BufferedImage> images = new ArrayList<>();
    private final List<String> imageNames = new ArrayList<>();

    public SpriteGenerator(Vertx vertx, CliOptions options) {
        this.vertx = vertx;
        this.fs = vertx.fileSystem();
        this.inputDir = options.getInputPath();
        this.outputDir = options.getOutputPath();
        this.progressBar = new ProgressBar();
        this.allowAllImages = options.isAllImages();
        this.maxWidth = options.getMaxWidth();
    }

    public void generate() {
        // 1. 创建输出目录
        fs.mkdirsBlocking(outputDir.toString());

        // 2. 使用精确正则表达式过滤文件
        String pattern = !allowAllImages ? "^char_\\d+_[^_]+?\\.(png|jpg|jpeg)" : ".+\\.(png|jpg|jpeg)$";
        fs.readDir(inputDir.toString(), pattern) // 更新过滤正则
                .onSuccess(files -> {
                    List<Path> imagePaths = files.stream()
                            .map(Paths::get) // 替换 Path::of 为 Paths::get
                            .collect(Collectors.toList());

                    if (imagePaths.isEmpty()) {
                        System.out.println("未找到有效的图像");
                        vertx.close();
                        return;
                    }

                    // 3. 处理图片
                    processImages(imagePaths);
                })
                .onFailure(Throwable::printStackTrace);

    }

    private void processImages(List<Path> imagePaths) {
        AtomicInteger processed = new AtomicInteger(0);
        int total = imagePaths.size();

        imagePaths.forEach(imagePath -> {
            String fileName = imagePath.getFileName().toString();


            fs.readFile(imagePath.toString())
                    .onSuccess(buffer -> {
                        // 将图片加载到内存中
                        try {
                            BufferedImage image = ImageIO.read(new ByteArrayInputStream(buffer.getBytes()));
                            images.add(image);
                            imageNames.add(fileName.split("\\.")[0]); // 保存图片名称（不含扩展名）
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // 更新进度条
                        progressBar.update(processed.incrementAndGet(), total);
                        if (processed.get() == total) {
                            generateSpriteSheet(); // 所有图片加载完成后生成精灵图
                        }
                    })
                    .onFailure(Throwable::printStackTrace);

        });
    }


    private void generateSpriteSheet() {
        // 1. 计算精灵图尺寸（修复坐标重置问题）
        int currentX = 0, currentY = 0, rowHeight = 0, totalHeight = 0;
        // 首次循环：仅用于计算总高度
        for (BufferedImage image : images) {
            if (currentX + image.getWidth() > maxWidth) {
                currentX = 0;
                currentY += rowHeight;
                rowHeight = 0;
            }
            rowHeight = Math.max(rowHeight, image.getHeight());
            currentX += image.getWidth();
        }
        totalHeight = currentY + rowHeight;
        // 2. 创建画布
        BufferedImage spriteSheet = new BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = spriteSheet.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
        g2d.setBackground(new Color(0, 0, 0, 0));
        g2d.clearRect(0, 0, maxWidth, totalHeight);
        // 3. 绘制图片（重置坐标变量！）
        currentX = 0;
        currentY = 0;
        rowHeight = 0;
        StringBuilder css = new StringBuilder();
        for (int i = 0; i < images.size(); i++) {
            BufferedImage image = images.get(i);
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();
            // 换行判断
            if (currentX + imgWidth > maxWidth) {
                currentX = 0;
                currentY += rowHeight;
                rowHeight = 0; // 重置行高
            }
            // 转换为统一颜色模式
            BufferedImage compatibleImage = new BufferedImage(
                    imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB
            );
            compatibleImage.getGraphics().drawImage(image, 0, 0, null);
            // 绘制到精灵图
            g2d.drawImage(compatibleImage, currentX, currentY, null);
            // 记录 CSS 坐标
            String imageName = imageNames.get(i);
            css.append(String.format(".bg-%s { width: %dpx; height: %dpx; background: url(####) -%dpx -%dpx; }\n",
                    imageName, imgWidth, imgHeight, currentX, currentY));
            // 更新坐标
            currentX += imgWidth;
            rowHeight = Math.max(rowHeight, imgHeight);
        }
        g2d.dispose();

        // 4. 保存图片
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(spriteSheet, "PNG", baos)) { // 先保存为PNG测试
                throw new IOException("PNG编码失败");
            }
            vertx.executeBlocking(promise -> {
                try {
                    // 生成雪碧图 PNG
                    Path pngPath = outputDir.resolve("sprite.png");
                    ImageIO.write(spriteSheet, "PNG", pngPath.toFile());
                    System.out.println("\npng格式图片已保存到: " + outputDir.resolve("sprite.png"));
                    // 调用本地 WebP 转换
                    File webpFile = outputDir.resolve("sprite.webp").toFile();
                    WebPConverter.convertToWebP(pngPath.toFile(), webpFile, 90);
                    System.out.println("webp格式图片已保存到: " + outputDir.resolve("sprite.webp"));
                    promise.complete();
                } catch (IOException e) {
                    promise.fail(e);
                }
            }).onFailure(v -> {
                System.err.println("\nWebP 转换失败");
//                fs.writeFile(outputDir.resolve("sprite.png").toString(), Buffer.buffer(baos.toByteArray()))
//                        .onSuccess(a -> {
//
//                        })
//                        .onFailure(this::handleError);
            }).onComplete(s -> {
                // 保存 CSS
                saveCssAndExit(css);
            });

        } catch (Exception e) {
            handleError(e);
        }

    }

    private void saveCssAndExit(StringBuilder css) {
        // 保存 CSS 文件
        fs.writeFile(outputDir.resolve("sprite_avatar.css").toString(), Buffer.buffer(css.toString().getBytes()))
                .onSuccess(v -> {
                    System.out.println("CSS 文件已保存到: " + outputDir.resolve("sprite_avatar.css"));
                    vertx.close(); // 所有任务完成后关闭程序
                })
                .onFailure(this::handleError); // 失败时调用错误处理
    }

    private void handleError(Throwable e) {
        System.err.println("程序运行出错:");
        e.printStackTrace();
        vertx.close(); // 强制关闭程序
    }

}
