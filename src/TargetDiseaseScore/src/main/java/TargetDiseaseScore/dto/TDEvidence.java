package TargetDiseaseScore.dto;

public class TDEvidence {

    private String targetId;
    private String diseaseId;
    private double score;

    public TDEvidence() {}

    public TDEvidence(String targetId, String diseaseId, double score) {
        this.targetId = targetId;
        this.diseaseId = diseaseId;
        this.score = score;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDiseaseId() {
        return diseaseId;
    }

    public void setDiseaseId(String diseaseId) {
        this.diseaseId = diseaseId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return String.format("targetId = %s, diseaseId = %s, score = %.2f"
        , targetId, diseaseId, score);
    }
}
