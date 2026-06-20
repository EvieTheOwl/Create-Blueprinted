package net.swzo.create_blueprinted.render;

public final class SchematicRenderSettings {

    public static final float ISOMETRIC_PITCH = 35.264f;

    public static final int DEFAULT_IMAGE_WIDTH = 1024;

    public static final int DEFAULT_ANTIALIASING = 2;
    public static final int MAX_ANTIALIASING = 4;

    public enum View {
        ISOMETRIC_RIGHT(-45f, ISOMETRIC_PITCH, 0f),
        ISOMETRIC_LEFT(45f, ISOMETRIC_PITCH, 0f),
        FRONT(0f, 0f, 0f),
        BACK(180f, 0f, 0f),
        LEFT(90f, 0f, 0f),
        RIGHT(-90f, 0f, 0f),
        TOP(0f, 90f, 0f),
        BOTTOM(0f, -90f, 0f);

        public final float yaw;
        public final float pitch;
        public final float roll;

        View(float yaw, float pitch, float roll) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }
    }

    private final float yaw;
    private final float pitch;
    private final float roll;
    private final int imageWidth;
    private final int backgroundColor;
    private final int antialiasing;

    private SchematicRenderSettings(Builder b) {
        this.yaw = b.yaw;
        this.pitch = b.pitch;
        this.roll = b.roll;
        this.imageWidth = b.imageWidth;
        this.backgroundColor = b.backgroundColor;
        this.antialiasing = b.antialiasing;
    }

    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public float roll() { return roll; }
    public int imageWidth() { return imageWidth; }
    public int backgroundColor() { return backgroundColor; }
    public int antialiasing() { return antialiasing; }

    public static Builder builder() {
        return new Builder();
    }

    public static SchematicRenderSettings isometric(View view, int imageWidth) {
        return builder().view(view).imageWidth(imageWidth).build();
    }

    public SchematicRenderSettings withImageWidth(int newImageWidth) {
        return builder().angles(yaw, pitch, roll).imageWidth(newImageWidth)
                .backgroundColor(backgroundColor).antialiasing(antialiasing).build();
    }

    public static SchematicRenderSettings fromOrientation(String orientation, int imageWidth) {
        return builder().view(parseView(orientation)).imageWidth(imageWidth).build();
    }

    public static View parseView(String name) {
        if (name == null) return View.ISOMETRIC_RIGHT;
        switch (name.trim().toLowerCase()) {
            case "left":
                return View.ISOMETRIC_LEFT;
            case "right":
                return View.ISOMETRIC_RIGHT;
            default:
                String normalized = name.trim().toUpperCase().replace('-', '_').replace(' ', '_');
                try {
                    return View.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    return View.ISOMETRIC_RIGHT;
                }
        }
    }

    public static final class Builder {
        private float yaw = View.ISOMETRIC_RIGHT.yaw;
        private float pitch = View.ISOMETRIC_RIGHT.pitch;
        private float roll = View.ISOMETRIC_RIGHT.roll;
        private int imageWidth = DEFAULT_IMAGE_WIDTH;
        private int backgroundColor = 0x00000000;
        private int antialiasing = DEFAULT_ANTIALIASING;

        public Builder view(View view) {
            this.yaw = view.yaw;
            this.pitch = view.pitch;
            this.roll = view.roll;
            return this;
        }

        public Builder angles(float yaw, float pitch, float roll) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
            return this;
        }

        public Builder yaw(float yaw) { this.yaw = yaw; return this; }
        public Builder pitch(float pitch) { this.pitch = pitch; return this; }
        public Builder roll(float roll) { this.roll = roll; return this; }

        public Builder imageWidth(int imageWidth) {
            this.imageWidth = Math.max(16, imageWidth);
            return this;
        }

        public Builder backgroundColor(int argb) {
            this.backgroundColor = argb;
            return this;
        }

        public Builder antialiasing(int factor) {
            this.antialiasing = Math.max(1, Math.min(MAX_ANTIALIASING, factor));
            return this;
        }

        public SchematicRenderSettings build() {
            return new SchematicRenderSettings(this);
        }
    }
}
