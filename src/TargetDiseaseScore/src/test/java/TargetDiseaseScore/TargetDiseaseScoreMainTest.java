package TargetDiseaseScore;

import TargetDiseaseScore.dto.TDAssociation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TargetDiseaseScoreMainTest {

    private TargetDiseaseScoreMain processor = new TargetDiseaseScoreMain();

    @Test
    public void givenSingleTDAssociation_and_outDir_jsonFileGenerated(@TempDir Path outputDir) {
        List<TDAssociation> list = new ArrayList<>();
        list.add(new TDAssociation());

        processor.exportJointDataSet(list, outputDir, "json_file.json");

        assertTrue(Files.exists(outputDir), ()->"Method should create a json file on the output directory");
    }
}
