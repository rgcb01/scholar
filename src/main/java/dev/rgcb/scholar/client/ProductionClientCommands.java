package dev.rgcb.scholar.client;

import com.mojang.brigadier.Command;
import dev.rgcb.scholar.client.screen.ScholarHomeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

final class ProductionClientCommands {
    private ProductionClientCommands() { }

    static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scholar").executes(context -> {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(ScholarHomeScreen.create()));
            return Command.SINGLE_SUCCESS;
        }));
    }
}
