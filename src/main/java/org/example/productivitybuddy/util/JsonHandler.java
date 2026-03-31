package org.example.productivitybuddy.util;

import org.example.productivitybuddy.dto.ProcessInfoWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;

public class JsonHandler {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void writeJson(Object obj, Path path) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), obj);
    }
    public static ProcessInfoWrapper readJson(Path path) throws IOException {
        return mapper.readValue(path.toFile(), ProcessInfoWrapper.class);
    }
}
