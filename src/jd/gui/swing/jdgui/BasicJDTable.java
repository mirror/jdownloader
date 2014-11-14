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

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.TableUI;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthGraphicsUtils;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
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

    private static final long serialVersionUID = -9181860215412270250L;
    protected int             mouseOverRow     = -1;
    private Color             sortNotifyColor;
    private final boolean     overwriteHorizontalLinesPossible;

    public BasicJDTable(ExtTableModel<T> tableModel) {
        super(tableModel);
        this.setShowVerticalLines(true);
        this.setShowGrid(true);
        this.setShowHorizontalLines(true);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sortNotifyColor = CFG_GUI.SORT_COLUMN_HIGHLIGHT_ENABLED.getValue() ? (LAFOptions.getInstance().getColorForTableSortedColumnView()) : null;
        Color c = (LAFOptions.getInstance().getColorForPanelHeaderBackground());

        this.setBackground((LAFOptions.getInstance().getColorForPanelBackground()));

        addSelectionHighlighter();

        if (CFG_GUI.TABLE_MOUSE_OVER_HIGHLIGHT_ENABLED.isEnabled()) {

            initMouseOverRowHighlighter();
        }
        initRowHeight();
        // this.addRowHighlighter(new AlternateHighlighter(null, ColorUtils.getAlphaInstance(new JLabel().getForeground(), 6)));

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
    }

    private boolean showHorizontalLineBelowLastEntry = true;

    public boolean isShowHorizontalLineBelowLastEntry() {
        return showHorizontalLineBelowLastEntry;
    }

    public void setShowHorizontalLineBelowLastEntry(boolean showHorizontalLineBelowLastEntry) {
        this.showHorizontalLineBelowLastEntry = showHorizontalLineBelowLastEntry;
    }

    public boolean isOriginalOrder() {
        return getModel().getSortColumn() == null;
    }

    @Override
    public void paintComponent(Graphics g) {
        if (overwriteHorizontalLinesPossible == false || isShowHorizontalLineBelowLastEntry()) {
            super.paintComponent(g);
        } else {
            boolean before = getShowHorizontalLines();
            try {
                if (!isShowHorizontalLineBelowLastEntry()) {
                    setShowHorizontalLines(false);
                }
                super.paintComponent(g);
            } finally {
                setShowHorizontalLines(before);
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

    protected void addSelectionHighlighter() {
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<T>((LAFOptions.getInstance().getColorForTableSelectedRowsForeground()), (LAFOptions.getInstance().getColorForTableSelectedRowsBackground()), null) {
            public int getPriority() {
                return Integer.MAX_VALUE;
            }

            @Override
            public boolean accept(ExtColumn<T> column, T value, boolean selected, boolean focus, int row) {
                return selected;
            }

        });
    }

    protected void initAlternateRowHighlighter() {
        if (CFG_GUI.TABLE_ALTERNATE_ROW_HIGHLIGHT_ENABLED.isEnabled()) {

            this.getModel().addExtComponentRowHighlighter(new AlternateHighlighter<T>((LAFOptions.getInstance().getColorForTableAlternateRowForeground()), (LAFOptions.getInstance().getColorForTableAlternateRowBackground()), null));
        }
    }

    protected void initMouseOverRowHighlighter() {
        addMouseMotionListener(new MouseMotionListener() {

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

            @Override
            public void mouseDragged(MouseEvent e) {
            }
        });
        addMouseListener(new MouseAdapter() {

            protected void repaintRow(int newRow) {
                Rectangle rect = getCellRect(newRow, 0, true);
                rect.width = getWidth();
                repaint(rect.x, rect.y, rect.width, rect.height);
            }

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
        });
        Color f = (LAFOptions.getInstance().getColorForTableMouseOverRowForeground());
        Color b = (LAFOptions.getInstance().getColorForTableMouseOverRowBackground());
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<T>(f, b, null) {
            public int getPriority() {
                return Integer.MAX_VALUE - 1;
            }

            @Override
            protected Color getBackground(Color current) {
                return super.getBackground(current);
            }

            @Override
            public boolean accept(ExtColumn<T> column, T value, boolean selected, boolean focus, int row) {
                return mouseOverRow == row;
            }

        });
    }

    protected JPopupMenu columnControlMenu(final ExtColumn<T> extColumn) {
        JPopupMenu popup = super.columnControlMenu(extColumn);
        // popup.add(new JSeparator());
        popup.add(new JMenuItem(new LockAllColumnsAction(this)));

        return popup;
    }

    public boolean osResizeableColumns() {

        return true;
    }

    /**
     * 
     */
    protected void initRowHeight() {
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

        Integer custom = CFG_GUI.CUSTOM_TABLE_ROW_HEIGHT.getValue();
        CFG_GUI.CUSTOM_TABLE_ROW_HEIGHT.getEventSender().addListener(this, true);
        if (custom != null && custom > 0) {
            this.setRowHeight(custom);
        } else {
            this.setRowHeight(prefHeight + 3);
        }
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

}
