package com.bmsit.aws.media.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/files")
public class MediaController {
    @Autowired
    private FileSystem fileSystem;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Integer>> uploadFile(@RequestParam("file") MultipartFile file) {
        Path filePath = new Path("/user/hadoop/upload/" + file.getOriginalFilename());
        try (FSDataOutputStream outputStream = fileSystem.create(filePath)) {
            outputStream.write(file.getBytes());
            Map<String, Integer> wordCountMap = new HashMap<>();
            return ResponseEntity.ok(calculateWordCount(filePath));
        } catch (IOException e) {
            System.out.println(e);
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/download")
    public void downloadFile(@RequestParam("filename") String filename, HttpServletResponse response) {
        try (FSDataInputStream inputStream = fileSystem.open(new Path("/user/hadoop/upload/" + filename))) {
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles() {
        List<String> fileNames = new ArrayList<>();
        Path uploadsDir = new Path("/user/hadoop/upload/");
        try {
            if (fileSystem.exists(uploadsDir)) {
                RemoteIterator<LocatedFileStatus> fileStatusListIterator = fileSystem.listFiles(uploadsDir, false);
                while (fileStatusListIterator.hasNext()) {
                    LocatedFileStatus fileStatus = fileStatusListIterator.next();
                    fileNames.add(fileStatus.getPath().getName());
                }
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        return ResponseEntity.ok(fileNames);
    }

    @GetMapping("/wordcount")
    public ResponseEntity<Map<String, Integer>> getWordCount(@RequestParam("filename") String filename) {
        Path filePath = new Path("/user/hadoop/upload/" + filename);
        try {
            if (fileSystem.exists(filePath)) {
                return ResponseEntity.ok(calculateWordCount(filePath));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private Map<String, Integer> calculateWordCount(Path filePath) throws IOException {
        Map<String, Integer> wordCountMap = new HashMap<>();
        Pattern pattern = Pattern.compile("\\W+"); // Regex pattern to match non-word characters
        try (FSDataInputStream inputStream = fileSystem.open(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = pattern.split(line.toLowerCase());
                for (String word : words) {
                    if (!word.trim().isEmpty()) {
                        wordCountMap.put(word, wordCountMap.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }
        return wordCountMap;
    }
}
