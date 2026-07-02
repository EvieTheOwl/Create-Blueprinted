package net.swzo.create_blueprinted.gui;

import java.util.concurrent.atomic.AtomicReference;

public enum RenderProgress {

    DONE(1.0f),
    RENDERED(0.75f),
    BUILT(0.15f),
    PARSED(0.05f),
    INACTIVE(0.0f);

    private static final AtomicReference<RenderProgress> PROGRESS = new AtomicReference<>(INACTIVE);

    public final float fraction;

    RenderProgress(float fraction) {
        this.fraction = Math.clamp(fraction, 0f, 1f);
    }

    public static void set(RenderProgress progress) {
        PROGRESS.set(progress);
    }

    public static void reset() {
        PROGRESS.set(null);
    }
}
