package com.yichen;

import java.io.*;
import java.nio.file.*;

public class WebPConverter {

    public static void convertToWebP(File inputFile, File outputFile, int quality) throws IOException {
        // 提取 cwebp.exe 到临时目录
        Path tempDir = Files.createTempDirectory("webp-native");
        tempDir.toFile().deleteOnExit();
        Path cwebpExe = extractNativeBinary(tempDir);

        // 构建命令
        String command = String.format("\"%s\" -q %d \"%s\" -o \"%s\"",
            cwebpExe.toString(), quality, inputFile.getAbsolutePath(), outputFile.getAbsolutePath());

        // 执行命令
        Process process = Runtime.getRuntime().exec(command);

//        // 启动线程读取子进程的输出流和错误流
//        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");
//        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "INFO"); // 将错误流标记为 INFO
//        outputGobbler.start();
//        errorGobbler.start();

        // 等待子进程结束
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("cwebp 执行失败，退出码: " + exitCode);
            }
            System.out.println("\n调用cwebp.exe程序执行完毕");
        } catch (InterruptedException e) {
            throw new IOException("cwebp 执行被中断", e);
        } finally {
            process.destroy();
            System.out.println("子进程已销毁");
        }
    }

    /**
     * 从资源目录提取 cwebp.exe 到临时目录
     */
    private static Path extractNativeBinary(Path tempDir) throws IOException {
        String resourcePath = "/native/windows/cwebp.exe";
        try (InputStream in = WebPConverter.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("未找到 cwebp.exe 资源");
            }
            Path exePath = tempDir.resolve("cwebp.exe");
            Files.copy(in, exePath, StandardCopyOption.REPLACE_EXISTING);
            return exePath;
        }
    }

    /**
     * 用于读取子进程输出流和错误流的线程
     */
    private static class StreamGobbler extends Thread {
        private InputStream inputStream;
        private String type;

        public StreamGobbler(InputStream inputStream, String type) {
            this.inputStream = inputStream;
            this.type = type;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(type + "> " + line); // 打印子进程输出
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
