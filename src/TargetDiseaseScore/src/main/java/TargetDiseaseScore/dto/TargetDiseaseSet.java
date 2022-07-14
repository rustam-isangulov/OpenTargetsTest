package TargetDiseaseScore.dto;

import java.util.Set;

public class TargetDiseaseSet {
    public TargetDiseaseSet(String targetId, Set<String> diseases) {
        this.targetId = targetId;
        this.diseases = diseases;
    }

    public String targetId;
    public Set<String> diseases;

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
}