package uk.ac.ebi.subs.biostudies.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

public class TestUtil {
    public static Object loadObjectFromJson(String filePath, Class<?> clazz) {
        Object result;
        try {
            String json = readFile(filePath);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            result = objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static String readFile(String path) {
        String fileContent;

        try {
            fileContent = IOUtils.toString(TestUtil.class.getClassLoader().getResourceAsStream(path), "UTF-8");
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }

        return fileContent;
    }

}
