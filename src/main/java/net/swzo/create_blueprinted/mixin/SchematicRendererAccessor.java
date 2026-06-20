package net.swzo.create_blueprinted.mixin;

import com.simibubi.create.content.schematics.client.SchematicRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SchematicRenderer.class)
public interface SchematicRendererAccessor {

    @Invoker("redraw")
    void create_blueprinted$redraw();

    @Accessor("changed")
    void create_blueprinted$setChanged(boolean changed);
}
