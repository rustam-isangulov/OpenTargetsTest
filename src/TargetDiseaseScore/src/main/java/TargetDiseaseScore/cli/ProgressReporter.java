package TargetDiseaseScore.cli;

public class ProgressReporter {
    private long markTime;

    public  void mark() {
        markTime = System.nanoTime();
    }

    public  void report(String processDesc, String numberOfDesc, int numberToReport) {
        long elapsedTime = System.nanoTime() - markTime;

        System.out.println();
        System.out.format("Elapsed time for %s: %.0f (ms)", processDesc, elapsedTime * 1e-6);
        System.out.println();
        System.out.format("Number of %s: %d", numberOfDesc, numberToReport);
        System.out.println();

        markTime = System.nanoTime();
    }
}
