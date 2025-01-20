package com.example.dependencyanalyzer;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class ParserService {
    private static final Logger logger = Logger.getLogger(ParserService.class.getName());
    public List<String> parse(InputStream inputStream) throws ParseException {
        Parser parser = new Parser(inputStream);
        parser.Start();
        return parser.getImports();
    }

    public List<String> findCircularDependencies(Map<String, List<String>> fileImports) {
        List<String> circularDependencies = new LinkedList<>();
        for (Map.Entry<String, List<String>> entry : fileImports.entrySet()) {
            String fileName = entry.getKey();
            List<String> imports = entry.getValue();
            for (String importedFile : imports) {
                if (fileImports.containsKey(importedFile) && fileImports.get(importedFile).contains(fileName)) {
                    circularDependencies.add("Circular dependency found between " + fileName + " and " + importedFile);
                }
            }
        }
        return circularDependencies;
    }

    /**
     * This method truncates targets like class, function, etc., leaving just a module path.
     * @param fileImports - map of file names and their imports
     * @return map of file names and their imports with truncated targets
     */
    public Map<String, List<String>> resolveTargetFiles(Map<String, List<String>> fileImports) {
        Map<String, List<String>> moduleImports = new java.util.HashMap<>();
        for (Map.Entry<String, List<String>> entry : fileImports.entrySet()) {
            String fileName = entry.getKey();
            List<String> imports = entry.getValue();
            List<String> moduleImportsList = new LinkedList<>();

            for (String importedFile : imports) {
                if (fileImports.keySet().stream().noneMatch(importedFile::endsWith)) {
                    logger.info("Truncated target: " + importedFile);
                    String modulePath = truncateTarget(importedFile);
                    moduleImportsList.add(modulePath);
                }
            }
            moduleImports.put(fileName, moduleImportsList);
        }
        logger.info("Resolved modules: " + moduleImports);
        return moduleImports;
    }

    private String truncateTarget(String path) {
        int lastDotIndex = path.lastIndexOf("/");
        return lastDotIndex == -1 ? path : path.substring(0, lastDotIndex);
    }

    private List<String> findDirectDependencies(Map<String, List<String>> fileImports, String fileName) {
        List<String> directDependencies = new LinkedList<>();
        for (Map.Entry<String, List<String>> entry : fileImports.entrySet()) {
            if (entry.getValue().contains(fileName)) {
                directDependencies.add(entry.getKey());
            }
        }
        return directDependencies;
    }
}
