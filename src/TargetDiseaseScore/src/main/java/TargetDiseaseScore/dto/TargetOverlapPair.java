package TargetDiseaseScore.dto;

import java.util.Set;

public class TargetOverlapPair {
    public TargetOverlapPair(String targetIdA, String targetIdB, Set<String> diseasesShared){
        this.targetIdA = targetIdA;
        this.targetIdB = targetIdB;
        this.diseasesShared = diseasesShared;
    }

    public String toString() {
        return String.format("TargetA:[%s] TargetB:[%s] Shared diseases:%s"
                , targetIdA, targetIdB, diseasesShared);
    }

    public String getTargetIdA() {
        return targetIdA;
    }

    public String getTargetIdB() {
        return targetIdB;
    }

    private String targetIdA;
    private String targetIdB;
    private Set<String> diseasesShared;
}