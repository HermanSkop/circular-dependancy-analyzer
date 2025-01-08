package com.example.dependencyanalyzer;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

@Service
public class ParserService {
    private static final Logger logger = Logger.getLogger(ParserService.class.getName());
    public List<String> parse(InputStream inputStream) throws ParseException {
        Parser parser = new Parser(inputStream);
        parser.Start();
        return parser.getImports();
    }

}
