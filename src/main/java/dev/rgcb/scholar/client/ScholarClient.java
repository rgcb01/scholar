package dev.rgcb.scholar.client;

import dev.rgcb.scholar.Scholar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = Scholar.MOD_ID, value = Dist.CLIENT)
public final class ScholarClient {
    private ScholarClient() {
    }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        ProductionClientCommands.register(event);
    }
}
