package TargetDiseaseScore.dto;

import java.util.List;

public class TDComposite {
    private String targetId;
    private String diseaseId;
    private double medianScore;
    private List<Double> topScores;

    public TDComposite(){}

    public TDComposite(String targetId, String diseaseId, double medianScore, List<Double> topScores) {
        this.targetId = targetId;
        this.diseaseId = diseaseId;
        this.medianScore = medianScore;
        this.topScores =  topScores;
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

    public double getMedianScore() {
        return medianScore;
    }

    public void setMedianScore(double medianScore) {
        this.medianScore = medianScore;
    }

    public List<Double> getTopScores() {
        return topScores;
    }

    public void setTopScores(List<Double> topScores) {
        this.topScores = topScores;
    }

    @Override
    public String toString() {
        return String.format("targetId = %s, diseaseId = %s, medianScore = %.2f, topScores=%s"
                , targetId, diseaseId, medianScore, topScores.toString());
    }
}
