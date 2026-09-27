package dev.rgcb.scholar.client;

import dev.rgcb.scholar.Scholar;
import dev.rgcb.scholar.integration.ScholarApiRuntime;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

@EventBusSubscriber(modid = Scholar.MOD_ID, value = Dist.CLIENT)
public final class ScholarClient {
    private ScholarClient() {
    }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        ProductionClientCommands.register(event);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        ScholarApiRuntime.tickRecovery();
    }

    @SubscribeEvent
    public static void gameShuttingDown(GameShuttingDownEvent event) {
        ScholarApiRuntime.captureRecoveryNow();
    }
}
