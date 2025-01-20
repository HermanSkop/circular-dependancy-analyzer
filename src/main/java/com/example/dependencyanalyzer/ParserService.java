package com.example.dependencyanalyzer;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

@Service
public class ParserService {
    private static final Logger logger = Logger.getLogger(ParserService.class.getName());
    public List<String> parse(InputStream inputStream) throws ParseException {
        Parser parser = new Parser(inputStream);
        parser.Start();
        return parser.getImports();
    }

    public List<Stack<String>> findCircularDependencies(Map<String, List<String>> fileImports) {
        List<Stack<String>> circularDependencies = new LinkedList<>();
        for (Map.Entry<String, List<String>> entry : fileImports.entrySet()) {
            findCircularDependenciesInModule(fileImports, entry.getKey(), entry.getKey(), circularDependencies, new Stack<>());
        }

        circularDependencies.removeIf(stack ->
                circularDependencies.stream()
                        .filter(stack2 -> stack != stack2)
                        .anyMatch(stack2 -> new HashSet<>(stack).equals(new HashSet<>(stack2)))
        );
        return circularDependencies;
    }

    private void findCircularDependenciesInModule(Map<String, List<String>> fileImports, String rootModulePath, String modulePath,
                                                  List<Stack<String>> circularDependencies, Stack<String> visitedModules) {
        visitedModules.add(modulePath);
        List<String> imports = fileImports.get(modulePath);
        for (String importedFile : imports) {
            if (!fileImports.containsKey(importedFile))
                continue;
            if (Objects.equals(rootModulePath, importedFile)) {
                circularDependencies.add(visitedModules);
            } else {
                if (visitedModules.contains(importedFile))
                    continue;

                findCircularDependenciesInModule(fileImports, rootModulePath, importedFile, circularDependencies, (Stack<String>) visitedModules.clone());
            }
        }
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
                String modulePath = importedFile;
                if (fileImports.keySet().stream().noneMatch(importedFile::endsWith)) {
                    modulePath = truncateTarget(importedFile);
                    if (fileImports.keySet().stream().noneMatch(modulePath::endsWith))
                        continue;

                }
                moduleImportsList.add(modulePath);
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
