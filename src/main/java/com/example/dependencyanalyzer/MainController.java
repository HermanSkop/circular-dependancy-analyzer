package com.example.dependencyanalyzer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@Controller
public class MainController {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("text/x-python-script", "text/javascript");
    private String currentFileType = null;
    private final ParserService parserService;
    private static final Logger logger = Logger.getLogger(MainController.class.getName());

    @Autowired
    public MainController(ParserService parserService) {
        this.parserService = parserService;
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "analysisResult", required = false) String analysisResult,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        if (analysisResult != null) {
            model.addAttribute("analysisResult", analysisResult);
        }
        return "index";
    }

    @PostMapping("/upload-directory")
    public String handleDirectoryUpload(@RequestParam("files") List<MultipartFile> files, Model model) {
        try {
            validateFiles(files);

            List<String> analysisResult = analyzeDirectory(files);
            model.addAttribute("analysisResult", analysisResult);

            return "index";
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getMessage());
            return "index";
        }
    }


    private List<String> analyzeDirectory(List<MultipartFile> files) throws IOException {
        List<Future<Map<String, List<String>>>> futures = new ArrayList<>();
        List<String> errors;

        Map<String, List<String>> fileImports;
        try {
            fileImports = fetchImports(files, futures);
            fileImports = parserService.resolveTargetFiles(fileImports);
            errors = parserService.findCircularDependencies(fileImports);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to analyze directory", e);
        }
        return errors;
    }

    private Map<String, List<String>> fetchImports(List<MultipartFile> files, List<Future<Map<String, List<String>>>> futures) {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (MultipartFile file : files) {
            Future<Map<String, List<String>>> future = executorService.submit(() -> {
                try {
                    Map<String, List<String>> map = new HashMap<>();
                    String preparedPath = truncateExtension(truncateRoot(Objects.requireNonNull(file.getOriginalFilename())));

                    map.put(preparedPath, parserService.parse(file.getInputStream()));
                    return map;
                } catch (IOException e) {
                    throw new IOException("Failed to read file: " + file.getOriginalFilename());
                } catch (TokenMgrError e) {
                    throw new RuntimeException("Parsing error in file: " + file.getOriginalFilename(), e);
                }
            });
            futures.add(future);
        }

        Map<String, List<String>> fileImports = new HashMap<>();
        for (Future<Map<String, List<String>>> future : futures) {
            try {
                fileImports.putAll(future.get());
            } catch (ExecutionException e) {
                executorService.shutdown();
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return fileImports;
    }

    private void validateFiles(List<MultipartFile> files) throws IOException {
        Iterator<MultipartFile> iterator = files.iterator();
        while (iterator.hasNext()) {
            MultipartFile file = iterator.next();
            String originalPath = file.getOriginalFilename();
            if (originalPath == null || originalPath.isEmpty() || file.getContentType() == null ||
                    !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
                iterator.remove();
                continue;
            }

            if (currentFileType == null) {
                currentFileType = file.getContentType();
            } else if (!Objects.equals(currentFileType, file.getContentType())) {
                throw new IOException("Directory contains more than one allowed file type.");
            }
        }
    }

    private String truncateRoot(String path) {
        int index = path.indexOf('/');
        return (index != -1) ? path.substring(index + 1) : path;
    }

    private String truncateExtension(String path) {
        int index = path.lastIndexOf('.');
        return (index != -1) ? path.substring(0, index) : path;
    }

}
