package com.yichen;

public class ProgressBar {
    private static final int WIDTH = 40;
    private int lastProgress = -1;

    public synchronized void update(int current, int total) {
        int progress = (int) ((double) current / total * 100);
        if (progress != lastProgress) {
            int filled = (int) ((double) WIDTH * current / total);
            String bar = "正在读取图片中[" + repeat("=", filled) + repeat(" ", WIDTH - filled) + "] " + progress + "%";
            System.out.print("\r" + bar);
            lastProgress = progress;
        }
    }

    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
