package TargetDiseaseScore.dto;

import java.util.List;

public class TDAssociation {
    private String targetId;
    private String diseaseId;
    private double median;
    private List<Double> top3;
    private String approvedSymbol;
    private String name;

    public TDAssociation() {}

    public TDAssociation(String targetId, String diseaseId, double median, List<Double> top3, String approvedSymbol, String name) {
        this.targetId = targetId;
        this.diseaseId = diseaseId;
        this.median = median;
        this.top3 = top3;
        this.approvedSymbol = approvedSymbol;
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("targetId = %s, diseaseId = %s, median = %.2f, top3 = %s, approvedSymbol = %s, name = [%s]"
                , targetId, diseaseId, median, top3.toString(), approvedSymbol, name);
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

    public double getMedian() {
        return median;
    }

    public void setMedian(double median) {
        this.median = median;
    }

    public List<Double> getTop3() {
        return top3;
    }

    public void setTop3(List<Double> top3) {
        this.top3 = top3;
    }

    public String getApprovedSymbol() {
        return approvedSymbol;
    }

    public void setApprovedSymbol(String approvedSymbol) {
        this.approvedSymbol = approvedSymbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
