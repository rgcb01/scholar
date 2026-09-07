package dev.rgcb.scholar.client;

import com.mojang.brigadier.Command;
import dev.rgcb.scholar.Scholar;
import dev.rgcb.scholar.client.screen.ScholarDocumentScreen;
import dev.rgcb.scholar.client.screen.ScholarEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
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
        event.getDispatcher().register(Commands.literal("scholar_dev_viewer")
                .executes(context -> {
                    openViewer();
                    return Command.SINGLE_SUCCESS;
                }));
        event.getDispatcher().register(Commands.literal("scholar_dev_editor")
                .executes(context -> {
                    openEditor();
                    return Command.SINGLE_SUCCESS;
                }));
    }

    private static void openViewer() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(ScholarDocumentScreen.createDevelopmentScreen()));
    }

    private static void openEditor() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(ScholarEditorScreen.createDevelopmentScreen()));
    }
}
