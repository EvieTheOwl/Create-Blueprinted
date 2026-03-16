package net.swzo.create_blueprinted;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.util.RegistrateDistExecutor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(CB.MODID)
public class CB {
    public static final String MODID = "create_blueprinted";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CB(IEventBus modEventBus, ModContainer container) {
        RegistrateDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CBClient.onCtorClient(modEventBus, NeoForge.EVENT_BUS, container));
    }
}
