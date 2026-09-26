package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.client.ui.ScholarScreenRendering;
import dev.rgcb.scholar.client.ui.ScholarText;
import dev.rgcb.scholar.compute.ScientificValue;
import dev.rgcb.scholar.compute.ComputationFormatter;
import dev.rgcb.scholar.document.ComputedResult;
import dev.rgcb.scholar.document.VariableDefinition;
import dev.rgcb.scholar.editor.ComputationDialogKind;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.quantity.NumberNotation;
import dev.rgcb.scholar.quantity.PhysicalDimension;
import dev.rgcb.scholar.quantity.Quantity;
import dev.rgcb.scholar.quantity.QuantitySemantics;
import dev.rgcb.scholar.quantity.UnitExpression;
import dev.rgcb.scholar.quantity.UnitParser;
import dev.rgcb.scholar.quantity.UnitRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Production authoring form; semantic edits remain in EditorSession. */
final class ScholarComputationDialog extends Screen {
    private final ScholarEditorScreen parent;
    private final EditorSession session;
    private final ComputationDialogKind kind;
    private EditBox primary;
    private EditBox secondary;
    private EditBox unit;
    private EditBox label;
    private QuantitySemantics semantics = QuantitySemantics.LINEAR;
    private NumberNotation notation = NumberNotation.DECIMAL;
    private Button semanticsButton;
    private Button notationButton;
    private Button unitPickerButton;
    private EditBox unitSearch;
    private final List<Button> unitOptions = new ArrayList<>();
    private List<UnitPickerChoices.Choice> visibleUnits = List.of();
    private boolean unitPickerOpen;
    private int unitScroll;
    private int highlightedUnit;
    private String message = "";

    ScholarComputationDialog(ScholarEditorScreen parent, EditorSession session, ComputationDialogKind kind) {
        super(ScholarText.component(switch (kind) {
            case INSERT_VARIABLE -> "scholar.dialog.computation.insert_variable";
            case INSERT_RESULT -> "scholar.dialog.computation.insert_result";
            case EDIT_VARIABLE -> "scholar.dialog.computation.edit_variable";
            case EDIT_RESULT -> "scholar.dialog.computation.edit_result";
            case INSERT_ANALYSIS, EDIT_ANALYSIS, ADD_FIT_OVERLAY -> throw new IllegalArgumentException("Use the analysis dialog.");
        }));
        this.parent = parent;
        this.session = session;
        this.kind = kind;
    }

    private boolean variableMode() {
        return kind == ComputationDialogKind.INSERT_VARIABLE || kind == ComputationDialogKind.EDIT_VARIABLE;
    }

    @Override protected void init() {
        var w = Math.min(340, width - 24);
        var x = (width - w) / 2;
        var y = Math.max(38, height / 2 - 112);
        primary = field(x, y + 18, w, variableMode() ? "scholar.dialog.variable_name" : "scholar.dialog.expression", "");
        secondary = field(x, y + 54, w, variableMode() ? "scholar.dialog.value" : "scholar.dialog.label_optional", "");
        unit = field(x, y + 90, variableMode() ? w - 104 : w,
                variableMode() ? "scholar.dialog.unit_expression" : "scholar.dataset.display_unit_optional", "");
        label = variableMode() ? field(x, y + 126, w, "scholar.dialog.label_optional", "") : null;
        if (variableMode()) {
            unitPickerButton = addRenderableWidget(Button.builder(ScholarText.component("scholar.dialog.choose_unit"), b -> openUnitPicker())
                    .bounds(x + w - 98, y + 90, 98, 20).build());
            unitSearch = new EditBox(font, x, y + 114, w, 20, ScholarText.component("scholar.dialog.search_units"));
            unitSearch.setHint(ScholarText.component("scholar.dialog.search_units"));
            unitSearch.setMaxLength(64);
            unitSearch.setResponder(query -> refreshUnitChoices());
            unitOptions.clear();
            for (var row = 0; row < 7; row++) {
                var visibleRow = row;
                unitOptions.add(Button.builder(Component.empty(), b -> {
                    var index = unitScroll + visibleRow;
                    if (index < visibleUnits.size()) selectUnit(visibleUnits.get(index));
                })
                        .bounds(x + 3, 0, w - 6, 18).build());
            }
            semanticsButton = addRenderableWidget(Button.builder(Component.literal(semanticsLabel()), b -> {
                semantics = switch (semantics) {
                    case LINEAR -> QuantitySemantics.ABSOLUTE_TEMPERATURE;
                    case ABSOLUTE_TEMPERATURE -> QuantitySemantics.TEMPERATURE_DIFFERENCE;
                    case TEMPERATURE_DIFFERENCE -> QuantitySemantics.LINEAR;
                };
                semanticsButton.setMessage(Component.literal(semanticsLabel()));
            }).bounds(x, y + 158, w, 20).build());
            unit.setResponder(text -> {
                try {
                    var parsed = new UnitParser().parseRequired(text.trim());
                    var temperature = parsed.dimension(UnitRegistry.builtIn()).equals(PhysicalDimension.TEMPERATURE);
                    if (temperature && semantics == QuantitySemantics.LINEAR) semantics = QuantitySemantics.ABSOLUTE_TEMPERATURE;
                    if (!temperature && semantics != QuantitySemantics.LINEAR) semantics = QuantitySemantics.LINEAR;
                    semanticsButton.setMessage(Component.literal(semanticsLabel()));
                } catch (IllegalArgumentException ignored) {
                    // Incomplete input is validated only when Apply is pressed.
                }
            });
        } else {
            notationButton = addRenderableWidget(Button.builder(Component.literal(notationLabel()), b -> {
                notation = switch (notation) {
                    case DECIMAL -> NumberNotation.SCIENTIFIC;
                    case SCIENTIFIC -> NumberNotation.ENGINEERING;
                    case ENGINEERING -> NumberNotation.DECIMAL;
                };
                notationButton.setMessage(Component.literal(notationLabel()));
            }).bounds(x, y + 126, w, 20).build());
        }
        if (kind == ComputationDialogKind.EDIT_VARIABLE) {
            var current = (VariableDefinition) session.current().document().blocks().get(session.current().blockSelection().blockIndex());
            primary.setValue(current.name());
            label.setValue(current.label().orElse(""));
            if (current.value() instanceof ScientificValue.Scalar scalar) secondary.setValue(scalar.value().toPlainString());
            else if (current.value() instanceof ScientificValue.Physical physical) {
                secondary.setValue(physical.value().nominal().value().toPlainString());
                unit.setValue(physical.value().nominal().unit().displaySymbol(UnitRegistry.builtIn()));
                semantics = physical.value().nominal().semantics();
                semanticsButton.setMessage(Component.literal(semanticsLabel()));
            }
        } else if (kind == ComputationDialogKind.EDIT_RESULT) {
            var current = (ComputedResult) session.current().document().blocks().get(session.current().blockSelection().blockIndex());
            primary.setValue(new ComputationFormatter().expression(current.expression(), session.current().document(), false));
            secondary.setValue(current.label().orElse(""));
            unit.setValue(current.displayUnit().map(value -> value.displaySymbol(UnitRegistry.builtIn())).orElse(""));
            notation = current.notation();
            notationButton.setMessage(Component.literal(notationLabel()));
        }
        var buttonsY = y + (variableMode() ? 190 : 158);
        addRenderableWidget(Button.builder(ScholarText.component("scholar.dialog.apply"), b -> apply()).bounds(x, buttonsY, w / 2 - 3, 20).build());
        addRenderableWidget(Button.builder(ScholarText.component("scholar.dialog.cancel"), b -> onClose()).bounds(x + w / 2 + 3, buttonsY, w / 2 - 3, 20).build());
        setInitialFocus(primary);
    }

    private EditBox field(int x, int y, int width, String hintKey, String initial) {
        var box = new EditBox(font, x, y, width, 20, ScholarText.component(hintKey));
        box.setHint(ScholarText.component(hintKey));
        box.setMaxLength(256);
        box.setValue(initial);
        return addRenderableWidget(box);
    }

    private String semanticsLabel() {
        var value = switch (semantics) {
            case LINEAR -> ScholarText.get("scholar.dataset.value_kind.ordinary");
            case ABSOLUTE_TEMPERATURE -> ScholarText.get("scholar.dataset.value_kind.absolute_temperature");
            case TEMPERATURE_DIFFERENCE -> ScholarText.get("scholar.dataset.value_kind.temperature_difference");
        };
        return ScholarText.get("scholar.dataset.value_kind", value);
    }

    private String notationLabel() {
        return ScholarText.get("scholar.dataset.selector", ScholarText.get("scholar.dataset.number_format"),
                ScholarText.get("scholar.dataset.number_format." + notation.name().toLowerCase(java.util.Locale.ROOT)));
    }

    private void openUnitPicker() {
        unitPickerOpen = !unitPickerOpen;
        if (!unitPickerOpen) return;
        unitSearch.setValue("");
        refreshUnitChoices();
        var current = unit.getValue().trim();
        if (current.isEmpty()) {
            highlightedUnit = 0;
        } else {
            try {
                var expression = new UnitParser().parseRequired(current);
                for (var i = 0; i < visibleUnits.size(); i++) {
                    var choice = visibleUnits.get(i);
                    if (choice.unit().equals(Optional.of(expression)) && choice.semantics() == semantics) {
                        highlightedUnit = i;
                        break;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Advanced expressions remain in the raw field even if absent from the picker.
            }
        }
        unitScroll = Math.max(0, highlightedUnit - 3);
        setFocused(unitSearch);
    }

    private void refreshUnitChoices() {
        var query = unitSearch.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        visibleUnits = UnitPickerChoices.available().stream()
                .filter(choice -> choice.label().toLowerCase(java.util.Locale.ROOT).contains(query)).toList();
        highlightedUnit = 0;
        unitScroll = 0;
    }

    private int pickerRows() {
        return Math.min(7, Math.max(1, (height - (unit.getY() + 24) - 12) / 18));
    }

    private int pickerTop() {
        var below = unit.getY() + 22;
        var popupHeight = 24 + pickerRows() * 18;
        return below + popupHeight <= height - 8 ? below : Math.max(8, unit.getY() - popupHeight - 2);
    }

    private void selectUnit(UnitPickerChoices.Choice choice) {
        try {
            var previousText = unit.getValue().trim();
            var previous = previousText.isEmpty() ? Optional.<UnitExpression>empty()
                    : Optional.of(new UnitParser().parseRequired(previousText));
            if (kind == ComputationDialogKind.EDIT_VARIABLE && !secondary.getValue().isBlank()) {
                var converted = UnitPickerChoices.convertedValue(new BigDecimal(secondary.getValue().trim()),
                        previous, semantics, choice);
                secondary.setValue(converted.toPlainString());
            }
        } catch (IllegalArgumentException ignored) {
            // An incomplete advanced expression has no convertible authored value.
        }
        semantics = choice.semantics();
        unit.setValue(choice.unit().map(value -> value.displaySymbol(UnitRegistry.builtIn())).orElse(""));
        semanticsButton.setMessage(Component.literal(semanticsLabel()));
        unitPickerOpen = false;
        setFocused(unit);
    }

    private void apply() {
        try {
            var source = primary.getValue().trim();
            if (source.isEmpty()) throw new IllegalArgumentException(ScholarText.get(variableMode()
                    ? "scholar.error.variable_name_required" : "scholar.error.expression_required"));
            var unitText = unit.getValue().trim();
            Optional<UnitExpression> parsedUnit = unitText.isEmpty() ? Optional.empty() : Optional.of(new UnitParser().parseRequired(unitText));
            boolean changed;
            if (variableMode()) {
                var number = new BigDecimal(secondary.getValue().trim());
                ScientificValue value = parsedUnit.isEmpty()
                        ? new ScientificValue.Scalar(number)
                        : new ScientificValue.Physical(new Quantity(number, parsedUnit.orElseThrow(), semantics));
                if (parsedUnit.isEmpty() && semantics != QuantitySemantics.LINEAR) {
                    throw new IllegalArgumentException(ScholarText.get("scholar.error.temperature_unit_required"));
                }
                var optionalLabel = Optional.of(label.getValue().trim()).filter(text -> !text.isEmpty());
                changed = kind == ComputationDialogKind.INSERT_VARIABLE
                        ? session.insertVariable(source, value, optionalLabel)
                        : session.editVariable(source, value, optionalLabel);
            } else {
                var optionalLabel = Optional.of(secondary.getValue().trim()).filter(text -> !text.isEmpty());
                changed = kind == ComputationDialogKind.INSERT_RESULT
                        ? session.insertComputedResult(source, optionalLabel, parsedUnit, notation)
                        : session.editComputedResult(source, optionalLabel, parsedUnit, notation);
            }
            if (!changed) throw new IllegalArgumentException(ScholarText.get("scholar.error.no_document_change"));
            parent.refreshComputationLayout();
            onClose();
        } catch (IllegalArgumentException exception) {
            message = exception.getMessage();
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF202020);
        graphics.drawCenteredString(font, title, width / 2, Math.max(18, height / 2 - 132), 0xFFFFFFFF);
        graphics.drawString(font, ScholarText.get(variableMode() ? "scholar.dialog.name" : "scholar.dialog.expression"), primary.getX(), primary.getY() - 10, 0xFFD1D5DB);
        graphics.drawString(font, ScholarText.get(variableMode() ? "scholar.dialog.value" : "scholar.dialog.label"), secondary.getX(), secondary.getY() - 10, 0xFFD1D5DB);
        graphics.drawString(font, ScholarText.get(variableMode() ? "scholar.dialog.unit" : "scholar.dialog.display_unit"), unit.getX(), unit.getY() - 10, 0xFFD1D5DB);
        if (label != null) graphics.drawString(font, ScholarText.get("scholar.dialog.label"), label.getX(), label.getY() - 10, 0xFFD1D5DB);
        ScholarScreenRendering.renderWidgets(renderables, graphics, mouseX, mouseY, partialTick);
        if (!message.isEmpty()) graphics.drawCenteredString(font, message, width / 2,
                Math.min(height - 18, height / 2 + 116), 0xFFFFDD88);
        if (unitPickerOpen) {
            graphics.flush();
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, 0, 200);
                renderUnitPicker(graphics, mouseX, mouseY, partialTick);
                graphics.flush();
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    private void renderUnitPicker(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var x = unit.getX();
        var top = pickerTop();
        var popupWidth = Math.min(340, width - 24);
        var rows = pickerRows();
        graphics.fill(x - 2, top - 2, x + popupWidth + 2, top + 24 + rows * 18 + 2, 0xFFAAAAAA);
        graphics.fill(x, top, x + popupWidth, top + 24 + rows * 18, 0xFF101010);
        unitSearch.setX(x + 3);
        unitSearch.setY(top + 2);
        unitSearch.setWidth(popupWidth - 6);
        unitSearch.render(graphics, mouseX, mouseY, partialTick);
        for (var row = 0; row < rows && unitScroll + row < visibleUnits.size(); row++) {
            var index = unitScroll + row;
            var rowY = top + 24 + row * 18;
            var option = unitOptions.get(row);
            option.setY(rowY);
            option.setMessage(Component.literal((index == highlightedUnit ? "> " : "")
                    + visibleUnits.get(index).label()));
            option.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!unitPickerOpen) return super.mouseClicked(mouseX, mouseY, button);
        var top = pickerTop();
        var x = unit.getX();
        var popupWidth = Math.min(340, width - 24);
        if (mouseX >= x && mouseX < x + popupWidth && mouseY >= top && mouseY < top + 24) {
            unitSearch.mouseClicked(mouseX, mouseY, button);
            setFocused(unitSearch);
            return true;
        }
        if (mouseX >= x && mouseX < x + popupWidth && mouseY >= top + 24
                && mouseY < top + 24 + pickerRows() * 18) {
            var row = ((int) mouseY - top - 24) / 18;
            if (row < unitOptions.size()) unitOptions.get(row).mouseClicked(mouseX, mouseY, button);
            return true;
        }
        unitPickerOpen = false;
        return true;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!unitPickerOpen) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        unitScroll = Math.max(0, Math.min(Math.max(0, visibleUnits.size() - pickerRows()),
                unitScroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!unitPickerOpen) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { unitPickerOpen = false; return true; }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
            highlightedUnit = Math.max(0, Math.min(visibleUnits.size() - 1,
                    highlightedUnit + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
            unitScroll = Math.min(unitScroll, highlightedUnit);
            unitScroll = Math.max(unitScroll, highlightedUnit - pickerRows() + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (!visibleUnits.isEmpty()) selectUnit(visibleUnits.get(highlightedUnit));
            return true;
        }
        unitSearch.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        if (!unitPickerOpen) return super.charTyped(codePoint, modifiers);
        unitSearch.charTyped(codePoint, modifiers);
        return true;
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
