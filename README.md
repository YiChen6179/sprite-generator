# Sprite Generator

[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![WebP集成](https://img.shields.io/badge/WebP-内置支持-important)](https://github.com/webmproject/libwebp)
![JDK需求](https://img.shields.io/badge/JDK-8%2B-orange)

一键精灵图生成解决方案，专为开发者设计的自动化图片优化工具。原生支持WebP格式，适用于网页性能优化、游戏资源打包等场景。

![使用案例](image%2Fimg_1.png)

## 🌟 核心功能

- **格式转换一体** - 内置WebP编码器，支持PNG/WebP双格式输出
- **并行处理引擎** - 基于Vert.x的异步处理流水线
- **弹性输出控制** - 自定义最大宽度与编码质量

## 🛠️ 系统要求

### 基础环境
- Java 8+ Runtime

### 推荐配置
- 4核CPU及以上
- SSD存储设备
- 1GB可用内存

## 🚀 快速入门

### 基础使用
```bash
# 下载最新版本
curl -LO https://github.com/yourname/sprite-generator/releases/latest/sprite-generator.jar

# 生成精灵图（通用模式）
java -jar sprite-generator.jar \
    -i ./input_images \
    -o ./output \
    -w 2048 \
    -q 90
```

## ⚙️ 参数详解

| 参数 | 缩写 | 类型 | 默认值 | 说明 |
|------|------|------|--------|------|
| `--input` | `-i` | 路径 | ./input | 输入目录路径 |
| `--output` | `-o` | 路径 | ./output | 输出目录路径 |
| `--all-images` | `-a` | 布尔 | false | 处理全部图片(禁用智能过滤) |
| `--max-width` | `-w` | 整数 | 4096 | 精灵图最大宽度(64-16384) |
| `--quality` | `-q` | 整数 | 90 | 输出质量(1-100) |
| `--output-format` | `-f` | 枚举 | png | 输出格式(png/webp) |

## 📂 输出结构

```
output/
├── sprite.png               # PNG格式精灵图
├── sprite.webp              # WebP格式精灵图
└── sprite.css               # CSS样式文件
```

CSS文件示例：
```css
/* 自动生成的CSS片段 */
.sprite-image_1 {
    background: url(sprite.webp) -0px -0px;
    width: 300px;
    height: 150px;
}
```

> 💡 样式文件中的`url()`路径需根据实际部署位置调整

## 📜 许可声明

### 主项目
[MIT License](LICENSE) - 自由使用和修改

### 第三方组件
- **libwebp**：BSD-like许可 [详情](https://github.com/webmproject/libwebp/blob/main/COPYING)
- **Vert.x**：Apache License 2.0
