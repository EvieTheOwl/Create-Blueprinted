package net.swzo.create_blueprinted.api;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.swzo.create_blueprinted.render.SchematicRenderSettings;
import org.jetbrains.annotations.Nullable;

public abstract class RenderSchematicImageEvent<T> extends Event implements ICancellableEvent {

    private final String fileName;
    private final @Nullable T imageContent;
    private final SchematicRenderSettings.Builder settingsBuilder;
    private final Action action;

    public enum Action { EXPORT, SHARE }

    protected RenderSchematicImageEvent(String fileName, @Nullable T imageContent, SchematicRenderSettings.Builder settingsBuilder, Action action) {
        this.fileName = fileName;
        this.imageContent = imageContent;
        this.settingsBuilder = settingsBuilder;
        this.action = action;
    }

    protected SchematicRenderSettings.Builder modifyRenderSettings() {
        return settingsBuilder;
    }

    public String getFileName() { return fileName; }
    public @Nullable T getImageContent() { return imageContent; }
    public SchematicRenderSettings getRenderSettings() { return settingsBuilder.build(); }
    public Action getAction() { return action; }

    public static class Pre extends RenderSchematicImageEvent<SchematicLevel> {

        public Pre(String fileName, @Nullable SchematicLevel schematicLevel, SchematicRenderSettings.Builder settingsBuilder, Action action) {
            super(fileName, schematicLevel, settingsBuilder, action);
        }

        @Override
        public SchematicRenderSettings.Builder modifyRenderSettings() {
            return super.modifyRenderSettings();
        }
    }

    public static class Post extends RenderSchematicImageEvent<byte @Nullable []> {

        public Post(String fileName, byte @Nullable [] imageByteArray, SchematicRenderSettings.Builder finalSettingsBuilder, Action action) {
            super(fileName, imageByteArray, finalSettingsBuilder, action);
        }
    }
}
