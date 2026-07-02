package net.swzo.create_blueprinted.mixin;

import org.example.test.createschematicpreview.client.SchematicPreviewPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SchematicPreviewPanel.class)
public interface SchematicPreviewAccessor {

    @Accessor("pitch")
    float pitch();

    @Accessor("yaw")
    float yaw();
}
