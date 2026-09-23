package dev.rgcb.scholar.client.screen;

import dev.rgcb.scholar.application.ApplicationDocumentWorkspace;
import dev.rgcb.scholar.application.ScholarApplication;
import dev.rgcb.scholar.client.editor.MinecraftClipboardAdapter;
import dev.rgcb.scholar.client.editor.ScholarEditorController;
import dev.rgcb.scholar.client.render.MinecraftDocumentRenderer;
import dev.rgcb.scholar.client.render.DocumentViewTransform;
import dev.rgcb.scholar.client.render.MinecraftMathTextMeasurer;
import dev.rgcb.scholar.client.render.MinecraftTextMeasurer;
import dev.rgcb.scholar.client.render.MinecraftTypographyResolver;
import dev.rgcb.scholar.client.ui.ContextMenuWidget;
import dev.rgcb.scholar.client.ui.ApplicationHeaderWidget;
import dev.rgcb.scholar.client.ui.MenuBarWidget;
import dev.rgcb.scholar.client.ui.ModalGeometry;
import dev.rgcb.scholar.client.ui.MenuDefinition;
import dev.rgcb.scholar.client.ui.MenuEntry;
import dev.rgcb.scholar.client.ui.MinecraftShortcutMatcher;
import dev.rgcb.scholar.client.ui.ScholarShellLayout;
import dev.rgcb.scholar.client.ui.ScholarStatusBarLayout;
import dev.rgcb.scholar.client.ui.CaretBlink;
import dev.rgcb.scholar.client.ui.DocumentStatus;
import dev.rgcb.scholar.client.ui.ScholarIcons;
import dev.rgcb.scholar.client.ui.ScholarShellRenderer;
import dev.rgcb.scholar.client.ui.ScholarShellModel;
import dev.rgcb.scholar.client.ui.ScholarShellStyle;
import dev.rgcb.scholar.client.ui.ScholarRibbonModel;
import dev.rgcb.scholar.client.ui.RibbonWidget;
import dev.rgcb.scholar.client.ui.ShellRect;
import dev.rgcb.scholar.client.ui.ToolbarItem;
import dev.rgcb.scholar.client.ui.ToolbarWidget;
import dev.rgcb.scholar.editor.BuiltInEditorActions;
import dev.rgcb.scholar.editor.ActionShortcut;
import dev.rgcb.scholar.editor.CaretGeometryResolver;
import dev.rgcb.scholar.editor.DocumentHitTester;
import dev.rgcb.scholar.editor.DocumentHit;
import dev.rgcb.scholar.editor.DocumentPosition;
import dev.rgcb.scholar.editor.EditorAction;
import dev.rgcb.scholar.editor.EditorActionId;
import dev.rgcb.scholar.editor.EditorDocumentWorkspace;
import dev.rgcb.scholar.editor.EditorContextActionResolver;
import dev.rgcb.scholar.editor.EditorSession;
import dev.rgcb.scholar.editor.EditorState;
import dev.rgcb.scholar.editor.TextSelection;
import dev.rgcb.scholar.editor.SelectionDragResolver;
import dev.rgcb.scholar.editor.SelectionGeometryResolver;
import dev.rgcb.scholar.editor.TableCaretGeometryResolver;
import dev.rgcb.scholar.editor.TableCellTextSelection;
import dev.rgcb.scholar.editor.TableEditingSelection;
import dev.rgcb.scholar.editor.TableHitTester;
import dev.rgcb.scholar.editor.TableSelectionGeometryResolver;
import dev.rgcb.scholar.editor.PlotEditingSelection;
import dev.rgcb.scholar.editor.PlotEditTarget;
import dev.rgcb.scholar.editor.PlotPropertyTarget;
import dev.rgcb.scholar.editor.PlotProperty;
import dev.rgcb.scholar.editor.PlotSeriesTarget;
import dev.rgcb.scholar.editor.PlotPointTarget;
import dev.rgcb.scholar.editor.PlotHitTester;
import dev.rgcb.scholar.editor.DiagramHitTester;
import dev.rgcb.scholar.editor.DiagramEditTarget;
import dev.rgcb.scholar.editor.DiagramElementTarget;
import dev.rgcb.scholar.editor.DiagramPropertyTarget;
import dev.rgcb.scholar.editor.DiagramProperty;
import dev.rgcb.scholar.editor.DiagramPortTarget;
import dev.rgcb.scholar.editor.DiagramConnectionTarget;
import dev.rgcb.scholar.layout.DocumentLayoutEngine;
import dev.rgcb.scholar.layout.LaidOutDocument;
import dev.rgcb.scholar.diagram.layout.LaidOutDiagram;
import dev.rgcb.scholar.diagram.layout.DiagramViewport;
import dev.rgcb.scholar.diagram.layout.DiagramViewportStore;
import dev.rgcb.scholar.document.DiagramBlock;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.CrossReferenceTarget;
import dev.rgcb.scholar.document.DocumentStructureResolver;
import dev.rgcb.scholar.document.SectionEntry;
import dev.rgcb.scholar.layout.LaidOutTableCell;
import dev.rgcb.scholar.plot.layout.LaidOutPlot;
import dev.rgcb.scholar.math.editor.MathCaretGeometryResolver;
import dev.rgcb.scholar.math.editor.MathCaretSelection;
import dev.rgcb.scholar.math.editor.MathHitTester;
import dev.rgcb.scholar.math.editor.MathPosition;
import dev.rgcb.scholar.math.editor.MathRangeSelection;
import dev.rgcb.scholar.math.editor.MathSelectionGeometryResolver;
import dev.rgcb.scholar.math.editor.SemanticMathTokenKind;
import dev.rgcb.scholar.typography.ScholarTypography;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ScholarEditorScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0xFF202020;
    private static final int CARET_COLOR = 0xFF1F2933;
    private static final int SELECTION_COLOR = 0x663B82F6;
    private static final int OBJECT_SELECTION_FILL = 0x333B82F6;
    private static final int OBJECT_SELECTION_BORDER = 0xFF1D4ED8;
    private static final int OBJECT_SELECTION_INNER_BORDER = 0xFF93C5FD;
    private static final int EQUATION_FOCUS_BORDER = 0xFF6B7280;
    private static final int EQUATION_CARET_COLOR = 0xFF111827;
    private static final int TABLE_FOCUS_BORDER = 0xFF6B7280;
    private static final int PLOT_FOCUS_BORDER = 0xFF6B7280;
    private static final int PLOT_TARGET_BORDER = 0xFF2563EB;
    private static final int DIAGRAM_FOCUS_BORDER = 0xFF6B7280;
    private static final int DIAGRAM_TARGET_BORDER = 0xFF2563EB;
    private static final int DIAGRAM_CONNECTION_SOURCE_BORDER = 0xFFF59E0B;
    private static final ActionShortcut SELECT_ALL_SHORTCUT = ActionShortcut.ctrl(ActionShortcut.Key.A);

    private final ScholarTypography typography = ScholarTypography.defaultProfile();
    private final ApplicationHeaderWidget applicationHeader = new ApplicationHeaderWidget();
    private final DocumentLayoutEngine layoutEngine = new DocumentLayoutEngine(typography);
    private final EditorDocumentWorkspace workspace;
    private final ScholarApplication application;
    private final EditorSession session;
    private final List<EditorAction> fileActions;
    private final List<EditorAction> editActions = BuiltInEditorActions.editMenuActions();
    private final List<EditorAction> insertActions = BuiltInEditorActions.insertMenuActions();
    private final List<EditorAction> blockStyleActions = BuiltInEditorActions.blockStyleActions();
    private final List<EditorAction> semanticConversionActions = List.of(
            action(insertActions, EditorActionId.MATH_CONVERT_NAMED_OPERATOR),
            action(insertActions, EditorActionId.MATH_CONVERT_TEXT));
    private final List<EditorAction> groupActions = List.of(
            action(insertActions, EditorActionId.MATH_INSERT_PARENTHESES_GROUP),
            action(insertActions, EditorActionId.MATH_INSERT_BRACKETS_GROUP),
            action(insertActions, EditorActionId.MATH_INSERT_BRACES_GROUP));
    private final List<EditorAction> formatActions = BuiltInEditorActions.formatMenuActions();
    private final List<EditorAction> tableActions = BuiltInEditorActions.tableMenuActions();
    private final List<EditorAction> plotActions = BuiltInEditorActions.plotMenuActions();
    private final List<EditorAction> diagramActions = BuiltInEditorActions.diagramMenuActions();
    private final List<EditorAction> figureActions = BuiltInEditorActions.figureMenuActions();
    private final List<EditorAction> viewActions = BuiltInEditorActions.viewMenuActions();
    private final List<EditorAction> viewportActions;
    private final List<EditorAction> layoutActions = BuiltInEditorActions.layoutMenuActions();
    private final List<EditorAction> dataActions = BuiltInEditorActions.dataMenuActions();
    private final List<EditorAction> allActions = List.of(
            action(editActions, EditorActionId.UNDO),
            action(editActions, EditorActionId.REDO),
            action(editActions, EditorActionId.CUT),
            action(editActions, EditorActionId.COPY),
            action(editActions, EditorActionId.PASTE),
            action(editActions, EditorActionId.DELETE),
            action(insertActions, EditorActionId.INSERT_EQUATION),
            action(insertActions, EditorActionId.INSERT_VARIABLE),
            action(insertActions, EditorActionId.INSERT_COMPUTED_RESULT),
            action(insertActions, EditorActionId.EDIT_VARIABLE),
            action(insertActions, EditorActionId.EDIT_COMPUTED_RESULT),
            action(insertActions, EditorActionId.INSERT_TABLE),
            action(insertActions, EditorActionId.INSERT_PLOT),
            action(insertActions, EditorActionId.INSERT_DIAGRAM),
            action(insertActions, EditorActionId.INSERT_CROSS_REFERENCE),
            action(insertActions, EditorActionId.INSERT_TABLE_OF_CONTENTS),
            action(viewActions, EditorActionId.TOGGLE_OUTLINE),
            action(dataActions, EditorActionId.DATA_NEW_DATASET),
            action(dataActions, EditorActionId.DATA_INSERT_DATASET_TABLE),
            action(dataActions, EditorActionId.DATA_BIND_PLOT_TO_DATASET),
            action(dataActions, EditorActionId.DATA_INSERT_ANALYSIS),
            action(dataActions, EditorActionId.DATA_EDIT_ANALYSIS),
            action(dataActions, EditorActionId.DATA_ADD_FIT_OVERLAY),
            action(figureActions, EditorActionId.FIGURE_WRAP_PLOT),
            action(figureActions, EditorActionId.FIGURE_WRAP_DIAGRAM),
            action(figureActions, EditorActionId.FIGURE_EDIT_CAPTION),
            action(figureActions, EditorActionId.FIGURE_UNWRAP),
            action(insertActions, EditorActionId.MATH_INSERT_FRACTION),
            action(insertActions, EditorActionId.MATH_INSERT_ROOT),
            action(insertActions, EditorActionId.MATH_INSERT_PARENTHESES_GROUP),
            action(insertActions, EditorActionId.MATH_INSERT_BRACKETS_GROUP),
            action(insertActions, EditorActionId.MATH_INSERT_BRACES_GROUP),
            action(insertActions, EditorActionId.MATH_INSERT_SUPERSCRIPT),
            action(insertActions, EditorActionId.MATH_INSERT_SUBSCRIPT),
            action(insertActions, EditorActionId.MATH_CONVERT_NAMED_OPERATOR),
            action(insertActions, EditorActionId.MATH_CONVERT_TEXT),
            action(blockStyleActions, EditorActionId.PARAGRAPH),
            action(blockStyleActions, EditorActionId.HEADING_1),
            action(blockStyleActions, EditorActionId.HEADING_2),
            action(blockStyleActions, EditorActionId.HEADING_3),
            action(blockStyleActions, EditorActionId.HEADING_4),
            action(blockStyleActions, EditorActionId.HEADING_5),
            action(blockStyleActions, EditorActionId.HEADING_6),
            action(formatActions, EditorActionId.BOLD),
            action(formatActions, EditorActionId.ITALIC),
            action(tableActions, EditorActionId.TABLE_INSERT_ROW_ABOVE),
            action(tableActions, EditorActionId.TABLE_INSERT_ROW_BELOW),
            action(tableActions, EditorActionId.TABLE_DELETE_ROW),
            action(tableActions, EditorActionId.TABLE_INSERT_COLUMN_LEFT),
            action(tableActions, EditorActionId.TABLE_INSERT_COLUMN_RIGHT),
            action(tableActions, EditorActionId.TABLE_DELETE_COLUMN),
            action(plotActions, EditorActionId.PLOT_TOGGLE_GRID),
            action(plotActions, EditorActionId.PLOT_TOGGLE_LEGEND),
            action(plotActions, EditorActionId.PLOT_ADD_LINE_SERIES),
            action(plotActions, EditorActionId.PLOT_ADD_SCATTER_SERIES),
            action(plotActions, EditorActionId.PLOT_SET_SERIES_LINE),
            action(plotActions, EditorActionId.PLOT_SET_SERIES_SCATTER),
            action(plotActions, EditorActionId.PLOT_ADD_POINT),
            action(plotActions, EditorActionId.PLOT_DELETE_POINT),
            action(plotActions, EditorActionId.PLOT_DELETE_SERIES),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_NODE),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_RESISTOR),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_CAPACITOR),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_DC_VOLTAGE_SOURCE),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_GROUND),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_DIODE),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_LED),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_SWITCH_SPST),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_JUNCTION),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_CENTERLINE),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_RECTANGLE),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_CIRCLE),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_ARC),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_ARROW),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_PART_REFERENCE),
            action(diagramActions, EditorActionId.DIAGRAM_GENERATE_MECHANICAL_BOM),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL),
            action(diagramActions, EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL),
            action(diagramActions, EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT),
            action(diagramActions, EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL),
            action(diagramActions, EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR),
            action(diagramActions, EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC),
            action(diagramActions, EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT),
            action(diagramActions, EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT),
            action(diagramActions, EditorActionId.DIAGRAM_SCALE_SYMBOLS_DOWN),
            action(diagramActions, EditorActionId.DIAGRAM_SCALE_SYMBOLS_UP),
            action(diagramActions, EditorActionId.DIAGRAM_WORKSPACE_SHORTER),
            action(diagramActions, EditorActionId.DIAGRAM_WORKSPACE_TALLER),
            action(diagramActions, EditorActionId.DIAGRAM_WORKSPACE_RESET_HEIGHT),
            action(diagramActions, EditorActionId.DIAGRAM_ROTATE_CLOCKWISE),
            action(diagramActions, EditorActionId.DIAGRAM_ROTATE_COUNTERCLOCKWISE),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_JUNCTION),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_NODE),
            action(diagramActions, EditorActionId.DIAGRAM_START_CONNECTION),
            action(diagramActions, EditorActionId.DIAGRAM_FINISH_CONNECTION),
            action(diagramActions, EditorActionId.DIAGRAM_CANCEL_CONNECTION),
            action(diagramActions, EditorActionId.DIAGRAM_DELETE_CONNECTION));
    private final CaretGeometryResolver caretGeometryResolver = new CaretGeometryResolver();
    private final DocumentHitTester hitTester = new DocumentHitTester();
    private final MathHitTester mathHitTester = new MathHitTester();
    private final MathCaretGeometryResolver mathCaretGeometryResolver = new MathCaretGeometryResolver();
    private final MathSelectionGeometryResolver mathSelectionGeometryResolver = new MathSelectionGeometryResolver();
    private final SelectionDragResolver selectionDragResolver = new SelectionDragResolver();
    private final SelectionGeometryResolver selectionGeometryResolver = new SelectionGeometryResolver();
    private final TableHitTester tableHitTester = new TableHitTester();
    private final TableCaretGeometryResolver tableCaretGeometryResolver = new TableCaretGeometryResolver();
    private final TableSelectionGeometryResolver tableSelectionGeometryResolver = new TableSelectionGeometryResolver();
    private final PlotHitTester plotHitTester = new PlotHitTester();
    private final DiagramHitTester diagramHitTester = new DiagramHitTester();
    private final EditorContextActionResolver contextActionResolver = new EditorContextActionResolver();
    private ScholarEditorController controller;
    private MenuBarWidget menuBar;
    private ToolbarWidget toolbar;
    private RibbonWidget ribbon;
    private ContextMenuWidget contextMenu;
    private boolean equationToolbarActive;
    private boolean semanticTokenPopupOpen;
    private boolean semanticTokenTypeOpen;
    private SemanticMathTokenKind semanticTokenKind = SemanticMathTokenKind.NAMED_OPERATOR;
    private String semanticTokenContent = "";
    private boolean crossReferencePopupOpen;
    private List<CrossReferenceTarget> crossReferenceTargets = List.of();
    private boolean plotValuePopupOpen;
    private boolean plotPointSecondField;
    private PlotEditTarget plotPopupTarget;
    private String plotValuePrimary = "";
    private String plotValueSecondary = "";
    private boolean diagramLabelPopupOpen;
    private DiagramEditTarget diagramLabelPopupTarget;
    private String diagramLabelValue = "";
    private boolean electricalComponentPopupOpen;
    private DiagramElementTarget electricalComponentPopupTarget;
    private String electricalReferenceValue = "";
    private String electricalComponentValue = "";
    private boolean electricalComponentSecondField;
    private boolean diagramCanvasPopupOpen;
    private boolean diagramCanvasSecondField;
    private String diagramCanvasWidthValue = "";
    private String diagramCanvasHeightValue = "";
    private String diagramCanvasValidation = "";
    private final DiagramViewportStore diagramViewports = new DiagramViewportStore();
    private final DocumentStructureResolver structureResolver = new DocumentStructureResolver();
    private boolean outlineOpen;
    private int outlineScroll;
    private boolean diagramPanning;
    private int diagramPanBlockIndex = -1;
    private ScholarShellLayout shellLayout = ScholarShellLayout.compute(0, 0);
    private LaidOutDocument laidOutDocument;
    private MinecraftTextMeasurer textMeasurer;
    private MinecraftMathTextMeasurer mathTextMeasurer;
    private MinecraftTypographyResolver typographyResolver;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int scrollOffset;
    private float zoom = 1.0f;
    private final CaretBlink caretBlink = new CaretBlink();
    private boolean caretVisible;
    private boolean zoomSliderDragging;
    private Document statusWordDocument;
    private int statusWordCount;
    private boolean dragging;
    private MathPosition mathDragAnchor;
    private TableCellTextSelection tableDragAnchor;

    private ScholarEditorScreen(EditorDocumentWorkspace workspace, ScholarApplication application) {
        super(Component.literal(application == null ? "Scholar Development Editor" : "Scholar"));
        this.workspace = workspace;
        this.application = application;
        viewportActions = List.of(
                fileAction(EditorActionId.VIEW_ZOOM_OUT, "Zoom Out", () -> setZoom(zoom - 0.1f)),
                fileAction(EditorActionId.VIEW_ZOOM_IN, "Zoom In", () -> setZoom(zoom + 0.1f)),
                fileAction(EditorActionId.VIEW_FIT_PAGE, "Fit Page", this::fitPage),
                fileAction(EditorActionId.VIEW_FIT_WIDTH, "Fit Width", this::fitWidth));
        session = workspace.session();
        fileActions = List.of(
                fileAction(EditorActionId.FILE_NEW, "New", () -> protectUnsaved(() -> {
                    if (application == null) {
                        ((dev.rgcb.scholar.editor.DocumentWorkspace) workspace).newDocument();
                        minecraft.setScreen(forWorkspace((dev.rgcb.scholar.editor.DocumentWorkspace) workspace));
                    } else openCreatedDocument();
                })),
                fileAction(EditorActionId.FILE_OPEN, application == null ? "Open..." : "Open / Home", () -> protectUnsaved(this::openHomeOrLegacyDialog)),
                fileAction(EditorActionId.FILE_SAVE, "Save", this::saveDocument),
                fileAction(EditorActionId.FILE_SAVE_AS, "Save As...", () -> minecraft.setScreen(ScholarFileDialog.save(this, workspace, () -> { }))),
                fileAction(EditorActionId.FILE_RENAME, "Rename...", () -> minecraft.setScreen(ScholarFileDialog.rename(this, workspace))),
                fileAction(EditorActionId.FILE_CLOSE, "Close", () -> protectUnsaved(this::returnFromEditor)));
    }

    static ScholarEditorScreen forWorkspace(dev.rgcb.scholar.editor.DocumentWorkspace workspace) {
        return new ScholarEditorScreen(workspace, null);
    }

    public static ScholarEditorScreen forApplication(ScholarApplication application, ApplicationDocumentWorkspace workspace) {
        return new ScholarEditorScreen(workspace, application);
    }

    private EditorAction fileAction(EditorActionId id, String label, Runnable operation) {
        return new EditorAction() {
            public EditorActionId id() { return id; }
            public String label() { return label; }
            public String tooltip() {
                return switch (id) {
                    case FILE_NEW -> "Create a new Scholar document";
                    case FILE_OPEN -> "Open a Scholar document";
                    case FILE_SAVE -> "Save the current Scholar document";
                    case FILE_SAVE_AS -> "Save a copy with a different name";
                    case FILE_RENAME -> "Rename the current Scholar document";
                    case FILE_CLOSE -> "Close the current document";
                    default -> label;
                };
            }
            public java.util.Optional<ActionShortcut> shortcut() {
                return dev.rgcb.scholar.client.ui.ScholarScreenActionShortcuts.forAction(id);
            }
            public boolean isEnabled(dev.rgcb.scholar.editor.EditorActionContext context) { return true; }
            public dev.rgcb.scholar.editor.EditorActionResult execute(dev.rgcb.scholar.editor.EditorActionContext context) {
                contextMenu = null;
                if (menuBar != null) menuBar.close();
                if (toolbar != null) toolbar.closePopup();
                if (ribbon != null) ribbon.closePopup();
                operation.run();
                return dev.rgcb.scholar.editor.EditorActionResult.NONE;
            }
        };
    }

    private void saveDocument() {
        if (workspace.name().isEmpty()) {
            minecraft.setScreen(ScholarFileDialog.save(this, workspace, () -> { }));
        } else {
            var result = workspace.save();
            if (result instanceof dev.rgcb.scholar.persistence.PersistenceResult.Failure<?>) {
                minecraft.setScreen(ScholarFileDialog.error(this, workspace, result));
            }
        }
    }

    private void openCreatedDocument() {
        var result = application.createDocument();
        if (result instanceof dev.rgcb.scholar.persistence.PersistenceResult.Success<ApplicationDocumentWorkspace> success) {
            application.closeWorkspace((ApplicationDocumentWorkspace) workspace);
            minecraft.setScreen(forApplication(application, success.value()));
        } else minecraft.setScreen(ScholarFileDialog.error(this, workspace, result));
    }

    private void openHomeOrLegacyDialog() {
        if (application == null) minecraft.setScreen(ScholarFileDialog.open(this, (dev.rgcb.scholar.editor.DocumentWorkspace) workspace));
        else returnFromEditor();
    }

    private void returnFromEditor() {
        if (application != null) application.closeWorkspace((ApplicationDocumentWorkspace) workspace);
        minecraft.setScreen(application == null ? null : ScholarHomeScreen.forApplication(application));
    }

    private void protectUnsaved(Runnable continuation) {
        dragging = false;
        mathDragAnchor = null;
        tableDragAnchor = null;
        session.cancelDiagramElementDrag();
        contextMenu = null;
        if (menuBar != null) menuBar.close();
        if (toolbar != null) toolbar.closePopup();
        if (ribbon != null) ribbon.closePopup();
        if (workspace.isDirty()) minecraft.setScreen(ScholarFileDialog.unsaved(this, workspace, continuation));
        else continuation.run();
    }

    @Override public void onClose() {
        protectUnsaved(this::returnFromEditor);
    }

    @Override
    protected void init() {
        if (controller == null) {
            controller = new ScholarEditorController(
                    session,
                    new MinecraftClipboardAdapter(minecraft),
                    this::relayout,
                    this::keepCaretVisible,
                    () -> laidOutDocument,
                    () -> textMeasurer,
                    this::openSemanticTokenPopup,
                    this::openCrossReferencePopup,
                    this::toggleOutline,
                    kind -> minecraft.setScreen(switch (kind) {
                        case INSERT_ANALYSIS, EDIT_ANALYSIS, ADD_FIT_OVERLAY -> new ScholarAnalysisDialog(this, session, kind);
                        default -> new ScholarComputationDialog(this, session, kind);
                    }));
            menuBar = new MenuBarWidget(controller, menuDefinitions());
            if (application == null) {
                refreshContextualToolbar();
            } else {
                menuBar = null;
                ribbon = new RibbonWidget(controller, ScholarRibbonModel.production(fileActions, editActions,
                        formatActions, blockStyleActions, insertActions, dataActions, tableActions, plotActions,
                        figureActions, diagramActions, layoutActions, combinedViewActions()));
            }
        }
        relayout();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        session.cancelDiagramElementDrag();
        dragging = false;
        diagramPanning = false;
        diagramPanBlockIndex = -1;
        mathDragAnchor = null;
        tableDragAnchor = null;
        if (toolbar != null) { toolbar.closePopup(); }
        if (menuBar != null) { menuBar.close(); }
        if (ribbon != null) { ribbon.closePopup(); }
        super.resize(minecraft, width, height);
        contextMenu = null;
        relayout();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (application == null) refreshContextualToolbar();
        caretVisible = caretBlink.visible(Util.getMillis(), minecraft != null && minecraft.screen == this
                        && minecraft.isWindowActive() && !anyModalPopupOpen() && contextMenu == null
                        && (menuBar == null || !menuBar.isOpen())
                        && (toolbar == null || !toolbar.isPopupOpen())
                        && (ribbon == null || !ribbon.isPopupOpen()),
                editorState().document(), editorState().selection());
        graphics.fill(0, 0, width, height, BACKGROUND_COLOR);
        if (laidOutDocument != null && typographyResolver != null && textMeasurer != null
                && viewportWidth > 0 && viewportHeight > 0) {
            graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
            graphics.pose().pushPose();
            try {
                documentViewTransform().apply(graphics.pose());
                new MinecraftDocumentRenderer(typographyResolver, documentViewTransform()).render(
                        graphics,
                        laidOutDocument,
                        viewportX,
                        viewportY,
                        logicalViewportWidth(),
                        logicalViewportHeight(),
                        scrollOffset);
                renderSelection(graphics);
                renderObjectSelection(graphics);
                renderEquationEditing(graphics);
                renderTableEditing(graphics);
                renderPlotEditing(graphics);
                renderDiagramEditing(graphics);
            } finally {
                graphics.pose().popPose();
                try {
                    renderCaret(graphics);
                } finally {
                    graphics.disableScissor();
                }
            }
        }
        renderStatusBar(graphics, mouseX, mouseY);
        if (toolbar != null) {
            toolbar.render(graphics, font, mouseX, mouseY);
        }
        applicationHeader.render(graphics, font, shellLayout.applicationHeaderBounds(), workspace.name().orElse("Untitled"), workspace.isDirty());
        if (ribbon != null) {
            ribbon.render(graphics, font, mouseX, mouseY);
        }
        if (menuBar != null) {
            menuBar.render(graphics, font, width, height, mouseX, mouseY);
        }
        if (toolbar != null && toolbar.isPopupOpen()) {
            toolbar.renderPopup(graphics, font, mouseX, mouseY);
        }
        if (toolbar != null && (menuBar == null || !menuBar.isOpen())) {
            toolbar.renderTooltip(graphics, font, mouseX, mouseY);
        }
        if (ribbon != null) ribbon.renderTooltip(graphics, font, mouseX, mouseY, height);
        if (contextMenu != null && !anyModalPopupOpen()) {
            contextMenu.render(graphics, font, width, height, mouseX, mouseY);
        }
        renderSemanticTokenPopup(graphics, mouseX, mouseY);
        renderCrossReferencePopup(graphics, mouseX, mouseY);
        renderPlotValuePopup(graphics, mouseX, mouseY);
        renderDiagramLabelPopup(graphics, mouseX, mouseY);
        renderDiagramCanvasPopup(graphics, mouseX, mouseY);
        renderElectricalComponentPopup(graphics, mouseX, mouseY);
        renderOutlinePanel(graphics, mouseX, mouseY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (diagramCanvasPopupOpen) {
            if (Character.isDigit(codePoint) || codePoint == '.') {
                if (diagramCanvasSecondField) {
                    diagramCanvasHeightValue = diagramCanvasHeightValue + codePoint;
                } else {
                    diagramCanvasWidthValue = diagramCanvasWidthValue + codePoint;
                }
                diagramCanvasValidation = "";
            }
            return true;
        }
        if (electricalComponentPopupOpen) {
            if (!Character.isISOControl(codePoint)) {
                if (electricalComponentSecondField) {
                    electricalComponentValue = electricalComponentValue + codePoint;
                } else {
                    electricalReferenceValue = electricalReferenceValue + codePoint;
                }
            }
            return true;
        }
        if (diagramLabelPopupOpen) {
            if (!Character.isISOControl(codePoint)) {
                diagramLabelValue = diagramLabelValue + codePoint;
            }
            return true;
        }
        if (plotValuePopupOpen) {
            if (!Character.isISOControl(codePoint)) {
                appendPlotPopupCharacter(codePoint);
            }
            return true;
        }
        if (semanticTokenPopupOpen) {
            if (!Character.isISOControl(codePoint)) {
                semanticTokenContent = semanticTokenContent + codePoint;
            }
            return true;
        }
        if (crossReferencePopupOpen) {
            return true;
        }
        if (menuBar != null && menuBar.isOpen()) {
            return true;
        }
        if (Screen.hasControlDown()) {
            return false;
        }
        if (Character.isISOControl(codePoint)) {
            return false;
        }
        controller.typeText(String.valueOf(codePoint));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (diagramCanvasPopupOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeDiagramCanvasPopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyDiagramCanvasPopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (diagramCanvasSecondField) {
                    diagramCanvasHeightValue = removeLastCodePoint(diagramCanvasHeightValue);
                } else {
                    diagramCanvasWidthValue = removeLastCodePoint(diagramCanvasWidthValue);
                }
                diagramCanvasValidation = "";
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                diagramCanvasSecondField = !diagramCanvasSecondField;
                return true;
            }
            return true;
        }
        if (electricalComponentPopupOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeElectricalComponentPopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyElectricalComponentPopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (electricalComponentSecondField) {
                    electricalComponentValue = removeLastCodePoint(electricalComponentValue);
                } else {
                    electricalReferenceValue = removeLastCodePoint(electricalReferenceValue);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                electricalComponentSecondField = !electricalComponentSecondField;
                return true;
            }
            return true;
        }
        if (diagramLabelPopupOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeDiagramLabelPopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyDiagramLabelPopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                diagramLabelValue = removeLastCodePoint(diagramLabelValue);
                return true;
            }
            return true;
        }
        if (plotValuePopupOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closePlotValuePopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyPlotValuePopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                removeLastPlotPopupCharacter();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB && plotPopupTarget instanceof PlotPointTarget) {
                plotPointSecondField = !plotPointSecondField;
                return true;
            }
            return true;
        }
        if (semanticTokenPopupOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeSemanticTokenPopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applySemanticTokenPopup();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                removeLastSemanticTokenCharacter();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                semanticTokenKind = semanticTokenKind == SemanticMathTokenKind.NAMED_OPERATOR
                        ? SemanticMathTokenKind.MATH_TEXT
                        : SemanticMathTokenKind.NAMED_OPERATOR;
                return true;
            }
            return true;
        }
        if (crossReferencePopupOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeCrossReferencePopup();
            }
            return true;
        }
        if (contextMenu != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                contextMenu = null;
                return true;
            }
            if (contextMenu.keyPressed(keyCode)) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    contextMenu = null;
                }
                return true;
            }
        }
        if (menuBar != null && menuBar.keyPressed(keyCode)) {
            return true;
        }
        if (toolbar != null && toolbar.keyPressed(keyCode)) {
            return true;
        }
        if (ribbon != null && ribbon.isPopupOpen() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            ribbon.closePopup();
            return true;
        }

        var shiftDown = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (editorState().isTableEditingSelection()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                controller.exitTableEditing();
                tableDragAnchor = null;
                dragging = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                if (shiftDown) {
                    controller.movePreviousTableCell();
                } else {
                    controller.moveNextTableCell();
                }
                return true;
            }
        }
        if (editorState().isDiagramEditingSelection()) {
            if (Screen.hasControlDown()) {
                if (keyCode == GLFW.GLFW_KEY_0) {
                    fitActiveDiagramViewport();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
                    zoomActiveDiagramViewport(1.15);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                    zoomActiveDiagramViewport(1.0 / 1.15);
                    return true;
                }
            }
            if (controller.isDiagramElementDragging()) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    controller.cancelDiagramElementDrag();
                    dragging = false;
                    mathDragAnchor = null;
                    tableDragAnchor = null;
                }
                // A drag gesture owns keyboard interaction until release/cancel,
                // preventing commands from leaving a transient preview on screen.
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (controller.isEnabled(action(EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT))) {
                    executeAction(EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT);
                } else if (controller.diagramConnectionInProgress()) {
                    executeAction(EditorActionId.DIAGRAM_CANCEL_CONNECTION);
                } else {
                    controller.exitDiagramEditing();
                }
                dragging = false;
                mathDragAnchor = null;
                tableDragAnchor = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                if (shiftDown) {
                    controller.movePreviousDiagramTarget();
                } else {
                    controller.moveNextDiagramTarget();
                }
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && !Screen.hasControlDown()) {
                if (controller.isEnabled(action(EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT))) {
                    executeAction(EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT);
                    return true;
                }
                if (controller.diagramConnectionInProgress()) {
                    if (controller.canCompleteDiagramConnection()) {
                        controller.completeDiagramConnection();
                    }
                    return true;
                }
                if (editorState().diagramEditingSelection().target() instanceof DiagramPropertyTarget propertyTarget
                        && propertyTarget.property() == DiagramProperty.CANVAS) {
                    openDiagramCanvasPopup();
                } else {
                    openDiagramLabelPopup();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (controller.isEnabled(action(EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT))) {
                    executeAction(EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT);
                } else if (controller.isEnabled(action(EditorActionId.DIAGRAM_DELETE_JUNCTION))) {
                    executeAction(EditorActionId.DIAGRAM_DELETE_JUNCTION);
                } else if (controller.isEnabled(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT))) {
                    executeAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT);
                } else if (controller.isEnabled(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION))) {
                    executeAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION);
                } else if (controller.isEnabled(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION))) {
                    executeAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION);
                } else if (controller.isEnabled(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL))) {
                    executeAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL);
                } else if (controller.isEnabled(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE))) {
                    executeAction(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE);
                } else if (controller.isEnabled(action(EditorActionId.DIAGRAM_DELETE_NODE))) {
                    executeAction(EditorActionId.DIAGRAM_DELETE_NODE);
                } else if (controller.isEnabled(action(EditorActionId.DIAGRAM_DELETE_CONNECTION))) {
                    executeAction(EditorActionId.DIAGRAM_DELETE_CONNECTION);
                }
                return true;
            }
        }
        if (editorState().isPlotEditingSelection()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                controller.exitPlotEditing();
                dragging = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                if (shiftDown) {
                    controller.movePreviousPlotTarget();
                } else {
                    controller.moveNextPlotTarget();
                }
                return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && !Screen.hasControlDown()) {
                openPlotValuePopup();
                return true;
            }
        }
        for (var action : java.util.stream.Stream.concat(fileActions.stream(), allActions.stream()).toList()) {
            if (action.shortcut().isPresent()
                    && MinecraftShortcutMatcher.matches(action.shortcut().orElseThrow(), keyCode, modifiers)) {
                controller.execute(action);
                return true;
            }
        }
        if (MinecraftShortcutMatcher.matches(SELECT_ALL_SHORTCUT, keyCode, modifiers)) {
            controller.selectAll();
            return true;
        }
        if (menuBar != null && menuBar.isOpen()) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (shiftDown || Screen.hasControlDown()) {
                return true;
            }
            controller.enter();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (shiftDown) {
                controller.extendLeft();
            } else {
                controller.moveLeft();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (shiftDown) {
                controller.extendRight();
            } else {
                controller.moveRight();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (shiftDown) {
                controller.extendUp();
            } else {
                controller.moveUp();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (shiftDown) {
                controller.extendDown();
            } else {
                controller.moveDown();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            if (shiftDown) {
                controller.extendHome();
            } else {
                controller.moveHome();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            if (shiftDown) {
                controller.extendEnd();
            } else {
                controller.moveEnd();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            controller.deleteBackward();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            controller.deleteForward();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (diagramCanvasPopupOpen) {
            return diagramCanvasPopupClicked(mouseX, mouseY);
        }
        if (electricalComponentPopupOpen) {
            return electricalComponentPopupClicked(mouseX, mouseY);
        }
        if (diagramLabelPopupOpen) {
            return diagramLabelPopupClicked(mouseX, mouseY);
        }
        if (plotValuePopupOpen) {
            return plotValuePopupClicked(mouseX, mouseY);
        }
        if (semanticTokenPopupOpen) {
            return semanticTokenPopupClicked(mouseX, mouseY);
        }
        if (crossReferencePopupOpen) {
            return crossReferencePopupClicked(mouseX, mouseY);
        }
        if (contextMenu != null) {
            if (contextMenu.mouseClicked(mouseX, mouseY)) {
                contextMenu = null;
                return true;
            }
            contextMenu = null;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return true;
            }
        }
        if (shellLayout.statusBarBounds().contains(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) statusBarClicked(mouseX, mouseY);
            return true;
        }
        if (outlineOpen && contains(outlinePanelRect(), mouseX, mouseY)) {
            return outlinePanelClicked(mouseX, mouseY);
        }
        if (ribbon != null && ribbon.isPopupOpen()) {
            return ribbon.mouseClicked(mouseX, mouseY, button);
        }
        if (toolbar != null && toolbar.isPopupOpen()) {
            toolbar.mouseClicked(mouseX, mouseY);
            return true;
        }
        if (menuBar != null && menuBar.mouseClicked(mouseX, mouseY)) {
            if (toolbar != null) {
                toolbar.closePopup();
            }
            return true;
        }
        if (toolbar != null && toolbar.mouseClicked(mouseX, mouseY)) {
            if (menuBar != null && toolbar.isPopupOpen()) {
                menuBar.close();
            }
            return true;
        }
        if (ribbon != null && ribbon.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return openContextMenu(mouseX, mouseY);
        }
        if (laidOutDocument != null && isInsideViewport(mouseX, mouseY)
                && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            var workspaceHit = diagramWorkspaceAt(documentLocalX(mouseX), documentLocalY(mouseY));
            if (workspaceHit != null) {
                diagramPanning = true;
                diagramPanBlockIndex = workspaceHit.blockIndex();
                return true;
            }
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || laidOutDocument == null || textMeasurer == null || !isInsideViewport(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        var localX = documentLocalX(mouseX);
        var localY = documentLocalY(mouseY);
        if (navigateFromTableOfContents(localX, localY)) {
            return true;
        }
        if (editorState().isEquationEditingSelection()) {
            var equationSelection = editorState().equationEditingSelection();
            var equationBlock = laidOutDocument.blocks().get(equationSelection.blockIndex());
            if (containsBlock(equationBlock, localX, localY) && equationBlock.math().isPresent()) {
                var math = equationBlock.math().orElseThrow();
                var mathX = equationBlock.x() + Math.max(0, (equationBlock.width() - math.width()) / 2);
                var mathBaselineY = equationBlock.y() + math.root().ascent();
                var hit = mathHitTester.hit(math, localX - mathX, localY - mathBaselineY, mathTextMeasurer);
                controller.setEquationEditingSelection(new MathCaretSelection(hit));
                mathDragAnchor = hit;
                dragging = true;
                return true;
            }
        }
        var diagramHit = hitDiagramTarget(localX, localY);
        if (diagramHit != null) {
            var handled = false;
            if (editorState().isDiagramEditingSelection()
                    && editorState().diagramEditingSelection().blockIndex() == diagramHit.blockIndex()) {
                controller.setDiagramEditingTarget(diagramHit.target());
                handled = true;
            } else if (editorState().isBlockSelection()
                    && editorState().blockSelection().blockIndex() == diagramHit.blockIndex()) {
                controller.enterDiagramEditing(diagramHit.blockIndex(), diagramHit.target());
                handled = true;
            }
            if (handled) {
                dragging = false;
                mathDragAnchor = null;
                tableDragAnchor = null;
                if (diagramHit.target() instanceof DiagramElementTarget) {
                    var diagram = laidOutDocument.blocks().get(diagramHit.blockIndex()).diagram().orElseThrow();
                    var logicalX = diagram.transform().unmapX(localX);
                    var logicalY = diagram.transform().unmapY(localY);
                    dragging = controller.beginDiagramElementDrag(logicalX, logicalY);
                }
                return true;
            }
        }

        var plotHit = hitPlotTarget(localX, localY);
        if (plotHit != null) {
            if (editorState().isPlotEditingSelection()
                    && editorState().plotEditingSelection().blockIndex() == plotHit.blockIndex()) {
                controller.setPlotEditingTarget(plotHit.target());
                dragging = false;
                return true;
            }
            if (editorState().isBlockSelection()
                    && editorState().blockSelection().blockIndex() == plotHit.blockIndex()) {
                controller.enterPlotEditing(plotHit.blockIndex(), plotHit.target());
                dragging = false;
                return true;
            }
        }
        var tableHit = hitTableCell(localX, localY);
        if (tableHit != null) {
            controller.setState(new EditorState(
                    editorState().document(),
                    new TableEditingSelection(tableHit.blockIndex(), tableHit.hit().caretSelection()),
                    java.util.Optional.empty()));
            tableDragAnchor = tableHit.hit().caretSelection();
            mathDragAnchor = null;
            dragging = true;
            return true;
        }
        var hit = hitTester.hit(laidOutDocument, localX, localY, textMeasurer);
        if (hit.kind() == DocumentHit.Kind.TEXT) {
            dragging = true;
            tableDragAnchor = null;
            mathDragAnchor = null;
            controller.setState(editorState().collapseTo(hit.position().orElseThrow()));
            return true;
        }
        if (hit.kind() == DocumentHit.Kind.BLOCK) {
            dragging = false;
            tableDragAnchor = null;
            mathDragAnchor = null;
            controller.setState(editorState().selectBlock(hit.blockIndex().orElseThrow()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (zoomSliderDragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            setZoom(ScholarStatusBarLayout.zoomAt(
                    ScholarStatusBarLayout.compute(shellLayout.statusBarBounds()).slider(), mouseX));
            return true;
        }
        if (contextMenu != null) {
            return true;
        }
        if (menuBar != null && menuBar.isOpen()) {
            return true;
        }
        if (toolbar != null && toolbar.isPopupOpen()) {
            return true;
        }
        if (toolbar != null && toolbar.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        if (diagramPanning && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && laidOutDocument != null) {
            var diagram = diagramLayout(diagramPanBlockIndex);
            if (diagram != null && diagram.transform().scale() > 0.0) {
                var viewport = diagram.viewport();
                diagramViewports.put(diagramPanBlockIndex, new DiagramViewport(
                        viewport.zoom(),
                        viewport.centerX() - dragX / diagram.transform().scale(),
                        viewport.centerY() - dragY / diagram.transform().scale()));
                relayout();
            }
            return true;
        }
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || laidOutDocument == null || textMeasurer == null) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        var localX = documentLocalX(mouseX);
        var localY = documentLocalY(mouseY);
        if (editorState().isDiagramEditingSelection() && controller.isDiagramElementDragging()) {
            var diagram = activeDiagramLayout();
            if (diagram != null) {
                var logicalX = diagram.transform().unmapX(localX);
                var logicalY = diagram.transform().unmapY(localY);
                controller.previewDiagramElementDrag(logicalX, logicalY).ifPresent(this::relayoutPreview);
            }
        } else if (editorState().isEquationEditingSelection() && mathDragAnchor != null && mathTextMeasurer != null) {
            var hit = hitMathPosition(localX, localY);
            if (hit != null) {
                controller.setEquationEditingSelection(mathDragAnchor.equals(hit)
                        ? new MathCaretSelection(mathDragAnchor)
                        : new MathRangeSelection(mathDragAnchor, hit));
            }
        } else if (editorState().isTableEditingSelection() && tableDragAnchor != null) {
            var hit = hitTableCellInActiveTable(localX, localY);
            if (hit != null) {
                controller.setTableEditingSelection(tableDragAnchor.activeOffset() == hit.characterOffset()
                        ? TableCellTextSelection.caret(tableDragAnchor.cell(), tableDragAnchor.anchorOffset())
                        : new TableCellTextSelection(tableDragAnchor.cell(), tableDragAnchor.anchorOffset(), hit.characterOffset()));
            }
        } else if (editorState().isTextSelection()) {
            var position = selectionDragResolver.resolve(
                    laidOutDocument,
                    editorState().anchor(),
                    localX,
                    localY,
                    textMeasurer);
            position.ifPresent(documentPosition -> controller.setState(editorState().withActive(documentPosition)));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (zoomSliderDragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            zoomSliderDragging = false;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && diagramPanning) {
            diagramPanning = false;
            diagramPanBlockIndex = -1;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragging && controller.isDiagramElementDragging()) {
            var localX = documentLocalX(mouseX);
            var localY = documentLocalY(mouseY);
            var diagram = activeDiagramLayout();
            if (diagram != null) {
                controller.commitDiagramElementDrag(
                        diagram.transform().unmapX(localX),
                        diagram.transform().unmapY(localY));
            } else {
                controller.cancelDiagramElementDrag();
            }
            dragging = false;
            mathDragAnchor = null;
            tableDragAnchor = null;
            return true;
        }
        if (menuBar != null && menuBar.isOpen()) {
            return true;
        }
        if (toolbar != null && toolbar.isPopupOpen()) {
            return true;
        }
        if (toolbar != null && toolbar.mouseReleased(mouseX, mouseY)) {
            return true;
        }
        if (ribbon != null && ribbon.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragging) {
            dragging = false;
            mathDragAnchor = null;
            tableDragAnchor = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (contextMenu == null && !anyModalPopupOpen() && menuBar != null && mouseY < MenuBarWidget.HEIGHT
                && menuBar.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (contextMenu != null) {
            if (contextMenu.mouseScrolled(mouseX, mouseY, scrollY)) {
                return true;
            }
            contextMenu = null;
        }
        if (menuBar != null && menuBar.isOpen()) {
            menuBar.mouseScrolled(mouseX, mouseY, scrollY);
            return true;
        }
        if (toolbar != null && toolbar.isPopupOpen()) {
            return true;
        }
        if (toolbar != null && toolbar.contains(mouseX, mouseY)) {
            return true;
        }
        if (ribbon != null && ribbon.contains(mouseX, mouseY)) {
            ribbon.mouseScrolled(mouseX, mouseY, scrollY);
            return true;
        }
        if (outlineOpen && contains(outlinePanelRect(), mouseX, mouseY)) {
            outlineScroll = clampOutlineScroll(outlineScroll - (int) Math.signum(scrollY) * ScholarShellLayout.DOCUMENT_SCROLL_STEP);
            return true;
        }
        if (laidOutDocument == null || !isInsideViewport(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (Screen.hasControlDown() && scrollY != 0.0) {
            var localX = documentLocalX(mouseX);
            var localY = documentLocalY(mouseY);
            var workspaceHit = diagramWorkspaceAt(localX, localY);
            if (workspaceHit != null) {
                zoomDiagramViewportAt(
                        workspaceHit.blockIndex(),
                        workspaceHit.diagram(),
                        localX,
                        localY,
                        scrollY > 0.0 ? 1.15 : 1.0 / 1.15);
                return true;
            }
        }

        scrollOffset = clampScroll(scrollOffset - (int) Math.signum(scrollY) * ScholarShellLayout.DOCUMENT_SCROLL_STEP);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private EditorState editorState() {
        return session.current();
    }

    private boolean openContextMenu(double mouseX, double mouseY) {
        if (controller == null) {
            return false;
        }
        if (menuBar != null) {
            menuBar.close();
        }
        if (toolbar != null) {
            toolbar.closePopup();
        }
        if (ribbon != null) {
            ribbon.closePopup();
        }
        dragging = false;
        mathDragAnchor = null;
        tableDragAnchor = null;
        controller.cancelDiagramElementDrag();

        var emptyArea = laidOutDocument == null || textMeasurer == null || !isInsideViewport(mouseX, mouseY);
        if (!emptyArea) {
            emptyArea = !selectRightClickTarget(documentLocalX(mouseX), documentLocalY(mouseY));
        }
        var entries = emptyArea
                ? contextActionResolver.resolveEmptyArea(editorState(), actionMap())
                : contextActionResolver.resolve(editorState(), actionMap());
        if (entries.isEmpty()) {
            contextMenu = null;
            return true;
        }
        contextMenu = new ContextMenuWidget(controller, entries, (int) mouseX, (int) mouseY);
        return true;
    }

    private boolean selectRightClickTarget(int localX, int localY) {
        if (editorState().isEquationEditingSelection()) {
            var equationSelection = editorState().equationEditingSelection();
            var equationBlock = laidOutDocument.blocks().get(equationSelection.blockIndex());
            if (containsBlock(equationBlock, localX, localY) && equationBlock.math().isPresent()) {
                controller.setEquationEditingSelection(new MathCaretSelection(hitMathPosition(localX, localY)));
                return true;
            }
        }

        var diagramHit = hitDiagramTarget(localX, localY);
        if (diagramHit != null) {
            controller.enterDiagramEditing(diagramHit.blockIndex(), diagramHit.target());
            return true;
        }

        var plotHit = hitPlotTarget(localX, localY);
        if (plotHit != null) {
            controller.enterPlotEditing(plotHit.blockIndex(), plotHit.target());
            return true;
        }

        var tableHit = hitTableCell(localX, localY);
        if (tableHit != null) {
            var next = tableHit.hit().caretSelection();
            if (rightClickInsideCurrentTableSelection(tableHit, next.activeOffset())) {
                return true;
            }
            controller.setState(new EditorState(
                    editorState().document(),
                    new TableEditingSelection(tableHit.blockIndex(), next),
                    java.util.Optional.empty()));
            return true;
        }

        var hit = hitTester.hit(laidOutDocument, localX, localY, textMeasurer);
        if (hit.kind() == DocumentHit.Kind.TEXT) {
            var position = hit.position().orElseThrow();
            if (rightClickInsideCurrentTextSelection(position)) {
                return true;
            }
            controller.setState(editorState().collapseTo(position));
            return true;
        }
        if (hit.kind() == DocumentHit.Kind.BLOCK) {
            controller.setState(editorState().selectBlock(hit.blockIndex().orElseThrow()));
            return true;
        }
        return false;
    }

    private boolean rightClickInsideCurrentTextSelection(DocumentPosition position) {
        if (!editorState().isTextSelection() || !editorState().hasSelection()) {
            return false;
        }
        var range = editorState().selectionRange();
        if (position.blockIndex() < range.start().blockIndex() || position.blockIndex() > range.end().blockIndex()) {
            return false;
        }
        if (position.blockIndex() == range.start().blockIndex() && position.characterOffset() < range.start().characterOffset()) {
            return false;
        }
        return position.blockIndex() != range.end().blockIndex() || position.characterOffset() <= range.end().characterOffset();
    }

    private boolean rightClickInsideCurrentTableSelection(TableCellDocumentHit hit, int offset) {
        if (!editorState().isTableEditingSelection()) {
            return false;
        }
        var selection = editorState().tableEditingSelection();
        return selection.blockIndex() == hit.blockIndex()
                && selection.selection().cell().equals(hit.hit().cell())
                && !selection.selection().isCaret()
                && offset >= selection.selection().startOffset()
                && offset <= selection.selection().endOffset();
    }

    private Map<EditorActionId, EditorAction> actionMap() {
        return allActions.stream().collect(Collectors.toMap(EditorAction::id, Function.identity(), (first, duplicate) -> first));
    }

    private boolean anyModalPopupOpen() {
        return semanticTokenPopupOpen
                || crossReferencePopupOpen
                || plotValuePopupOpen
                || diagramLabelPopupOpen
                || diagramCanvasPopupOpen
                || electricalComponentPopupOpen;
    }

    private void executeAction(EditorActionId actionId) {
        allActions.stream()
                .filter(action -> action.id() == actionId)
                .findFirst()
                .ifPresent(controller::execute);
    }

    private void relayout() {
        if (font == null) {
            return;
        }

        shellLayout = ScholarShellLayout.compute(width, height, application != null);
        if (menuBar != null) { menuBar.setViewportSize(width, height); }
        if (application == null) refreshContextualToolbar();
        if (toolbar != null) {
            toolbar.setBounds(shellLayout.toolbarBounds());
            toolbar.setViewportHeight(height);
        }
        if (ribbon != null) {
            ribbon.setBounds(shellLayout.menuBarBounds(), shellLayout.toolbarBounds());
            ribbon.setViewportHeight(height);
        }
        var horizontalMargin = Math.max(typography.minPageMargin(), width / 12);
        viewportY = shellLayout.documentWorkspaceBounds().y() + typography.minPageMargin();
        viewportHeight = Math.max(0, shellLayout.documentWorkspaceBounds().bottom()
                - viewportY - typography.minPageMargin());
        typographyResolver = new MinecraftTypographyResolver(font, typography).highResolution();
        textMeasurer = new MinecraftTextMeasurer(typographyResolver);
        mathTextMeasurer = new MinecraftMathTextMeasurer(typographyResolver);
        diagramViewports.reconcile(editorState().document());
        laidOutDocument = layoutEngine.layoutPaginated(
                editorState().document(),
                textMeasurer,
                mathTextMeasurer,
                this::diagramViewportFor);
        viewportWidth = Math.min(laidOutDocument.width(), Math.max(80, width - horizontalMargin * 2));
        viewportX = (width - viewportWidth) / 2;
        scrollOffset = clampScroll(scrollOffset);
        if (controller != null) {
            controller.clearPreferredCaretX();
        }
    }

    void refreshComputationLayout() {
        relayout();
    }

    private void relayoutPreview(Document document) {
        if (document == null || textMeasurer == null || mathTextMeasurer == null || viewportWidth <= 0) {
            return;
        }
        laidOutDocument = layoutEngine.layoutPaginated(
                document, textMeasurer, mathTextMeasurer, this::diagramViewportFor);
        scrollOffset = clampScroll(scrollOffset);
    }

    private void renderCaret(GuiGraphics graphics) {
        if (!caretVisible || !editorState().isTextSelection() || editorState().hasSelection()) {
            return;
        }
        var caret = caretGeometryResolver.resolve(editorState().caret(), laidOutDocument, textMeasurer);
        var rect = documentViewTransform().caretRect(caret, viewportX, viewportY, scrollOffset);
        if (rect.bottom() < viewportY || rect.top() > viewportY + viewportHeight) {
            return;
        }
        graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), CARET_COLOR);
    }

    private void renderStatusBar(GuiGraphics graphics, int mouseX, int mouseY) {
        var bar = shellLayout.statusBarBounds();
        if (bar.height() < 18) return;
        ScholarShellRenderer.drawRaisedPanel(graphics, bar.x(), bar.y(), bar.width(), bar.height(), ScholarShellStyle.PANEL);
        var slots = ScholarStatusBarLayout.compute(bar);
        if (laidOutDocument != null) {
            int pageY = scrollOffset + logicalViewportHeight() / 2;
            if (editorState().isTextSelection() && textMeasurer != null) {
                pageY = caretGeometryResolver.resolve(editorState().caret(), laidOutDocument, textMeasurer).y();
            } else if (editorState().isBlockSelection()) {
                pageY = laidOutDocument.blocks().get(editorState().blockSelection().blockIndex()).y();
            }
            drawStatusText(graphics, slots.page(), "Page " + DocumentStatus.pageAt(laidOutDocument, pageY)
                    + " of " + laidOutDocument.pages().size());
        }
        var document = editorState().document();
        if (statusWordDocument != document) {
            statusWordDocument = document;
            statusWordCount = DocumentStatus.wordCount(document);
        }
        drawStatusText(graphics, slots.words(), statusWordCount + " words");
        drawStatusIcon(graphics, slots.fitPage(), ScholarIcons.FIT_PAGE, mouseX, mouseY);
        drawStatusIcon(graphics, slots.fitWidth(), ScholarIcons.FIT_WIDTH, mouseX, mouseY);
        drawStatusIcon(graphics, slots.zoomOut(), ScholarIcons.ZOOM_OUT, mouseX, mouseY);
        if (slots.slider().width() > 0) {
            var slot = slots.slider();
            int trackY = slot.y() + slot.height() / 2;
            graphics.fill(slot.x(), trackY, slot.right(), trackY + 2, ScholarShellStyle.SEPARATOR_DARK);
            int thumbX = ScholarStatusBarLayout.thumbX(slot, zoom);
            ScholarShellRenderer.drawRaisedPanel(graphics, thumbX - 3, slot.y() + 2, 7, 14,
                    ScholarShellStyle.HIGHLIGHT);
        }
        drawStatusIcon(graphics, slots.zoomIn(), ScholarIcons.ZOOM_IN, mouseX, mouseY);
        drawStatusText(graphics, slots.percentage(), Math.round(zoom * 100) + "%");
        String tooltip = slots.fitPage().contains(mouseX, mouseY) ? "Fit Page"
                : slots.fitWidth().contains(mouseX, mouseY) ? "Fit Width"
                : slots.zoomOut().contains(mouseX, mouseY) ? "Zoom Out"
                : slots.zoomIn().contains(mouseX, mouseY) ? "Zoom In"
                : slots.slider().contains(mouseX, mouseY) ? "Zoom" : null;
        if (tooltip != null) {
            int tipWidth = font.width(tooltip) + 10;
            int tipX = Math.max(2, Math.min(mouseX, width - tipWidth - 2));
            graphics.fill(tipX, bar.y() - 18, tipX + tipWidth, bar.y() - 2, ScholarShellStyle.PANEL_RECESSED);
            graphics.drawString(font, tooltip, tipX + 5, bar.y() - 14, ScholarShellStyle.TEXT, false);
        }
    }

    private void drawStatusText(GuiGraphics graphics, ShellRect slot, String text) {
        if (slot.width() > 0) graphics.drawString(font, text, slot.x(), slot.y() + 5, ScholarShellStyle.TEXT, false);
    }

    private void drawStatusIcon(GuiGraphics graphics, ShellRect slot, dev.rgcb.scholar.client.ui.ScholarIcon icon,
                                int mouseX, int mouseY) {
        if (slot.width() == 0) return;
        if (slot.contains(mouseX, mouseY)) graphics.fill(slot.x(), slot.y(), slot.right(), slot.bottom(), ScholarShellStyle.HOVER);
        icon.render(graphics, slot.x() + (slot.width() - icon.width()) / 2,
                slot.y() + (slot.height() - icon.height()) / 2, 1, ScholarShellStyle.TEXT);
    }

    private void statusBarClicked(double mouseX, double mouseY) {
        var slots = ScholarStatusBarLayout.compute(shellLayout.statusBarBounds());
        if (slots.fitPage().contains(mouseX, mouseY)) executeViewportAction(EditorActionId.VIEW_FIT_PAGE);
        else if (slots.fitWidth().contains(mouseX, mouseY)) executeViewportAction(EditorActionId.VIEW_FIT_WIDTH);
        else if (slots.zoomOut().contains(mouseX, mouseY)) executeViewportAction(EditorActionId.VIEW_ZOOM_OUT);
        else if (slots.zoomIn().contains(mouseX, mouseY)) executeViewportAction(EditorActionId.VIEW_ZOOM_IN);
        else if (slots.slider().contains(mouseX, mouseY)) {
            zoomSliderDragging = true;
            setZoom(ScholarStatusBarLayout.zoomAt(slots.slider(), mouseX));
        }
    }

    private void executeViewportAction(EditorActionId id) {
        viewportActions.stream().filter(action -> action.id() == id).findFirst().ifPresent(controller::execute);
    }

    private void renderSelection(GuiGraphics graphics) {
        if (!editorState().isTextSelection() || !editorState().hasSelection()) {
            return;
        }

        var rects = selectionGeometryResolver.resolve(editorState().selectionRange(), laidOutDocument, textMeasurer);
        enableViewportScissor(graphics);
        try {
            for (var rect : rects) {
                var x = viewportX + rect.x();
                var y = viewportY + rect.y() - scrollOffset;
                graphics.fill(x, y, x + rect.width(), y + rect.height(), SELECTION_COLOR);
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderObjectSelection(GuiGraphics graphics) {
        if (!editorState().isBlockSelection()) {
            return;
        }
        var blockIndex = editorState().blockSelection().blockIndex();
        if (blockIndex < 0 || blockIndex >= laidOutDocument.blocks().size()) {
            return;
        }
        var block = laidOutDocument.blocks().get(blockIndex);
        var padding = 4;
        var x = viewportX + block.x() - padding;
        var y = viewportY + block.y() - scrollOffset - padding;
        var width = Math.max(24, block.width() + padding * 2);
        var height = Math.max(18, block.height() + padding * 2);
        if (y + height < viewportY || y > viewportY + viewportHeight) {
            return;
        }
        enableViewportScissor(graphics);
        try {
            graphics.fill(x, y, x + width, y + height, OBJECT_SELECTION_FILL);
            graphics.renderOutline(x, y, width, height, OBJECT_SELECTION_BORDER);
            graphics.renderOutline(x + 1, y + 1, Math.max(1, width - 2), Math.max(1, height - 2), OBJECT_SELECTION_INNER_BORDER);
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderEquationEditing(GuiGraphics graphics) {
        if (!editorState().isEquationEditingSelection() || mathTextMeasurer == null) {
            return;
        }
        var blockIndex = editorState().equationEditingSelection().blockIndex();
        if (blockIndex < 0 || blockIndex >= laidOutDocument.blocks().size()) {
            return;
        }
        var block = laidOutDocument.blocks().get(blockIndex);
        if (block.math().isEmpty()) {
            return;
        }
        var math = block.math().orElseThrow();
        var mathX = block.x() + Math.max(0, (block.width() - math.width()) / 2);
        var mathBaselineY = block.y() + math.root().ascent();
        var padding = 3;
        var focusX = viewportX + block.x() - padding;
        var focusY = viewportY + block.y() - scrollOffset - padding;
        var focusWidth = Math.max(24, block.width() + padding * 2);
        var focusHeight = Math.max(18, block.height() + padding * 2);
        if (focusY + focusHeight < viewportY || focusY > viewportY + viewportHeight) {
            return;
        }
        enableViewportScissor(graphics);
        try {
            graphics.renderOutline(focusX, focusY, focusWidth, focusHeight, EQUATION_FOCUS_BORDER);
            var selection = editorState().equationEditingSelection().selection();
            if (selection instanceof MathRangeSelection) {
                var expression = ((dev.rgcb.scholar.document.EquationBlock) editorState().document().blocks().get(blockIndex)).expression();
                for (var rect : mathSelectionGeometryResolver.resolve(expression, selection, math, mathTextMeasurer)) {
                    var x = viewportX + mathX + rect.x();
                    var y = viewportY + mathBaselineY + rect.y() - scrollOffset;
                    graphics.fill(x, y, x + Math.max(1, rect.width()), y + Math.max(1, rect.height()), SELECTION_COLOR);
                }
            } else if (selection instanceof MathCaretSelection caretSelection) {
                var caret = mathCaretGeometryResolver.resolve(caretSelection.caret(), math, mathTextMeasurer);
                var caretX = viewportX + mathX + caret.x();
                var caretY = viewportY + mathBaselineY + caret.y() - scrollOffset;
                if (caretVisible) {
                    graphics.fill(caretX, caretY, caretX + 1, caretY + caret.height(), EQUATION_CARET_COLOR);
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderTableEditing(GuiGraphics graphics) {
        if (!editorState().isTableEditingSelection()) {
            return;
        }
        var tableSelection = editorState().tableEditingSelection();
        var cell = activeTableCell(tableSelection);
        if (cell == null) {
            return;
        }
        var focusX = viewportX + cell.x();
        var focusY = viewportY + cell.y() - scrollOffset;
        if (focusY + cell.height() < viewportY || focusY > viewportY + viewportHeight) {
            return;
        }
        enableViewportScissor(graphics);
        try {
            graphics.renderOutline(focusX, focusY, Math.max(1, cell.width()), Math.max(1, cell.height()), TABLE_FOCUS_BORDER);
            for (var rect : tableSelectionGeometryResolver.resolve(cell, tableSelection.selection(), textMeasurer)) {
                var x = viewportX + rect.x();
                var y = viewportY + rect.y() - scrollOffset;
                graphics.fill(x, y, x + rect.width(), y + rect.height(), SELECTION_COLOR);
            }
            if (tableSelection.selection().isCaret() && caretVisible) {
                var caret = tableCaretGeometryResolver.resolve(cell, tableSelection.selection().activeOffset(), textMeasurer);
                var x = viewportX + caret.x();
                var y = viewportY + caret.y() - scrollOffset;
                graphics.fill(x, y, x + 1, y + Math.max(1, caret.height()), CARET_COLOR);
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderPlotEditing(GuiGraphics graphics) {
        if (!editorState().isPlotEditingSelection()) {
            return;
        }
        var selection = editorState().plotEditingSelection();
        if (selection.blockIndex() < 0 || selection.blockIndex() >= laidOutDocument.blocks().size()) {
            return;
        }
        var block = laidOutDocument.blocks().get(selection.blockIndex());
        if (block.plot().isEmpty()) {
            return;
        }
        var plot = block.plot().orElseThrow();
        var focusX = viewportX + block.x() - 3;
        var focusY = viewportY + block.y() - scrollOffset - 3;
        var focusWidth = Math.max(24, block.width() + 6);
        var focusHeight = Math.max(18, block.height() + 6);
        if (focusY + focusHeight < viewportY || focusY > viewportY + viewportHeight) {
            return;
        }
        enableViewportScissor(graphics);
        try {
            graphics.renderOutline(focusX, focusY, focusWidth, focusHeight, PLOT_FOCUS_BORDER);
            renderPlotTarget(graphics, plot, selection.target());
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderDiagramEditing(GuiGraphics graphics) {
        if (!editorState().isDiagramEditingSelection()) {
            return;
        }
        var selection = editorState().diagramEditingSelection();
        if (selection.blockIndex() < 0 || selection.blockIndex() >= laidOutDocument.blocks().size()) {
            return;
        }
        var block = laidOutDocument.blocks().get(selection.blockIndex());
        if (block.diagram().isEmpty()) {
            return;
        }
        var diagram = block.diagram().orElseThrow();
        var focusX = viewportX + block.x() - 3;
        var focusY = viewportY + block.y() - scrollOffset - 3;
        var focusWidth = Math.max(24, block.width() + 6);
        var focusHeight = Math.max(18, block.height() + 6);
        if (focusY + focusHeight < viewportY || focusY > viewportY + viewportHeight) {
            return;
        }
        enableViewportScissor(graphics);
        try {
            graphics.renderOutline(focusX, focusY, focusWidth, focusHeight, DIAGRAM_FOCUS_BORDER);
            var titleTarget = selection.target() instanceof DiagramPropertyTarget propertyTarget
                    && propertyTarget.property() == DiagramProperty.TITLE;
            if (titleTarget) {
                renderDiagramTarget(graphics, diagram, selection.target());
            } else {
                var workspaceX = viewportX + diagram.workspaceX();
                var workspaceY = viewportY + diagram.workspaceY() - scrollOffset;
                enableDocumentScissor(graphics,
                        workspaceX, workspaceY,
                        workspaceX + diagram.workspaceWidth(), workspaceY + diagram.workspaceHeight());
                try {
                    renderDiagramTarget(graphics, diagram, selection.target());
                    controller.diagramConnectionSourceTarget().ifPresent(
                            source -> renderDiagramConnectionSource(graphics, diagram, source));
                } finally {
                    graphics.disableScissor();
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderDiagramTarget(GuiGraphics graphics, LaidOutDiagram diagram, DiagramEditTarget target) {
        if (target instanceof DiagramPropertyTarget propertyTarget) {
            if (propertyTarget.property() == DiagramProperty.TITLE && diagram.title().isPresent()) {
                var label = diagram.title().orElseThrow();
                renderDiagramTargetRect(
                        graphics,
                        label.x() - 3,
                        label.y() - 2,
                        Math.max(8, label.width() + 6),
                        Math.max(10, label.height() + 4));
            } else {
                renderDiagramTargetRect(
                        graphics,
                        diagram.workspaceX(),
                        diagram.workspaceY(),
                        diagram.workspaceWidth(),
                        diagram.workspaceHeight());
            }
            return;
        }

        if (target instanceof DiagramElementTarget elementTarget) {
            for (var node : diagram.nodes()) {
                if (node.elementIndex() == elementTarget.elementIndex()
                        && node.elementId().equals(elementTarget.elementId())) {
                    renderDiagramTargetRect(
                            graphics,
                            node.x() - 3,
                            node.y() - 3,
                            node.width() + 6,
                            node.height() + 6);
                    return;
                }
            }
            for (var component : diagram.electricalComponents()) {
                if (component.elementIndex() == elementTarget.elementIndex()
                        && component.elementId().equals(elementTarget.elementId())) {
                    renderDiagramTargetRect(
                            graphics,
                            component.x() - 3,
                            component.y() - 3,
                            component.width() + 6,
                            component.height() + 6);
                    return;
                }
            }
            for (var junction : diagram.electricalJunctions()) {
                if (junction.elementIndex() == elementTarget.elementIndex()
                        && junction.elementId().equals(elementTarget.elementId())) {
                    renderDiagramTargetRect(
                            graphics, junction.x() - 5, junction.y() - 5, junction.width() + 10, junction.height() + 10);
                    return;
                }
            }
            for (var primitive : diagram.mechanicalPrimitives()) {
                if (primitive.elementIndex() == elementTarget.elementIndex()
                        && primitive.elementId().equals(elementTarget.elementId())) {
                    var rect = primitive.bounds();
                    renderDiagramTargetRect(graphics, rect.x() - 3, rect.y() - 3, rect.width() + 6, rect.height() + 6);
                    return;
                }
            }
            for (var symbol : diagram.mechanicalSymbols()) {
                if (symbol.elementIndex() == elementTarget.elementIndex() && symbol.elementId().equals(elementTarget.elementId())) {
                    var rect=symbol.bounds(); renderDiagramTargetRect(graphics,rect.x()-3,rect.y()-3,rect.width()+6,rect.height()+6); return;
                }
            }
            for (var annotation : diagram.mechanicalAnnotations()) {
                if(annotation.elementIndex()==elementTarget.elementIndex() && annotation.elementId().equals(elementTarget.elementId())) {
                    var rect=annotation.bounds(); renderDiagramTargetRect(graphics,rect.x()-3,rect.y()-3,rect.width()+6,rect.height()+6); return;
                }
            }
            for (var dimension : diagram.mechanicalDimensions()) {
                if (dimension.elementIndex() == elementTarget.elementIndex()
                        && dimension.elementId().equals(elementTarget.elementId())) {
                    var rect = dimension.bounds();
                    renderDiagramTargetRect(graphics, rect.x() - 3, rect.y() - 3, rect.width() + 6, rect.height() + 6);
                    return;
                }
            }
            for (var constraint : diagram.mechanicalConstraints()) {
                if (constraint.elementIndex() == elementTarget.elementIndex()
                        && constraint.elementId().equals(elementTarget.elementId())) {
                    var rect = constraint.bounds();
                    renderDiagramTargetRect(graphics, rect.x() - 3, rect.y() - 3, rect.width() + 6, rect.height() + 6);
                    return;
                }
            }
            return;
        }

        if (target instanceof DiagramPortTarget portTarget) {
            for (var node : diagram.nodes()) {
                if (renderMatchingDiagramPort(graphics, node.ports(), portTarget)) {
                    return;
                }
            }
            for (var component : diagram.electricalComponents()) {
                if (renderMatchingDiagramPort(graphics, component.ports(), portTarget)) {
                    return;
                }
            }
            for (var junction : diagram.electricalJunctions()) {
                if (renderMatchingDiagramPort(graphics, junction.ports(), portTarget)) {
                    return;
                }
            }
            return;
        }

        if (target instanceof DiagramConnectionTarget connectionTarget
                && connectionTarget.connectionIndex() < diagram.connections().size()) {
            var connection = diagram.connections().get(connectionTarget.connectionIndex());
            var path = connection.path();
            for (var index = 1; index < path.size(); index++) {
                renderDiagramTargetSegment(graphics, path.get(index - 1), path.get(index));
            }
            connection.label().ifPresent(label -> renderDiagramTargetRect(
                    graphics,
                    label.x() - 3,
                    label.y() - 2,
                    Math.max(8, label.width() + 6),
                    Math.max(10, label.height() + 4)));
        }
    }

    private boolean renderMatchingDiagramPort(
            GuiGraphics graphics,
            java.util.List<dev.rgcb.scholar.diagram.layout.LaidOutDiagramPort> ports,
            DiagramPortTarget target
    ) {
        for (var port : ports) {
            if (port.elementIndex() == target.elementIndex()
                    && port.elementId().equals(target.elementId())
                    && port.portIndex() == target.portIndex()
                    && port.portId().equals(target.portId())) {
                renderDiagramTargetRect(graphics, port.centerX() - 5, port.centerY() - 5, 11, 11);
                return true;
            }
        }
        return false;
    }

    private void renderDiagramTargetSegment(
            GuiGraphics graphics,
            dev.rgcb.scholar.diagram.layout.LaidOutDiagramPoint from,
            dev.rgcb.scholar.diagram.layout.LaidOutDiagramPoint to
    ) {
        var x1 = viewportX + from.x();
        var y1 = viewportY + from.y() - scrollOffset;
        var x2 = viewportX + to.x();
        var y2 = viewportY + to.y() - scrollOffset;
        if (x1 == x2) {
            graphics.fill(x1 - 1, Math.min(y1, y2), x1 + 2, Math.max(y1, y2) + 1, DIAGRAM_TARGET_BORDER);
        } else if (y1 == y2) {
            graphics.fill(Math.min(x1, x2), y1 - 1, Math.max(x1, x2) + 1, y1 + 2, DIAGRAM_TARGET_BORDER);
        }
    }

    private void renderDiagramTargetRect(GuiGraphics graphics, int localX, int localY, int width, int height) {
        graphics.renderOutline(
                viewportX + localX,
                viewportY + localY - scrollOffset,
                Math.max(1, width),
                Math.max(1, height),
                DIAGRAM_TARGET_BORDER);
    }

    private void renderDiagramConnectionSource(GuiGraphics graphics, LaidOutDiagram diagram, DiagramPortTarget source) {
        for (var node : diagram.nodes()) {
            if (renderDiagramConnectionSourcePort(graphics, node.ports(), source)) {
                return;
            }
        }
        for (var component : diagram.electricalComponents()) {
            if (renderDiagramConnectionSourcePort(graphics, component.ports(), source)) {
                return;
            }
        }
        for (var junction : diagram.electricalJunctions()) {
            if (renderDiagramConnectionSourcePort(graphics, junction.ports(), source)) {
                return;
            }
        }
    }

    private boolean renderDiagramConnectionSourcePort(
            GuiGraphics graphics,
            java.util.List<dev.rgcb.scholar.diagram.layout.LaidOutDiagramPort> ports,
            DiagramPortTarget source
    ) {
        for (var port : ports) {
            if (port.elementIndex() == source.elementIndex()
                    && port.elementId().equals(source.elementId())
                    && port.portIndex() == source.portIndex()
                    && port.portId().equals(source.portId())) {
                graphics.renderOutline(
                        viewportX + port.centerX() - 7,
                        viewportY + port.centerY() - scrollOffset - 7,
                        15,
                        15,
                        DIAGRAM_CONNECTION_SOURCE_BORDER);
                return true;
            }
        }
        return false;
    }

    private void renderPlotTarget(GuiGraphics graphics, LaidOutPlot plot, PlotEditTarget target) {
        if (target instanceof PlotPropertyTarget propertyTarget) {
            var label = switch (propertyTarget.property()) {
                case TITLE -> plot.title();
                case X_AXIS_LABEL -> plot.xAxisLabel();
                case Y_AXIS_LABEL -> plot.yAxisLabel();
            };
            if (label.isPresent()) {
                renderPlotTargetRect(graphics, label.orElseThrow().x() - 3, label.orElseThrow().y() - 2,
                        Math.max(8, label.orElseThrow().width() + 6), Math.max(10, label.orElseThrow().height() + 4));
            } else {
                var y = propertyTarget.property() == PlotProperty.TITLE ? plot.y() + 2 : plot.plotAreaY();
                renderPlotTargetRect(graphics, plot.x() + 2, y, Math.max(16, plot.width() - 4), 12);
            }
            return;
        }
        if (target instanceof PlotPointTarget pointTarget) {
            for (var series : plot.series()) {
                if (series.seriesIndex() != pointTarget.seriesIndex()) {
                    continue;
                }
                for (var point : series.points()) {
                    if (point.sourcePointIndex() == pointTarget.pointIndex()) {
                        renderPlotTargetRect(graphics, point.x() - 5, point.y() - 5, 11, 11);
                        return;
                    }
                }
            }
            renderPlotTargetRect(graphics, plot.plotAreaX(), plot.plotAreaY(), plot.plotAreaWidth(), plot.plotAreaHeight());
            return;
        }
        if (target instanceof PlotSeriesTarget seriesTarget) {
            if (plot.legend().isPresent()) {
                for (var item : plot.legend().orElseThrow().items()) {
                    if (item.seriesIndex() == seriesTarget.seriesIndex()) {
                        var left = Math.min(item.sampleX1(), item.label().x()) - 3;
                        var right = Math.max(item.sampleX2(), item.label().x() + item.label().width()) + 3;
                        var top = Math.min(item.sampleY() - 5, item.label().y()) - 2;
                        var bottom = Math.max(item.sampleY() + 6, item.label().y() + item.label().height()) + 2;
                        renderPlotTargetRect(graphics, left, top, Math.max(8, right - left), Math.max(8, bottom - top));
                        return;
                    }
                }
            }
            renderPlotTargetRect(graphics, plot.plotAreaX(), plot.plotAreaY(), plot.plotAreaWidth(), plot.plotAreaHeight());
        }
    }

    private void renderPlotTargetRect(GuiGraphics graphics, int localX, int localY, int width, int height) {
        graphics.renderOutline(
                viewportX + localX,
                viewportY + localY - scrollOffset,
                Math.max(1, width),
                Math.max(1, height),
                PLOT_TARGET_BORDER);
    }

    private void keepCaretVisible() {
        if (laidOutDocument == null || textMeasurer == null) {
            return;
        }
        if (editorState().isEquationEditingSelection() && mathTextMeasurer != null) {
            var block = laidOutDocument.blocks().get(editorState().equationEditingSelection().blockIndex());
            var math = block.math().orElseThrow();
            var mathBaselineY = block.y() + math.root().ascent();
            var mathSelection = editorState().equationEditingSelection().selection();
            var focusPosition = mathSelection instanceof MathRangeSelection rangeSelection
                    ? rangeSelection.active()
                    : ((MathCaretSelection) mathSelection).caret();
            var caret = mathCaretGeometryResolver.resolve(focusPosition, math, mathTextMeasurer);
            var caretY = mathBaselineY + caret.y();
            if (caretY - scrollOffset < 0) {
                scrollOffset = clampScroll(caretY);
            } else if (caretY + caret.height() - scrollOffset > viewportHeight) {
                scrollOffset = clampScroll(caretY + caret.height() - viewportHeight);
            }
            return;
        }
        if (editorState().isTableEditingSelection()) {
            var tableSelection = editorState().tableEditingSelection();
            var cell = activeTableCell(tableSelection);
            if (cell == null) {
                return;
            }
            var caret = tableCaretGeometryResolver.resolve(cell, tableSelection.selection().activeOffset(), textMeasurer);
            if (caret.y() - scrollOffset < 0) {
                scrollOffset = clampScroll(caret.y());
            } else if (caret.y() + caret.height() - scrollOffset > viewportHeight) {
                scrollOffset = clampScroll(caret.y() + caret.height() - viewportHeight);
            }
            return;
        }
        if (editorState().isPlotEditingSelection()) {
            var block = laidOutDocument.blocks().get(editorState().plotEditingSelection().blockIndex());
            scrollOffset = dev.rgcb.scholar.editor.ScrollGeometry.reveal(scrollOffset, laidOutDocument.height(), viewportHeight, block.y(), block.height());
            return;
        }
        if (editorState().isDiagramEditingSelection()) {
            var block = laidOutDocument.blocks().get(editorState().diagramEditingSelection().blockIndex());
            scrollOffset = dev.rgcb.scholar.editor.ScrollGeometry.reveal(scrollOffset, laidOutDocument.height(), viewportHeight, block.y(), block.height());
            return;
        }
        if (editorState().isBlockSelection()) {
            var block = laidOutDocument.blocks().get(editorState().blockSelection().blockIndex());
            scrollOffset = dev.rgcb.scholar.editor.ScrollGeometry.reveal(scrollOffset, laidOutDocument.height(), viewportHeight, block.y(), block.height());
            return;
        }
        var caret = caretGeometryResolver.resolve(editorState().caret(), laidOutDocument, textMeasurer);
        if (caret.y() - scrollOffset < 0) {
            scrollOffset = clampScroll(caret.y());
        } else if (caret.y() + caret.height() - scrollOffset > viewportHeight) {
            scrollOffset = clampScroll(caret.y() + caret.height() - viewportHeight);
        }
    }

    private MathPosition hitMathPosition(int localX, int localY) {
        var equationSelection = editorState().equationEditingSelection();
        var equationBlock = laidOutDocument.blocks().get(equationSelection.blockIndex());
        if (equationBlock.math().isEmpty()) {
            return null;
        }
        var math = equationBlock.math().orElseThrow();
        var mathX = equationBlock.x() + Math.max(0, (equationBlock.width() - math.width()) / 2);
        var mathBaselineY = equationBlock.y() + math.root().ascent();
        var clampedX = Math.max(mathX, Math.min(localX, mathX + Math.max(1, math.width())));
        var top = mathBaselineY - math.root().ascent();
        var bottom = mathBaselineY + math.root().descent();
        var clampedY = Math.max(top, Math.min(localY, bottom));
        return mathHitTester.hit(math, clampedX - mathX, clampedY - mathBaselineY, mathTextMeasurer);
    }

    private DiagramTargetDocumentHit hitDiagramTarget(int localX, int localY) {
        for (var blockIndex = 0; blockIndex < laidOutDocument.blocks().size(); blockIndex++) {
            var block = laidOutDocument.blocks().get(blockIndex);
            if (block.diagram().isEmpty() || !containsBlock(block, localX, localY)) {
                continue;
            }
            var target = diagramHitTester.hit(block.diagram().orElseThrow(), localX, localY);
            if (target.isPresent()) {
                return new DiagramTargetDocumentHit(blockIndex, target.orElseThrow());
            }
            return null;
        }
        return null;
    }

    private LaidOutDiagram activeDiagramLayout() {
        if (!editorState().isDiagramEditingSelection()) {
            return null;
        }
        var blockIndex = editorState().diagramEditingSelection().blockIndex();
        if (blockIndex < 0 || blockIndex >= laidOutDocument.blocks().size()) {
            return null;
        }
        return laidOutDocument.blocks().get(blockIndex).diagram().orElse(null);
    }

    private DiagramViewport diagramViewportFor(int blockIndex, DiagramBlock diagram) {
        return diagramViewports.viewportFor(blockIndex, diagram);
    }

    private LaidOutDiagram diagramLayout(int blockIndex) {
        if (laidOutDocument == null || blockIndex < 0 || blockIndex >= laidOutDocument.blocks().size()) {
            return null;
        }
        return laidOutDocument.blocks().get(blockIndex).diagram().orElse(null);
    }

    private DiagramWorkspaceHit diagramWorkspaceAt(int localX, int localY) {
        if (laidOutDocument == null) {
            return null;
        }
        for (var blockIndex = 0; blockIndex < laidOutDocument.blocks().size(); blockIndex++) {
            var diagram = laidOutDocument.blocks().get(blockIndex).diagram().orElse(null);
            if (diagram != null && diagram.workspaceBounds().contains(localX, localY)) {
                return new DiagramWorkspaceHit(blockIndex, diagram);
            }
        }
        return null;
    }

    private void fitActiveDiagramViewport() {
        if (!editorState().isDiagramEditingSelection()) {
            return;
        }
        var blockIndex = editorState().diagramEditingSelection().blockIndex();
        var block = editorState().document().blocks().get(blockIndex);
        if (block instanceof DiagramBlock diagram) {
            diagramViewports.reset(blockIndex, diagram);
            relayout();
        }
    }

    private void zoomActiveDiagramViewport(double factor) {
        var diagram = activeDiagramLayout();
        if (diagram == null || !editorState().isDiagramEditingSelection()) {
            return;
        }
        var localX = diagram.workspaceX() + diagram.workspaceWidth() / 2;
        var localY = diagram.workspaceY() + diagram.workspaceHeight() / 2;
        zoomDiagramViewportAt(editorState().diagramEditingSelection().blockIndex(), diagram, localX, localY, factor);
    }

    private void zoomDiagramViewportAt(
            int blockIndex,
            LaidOutDiagram diagram,
            int localX,
            int localY,
            double factor
    ) {
        if (!Double.isFinite(factor) || factor <= 0.0) {
            return;
        }
        var oldViewport = diagram.viewport();
        var newZoom = DiagramViewport.clampZoom(oldViewport.zoom() * factor);
        if (Math.abs(newZoom - oldViewport.zoom()) < 1.0e-9) {
            return;
        }
        var logicalX = diagram.transform().unmapX(localX);
        var logicalY = diagram.transform().unmapY(localY);
        var fitScale = diagram.transform().scale() / oldViewport.zoom();
        var newScale = fitScale * newZoom;
        var workspaceCenterX = diagram.workspaceX() + diagram.workspaceWidth() / 2.0;
        var workspaceCenterY = diagram.workspaceY() + diagram.workspaceHeight() / 2.0;
        var centerX = logicalX - (localX - workspaceCenterX) / newScale;
        var centerY = logicalY - (localY - workspaceCenterY) / newScale;
        diagramViewports.put(blockIndex, new DiagramViewport(newZoom, centerX, centerY));
        relayout();
    }

    private PlotTargetDocumentHit hitPlotTarget(int localX, int localY) {
        for (var blockIndex = 0; blockIndex < laidOutDocument.blocks().size(); blockIndex++) {
            var block = laidOutDocument.blocks().get(blockIndex);
            var plot = block.plot().orElse(null);
            if (plot == null
                    || localX < plot.x()
                    || localX > plot.x() + Math.max(1, plot.width())
                    || localY < plot.y()
                    || localY > plot.y() + Math.max(1, plot.height())) {
                continue;
            }
            return new PlotTargetDocumentHit(blockIndex, plotHitTester.hit(plot, localX, localY));
        }
        return null;
    }

    private TableCellDocumentHit hitTableCell(int localX, int localY) {
        for (var blockIndex = 0; blockIndex < laidOutDocument.blocks().size(); blockIndex++) {
            var block = laidOutDocument.blocks().get(blockIndex);
            if (block.table().isEmpty() || !containsBlock(block, localX, localY)) {
                continue;
            }
            var hit = tableHitTester.hit(block.table().orElseThrow(), localX, localY, textMeasurer);
            if (hit.isPresent()) {
                return new TableCellDocumentHit(blockIndex, hit.orElseThrow());
            }
        }
        return null;
    }

    private dev.rgcb.scholar.editor.TableCellHit hitTableCellInActiveTable(int localX, int localY) {
        var tableSelection = editorState().tableEditingSelection();
        var cell = activeTableCell(tableSelection);
        if (cell == null) {
            return null;
        }
        var clampedX = Math.max(cell.x(), Math.min(localX, cell.x() + Math.max(1, cell.width())));
        var clampedY = Math.max(cell.y(), Math.min(localY, cell.y() + Math.max(1, cell.height())));
        return tableHitTester.hit(cell, clampedX, clampedY, textMeasurer).orElse(null);
    }

    private LaidOutTableCell activeTableCell(TableEditingSelection selection) {
        if (selection.blockIndex() < 0 || selection.blockIndex() >= laidOutDocument.blocks().size()) {
            return null;
        }
        var block = laidOutDocument.blocks().get(selection.blockIndex());
        if (block.table().isEmpty()) {
            return null;
        }
        var coordinate = selection.selection().cell();
        return block.table().orElseThrow().rows().get(coordinate.rowIndex()).cells().get(coordinate.columnIndex());
    }

    private int clampScroll(int value) {
        return dev.rgcb.scholar.editor.ScrollGeometry.clamp(value, laidOutDocument == null ? 0 : laidOutDocument.height(), logicalViewportHeight());
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        return mouseX >= viewportX && mouseX <= viewportX + viewportWidth
                && mouseY >= viewportY && mouseY <= viewportY + viewportHeight;
    }

    private boolean navigateFromTableOfContents(int localX, int localY) {
        if (laidOutDocument == null) {
            return false;
        }
        for (var block : laidOutDocument.blocks()) {
            if (block.tableOfContents().isEmpty() || !containsBlock(block, localX, localY)) {
                continue;
            }
            for (var entry : block.tableOfContents().orElseThrow().entries()) {
                if (entry.contains(localX, localY)) {
                    controller.navigateToHeadingId(entry.targetId());
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleOutline() {
        outlineOpen = !outlineOpen;
        outlineScroll = clampOutlineScroll(outlineScroll);
        if (menuBar != null) {
            menuBar.close();
        }
        if (toolbar != null) {
            toolbar.closePopup();
        }
    }

    private boolean outlinePanelClicked(double mouseX, double mouseY) {
        var rect = outlinePanelRect();
        var closeRect = new ShellRect(rect.right() - 22, rect.y() + 5, 16, 14);
        if (contains(closeRect, mouseX, mouseY)) {
            outlineOpen = false;
            return true;
        }
        var contentTop = rect.y() + 26;
        var rowHeight = 18;
        var y = contentTop - outlineScroll;
        for (var section : outlineSections()) {
            var row = new ShellRect(rect.x() + 6, y, rect.width() - 12, rowHeight);
            if (contains(row, mouseX, mouseY)) {
                section.id().ifPresent(controller::navigateToHeadingId);
                return true;
            }
            y += rowHeight;
        }
        return true;
    }

    private void renderOutlinePanel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!outlineOpen) {
            return;
        }
        var rect = outlinePanelRect();
        ScholarShellRenderer.drawRaisedPanel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), ScholarShellStyle.PANEL);
        graphics.drawString(font, "Outline", rect.x() + 8, rect.y() + 9, ScholarShellStyle.TEXT, false);
        var closeRect = new ShellRect(rect.right() - 22, rect.y() + 5, 16, 14);
        renderDialogButton(graphics, closeRect, "x", true, contains(closeRect, mouseX, mouseY));

        var contentTop = rect.y() + 26;
        graphics.fill(rect.x() + 4, contentTop - 2, rect.right() - 4, rect.bottom() - 4, ScholarShellStyle.PANEL_INSET);
        graphics.enableScissor(rect.x() + 4, contentTop, rect.right() - 4, rect.bottom() - 4);
        try {
            var rowHeight = 18;
            var y = contentTop - outlineScroll;
            for (var section : outlineSections()) {
                var row = new ShellRect(rect.x() + 6, y, rect.width() - 12, rowHeight);
                if (row.bottom() >= contentTop && row.y() <= rect.bottom() - 4) {
                    if (contains(row, mouseX, mouseY)) {
                        graphics.fill(row.x(), row.y(), row.right(), row.bottom(), ScholarShellStyle.HOVER);
                    }
                    var indent = Math.max(0, section.level() - 1) * 10;
                    graphics.drawString(
                            font,
                            clippedFromEnd(section.displayText(), row.width() - indent - 6),
                            row.x() + indent + 3,
                            row.y() + 5,
                            section.id().isPresent() ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED,
                            false);
                }
                y += rowHeight;
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private List<SectionEntry> outlineSections() {
        return structureResolver.resolve(editorState().document()).sections();
    }

    private ShellRect outlinePanelRect() {
        var panelWidth = Math.min(260, Math.max(180, width / 3));
        var panelHeight = Math.max(90, height - MenuBarWidget.HEIGHT - 18);
        return new ShellRect(8, MenuBarWidget.HEIGHT + 8, panelWidth, panelHeight);
    }

    private int clampOutlineScroll(int value) {
        var visibleHeight = Math.max(1, outlinePanelRect().height() - 32);
        var contentHeight = outlineSections().size() * 18;
        return Math.max(0, Math.min(value, Math.max(0, contentHeight - visibleHeight)));
    }

    private record TableCellDocumentHit(int blockIndex, dev.rgcb.scholar.editor.TableCellHit hit) {
    }

    private record PlotTargetDocumentHit(int blockIndex, PlotEditTarget target) {
    }

    private record DiagramTargetDocumentHit(int blockIndex, DiagramEditTarget target) {
    }

    private record DiagramWorkspaceHit(int blockIndex, LaidOutDiagram diagram) {
    }

    private static boolean containsBlock(dev.rgcb.scholar.layout.LaidOutBlock block, int x, int y) {
        return x >= block.x()
                && x <= block.x() + Math.max(1, block.width())
                && y >= block.y()
                && y <= block.y() + Math.max(1, block.height());
    }

    private int documentLocalX(double mouseX) {
        return (int) Math.round(documentViewTransform().logicalX(mouseX) - viewportX);
    }

    private int documentLocalY(double mouseY) {
        return (int) Math.round(documentViewTransform().logicalY(mouseY) - viewportY) + scrollOffset;
    }

    private DocumentViewTransform documentViewTransform() {
        return new DocumentViewTransform(viewportX, viewportY, zoom);
    }

    private void enableViewportScissor(GuiGraphics graphics) {
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
    }

    private void enableDocumentScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        var clip = documentViewTransform().clip(left, top, right, bottom);
        graphics.enableScissor(clip.left(), clip.top(), clip.right(), clip.bottom());
    }

    private int logicalViewportHeight() {
        return Math.max(1, (int) Math.ceil(viewportHeight / zoom));
    }

    private int logicalViewportWidth() {
        return Math.max(1, (int) Math.ceil(viewportWidth / zoom));
    }

    private List<EditorAction> combinedViewActions() {
        var result = new java.util.ArrayList<EditorAction>(viewActions);
        result.addAll(viewportActions);
        return List.copyOf(result);
    }

    private void setZoom(float value) {
        zoom = Math.max(0.4f, Math.min(2.0f, Math.round(value * 100.0f) / 100.0f));
        scrollOffset = clampScroll(scrollOffset);
    }

    private void fitPage() {
        if (laidOutDocument == null || laidOutDocument.pages().isEmpty()) return;
        var page = laidOutDocument.pages().getFirst();
        setZoom(Math.min((float) viewportWidth / page.width(), (float) viewportHeight / page.height()));
    }

    private void fitWidth() {
        if (laidOutDocument == null) return;
        setZoom((float) viewportWidth / laidOutDocument.width());
    }

    private List<ToolbarItem> toolbarItems() {
        if (editorState().isEquationEditingSelection()) {
            return List.of(
                    ToolbarItem.action(action(EditorActionId.UNDO)),
                    ToolbarItem.action(action(EditorActionId.REDO)),
                    ToolbarItem.separator(),
                    ToolbarItem.action(action(EditorActionId.MATH_INSERT_FRACTION)),
                    ToolbarItem.action(action(EditorActionId.MATH_INSERT_ROOT)),
                    ToolbarItem.group(),
                    ToolbarItem.action(action(EditorActionId.MATH_INSERT_SUPERSCRIPT)),
                    ToolbarItem.action(action(EditorActionId.MATH_INSERT_SUBSCRIPT)),
                    ToolbarItem.separator(),
                    ToolbarItem.semanticConvert());
        }
        return List.of(
                ToolbarItem.action(action(EditorActionId.UNDO)),
                ToolbarItem.action(action(EditorActionId.REDO)),
                ToolbarItem.separator(),
                ToolbarItem.blockStyle(),
                ToolbarItem.separator(),
                ToolbarItem.action(action(EditorActionId.BOLD)),
                ToolbarItem.action(action(EditorActionId.ITALIC)));
    }

    private void refreshContextualToolbar() {
        if (controller == null || application != null) {
            return;
        }
        var mathMode = editorState().isEquationEditingSelection();
        if (toolbar != null && equationToolbarActive == mathMode) {
            return;
        }
        toolbar = new ToolbarWidget(controller, toolbarItems(), blockStyleActions, groupActions, semanticConversionActions);
        toolbar.setBounds(shellLayout.toolbarBounds());
        equationToolbarActive = mathMode;
    }

    private List<MenuDefinition> menuDefinitions() {
        if (application != null) {
            return ScholarShellModel.production(fileActions, editActions, formatActions, insertActions,
                    dataActions, tableActions, plotActions, figureActions, diagramActions, viewActions);
        }
        return List.of(
                new MenuDefinition("File", fileActions.stream().map(MenuEntry::action).toList()),
                new MenuDefinition("Edit", List.of(
                        MenuEntry.action(action(EditorActionId.UNDO)),
                        MenuEntry.action(action(EditorActionId.REDO)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.CUT)),
                        MenuEntry.action(action(EditorActionId.COPY)),
                        MenuEntry.action(action(EditorActionId.PASTE)))),
                new MenuDefinition("Insert", List.of(
                        MenuEntry.action(action(EditorActionId.INSERT_EQUATION)),
                        MenuEntry.action(action(EditorActionId.INSERT_TABLE)),
                        MenuEntry.action(action(EditorActionId.INSERT_PLOT)),
                        MenuEntry.action(action(EditorActionId.INSERT_DIAGRAM)),
                        MenuEntry.action(action(EditorActionId.INSERT_CROSS_REFERENCE)),
                        MenuEntry.action(action(EditorActionId.INSERT_TABLE_OF_CONTENTS)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.MATH_INSERT_FRACTION)),
                        MenuEntry.action(action(EditorActionId.MATH_INSERT_ROOT)),
                        MenuEntry.action(action(EditorActionId.MATH_INSERT_PARENTHESES_GROUP)),
                        MenuEntry.action(action(EditorActionId.MATH_INSERT_BRACKETS_GROUP)),
                        MenuEntry.action(action(EditorActionId.MATH_INSERT_BRACES_GROUP)),
                        MenuEntry.action(action(EditorActionId.MATH_INSERT_SUPERSCRIPT)),
                        MenuEntry.action(action(EditorActionId.MATH_INSERT_SUBSCRIPT)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.MATH_CONVERT_NAMED_OPERATOR)),
                        MenuEntry.action(action(EditorActionId.MATH_CONVERT_TEXT)))),
                new MenuDefinition("Format", List.of(
                        MenuEntry.action(action(EditorActionId.PARAGRAPH)),
                        MenuEntry.action(action(EditorActionId.HEADING_1)),
                        MenuEntry.action(action(EditorActionId.HEADING_2)),
                        MenuEntry.action(action(EditorActionId.HEADING_3)),
                        MenuEntry.action(action(EditorActionId.HEADING_4)),
                        MenuEntry.action(action(EditorActionId.HEADING_5)),
                        MenuEntry.action(action(EditorActionId.HEADING_6)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.BOLD)),
                        MenuEntry.action(action(EditorActionId.ITALIC)))),
                new MenuDefinition("View", List.of(
                        MenuEntry.action(action(EditorActionId.TOGGLE_OUTLINE)))),
                new MenuDefinition("Data", List.of(
                        MenuEntry.action(action(EditorActionId.DATA_NEW_DATASET)),
                        MenuEntry.action(action(EditorActionId.DATA_INSERT_DATASET_TABLE)),
                        MenuEntry.action(action(EditorActionId.DATA_INSERT_ANALYSIS)),
                        MenuEntry.action(action(EditorActionId.DATA_EDIT_ANALYSIS)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DATA_BIND_PLOT_TO_DATASET)),
                        MenuEntry.action(action(EditorActionId.DATA_ADD_FIT_OVERLAY)))),
                new MenuDefinition("Table", List.of(
                        MenuEntry.action(action(EditorActionId.TABLE_INSERT_ROW_ABOVE)),
                        MenuEntry.action(action(EditorActionId.TABLE_INSERT_ROW_BELOW)),
                        MenuEntry.action(action(EditorActionId.TABLE_DELETE_ROW)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.TABLE_INSERT_COLUMN_LEFT)),
                        MenuEntry.action(action(EditorActionId.TABLE_INSERT_COLUMN_RIGHT)),
                        MenuEntry.action(action(EditorActionId.TABLE_DELETE_COLUMN)))),
                new MenuDefinition("Plot", List.of(
                        MenuEntry.action(action(EditorActionId.PLOT_TOGGLE_GRID)),
                        MenuEntry.action(action(EditorActionId.PLOT_TOGGLE_LEGEND)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.PLOT_ADD_LINE_SERIES)),
                        MenuEntry.action(action(EditorActionId.PLOT_ADD_SCATTER_SERIES)),
                        MenuEntry.action(action(EditorActionId.PLOT_DELETE_SERIES)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.PLOT_SET_SERIES_LINE)),
                        MenuEntry.action(action(EditorActionId.PLOT_SET_SERIES_SCATTER)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.PLOT_ADD_POINT)),
                        MenuEntry.action(action(EditorActionId.PLOT_DELETE_POINT)))),
                new MenuDefinition("Figure", List.of(
                        MenuEntry.action(action(EditorActionId.FIGURE_WRAP_PLOT)),
                        MenuEntry.action(action(EditorActionId.FIGURE_WRAP_DIAGRAM)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.FIGURE_EDIT_CAPTION)),
                        MenuEntry.action(action(EditorActionId.FIGURE_UNWRAP)))),
                new MenuDefinition("Diagram", List.of(
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_NODE)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_RESISTOR)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_CAPACITOR)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_DC_VOLTAGE_SOURCE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_GROUND)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_DIODE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_LED)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_SWITCH_SPST)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_JUNCTION)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_LINE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_CENTERLINE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_RECTANGLE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_CIRCLE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_ARC)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_ARROW)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_REFERENCE_POINT)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_PART_REFERENCE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_GENERATE_MECHANICAL_BOM)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_PART_LABEL)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_NOTE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_ANNOTATION_LEADER)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SHAFT)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_GEAR)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BEARING)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_SPRING)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_PISTON)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_SYMBOL_BOLT)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_HORIZONTAL)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_VERTICAL)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ALIGNED)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_RADIUS)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_DIAMETER)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_DIMENSION_ANGLE)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_HORIZONTAL)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ADD_MECHANICAL_CONSTRAINT_VERTICAL)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_COINCIDENT)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PARALLEL)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_PERPENDICULAR)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_START_MECHANICAL_CONSTRAINT_CONCENTRIC)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_FINISH_MECHANICAL_CONSTRAINT)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_CANCEL_MECHANICAL_CONSTRAINT)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_SCALE_SYMBOLS_DOWN)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_SCALE_SYMBOLS_UP)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_WORKSPACE_SHORTER)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_WORKSPACE_TALLER)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_WORKSPACE_RESET_HEIGHT)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ROTATE_CLOCKWISE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_ROTATE_COUNTERCLOCKWISE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_ELECTRICAL_COMPONENT)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_JUNCTION)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_CONSTRAINT)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_DIMENSION)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PART_REFERENCE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_ANNOTATION)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_SYMBOL)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_MECHANICAL_PRIMITIVE)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_NODE)),
                        MenuEntry.separator(),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_START_CONNECTION)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_FINISH_CONNECTION)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_CANCEL_CONNECTION)),
                        MenuEntry.action(action(EditorActionId.DIAGRAM_DELETE_CONNECTION)))));
    }

    private EditorAction action(EditorActionId actionId) {
        return allActions.stream()
                .filter(action -> action.id() == actionId)
                .findFirst()
                .orElseThrow();
    }

    private static EditorAction action(List<EditorAction> actions, EditorActionId actionId) {
        return actions.stream()
                .filter(action -> action.id() == actionId)
                .findFirst()
                .orElseThrow();
    }

    private void openSemanticTokenPopup(SemanticMathTokenKind kind) {
        var draft = controller.semanticTokenDraft(kind);
        if (draft.isEmpty()) {
            return;
        }
        semanticTokenKind = draft.orElseThrow().kind();
        semanticTokenContent = draft.orElseThrow().content();
        semanticTokenPopupOpen = true;
        semanticTokenTypeOpen = false;
        crossReferencePopupOpen = false;
        contextMenu = null;
        if (menuBar != null) {
            menuBar.close();
        }
        if (toolbar != null) {
            toolbar.closePopup();
        }
    }

    private void closeSemanticTokenPopup() {
        semanticTokenPopupOpen = false;
        semanticTokenTypeOpen = false;
    }

    private void openCrossReferencePopup() {
        crossReferenceTargets = controller.availableCrossReferenceTargets();
        if (crossReferenceTargets.isEmpty()) {
            return;
        }
        crossReferencePopupOpen = true;
        semanticTokenPopupOpen = false;
        semanticTokenTypeOpen = false;
        plotValuePopupOpen = false;
        diagramLabelPopupOpen = false;
        diagramCanvasPopupOpen = false;
        electricalComponentPopupOpen = false;
        contextMenu = null;
        if (menuBar != null) {
            menuBar.close();
        }
        if (toolbar != null) {
            toolbar.closePopup();
        }
    }

    private void closeCrossReferencePopup() {
        crossReferencePopupOpen = false;
        crossReferenceTargets = List.of();
    }

    private boolean crossReferencePopupClicked(double mouseX, double mouseY) {
        var rect = crossReferencePopupRect();
        var cancelRect = new ShellRect(rect.x() + rect.width() - 72, rect.y() + rect.height() - 27, 58, 18);
        if (contains(cancelRect, mouseX, mouseY)) {
            closeCrossReferencePopup();
            return true;
        }
        var listTop = rect.y() + 28;
        var visibleRows = Math.min(crossReferenceTargets.size(), crossReferencePopupVisibleRows());
        for (var i = 0; i < visibleRows; i++) {
            var row = new ShellRect(rect.x() + 8, listTop + i * 20, rect.width() - 16, 20);
            if (contains(row, mouseX, mouseY)) {
                var target = crossReferenceTargets.get(i);
                controller.insertCrossReference(target.kind(), target.targetId());
                closeCrossReferencePopup();
                return true;
            }
        }
        return true;
    }

    private void renderCrossReferencePopup(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!crossReferencePopupOpen) {
            return;
        }
        var rect = crossReferencePopupRect();
        graphics.fill(0, 0, width, height, 0x88000000);
        ScholarShellRenderer.drawRaisedPanel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), ScholarShellStyle.PANEL);
        graphics.fill(rect.x() + 4, rect.y() + 20, rect.right() - 4, rect.bottom() - 34, ScholarShellStyle.PANEL_INSET);
        graphics.drawString(font, "Insert Cross Reference", rect.x() + 9, rect.y() + 8, ScholarShellStyle.TEXT, false);
        var listTop = rect.y() + 28;
        var visibleRows = Math.min(crossReferenceTargets.size(), crossReferencePopupVisibleRows());
        for (var i = 0; i < visibleRows; i++) {
            var row = new ShellRect(rect.x() + 8, listTop + i * 20, rect.width() - 16, 20);
            if (contains(row, mouseX, mouseY)) {
                graphics.fill(row.x(), row.y(), row.right(), row.bottom(), ScholarShellStyle.HOVER);
            }
            var target = crossReferenceTargets.get(i);
            graphics.drawString(font, clippedFromEnd(target.pickerLabel(), row.width() - 10), row.x() + 5, row.y() + 6, ScholarShellStyle.TEXT, false);
        }
        var cancelRect = new ShellRect(rect.x() + rect.width() - 72, rect.y() + rect.height() - 27, 58, 18);
        renderDialogButton(graphics, cancelRect, "Cancel", true, contains(cancelRect, mouseX, mouseY));
    }

    private void applySemanticTokenPopup() {
        if (!controller.isValidSemanticTokenContent(semanticTokenKind, semanticTokenContent)) {
            return;
        }
        controller.applySemanticToken(semanticTokenKind, semanticTokenContent);
        closeSemanticTokenPopup();
    }

    private void removeLastSemanticTokenCharacter() {
        if (!semanticTokenContent.isEmpty()) {
            semanticTokenContent = semanticTokenContent.substring(0, semanticTokenContent.offsetByCodePoints(semanticTokenContent.length(), -1));
        }
    }

    private boolean semanticTokenPopupClicked(double mouseX, double mouseY) {
        var rect = semanticTokenPopupRect();
        var typeRect = new ShellRect(rect.x() + 96, rect.y() + 35, 116, 18);
        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        if (semanticTokenTypeOpen) {
            var named = new ShellRect(typeRect.x(), typeRect.bottom() + 2, typeRect.width(), 18);
            var text = new ShellRect(typeRect.x(), typeRect.bottom() + 20, typeRect.width(), 18);
            if (contains(named, mouseX, mouseY)) {
                semanticTokenKind = SemanticMathTokenKind.NAMED_OPERATOR;
                semanticTokenTypeOpen = false;
                return true;
            }
            if (contains(text, mouseX, mouseY)) {
                semanticTokenKind = SemanticMathTokenKind.MATH_TEXT;
                semanticTokenTypeOpen = false;
                return true;
            }
        }
        if (contains(typeRect, mouseX, mouseY)) {
            semanticTokenTypeOpen = !semanticTokenTypeOpen;
            return true;
        }
        if (contains(cancelRect, mouseX, mouseY)) {
            closeSemanticTokenPopup();
            return true;
        }
        if (contains(applyRect, mouseX, mouseY)) {
            applySemanticTokenPopup();
            return true;
        }
        return true;
    }

    private void renderSemanticTokenPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!semanticTokenPopupOpen) {
            return;
        }
        var rect = semanticTokenPopupRect();
        graphics.fill(0, 0, width, height, 0x88000000);
        ScholarShellRenderer.drawRaisedPanel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), ScholarShellStyle.PANEL);
        graphics.fill(rect.x() + 4, rect.y() + 20, rect.right() - 4, rect.bottom() - 4, ScholarShellStyle.PANEL_INSET);
        graphics.drawString(font, "Semantic Math Token", rect.x() + 9, rect.y() + 8, ScholarShellStyle.TEXT, false);
        graphics.drawString(font, "Type", rect.x() + 16, rect.y() + 40, ScholarShellStyle.TEXT, false);

        var typeRect = new ShellRect(rect.x() + 96, rect.y() + 35, 116, 18);
        ScholarShellRenderer.drawRaisedPanel(graphics, typeRect.x(), typeRect.y(), typeRect.width(), typeRect.height(), ScholarShellStyle.PANEL_RAISED);
        graphics.drawString(font, semanticTokenKind.displayName(), typeRect.x() + 6, typeRect.y() + 5, ScholarShellStyle.TEXT, false);
        renderPopupTriangle(graphics, typeRect.right() - 12, typeRect.y() + 7, ScholarShellStyle.TEXT);

        graphics.drawString(font, "Content", rect.x() + 16, rect.y() + 66, ScholarShellStyle.TEXT, false);
        var contentRect = new ShellRect(rect.x() + 16, rect.y() + 78, rect.width() - 32, 20);
        ScholarShellRenderer.drawInsetPanel(graphics, contentRect.x(), contentRect.y(), contentRect.width(), contentRect.height(), ScholarShellStyle.PANEL_RECESSED);
        var displayed = semanticTokenContent + (((System.currentTimeMillis() / 500) % 2 == 0) ? "_" : "");
        graphics.drawString(font, clipped(displayed, contentRect.width() - 10), contentRect.x() + 5, contentRect.y() + 6, ScholarShellStyle.TEXT, false);

        var valid = controller.isValidSemanticTokenContent(semanticTokenKind, semanticTokenContent);
        if (!valid) {
            graphics.drawString(font, validationMessage(), rect.x() + 16, rect.y() + 104, ScholarShellStyle.TEXT_DISABLED, false);
        }

        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        renderDialogButton(graphics, cancelRect, "Cancel", true, contains(cancelRect, mouseX, mouseY));
        renderDialogButton(graphics, applyRect, "Apply", valid, contains(applyRect, mouseX, mouseY));

        if (semanticTokenTypeOpen) {
            renderTypeChoice(graphics, typeRect, SemanticMathTokenKind.NAMED_OPERATOR, 0, mouseX, mouseY);
            renderTypeChoice(graphics, typeRect, SemanticMathTokenKind.MATH_TEXT, 1, mouseX, mouseY);
        }
    }

    private void openPlotValuePopup() {
        if (!editorState().isPlotEditingSelection()) {
            return;
        }
        plotPopupTarget = editorState().plotEditingSelection().target();
        if (plotPopupTarget instanceof PlotPointTarget) {
            var point = controller.plotPointValue();
            if (point.isEmpty()) {
                return;
            }
            plotValuePrimary = Double.toString(point.orElseThrow().x());
            plotValueSecondary = Double.toString(point.orElseThrow().y());
        } else {
            var text = controller.plotTextValue();
            if (text.isEmpty()) {
                return;
            }
            plotValuePrimary = text.orElseThrow();
            plotValueSecondary = "";
        }
        plotPointSecondField = false;
        plotValuePopupOpen = true;
        semanticTokenPopupOpen = false;
        semanticTokenTypeOpen = false;
        contextMenu = null;
        if (menuBar != null) {
            menuBar.close();
        }
        if (toolbar != null) {
            toolbar.closePopup();
        }
    }

    private void closePlotValuePopup() {
        plotValuePopupOpen = false;
        plotPointSecondField = false;
        plotPopupTarget = null;
    }

    private void applyPlotValuePopup() {
        if (!plotValuePopupOpen || plotPopupTarget == null || !isPlotPopupValid()) {
            return;
        }
        if (plotPopupTarget instanceof PlotPointTarget) {
            controller.applyPlotPoint(Double.parseDouble(plotValuePrimary), Double.parseDouble(plotValueSecondary));
        } else {
            controller.applyPlotText(plotValuePrimary);
        }
        closePlotValuePopup();
    }

    private void appendPlotPopupCharacter(char codePoint) {
        if (plotPopupTarget instanceof PlotPointTarget) {
            if (!(Character.isDigit(codePoint) || codePoint == '-' || codePoint == '+' || codePoint == '.' || codePoint == 'e' || codePoint == 'E')) {
                return;
            }
        }
        if (plotPointSecondField) {
            plotValueSecondary = plotValueSecondary + codePoint;
        } else {
            plotValuePrimary = plotValuePrimary + codePoint;
        }
    }

    private void removeLastPlotPopupCharacter() {
        if (plotPointSecondField) {
            plotValueSecondary = removeLastCodePoint(plotValueSecondary);
        } else {
            plotValuePrimary = removeLastCodePoint(plotValuePrimary);
        }
    }

    private static String removeLastCodePoint(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(value.length(), -1));
    }

    private boolean isPlotPopupValid() {
        if (!(plotPopupTarget instanceof PlotPointTarget)) {
            return true;
        }
        try {
            return Double.isFinite(Double.parseDouble(plotValuePrimary))
                    && Double.isFinite(Double.parseDouble(plotValueSecondary));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean plotValuePopupClicked(double mouseX, double mouseY) {
        var rect = plotValuePopupRect();
        var primaryRect = new ShellRect(rect.x() + 70, rect.y() + 39, rect.width() - 86, 20);
        var secondaryRect = new ShellRect(rect.x() + 70, rect.y() + 68, rect.width() - 86, 20);
        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        if (contains(primaryRect, mouseX, mouseY)) {
            plotPointSecondField = false;
            return true;
        }
        if (plotPopupTarget instanceof PlotPointTarget && contains(secondaryRect, mouseX, mouseY)) {
            plotPointSecondField = true;
            return true;
        }
        if (contains(cancelRect, mouseX, mouseY)) {
            closePlotValuePopup();
            return true;
        }
        if (contains(applyRect, mouseX, mouseY)) {
            applyPlotValuePopup();
            return true;
        }
        return true;
    }

    private void renderPlotValuePopup(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!plotValuePopupOpen || plotPopupTarget == null) {
            return;
        }
        var rect = plotValuePopupRect();
        graphics.fill(0, 0, width, height, 0x88000000);
        ScholarShellRenderer.drawRaisedPanel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), ScholarShellStyle.PANEL);
        graphics.fill(rect.x() + 4, rect.y() + 20, rect.right() - 4, rect.bottom() - 4, ScholarShellStyle.PANEL_INSET);
        graphics.drawString(font, plotPopupTitle(), rect.x() + 9, rect.y() + 8, ScholarShellStyle.TEXT, false);

        var point = plotPopupTarget instanceof PlotPointTarget;
        graphics.drawString(font, point ? "X" : "Value", rect.x() + 16, rect.y() + 45, ScholarShellStyle.TEXT, false);
        var primaryRect = new ShellRect(rect.x() + 70, rect.y() + 39, rect.width() - 86, 20);
        renderPlotPopupField(graphics, primaryRect, plotValuePrimary, !plotPointSecondField);

        if (point) {
            graphics.drawString(font, "Y", rect.x() + 16, rect.y() + 74, ScholarShellStyle.TEXT, false);
            var secondaryRect = new ShellRect(rect.x() + 70, rect.y() + 68, rect.width() - 86, 20);
            renderPlotPopupField(graphics, secondaryRect, plotValueSecondary, plotPointSecondField);
        }

        var valid = isPlotPopupValid();
        if (!valid) {
            graphics.drawString(font, "X and Y must be finite numbers.", rect.x() + 16, rect.bottom() - 47, ScholarShellStyle.TEXT_DISABLED, false);
        } else if (point) {
            graphics.drawString(font, "Tab switches X/Y.", rect.x() + 16, rect.bottom() - 47, ScholarShellStyle.TEXT_DISABLED, false);
        }

        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        renderDialogButton(graphics, cancelRect, "Cancel", true, contains(cancelRect, mouseX, mouseY));
        renderDialogButton(graphics, applyRect, "Apply", valid, contains(applyRect, mouseX, mouseY));
    }

    private void renderPlotPopupField(GuiGraphics graphics, ShellRect rect, String value, boolean active) {
        ScholarShellRenderer.drawInsetPanel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), ScholarShellStyle.PANEL_RECESSED);
        var displayed = value + (active && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        graphics.drawString(font, clipped(displayed, rect.width() - 10), rect.x() + 5, rect.y() + 6, ScholarShellStyle.TEXT, false);
        if (active) {
            graphics.renderOutline(rect.x(), rect.y(), rect.width(), rect.height(), PLOT_TARGET_BORDER);
        }
    }

    private String plotPopupTitle() {
        if (plotPopupTarget instanceof PlotPointTarget pointTarget) {
            return "Edit Plot Point " + (pointTarget.pointIndex() + 1);
        }
        if (plotPopupTarget instanceof PlotSeriesTarget) {
            return "Edit Series Name";
        }
        if (plotPopupTarget instanceof PlotPropertyTarget propertyTarget) {
            return switch (propertyTarget.property()) {
                case TITLE -> "Edit Plot Title";
                case X_AXIS_LABEL -> "Edit X Axis Label";
                case Y_AXIS_LABEL -> "Edit Y Axis Label";
            };
        }
        return "Edit Plot";
    }

    private ShellRect plotValuePopupRect() {
        var popupWidth = 310;
        var popupHeight = plotPopupTarget instanceof PlotPointTarget ? 148 : 118;
        return ModalGeometry.centered(width, height, popupWidth, popupHeight, MenuBarWidget.HEIGHT + 12);
    }

    private void openDiagramCanvasPopup() {
        if (!editorState().isDiagramEditingSelection()) {
            return;
        }
        var canvas = controller.diagramCanvas();
        if (canvas.isEmpty()) {
            return;
        }
        diagramCanvasWidthValue = formatDiagramDimension(canvas.orElseThrow().width());
        diagramCanvasHeightValue = formatDiagramDimension(canvas.orElseThrow().height());
        diagramCanvasSecondField = false;
        diagramCanvasValidation = "";
        diagramCanvasPopupOpen = true;
        diagramLabelPopupOpen = false;
        electricalComponentPopupOpen = false;
        plotValuePopupOpen = false;
        semanticTokenPopupOpen = false;
        semanticTokenTypeOpen = false;
        contextMenu = null;
        if (menuBar != null) {
            menuBar.close();
        }
        if (toolbar != null) {
            toolbar.closePopup();
        }
    }

    private void closeDiagramCanvasPopup() {
        diagramCanvasPopupOpen = false;
        diagramCanvasWidthValue = "";
        diagramCanvasHeightValue = "";
        diagramCanvasValidation = "";
        diagramCanvasSecondField = false;
    }

    private void applyDiagramCanvasPopup() {
        if (!diagramCanvasPopupOpen) {
            return;
        }
        try {
            var width = Double.parseDouble(diagramCanvasWidthValue);
            var height = Double.parseDouble(diagramCanvasHeightValue);
            if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0.0 || height <= 0.0) {
                diagramCanvasValidation = "Width and height must be positive.";
                return;
            }
            var old = controller.diagramCanvas().orElse(null);
            if (old != null && Math.abs(old.width() - width) < 1.0e-9 && Math.abs(old.height() - height) < 1.0e-9) {
                closeDiagramCanvasPopup();
                return;
            }
            if (!controller.resizeDiagramCanvas(width, height)) {
                diagramCanvasValidation = "Canvas is too small for an existing element.";
                return;
            }
            fitActiveDiagramViewport();
            closeDiagramCanvasPopup();
        } catch (IllegalArgumentException exception) {
            diagramCanvasValidation = "Enter valid positive numeric dimensions.";
        }
    }

    private boolean diagramCanvasPopupClicked(double mouseX, double mouseY) {
        var rect = diagramCanvasPopupRect();
        var widthField = new ShellRect(rect.x() + 88, rect.y() + 38, rect.width() - 104, 20);
        var heightField = new ShellRect(rect.x() + 88, rect.y() + 66, rect.width() - 104, 20);
        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        if (contains(widthField, mouseX, mouseY)) {
            diagramCanvasSecondField = false;
            return true;
        }
        if (contains(heightField, mouseX, mouseY)) {
            diagramCanvasSecondField = true;
            return true;
        }
        if (contains(cancelRect, mouseX, mouseY)) {
            closeDiagramCanvasPopup();
            return true;
        }
        if (contains(applyRect, mouseX, mouseY)) {
            applyDiagramCanvasPopup();
            return true;
        }
        return true;
    }

    private void renderDiagramCanvasPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!diagramCanvasPopupOpen) {
            return;
        }
        var rect = diagramCanvasPopupRect();
        graphics.fill(0, 0, width, height, 0x88000000);
        ScholarShellRenderer.drawRaisedPanel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), ScholarShellStyle.PANEL);
        graphics.fill(rect.x() + 4, rect.y() + 20, rect.right() - 4, rect.bottom() - 4, ScholarShellStyle.PANEL_INSET);
        graphics.drawString(font, "Resize Diagram Canvas", rect.x() + 9, rect.y() + 8, ScholarShellStyle.TEXT, false);

        var widthField = new ShellRect(rect.x() + 88, rect.y() + 38, rect.width() - 104, 20);
        var heightField = new ShellRect(rect.x() + 88, rect.y() + 66, rect.width() - 104, 20);
        graphics.drawString(font, "Width", rect.x() + 16, rect.y() + 44, ScholarShellStyle.TEXT, false);
        graphics.drawString(font, "Height", rect.x() + 16, rect.y() + 72, ScholarShellStyle.TEXT, false);
        ScholarShellRenderer.drawInsetPanel(graphics, widthField.x(), widthField.y(), widthField.width(), widthField.height(), ScholarShellStyle.PANEL_RECESSED);
        ScholarShellRenderer.drawInsetPanel(graphics, heightField.x(), heightField.y(), heightField.width(), heightField.height(), ScholarShellStyle.PANEL_RECESSED);
        var blink = (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "";
        var widthText = diagramCanvasWidthValue + (!diagramCanvasSecondField ? blink : "");
        var heightText = diagramCanvasHeightValue + (diagramCanvasSecondField ? blink : "");
        graphics.drawString(font, clipped(widthText, widthField.width() - 10), widthField.x() + 5, widthField.y() + 6, ScholarShellStyle.TEXT, false);
        graphics.drawString(font, clipped(heightText, heightField.width() - 10), heightField.x() + 5, heightField.y() + 6, ScholarShellStyle.TEXT, false);
        var active = diagramCanvasSecondField ? heightField : widthField;
        graphics.renderOutline(active.x(), active.y(), active.width(), active.height(), DIAGRAM_TARGET_BORDER);
        if (!diagramCanvasValidation.isEmpty()) {
            graphics.drawString(font, clipped(diagramCanvasValidation, rect.width() - 32), rect.x() + 16, rect.y() + 94, 0xFFE57373, false);
        } else {
            graphics.drawString(font, "Tab switches fields. Canvas resize is undoable.", rect.x() + 16, rect.y() + 94, ScholarShellStyle.TEXT_DISABLED, false);
        }

        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        renderDialogButton(graphics, cancelRect, "Cancel", true, contains(cancelRect, mouseX, mouseY));
        renderDialogButton(graphics, applyRect, "Apply", true, contains(applyRect, mouseX, mouseY));
    }

    private ShellRect diagramCanvasPopupRect() {
        var popupWidth = 330;
        var popupHeight = 150;
        return ModalGeometry.centered(width, height, popupWidth, popupHeight, MenuBarWidget.HEIGHT + 12);
    }

    private static String formatDiagramDimension(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private void openDiagramLabelPopup() {
        if (!editorState().isDiagramEditingSelection()) {
            return;
        }
        if (editorState().diagramEditingSelection().target() instanceof DiagramElementTarget
                && controller.electricalComponentDraft().isPresent()) {
            openElectricalComponentPopup();
            return;
        }
        var value = controller.diagramTextValue();
        if (value.isEmpty()) {
            return;
        }
        diagramLabelPopupTarget = editorState().diagramEditingSelection().target();
        diagramLabelValue = value.orElseThrow();
        diagramLabelPopupOpen = true;
        plotValuePopupOpen = false;
        semanticTokenPopupOpen = false;
        semanticTokenTypeOpen = false;
        contextMenu = null;
        if (menuBar != null) {
            menuBar.close();
        }
        if (toolbar != null) {
            toolbar.closePopup();
        }
    }

    private void closeDiagramLabelPopup() {
        diagramLabelPopupOpen = false;
        diagramLabelPopupTarget = null;
        diagramLabelValue = "";
    }

    private void applyDiagramLabelPopup() {
        if (!diagramLabelPopupOpen || diagramLabelPopupTarget == null) {
            return;
        }
        controller.applyDiagramText(diagramLabelValue);
        closeDiagramLabelPopup();
    }

    private boolean diagramLabelPopupClicked(double mouseX, double mouseY) {
        var rect = diagramLabelPopupRect();
        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        if (contains(cancelRect, mouseX, mouseY)) {
            closeDiagramLabelPopup();
            return true;
        }
        if (contains(applyRect, mouseX, mouseY)) {
            applyDiagramLabelPopup();
            return true;
        }
        return true;
    }

    private void renderDiagramLabelPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!diagramLabelPopupOpen || diagramLabelPopupTarget == null) {
            return;
        }
        var rect = diagramLabelPopupRect();
        graphics.fill(0, 0, width, height, 0x88000000);
        ScholarShellRenderer.drawRaisedPanel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), ScholarShellStyle.PANEL);
        graphics.fill(rect.x() + 4, rect.y() + 20, rect.right() - 4, rect.bottom() - 4, ScholarShellStyle.PANEL_INSET);
        graphics.drawString(font, diagramLabelPopupTitle(), rect.x() + 9, rect.y() + 8, ScholarShellStyle.TEXT, false);
        graphics.drawString(font, "Label", rect.x() + 16, rect.y() + 45, ScholarShellStyle.TEXT, false);
        var field = new ShellRect(rect.x() + 70, rect.y() + 39, rect.width() - 86, 20);
        ScholarShellRenderer.drawInsetPanel(graphics, field.x(), field.y(), field.width(), field.height(), ScholarShellStyle.PANEL_RECESSED);
        var displayed = diagramLabelValue + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        graphics.drawString(font, clipped(displayed, field.width() - 10), field.x() + 5, field.y() + 6, ScholarShellStyle.TEXT, false);
        graphics.renderOutline(field.x(), field.y(), field.width(), field.height(), DIAGRAM_TARGET_BORDER);

        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        renderDialogButton(graphics, cancelRect, "Cancel", true, contains(cancelRect, mouseX, mouseY));
        renderDialogButton(graphics, applyRect, "Apply", true, contains(applyRect, mouseX, mouseY));
    }

    private String diagramLabelPopupTitle() {
        if (diagramLabelPopupTarget instanceof DiagramElementTarget) {
            return "Edit Node Label";
        }
        if (diagramLabelPopupTarget instanceof DiagramPortTarget) {
            return "Edit Port Label";
        }
        if (diagramLabelPopupTarget instanceof DiagramConnectionTarget) {
            return "Edit Connection Label";
        }
        if (diagramLabelPopupTarget instanceof DiagramPropertyTarget propertyTarget
                && propertyTarget.property() == DiagramProperty.TITLE) {
            return "Edit Diagram Title";
        }
        return "Edit Diagram Label";
    }

    private ShellRect diagramLabelPopupRect() {
        var popupWidth = 310;
        var popupHeight = 118;
        return ModalGeometry.centered(width, height, popupWidth, popupHeight, MenuBarWidget.HEIGHT + 12);
    }

    private void openElectricalComponentPopup() {
        if (!editorState().isDiagramEditingSelection()
                || !(editorState().diagramEditingSelection().target() instanceof DiagramElementTarget target)) {
            return;
        }
        var draft = controller.electricalComponentDraft();
        if (draft.isEmpty()) {
            return;
        }
        electricalComponentPopupTarget = target;
        electricalReferenceValue = draft.orElseThrow().referenceDesignator();
        electricalComponentValue = draft.orElseThrow().valueLabel();
        electricalComponentSecondField = false;
        electricalComponentPopupOpen = true;
        diagramLabelPopupOpen = false;
        plotValuePopupOpen = false;
        semanticTokenPopupOpen = false;
        semanticTokenTypeOpen = false;
        contextMenu = null;
        if (menuBar != null) {
            menuBar.close();
        }
        if (toolbar != null) {
            toolbar.closePopup();
        }
    }

    private void closeElectricalComponentPopup() {
        electricalComponentPopupOpen = false;
        electricalComponentPopupTarget = null;
        electricalReferenceValue = "";
        electricalComponentValue = "";
        electricalComponentSecondField = false;
    }

    private void applyElectricalComponentPopup() {
        if (!electricalComponentPopupOpen || electricalComponentPopupTarget == null) {
            return;
        }
        controller.applyElectricalComponentAnnotations(electricalReferenceValue, electricalComponentValue);
        closeElectricalComponentPopup();
    }

    private boolean electricalComponentPopupClicked(double mouseX, double mouseY) {
        var rect = electricalComponentPopupRect();
        var referenceField = electricalReferenceFieldRect(rect);
        var valueField = electricalValueFieldRect(rect);
        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        if (contains(referenceField, mouseX, mouseY)) {
            electricalComponentSecondField = false;
            return true;
        }
        if (contains(valueField, mouseX, mouseY)) {
            electricalComponentSecondField = true;
            return true;
        }
        if (contains(cancelRect, mouseX, mouseY)) {
            closeElectricalComponentPopup();
            return true;
        }
        if (contains(applyRect, mouseX, mouseY)) {
            applyElectricalComponentPopup();
            return true;
        }
        return true;
    }

    private void renderElectricalComponentPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!electricalComponentPopupOpen || electricalComponentPopupTarget == null) {
            return;
        }
        var rect = electricalComponentPopupRect();
        graphics.fill(0, 0, width, height, 0x88000000);
        ScholarShellRenderer.drawRaisedPanel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), ScholarShellStyle.PANEL);
        graphics.fill(rect.x() + 4, rect.y() + 20, rect.right() - 4, rect.bottom() - 4, ScholarShellStyle.PANEL_INSET);
        graphics.drawString(font, "Edit Electrical Component", rect.x() + 9, rect.y() + 8, ScholarShellStyle.TEXT, false);

        var referenceField = electricalReferenceFieldRect(rect);
        var valueField = electricalValueFieldRect(rect);
        graphics.drawString(font, "Reference", rect.x() + 16, referenceField.y() + 6, ScholarShellStyle.TEXT, false);
        graphics.drawString(font, "Value", rect.x() + 16, valueField.y() + 6, ScholarShellStyle.TEXT, false);
        ScholarShellRenderer.drawInsetPanel(graphics, referenceField.x(), referenceField.y(), referenceField.width(), referenceField.height(), ScholarShellStyle.PANEL_RECESSED);
        ScholarShellRenderer.drawInsetPanel(graphics, valueField.x(), valueField.y(), valueField.width(), valueField.height(), ScholarShellStyle.PANEL_RECESSED);

        var blink = (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "";
        var referenceDisplayed = electricalReferenceValue + (!electricalComponentSecondField ? blink : "");
        var valueDisplayed = electricalComponentValue + (electricalComponentSecondField ? blink : "");
        graphics.drawString(font, clipped(referenceDisplayed, referenceField.width() - 10), referenceField.x() + 5, referenceField.y() + 6, ScholarShellStyle.TEXT, false);
        graphics.drawString(font, clipped(valueDisplayed, valueField.width() - 10), valueField.x() + 5, valueField.y() + 6, ScholarShellStyle.TEXT, false);
        var active = electricalComponentSecondField ? valueField : referenceField;
        graphics.renderOutline(active.x(), active.y(), active.width(), active.height(), DIAGRAM_TARGET_BORDER);

        var cancelRect = new ShellRect(rect.x() + rect.width() - 124, rect.y() + rect.height() - 27, 52, 18);
        var applyRect = new ShellRect(rect.x() + rect.width() - 66, rect.y() + rect.height() - 27, 52, 18);
        renderDialogButton(graphics, cancelRect, "Cancel", true, contains(cancelRect, mouseX, mouseY));
        renderDialogButton(graphics, applyRect, "Apply", true, contains(applyRect, mouseX, mouseY));
    }

    private ShellRect electricalComponentPopupRect() {
        var popupWidth = 330;
        var popupHeight = 150;
        return ModalGeometry.centered(width, height, popupWidth, popupHeight, MenuBarWidget.HEIGHT + 12);
    }

    private static ShellRect electricalReferenceFieldRect(ShellRect popup) {
        return new ShellRect(popup.x() + 92, popup.y() + 38, popup.width() - 108, 20);
    }

    private static ShellRect electricalValueFieldRect(ShellRect popup) {
        return new ShellRect(popup.x() + 92, popup.y() + 70, popup.width() - 108, 20);
    }

    private void renderTypeChoice(GuiGraphics graphics, ShellRect typeRect, SemanticMathTokenKind kind, int row, int mouseX, int mouseY) {
        var option = new ShellRect(typeRect.x(), typeRect.bottom() + 2 + row * 18, typeRect.width(), 18);
        ScholarShellRenderer.drawRaisedPanel(graphics, option.x(), option.y(), option.width(), option.height(), ScholarShellStyle.PANEL_RECESSED);
        if (contains(option, mouseX, mouseY)) {
            graphics.fill(option.x() + 2, option.y() + 1, option.right() - 2, option.bottom() - 1, ScholarShellStyle.HOVER);
        }
        graphics.drawString(font, kind.displayName(), option.x() + 6, option.y() + 5, ScholarShellStyle.TEXT, false);
    }

    private void renderDialogButton(GuiGraphics graphics, ShellRect rect, String label, boolean enabled, boolean hovered) {
        var fill = hovered && enabled ? ScholarShellStyle.PANEL_RAISED : ScholarShellStyle.PANEL;
        ScholarShellRenderer.drawRaisedPanel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), fill);
        graphics.drawString(font, label, rect.x() + Math.max(4, (rect.width() - font.width(label)) / 2), rect.y() + 5,
                enabled ? ScholarShellStyle.TEXT : ScholarShellStyle.TEXT_DISABLED, false);
    }

    private ShellRect semanticTokenPopupRect() {
        var popupWidth = 270;
        var popupHeight = 144;
        return ModalGeometry.centered(width, height, popupWidth, popupHeight, MenuBarWidget.HEIGHT + 12);
    }

    private ShellRect crossReferencePopupRect() {
        var popupWidth = 360;
        var popupHeight = 66 + crossReferencePopupVisibleRows() * 20;
        return ModalGeometry.centered(width, height, popupWidth, popupHeight, MenuBarWidget.HEIGHT + 12);
    }

    private int crossReferencePopupVisibleRows() {
        return Math.max(1, Math.min(crossReferenceTargets.size(), 8));
    }

    private String validationMessage() {
        if (semanticTokenContent.isBlank()) {
            return "Content must not be blank.";
        }
        return semanticTokenKind == SemanticMathTokenKind.MATH_TEXT
                ? "Math Text cannot start or end with whitespace."
                : "";
    }

    private String clipped(String value, int maxWidth) {
        var result = value;
        while (font.width(result) > maxWidth && result.length() > 1) {
            result = result.substring(1);
        }
        return result;
    }

    private String clippedFromEnd(String value, int maxWidth) {
        var result = value;
        while (font.width(result) > maxWidth && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean contains(ShellRect rect, double mouseX, double mouseY) {
        return mouseX >= rect.x() && mouseX < rect.right()
                && mouseY >= rect.y() && mouseY < rect.bottom();
    }

    private static void renderPopupTriangle(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 5, y + 1, color);
        graphics.fill(x + 1, y + 1, x + 4, y + 2, color);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, color);
    }
}
