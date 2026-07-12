package net.swzo.create_blueprinted.render;

import net.createmod.catnip.theme.Color;

import static net.swzo.create_blueprinted.CreateBlueprintedConfig.CONFIG;

public class SchematicRenderSettings {

    public static final Color DEFAULT_BG_COLOR = Color.TRANSPARENT_BLACK;
    public static final int MAX_ANTIALIASING = 4;
    public static final int MIN_WIDTH = 64;
    public static final int MAX_WIDTH = 8192;

    private final int imageWidth;
    private final Orientation orientation;
    private final int antialiasingFactor;
    private final Color backgroundColor;

    private SchematicRenderSettings(int imageWidth, Orientation orientation, int antialiasingFactor, Color backgroundColor) {
        this.imageWidth = imageWidth;
        this.orientation = orientation;
        this.antialiasingFactor = antialiasingFactor;
        this.backgroundColor = backgroundColor;

        if (imageWidth < MIN_WIDTH || imageWidth > MAX_WIDTH)
            throw new IllegalArgumentException("Width must be between " + MIN_WIDTH + " and " + MAX_WIDTH);
        if (antialiasingFactor < 1 || antialiasingFactor > MAX_ANTIALIASING)
            throw new IllegalArgumentException("Antialiasing must be between 1 and " + MAX_ANTIALIASING);
    }

    public static Builder builder() {
        return new Builder();
    }

    public int imageWidth() { return imageWidth * antialiasingFactor; }
    public Orientation orientation() { return orientation; }
    public int antialiasingFactor() { return antialiasingFactor; }
    public Color backgroundColor() { return backgroundColor; }

    public record Orientation(float yaw, float pitch, float roll) {

        public static final Orientation ISOMETRIC_RIGHT = new Orientation(-45f, 35.264f, 0f);
        public static final Orientation ISOMETRIC_LEFT = new Orientation(45f, 35.264f, 0f);

        public Orientation(float yaw, float pitch) {
            this(yaw, pitch, 0);
        }
    }

    // Builder class
    public static class Builder {
        private int imageWidth;
        private Orientation orientation;
        private int antialiasingFactor;
        private Color backgroundColor;

        public Builder() {
            this.imageWidth = CONFIG.defaultWidth.get();
            this.orientation = Orientation.ISOMETRIC_RIGHT;
            this.antialiasingFactor = CONFIG.defaultAntialiasing.get();
            this.backgroundColor = DEFAULT_BG_COLOR;
        }

        public Builder(SchematicRenderSettings settings) {
            this.imageWidth = settings.imageWidth;
            this.orientation = settings.orientation;
            this.antialiasingFactor = settings.antialiasingFactor;
            this.backgroundColor = settings.backgroundColor;
        }

        public Builder imageWidth(int width) {
            this.imageWidth = width;
            return this;
        }

        public Builder orientation(Orientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder antialiasingFactor(int factor) {
            this.antialiasingFactor = factor;
            return this;
        }

        public Builder backgroundColor(Color color) {
            this.backgroundColor = color;
            return this;
        }

        public SchematicRenderSettings build() {
            return new SchematicRenderSettings(imageWidth, orientation, antialiasingFactor, backgroundColor);
        }
    }
}

