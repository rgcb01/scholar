// SPDX-License-Identifier: CC0-1.0
package example.scholar;

import com.mojang.brigadier.Command;
import dev.rgcb.scholar.api.ScholarApi;
import dev.rgcb.scholar.api.data.ScholarData;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Separate development mod: no imports from Scholar implementation packages. */
@Mod(ReferenceAddon.MOD_ID)
public final class ReferenceAddon {
    public static final String MOD_ID = "scholar_reference_addon";

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static final class CommandsRegistration {
        @SubscribeEvent
        public static void register(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("scholar_reference_demo").executes(context -> {
                createFreeFall();
                return Command.SINGLE_SUCCESS;
            }));
        }
    }

    public static void createFreeFall() {
        var document = ScholarApi.get().documents().create();
        document.edit(edit -> {
            var datasetId = edit.createDataset("Free Fall Measurements", List.of(
                    ScholarData.ColumnSpec.number("Time", "s"),
                    ScholarData.ColumnSpec.number("Distance", "m")));
            var columns = edit.columns(datasetId);
            edit.appendRows(datasetId, List.of(
                    row("0.0", "0.0"), row("0.2", "0.20"), row("0.4", "0.79"),
                    row("0.6", "1.77"), row("0.8", "3.14"), row("1.0", "4.91")));
            edit.insertDatasetTable(datasetId);
            edit.insertFigurePlot(datasetId, columns.get(0).id(), columns.get(1).id(),
                    "Free fall", "Measured distance over time");
            edit.requestAnalysis(datasetId, ScholarData.Analysis.QUADRATIC_FIT,
                    java.util.Optional.of(columns.get(0).id()), columns.get(1).id());
        });
        document.save();
    }

    private static List<ScholarData.Cell> row(String time, String distance) {
        return List.of(ScholarData.Cell.number(new BigDecimal(time)),
                ScholarData.Cell.number(new BigDecimal(distance)));
    }
}
