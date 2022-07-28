package TargetDiseaseScore.dto;

import java.util.Set;

public class TargetOverlapPair {
    private String targetIdA;
    private String targetIdB;
    private final Set<String> diseasesShared;

    public TargetOverlapPair(String targetIdA, String targetIdB, Set<String> diseasesShared){
        this.targetIdA = targetIdA;
        this.targetIdB = targetIdB;
        this.diseasesShared = diseasesShared;
    }

    public String getTargetIdA() {
        return targetIdA;
    }

    public String getTargetIdB() {
        return targetIdB;
    }

    public Set<String> getDiseasesShared() {
        return diseasesShared;
    }

    public String toString() {
        return String.format("TargetA:[%s] TargetB:[%s] Shared diseases:%s"
                , targetIdA, targetIdB, diseasesShared);
    }
}