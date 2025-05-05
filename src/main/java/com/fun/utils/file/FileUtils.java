package com.fun.utils.file;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static String readPath(String path) {
        StringBuilder builder = new StringBuilder();

        InputStream is = null;
        try {
            is = FileUtils.class.getResourceAsStream(path);
            if (is == null) return null;

            int b;
            while ((b = is.read()) != -1) {
                builder.append((char) b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return builder.toString();
    }

    /**
     * 获取文件后缀的方法
     *
     * @param file 要获取文件后缀的文件
     * @return 文件后缀
     * @author https://www.4spaces.org/
     */
    public static String getFileExtension(File file) {
        String extension = "";
        try {
            if (file != null && file.exists()) {
                String name = file.getName();
                extension = name.substring(name.lastIndexOf("."));
            }
        } catch (Exception e) {
            extension = "";
        }
        return extension;
    }
    public static String readInputStream(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line).append(System.lineSeparator());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static List<String> readURLLines(String urlString) {
        try {
            List<String> lines = new ArrayList<>();
            URL url = new URL(urlString);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            return lines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
