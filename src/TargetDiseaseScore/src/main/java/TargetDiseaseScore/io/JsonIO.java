package TargetDiseaseScore.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class JsonIO {
    private final ObjectMapper mapper;

    public JsonIO() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.CLOSE_CLOSEABLE, false);
    }

    public <T> T stringToObj(String jsonString, Class<T> type) {
        try {
            return mapper.readValue(jsonString, type);
        } catch (IOException ex) {
            throw new RuntimeException("JSON mapping has failed...", ex);
        }
    }

    public void ObjToJson(List<?> list, BufferedWriter writer) {

        list.forEach(a -> {
            try {
                writer.write(mapper.writeValueAsString(a));
                writer.newLine();
            } catch (IOException ex) {
                throw new RuntimeException("JSON write has failed...", ex);
            }
        });
    }
}











