package com.yichen;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SpriteGenerator {

    private final Vertx vertx;
    private final FileSystem fs;
    private final Path inputDir;
    private final Path outputDir;
    private final Integer maxWidth;
    private long startTime;
    private long endTime;
    private int quality;
    private final Boolean allowAllImages;
    private final ProgressBar progressBar;
    private final AtomicInteger processed = new AtomicInteger(0);
    private final List<BufferedImage> images = Collections.synchronizedList(new ArrayList<>());
    private final List<String> imageNames = Collections.synchronizedList(new ArrayList<>());


    public SpriteGenerator(Vertx vertx, CliOptions options) {
        this.vertx = vertx;
        this.fs = vertx.fileSystem();
        this.inputDir = options.getInputPath();
        this.outputDir = options.getOutputPath();
        this.progressBar = new ProgressBar();
        this.allowAllImages = options.isAllImages();
        this.maxWidth = options.getMaxWidth();
        this.quality = options.getQuality();
    }

    public void generate() {
        startTime = System.currentTimeMillis();
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
                    vertx.executeBlocking(promise -> {
                        processImages(imagePaths); // 现在运行在工作线程
                        promise.complete();
                    }, false, res -> {
                        if (res.failed()) {
                            handleError(res.cause());
                        }
                    });

                })
                .onFailure(Throwable::printStackTrace);
    }

    private void processImages(List<Path> imagePaths) {
        int total = imagePaths.size();

        List<Future> loadFutures = imagePaths.stream()
                .map(imagePath-> createImageLoadFuture(imagePath,processed, total))
                .collect(Collectors.toList());

        CompositeFuture.all(loadFutures)
                .onSuccess(v ->
                        vertx.executeBlocking(promise -> {
                            generateSpriteSheet();
                            promise.complete();
                        }, true, null));
    }

    private Future<Object> createImageLoadFuture(Path imagePath, AtomicInteger processed, int total) {
        return Future.future(promise -> {
            String fileName = imagePath.getFileName().toString();
            vertx.executeBlocking(blockingPromise -> {
                try {
                    byte[] bytes = Files.readAllBytes(imagePath);
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                    images.add(image);
                    imageNames.add(fileName.split("\\.")[0]);
                    blockingPromise.complete();
                } catch (IOException e) {
                    blockingPromise.fail(e);
                }
            }, false, res->{
                // 无论成功失败都更新进度
                vertx.runOnContext(v -> progressBar.update(processed.incrementAndGet(), total));
                promise.handle(res); // 关键！将结果传递到外层Future
            });
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
                    System.out.println("\n正在将图片另存为sprite.png...");
                    Path pngPath = outputDir.resolve("sprite.png");
                    ImageIO.write(spriteSheet, "PNG", pngPath.toFile());
                    System.out.println("png格式图片已另存为: " + outputDir.resolve("sprite.png"));
                    // 调用本地 WebP 转换
                    System.out.println("正在将图片转换并另存为sprite.webp...");
                    File webpFile = outputDir.resolve("sprite.webp").toFile();
                    WebPConverter.convertToWebP(pngPath.toFile(), webpFile, quality);
                    System.out.println("webp格式图片已另存为: " + outputDir.resolve("sprite.webp"));
                    promise.complete();
                } catch (IOException e) {
                    promise.fail(e);
                }
            }).onFailure(err -> {
                System.err.println("\nWebP 转换失败" + err.getMessage());
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
                    endTime= System.currentTimeMillis();
                    printStatisticsReport();
                    vertx.close(); // 所有任务完成后关闭程序
                })
                .onFailure(this::handleError); // 失败时调用错误处理
    }

    private void handleError(Throwable e) {
        System.err.println("程序运行出错:");
        e.printStackTrace();
        vertx.close(); // 强制关闭程序
    }
    private  void printStatisticsReport() {
        long durationMillis = endTime - startTime;

        // 格式化成 mm:ss.SSS 形式
        String formattedDuration = String.format("%02d:%02d.%03d",
                TimeUnit.MILLISECONDS.toMinutes(durationMillis),
                TimeUnit.MILLISECONDS.toSeconds(durationMillis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis)),
                durationMillis % 1000);

        System.out.println("\n\n===== 处理统计 =====");
        System.out.println("处理图片数量: " + processed.get() + " 张");
        System.out.println("总耗时:       " + formattedDuration+" 秒");
        System.out.println("平均速度:     " + (processed.get() > 0 ?
                        String.format("%.1f 张/秒", processed.get() / (durationMillis / 1000.0)) : "N/A"));
    }


}
