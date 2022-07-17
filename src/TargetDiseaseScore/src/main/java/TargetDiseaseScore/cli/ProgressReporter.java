package TargetDiseaseScore.cli;

import java.util.HashMap;
import java.util.Map;

public class ProgressReporter {
    private final String processingOf;
    private final String numberOf;
    private long startTime;
    private static Map<Action, ProgressReporter> map = new HashMap<>();

    public enum Action {EVIDENCE, TARGETS, DISEASES, JOINT_DATASET, SEARCHING}

    static
    {
        map.put(Action.EVIDENCE, new ProgressReporter
                ("evidence data"
                , "target-disease overall association scores"));

        map.put(Action.TARGETS, new ProgressReporter
                ("target data"
                        , "targets"));

        map.put(Action.DISEASES, new ProgressReporter
                ("disease data"
                        , "diseases"));

        map.put(Action.JOINT_DATASET, new ProgressReporter
                ("joint Association/Target/Disease data set"
                        , "overall association scores"));

        map.put(Action.SEARCHING, new ProgressReporter
                ("targets with shared disease connections"
                        , "target-target pairs with at least 2 shared connections"));
    }

    private ProgressReporter(String processingOf, String numberOf) {
        this.processingOf = processingOf;
        this.numberOf = numberOf;
    }

    public static ProgressReporter of(Action action) {
        return map.get(action);
    }

    public void start() {
        System.out.println();
        System.out.format("Start processing %s...", this.processingOf);
        System.out.println();
        this.startTime = System.nanoTime();
    }

    public void finish(int numberToReport) {
        long elapsedTime = System.nanoTime() - startTime;

        System.out.format("Total elapsed time for the %s processing: %.0f (ms)", processingOf, elapsedTime * 1e-6);
        System.out.println();
        System.out.format("Number of %s: %d", numberOf, numberToReport);
        System.out.println();
    }
}
