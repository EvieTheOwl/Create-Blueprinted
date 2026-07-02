package net.swzo.create_blueprinted.util;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.swzo.create_blueprinted.CreateBlueprinted.LOGGER;

public class DebugTimer {

    private final LinkedHashMap<String, Instant> stages;
    private String taskName;

    public DebugTimer() {
        this.stages = new LinkedHashMap<>();
    }

    public void createTask(String taskName, String firstStageName) {
        reset();
        this.taskName = taskName;
        markInstant(firstStageName);
    }

    public void markInstant(String stageName) {
        stages.put(stageName, Instant.now());
    }

    public void finishAndPrint(String finalStageName) {
        markInstant(finalStageName);
        LOGGER.info("Debug Timer - Task: {}", taskName);

        Instant prevInstant = stages.firstEntry().getValue();
        for (Map.Entry<String, Instant> stage : stages.entrySet()) {
            String stageName = stage.getKey();
            Instant nowInstant = stage.getValue();
            long timeElapsed = Duration.between(prevInstant, nowInstant).toMillis();

            LOGGER.info("> {} - {}ms", stageName, timeElapsed);
            prevInstant = nowInstant;
        }
        reset();
    }

    public void reset() {
        taskName = "";
        stages.clear();;
    }
}
