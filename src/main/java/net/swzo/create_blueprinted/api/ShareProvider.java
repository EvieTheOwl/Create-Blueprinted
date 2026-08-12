package net.swzo.create_blueprinted.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.swzo.create_blueprinted.render.SchematicRenderSettings;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.List;

/**
 * <p>A provider used whenever the share button or command is fired by the player.</p>
 * <p>If you want to provide an implementation that sends an image to a remote server you must
 *     implement some form of serverside image sanitization.</p>
 *
 * Register the provider within {@link net.swzo.create_blueprinted.api.ShareProviderRegistry}
 */
public interface ShareProvider {

    int MAX_URL_CHAR_LENGTH = 50;

    /**
     * Unique ID for the provider.
     *
     * @return Provider ID resource location
     */
    ResourceLocation id();

    /**
     * Describes where the file will be sent. For example: "Brassworks SMP Discord Server".
     *
     * @return A text component representing the schematic image files destination
     */
    Component destinationName();

    /**
     * URL representing where the file will be sent.
     *
     * @return Destination URL. Must use the HTTPS protocol and be shorter than {@link #MAX_URL_CHAR_LENGTH}
     *         characters long.
     */
    String destinationUrl();

    /**
     * Determine what happens to the schematic image after it is rendered. Called on the main client thread.
     *
     * @param handlerId The image handlers ID (Default ID = create_blueprinted:default)
     * @param schematicName Name of the schematic
     * @param renderSettings Settings used to render the schematic
     * @param imageByteArray The rendered PNG schematic image in a byte array format
     * @return A URL representing the location the image has been shared or null if the operation failed
     */
    @Nullable URL onRender(ResourceLocation handlerId, String schematicName, SchematicRenderSettings renderSettings, byte[] imageByteArray);

    /**
     * <p>Fires if schematic image rendering fails. Called on the main client thread.</p>
     * Expected exception types:
     * <ul>
     *     <li>{@code EmptyImageBakeException} - Failed to populate a schematic level with content</li>
     *     <li>{@code SchematicImageRenderException} - Image failed to render</li>
     *     <li>{@code IOException} - Image conversion or validation failed</li>
     *     <li>{@code TimeoutException} - Rendering timed out after
     *              {@link net.swzo.create_blueprinted.handler.SchematicImageHandler#RENDER_TIMEOUT_SECS RENDER_TIMEOUT_SECS}</li>
     * </ul>
     *
     * @param handlerId The image handlers ID (Default ID = create_blueprinted:default)
     * @param schematicName Name of the schematic
     * @param renderSettings Settings used to render the schematic
     * @param error The exception thrown after rendering failed and the associated error message
     * @param errorMessage Associated error message
     */
    default void onRenderFailure(ResourceLocation handlerId, String schematicName, SchematicRenderSettings renderSettings, Throwable error, Component errorMessage) {}

    /**
     * Additional information about the share provider. Displayed in the tooltip below the destination name and url.
     * Also shown before a file is shared using the <code>/schematic share</code> command.
     *
     * @return Text components representing extra information tied to the share provider.
     *         Each element represents a newline.
     */
    default List<Component> extras() { return List.of(); }

    /**
     * Whether to hide the SHIFT & CTRL hints displayed in the schematic table tooltip.
     *
     * @return If true disable hints, otherwise include them.
     */
    default boolean hideTooltipHints() { return false; }

    /**
     * Whether schematic data is also sent. This could include data about block types and positions,
     * block entity data, etc...
     */
    default boolean includeSchematicData() { return false; }

    /**
     * Gets the priority of the current provider. The highest priority provider is shown to the user
     * the first time Create: Configured is loaded. Then it is manually set by the user.
     *
     * @return Priority of the current provider (Default value = 1)
     */
    default int priority() { return 1; }
}
