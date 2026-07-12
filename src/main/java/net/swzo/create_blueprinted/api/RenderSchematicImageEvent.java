package net.swzo.create_blueprinted.api;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.swzo.create_blueprinted.render.SchematicRenderSettings;
import org.jetbrains.annotations.Nullable;

public abstract class RenderSchematicImageEvent<T> extends Event implements ICancellableEvent {

    private final ResourceLocation handlerId;
    private final String schematicName;
    private final @Nullable T imageContent;
    private final SchematicRenderSettings.Builder settingsBuilder;
    private final Action action;

    public enum Action { EXPORT, SHARE }

    protected RenderSchematicImageEvent(ResourceLocation handlerId, String schematicName, SchematicRenderSettings.Builder settingsBuilder, Action action, @Nullable T imageContent) {
        this.handlerId = handlerId;
        this.schematicName = schematicName;
        this.imageContent = imageContent;
        this.settingsBuilder = settingsBuilder;
        this.action = action;
    }

    protected SchematicRenderSettings.Builder modifyRenderSettings() {
        return settingsBuilder;
    }

    public ResourceLocation getHandlerId() { return handlerId; }
    public String getSchematicName() { return schematicName; }
    public @Nullable T getImageContent() { return imageContent; }
    public SchematicRenderSettings getRenderSettings() { return settingsBuilder.build(); }
    public Action getAction() { return action; }

    public static class Pre extends RenderSchematicImageEvent<SchematicLevel> {

        /**
         * Fired before a schematic is rendered. Includes the {@link SchematicLevel} which represents
         * the content that is about to be rendered.
         *
         * @param handlerId The image handlers ID (Default ID = create_blueprinted:default)
         * @param schematicName Name of the schematic
         * @param settingsBuilder Settings used to render the schematic
         * @param action Determines if this event is the result of an image being exported or shared
         * @param schematicLevel Level that represents the schematics content before it's rendered
         *                       The contents of the level can be modified on the client thread
         */
        public Pre(ResourceLocation handlerId, String schematicName,  SchematicRenderSettings.Builder settingsBuilder, Action action, @Nullable SchematicLevel schematicLevel) {
            super(handlerId, schematicName, settingsBuilder, action, schematicLevel);
        }

        @Override
        public SchematicRenderSettings.Builder modifyRenderSettings() {
            return super.modifyRenderSettings();
        }
    }

    public static class Post extends RenderSchematicImageEvent<byte @Nullable []> {

        /**
         * Fired after a schematic is rendered and before it is about to be exported or shared.
         * Includes a byte array of the rendered PNG image.
         *
         * @param handlerId The image handlers ID (Default ID = create_blueprinted:default)
         * @param schematicName Name of the schematic
         * @param finalSettingsBuilder The settings used to render the schematic
         * @param action  Determines if this event is the result of an image being exported or shared
         * @param imageByteArray The rendered PNG schematic image in a byte array format
         */
        public Post(ResourceLocation handlerId, String schematicName, SchematicRenderSettings.Builder finalSettingsBuilder, Action action, byte @Nullable [] imageByteArray) {
            super(handlerId, schematicName, finalSettingsBuilder, action, imageByteArray);
        }
    }
}
