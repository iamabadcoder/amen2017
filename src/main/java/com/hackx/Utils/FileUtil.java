package com.hackx.Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileUtil {
    public static void write2File(String fileName, String content) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, true), "UTF-8"));
            writer.write(content + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String tongChengUrlFile = "src/main/java/com/hackx/Utils/tongcheng_urls.txt";
        String tongChengDataFile = "src/main/java/com/hackx/Utils/tongcheng_datas.txt";
        String qunarUrlFile = "src/main/java/com/hackx/Utils/qunar_urls.txt";
        String qunarDataFile = "src/main/java/com/hackx/Utils/qunar_datas.txt";
        try {
            List<String> lines = Files.readAllLines(Paths.get(qunarUrlFile), StandardCharsets.UTF_8);
            for (String line : lines) {
                write2File(qunarDataFile, "url:" + "'" + line + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
