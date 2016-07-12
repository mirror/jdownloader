package jd.gui.swing.jdgui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.EventObject;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.TableUI;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthGraphicsUtils;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class BasicJDTable<T> extends ExtTable<T> implements GenericConfigEventListener<Integer> {

    private static final long serialVersionUID                 = -9181860215412270250L;
    protected int             mouseOverRow                     = -1;
    private Color             sortNotifyColor;
    private final boolean     overwriteHorizontalLinesPossible;

    private boolean           showHorizontalLineBelowLastEntry = true;

    private boolean           noRepaint                        = false;

    public BasicJDTable(ExtTableModel<T> tableModel) {
        super(tableModel);
        this.setShowVerticalLines(true);
        this.setShowGrid(true);
        this.setShowHorizontalLinesWithoutRepaint(true);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sortNotifyColor = CFG_GUI.SORT_COLUMN_HIGHLIGHT_ENABLED.isEnabled() ? (LAFOptions.getInstance().getColorForTableSortedColumnView()) : null;

        this.setBackground((LAFOptions.getInstance().getColorForPanelBackground()));

        addSelectionHighlighter();

        if (CFG_GUI.TABLE_MOUSE_OVER_HIGHLIGHT_ENABLED.isEnabled()) {

            initMouseOverRowHighlighter();
        }
        initRowHeight();

        this.setIntercellSpacing(new Dimension(0, 0));
        initAlternateRowHighlighter();
        if (Application.getJavaVersion() < Application.JAVA17) {
            overwriteHorizontalLinesPossible = false;
        } else {
            final TableUI lui = getUI();
            if (lui != null && lui instanceof javax.swing.plaf.synth.SynthTableUI) {
                overwriteHorizontalLinesPossible = true;
            } else {
                overwriteHorizontalLinesPossible = false;
            }
        }
        Color col = LAFOptions.getInstance().getColorForTableRowGap();
        if (col != null) {
            setGridColor(col);
        }

    }

    protected void addSelectionHighlighter() {
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<T>((LAFOptions.getInstance().getColorForTableSelectedRowsForeground()), (LAFOptions.getInstance().getColorForTableSelectedRowsBackground()), null) {
            @Override
            public boolean accept(ExtColumn<T> column, T value, boolean selected, boolean focus, int row) {
                return selected;
            }

            public int getPriority() {
                return Integer.MAX_VALUE;
            }

        });
    }

    //
    protected JPopupMenu columnControlMenu(final ExtColumn<T> extColumn) {
        JPopupMenu popup = super.columnControlMenu(extColumn);
        // popup.add(new JSeparator());
        if (getModel().getTable().isColumnLockingFeatureEnabled()) {
            popup.add(new JMenuItem(new LockAllColumnsAction(this)));
        }
        return popup;
    }

    protected void initAlternateRowHighlighter() {
        final BooleanKeyHandler enabled = LAFOptions.TABLE_ALTERNATE_ROW_HIGHLIGHT_ENABLED;
        if (enabled != null && enabled.isEnabled()) {
            this.getModel().addExtComponentRowHighlighter(new AlternateHighlighter<T>((LAFOptions.getInstance().getColorForTableAlternateRowForeground()), (LAFOptions.getInstance().getColorForTableAlternateRowBackground()), null));
        }
    }

    protected void initMouseOverRowHighlighter() {
        addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {

                int newRow = getRowIndexByPoint(e.getPoint());
                int oldRow = -1;
                if (newRow != mouseOverRow) {
                    oldRow = mouseOverRow;
                    mouseOverRow = newRow;

                    if (oldRow >= 0) {
                        repaintRow(oldRow);
                    }
                    if (mouseOverRow >= 0) {
                        repaintRow(mouseOverRow);
                    }
                }

            }

            protected void repaintRow(int newRow) {
                Rectangle rect = getCellRect(newRow, 0, true);
                rect.width = getWidth();
                repaint(rect.x, rect.y, rect.width, rect.height);
            }
        });
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseExited(MouseEvent e) {
                int newRow = -1;
                int oldRow = -1;

                oldRow = mouseOverRow;
                mouseOverRow = newRow;

                if (oldRow >= 0) {
                    repaintRow(oldRow);
                }

            }

            protected void repaintRow(int newRow) {
                Rectangle rect = getCellRect(newRow, 0, true);
                rect.width = getWidth();
                repaint(rect.x, rect.y, rect.width, rect.height);
            }
        });
        Color f = (LAFOptions.getInstance().getColorForTableMouseOverRowForeground());
        Color b = (LAFOptions.getInstance().getColorForTableMouseOverRowBackground());
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<T>(f, b, null) {
            @Override
            public boolean accept(ExtColumn<T> column, T value, boolean selected, boolean focus, int row) {
                return mouseOverRow == row || (getCellEditor() != null && editingRow == row);
            }

            @Override
            protected Color getBackground(Color current) {
                return super.getBackground(current);
            }

            public int getPriority() {
                return Integer.MAX_VALUE - 1;
            }

        });
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {

        return super.editCellAt(row, column, e);
    }

    /**
     *
     */
    protected void initRowHeight() {
        setRowHeight(calculateAutoRowHeight());
    }

    protected int calculateAutoRowHeight() {
        // Try to determine the correct auto row height.
        ExtTextColumn<String> col = new ExtTextColumn<String>("Test") {

            @Override
            public String getStringValue(String value) {
                return "Test";
            }
        };
        JComponent rend = col.getRendererComponent("Test", true, true, 1, 1);
        // use letters that are as height as possible
        col.configureRendererComponent("T§gj²*", true, true, 1, 1);
        int prefHeight = rend.getPreferredSize().height;

        final IntegerKeyHandler customRowHeight = LAFOptions.CUSTOM_TABLE_ROW_HEIGHT;
        Integer custom = null;
        if (customRowHeight != null) {
            custom = customRowHeight.getValue();
            customRowHeight.getEventSender().addListener(this, true);
        }
        if (custom != null && custom > 0) {
            return custom;
        } else {
            return prefHeight + 3;
        }

    }

    public boolean isOriginalOrder() {
        return getModel().getSortColumn() == null;
    }

    public boolean isResizeableColumns() {

        return true;
    }

    public boolean isShowHorizontalLineBelowLastEntry() {
        return showHorizontalLineBelowLastEntry;
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Integer> keyHandler, Integer invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Integer> keyHandler, Integer newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                initRowHeight();
                repaint();
            }
        };
    }

    @Override
    public boolean getShowHorizontalLines() {
        return true;
    }

    @Override
    public Color getGridColor() {
        return super.getGridColor();
    }

    @Override
    public void paintComponent(Graphics g) {

        if (overwriteHorizontalLinesPossible == false || isShowHorizontalLineBelowLastEntry()) {
            super.paintComponent(g);
        } else {
            boolean before = getShowHorizontalLines();
            try {
                if (!isShowHorizontalLineBelowLastEntry()) {
                    setShowHorizontalLinesWithoutRepaint(false);
                }
                super.paintComponent(g);
            } finally {
                setShowHorizontalLinesWithoutRepaint(before);
            }
            if (before && !isShowHorizontalLineBelowLastEntry()) {
                g.setColor(getGridColor());
                final TableUI lui = getUI();
                if (lui instanceof javax.swing.plaf.synth.SynthTableUI) {
                    SynthContext context = ((javax.swing.plaf.synth.SynthTableUI) lui).getContext(this);
                    int rMin = 0;
                    int cMin = 0;
                    Rectangle minCell = getCellRect(rMin, cMin, true);
                    int rMax = getRowCount() - 1;
                    int cMax = getColumnCount() - 1;
                    Rectangle maxCell = getCellRect(rMax, cMax, true);
                    Rectangle damagedArea = minCell.union(maxCell);
                    SynthGraphicsUtils synthG = context.getStyle().getGraphicsUtils(context);

                    int tableWidth = damagedArea.x + damagedArea.width;
                    int y = damagedArea.y;
                    for (int row = rMin; row <= rMax - 1; row++) {
                        y += getRowHeight(row);
                        synthG.drawLine(context, "Table.grid", g, damagedArea.x, y - 1, tableWidth - 1, y - 1);
                    }
                }
            }
        }
        if (getModel() instanceof TriStateSorterTableModel) {
            ExtColumn<T> sortColumn = getModel().getSortColumn();
            int filteredColumn = -1;
            if (sortNotifyColor != null && sortColumn != null) {
                filteredColumn = sortColumn.getIndex();
            }

            Graphics2D g2 = (Graphics2D) g;
            Composite comp = g2.getComposite();
            final Rectangle visibleRect = this.getVisibleRect();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            if (filteredColumn >= 0) {
                Rectangle first = this.getCellRect(0, filteredColumn, true);

                int w = getModel().getSortColumn().getWidth() - Math.max(0, visibleRect.x - first.x);
                if (w > 0) {
                    g2.setColor(sortNotifyColor);
                    g2.fillRect(Math.max(first.x, visibleRect.x), visibleRect.y, w, visibleRect.height);
                }
            }
            g2.setComposite(comp);
        }
    }

    @Override
    public void repaint() {
        if (noRepaint) {
            return;
        }
        super.repaint();
    }

    public void setShowHorizontalLineBelowLastEntry(boolean showHorizontalLineBelowLastEntry) {
        this.showHorizontalLineBelowLastEntry = showHorizontalLineBelowLastEntry;
    }

    private void setShowHorizontalLinesWithoutRepaint(boolean b) {
        noRepaint = true;
        try {
            setShowHorizontalLines(b);
        } finally {
            noRepaint = false;
        }
    }

}
