package TargetDiseaseScore.dto;

public class Target {
    private String id;
    private String approvedSymbol;

    public Target() {}

    public Target(String id, String approvedSymbol) {
        this.id = id;
        this.approvedSymbol = approvedSymbol;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApprovedSymbol() {
        return approvedSymbol;
    }

    public void setApprovedSymbol(String approvedSymbol) {
        this.approvedSymbol = approvedSymbol;
    }

    @Override
    public String toString() {
        return String.format("targetId: %s approvedSymbol: %s", id, approvedSymbol);
    }
}
