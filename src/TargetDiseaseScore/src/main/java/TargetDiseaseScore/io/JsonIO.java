package TargetDiseaseScore.io;

import TargetDiseaseScore.dto.TDAssociation;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonIO {
    private final ObjectMapper mapper;

    public JsonIO() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.CLOSE_CLOSEABLE, false);
    }

    public <T> List<T> LinesToObj(BufferedReader reader, Class<T> type) throws IOException {

        return mapper
                .readValues(mapper.createParser(reader), type)
                .readAll();

    }

    public <T> List<T> LinesToObj(Stream<String> stringStream, Class<T> type) {

        return stringStream.parallel()
                .map(l -> {
                    try {
                        return mapper.readValue(l, type);
                    } catch (IOException ex) {
                        throw new RuntimeException("JSON mapping failed...", ex);
                    }
                })
                .collect(Collectors.toList());
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











