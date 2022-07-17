package TargetDiseaseScore.dto;

import java.util.List;
import java.util.Set;

public class TargetDiseaseSearchCell {
    private String targetId;
    private Set<String> diseases;
    private List<TargetDiseaseSet> targetsToCheck;

    public TargetDiseaseSearchCell(String targetId, Set<String> diseases, List<TargetDiseaseSet> targetsToCheck) {
        this.targetId = targetId;
        this.diseases = diseases;
        this.targetsToCheck = targetsToCheck;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Set<String> getDiseases() {
        return diseases;
    }

    public void setDiseases(Set<String> diseases) {
        this.diseases = diseases;
    }

    public List<TargetDiseaseSet> getTargetsToCheck() {
        return targetsToCheck;
    }

    public void setTargetsToCheck(List<TargetDiseaseSet> targetsToCheck) {
        this.targetsToCheck = targetsToCheck;
    }
}