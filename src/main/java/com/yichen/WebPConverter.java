package com.yichen;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

public class WebPConverter {
    private static final int PROCESS_TIMEOUT_SECONDS = 40;

    public static void convertToWebP(File inputFile, File outputFile, int quality) throws IOException {
        Path tempDir = null;
        Process process = null;
        try {
            // 提取 cwebp.exe 到临时目录
            tempDir = Files.createTempDirectory("webp-converter-");
            Path cwebpExe = extractNativeBinary(tempDir);

            // 构建命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                    cwebpExe.toString(),
                    "-q", String.valueOf(quality),
                    inputFile.getAbsolutePath(),
                    "-o", outputFile.getAbsolutePath()
            );

            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            Process finalProcess = process;
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    System.out.println("读取进程输出时发生错误：{"+e.getMessage()+"}");
                }
            });
            outputReader.start();

            boolean completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                throw new InterruptedException("cwebp 执行超时,已终止进程");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException(String.format(
                        "cwebp 执行失败 (退出码: %d)\n错误输出: %s",
                        exitCode, output.toString().trim()
                ));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("cwebp 执行超时,已终止进程", e);
        } finally {
            if (tempDir != null) {
                cleanTempResources(tempDir);
            }
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }

    /**
     * 从资源目录提取 cwebp.exe 到临时目录
     */
    private static Path extractNativeBinary(Path tempDir) throws IOException {
        String resourcePath = "/native/windows/cwebp.exe";
        try (InputStream in = WebPConverter.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("未找到嵌入的 cwebp.exe 可执行文件");
            }
            Path exePath = tempDir.resolve("cwebp_" + System.nanoTime() + ".exe"); // 唯一文件名
            Files.copy(in, exePath);
            // 设置可执行权限 (跨平台兼容)
            if (!exePath.toFile().setExecutable(true)) {
                throw new IOException("无法设置 cwebp 可执行权限");
            }
            return exePath;
        }
    }

    private static void cleanTempResources(Path tempDir) {
        try {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("临时文件清理失败: " + e.getMessage());
        }
    }
}
