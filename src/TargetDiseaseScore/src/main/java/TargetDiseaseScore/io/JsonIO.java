package TargetDiseaseScore.io;

import TargetDiseaseScore.dto.TDAssociation;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class JsonIO {
    public static <T> List<T> ParseFile(Path inFile, Class<T> type) {

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try( var s = Files.lines(inFile)) {

            var list = s.parallel()
                    .map(l -> {
                        try{
                            return mapper.readValue(l, type);
                        } catch (IOException ex) {
                            throw new RuntimeException("JSON mapping failed...", ex);
                        }
                    })
                    .collect(Collectors.toList());

            return list;

        } catch (IOException ex) {
            throw new RuntimeException("Parsing evidence files: something bad happened with IO...", ex);
        }
    }

    public static void exportToJson(List<TDAssociation> list, Path outputDir, String filePattern)  {

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.CLOSE_CLOSEABLE, false);

        // for now assume single file export...
        Path file = outputDir.resolve(filePattern);

        try (var writer = Files.newBufferedWriter(file)) {
            list.forEach(a -> {
                try {
                    writer.write(objectMapper.writeValueAsString(a));
                    writer.newLine();
                } catch (IOException ex) {
                    throw new RuntimeException("JSON write has failed...", ex);
                }
            });

        } catch(IOException ex) {
            throw new RuntimeException("Writing json output: something bad happened with IO...");
        }
    }
}
