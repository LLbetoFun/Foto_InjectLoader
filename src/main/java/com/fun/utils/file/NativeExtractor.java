package com.fun.utils.file;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeExtractor {

    /**
     * 从JAR文件中提取natives/目录下的所有文件到指定目录
     * @param dest 目标目录（如果为null，则使用当前工作目录下的natives目录）
     */
    public static void extractNativesFromJar(File dest) {
        try {
            // 获取当前运行的JAR文件路径
            URL jarLocation = NativeExtractor.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            Path jarPath = Paths.get(jarLocation.toURI());

            // 确保是从JAR文件运行
            if (!jarPath.toString().endsWith(".jar")) {
                System.out.println("Not running from JAR file. Native extraction skipped.");
                return;
            }

            // 确定目标目录
            Path nativesDir = (dest != null) ? dest.toPath() : Paths.get("natives");
            if (!Files.exists(nativesDir)) {
                Files.createDirectories(nativesDir);
            }

            // 打开JAR文件
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();

                // 遍历JAR文件中的所有条目
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // 检查是否是natives/目录下的文件
                    if (entryName.startsWith("natives/") && !entry.isDirectory()) {
                        // 构建目标文件路径
                        String fileName = entryName.substring("natives/".length());
                        Path destPath = nativesDir.resolve(fileName);

                        // 确保父目录存在
                        Files.createDirectories(destPath.getParent());

                        // 提取文件
                        try (InputStream in = jarFile.getInputStream(entry);
                             OutputStream out = Files.newOutputStream(destPath)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }

                        System.out.println("Extracted: " + destPath);
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            System.err.println("Failed to extract native files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 保持原有无参方法向后兼容
    public static void extractNativesFromJar() {
        extractNativesFromJar(null);
    }

    // 示例用法
    public static void main(String[] args) {
        // 用法1：解压到默认的natives目录
        extractNativesFromJar();

        // 用法2：解压到指定目录
        File customDir = new File("custom_natives");
        extractNativesFromJar(customDir);
    }
}