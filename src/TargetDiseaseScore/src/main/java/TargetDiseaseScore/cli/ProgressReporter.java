package TargetDiseaseScore.cli;

import java.util.HashMap;
import java.util.Map;

public class ProgressReporter {
    private final String processingOf;
    private final String numberOf;
    private long startTime;
    private static Map<Action, ProgressReporter> map = new HashMap<>();

    public enum Action {EVIDENCE_FILE, EVIDENCE_MAP, TARGETS, DISEASES, JOINT_DATASET, SEARCHING}

    static
    {
        map.put(Action.EVIDENCE_FILE, new ProgressReporter
                ("evidence data files"
                , "target-disease associations"));

        map.put(Action.EVIDENCE_MAP, new ProgressReporter
                ("evidence map"
                        , "target-disease overall scores"));

        map.put(Action.TARGETS, new ProgressReporter
                ("target data"
                        , "targets"));

        map.put(Action.DISEASES, new ProgressReporter
                ("disease data"
                        , "diseases"));

        map.put(Action.JOINT_DATASET, new ProgressReporter
                ("targets with shared disease connections"
                        , "json files"));

        map.put(Action.SEARCHING, new ProgressReporter
                ("joint Association/Target/Disease data set"
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
