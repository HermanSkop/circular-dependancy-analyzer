package com.example.dependencyanalyzer;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Controller
public class MainController {
    private static final String UPLOAD_DIRECTORY = "uploads/";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("text/x-python-script", "text/javascript");
    private String currentFileType = null;

    @GetMapping("/")
    public String index(@RequestParam(value = "success", required = false) String success,
                        @RequestParam(value = "error", required = false) String error,
                        Model model) {
        if (success != null) {
            model.addAttribute("successMessage", "Directory uploaded successfully!");
        }
        if (error != null) {
            model.addAttribute("errorMessage", "Failed to upload directory.");
        }
        return "index";
    }

    @PostMapping("/upload-directory")
    public String handleDirectoryUpload(@RequestParam("files") List<MultipartFile> files) {
        try {
            File uploadDir = new File(UPLOAD_DIRECTORY);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String rootPath = extractRootDirectory(files);
            if (rootPath == null) {
                throw new IOException("Unable to determine root directory.");
            }
            File rootDir = new File(UPLOAD_DIRECTORY + rootPath);
            if (rootDir.exists()) {
                FileUtils.deleteDirectory(rootDir);
                rootDir.mkdirs();
            }

            saveFiles(files);
            return "redirect:/?success=true";
        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/?error=true";
        }
    }

    private String extractRootDirectory(List<MultipartFile> files) {
        if (files.isEmpty() || files.get(0).getOriginalFilename() == null) {
            return null;
        }

        String firstPath = files.get(0).getOriginalFilename();
        int rootEndIndex = firstPath.indexOf('/');
        return (rootEndIndex != -1) ? firstPath.substring(0, rootEndIndex + 1) : null;
    }

    private void saveFiles(List<MultipartFile> files) throws IOException {
        for (MultipartFile file : files) {
            String originalPath = file.getOriginalFilename();
            if (originalPath == null || originalPath.isEmpty()) {
                continue;
            }

            if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
                continue;
            }

            if (currentFileType == null) {
                currentFileType = file.getContentType();
            } else if (!Objects.equals(currentFileType, file.getContentType())) {
                throw new IOException("Directory contains more than one allowed file type.");
            }

            File destination = new File(UPLOAD_DIRECTORY + originalPath);

            if (!destination.getParentFile().exists()) {
                destination.getParentFile().mkdirs();
            }
            destination.createNewFile();

            try (OutputStream os = new FileOutputStream(destination)) {
                os.write(file.getBytes());
            }
        }
    }
}
