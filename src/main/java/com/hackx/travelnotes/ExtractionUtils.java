package com.hackx.travelnotes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by hackx on 2/10/17.
 */
public class ExtractionUtils {

    public static void main(String[] args) {

        String fileName = "src/main/java/com/hackx/travelnotes/ma_feng_wo_url.txt";
        generageUrls(fileName);

    }

    public static void generageUrls(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] fields = line.split("\\t");
                System.out.println("url:" + "'" + fields[2] + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
