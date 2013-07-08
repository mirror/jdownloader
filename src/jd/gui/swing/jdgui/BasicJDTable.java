package jd.gui.swing.jdgui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class BasicJDTable<T> extends ExtTable<T> implements GenericConfigEventListener<Integer> {

    private static final long serialVersionUID = -9181860215412270250L;
    protected int             mouseOverRow;

    public BasicJDTable(ExtTableModel<T> tableModel) {
        super(tableModel);
        this.setShowVerticalLines(true);
        this.setShowGrid(true);
        this.setShowHorizontalLines(true);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor();
        Color b2;
        Color f2;
        if (c >= 0) {
            b2 = new Color(c);
            f2 = new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderForegroundColor());
        } else {
            b2 = getForeground();
            f2 = getBackground();
        }
        this.setBackground(new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor()));

        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<T>(f2, b2, null) {

            @Override
            public boolean accept(ExtColumn<T> column, T value, boolean selected, boolean focus, int row) {
                return selected;
            }

        });

        if (CFG_GUI.TABLE_MOUSE_OVER_HIGHLIGHT_ENABLED.isEnabled()) {
            addMouseMotionListener(new MouseMotionListener() {

                @Override
                public void mouseMoved(MouseEvent e) {

                    int newRow = getRowIndexByPoint(e.getPoint());
                    int oldRow = -1;
                    if (newRow != mouseOverRow) {
                        oldRow = mouseOverRow;
                        mouseOverRow = newRow;

                        if (oldRow >= 0) repaintRow(oldRow);
                        if (mouseOverRow >= 0) repaintRow(mouseOverRow);
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

            // addRowHighlighter(new ExtOverlayRowHighlighter(null, Colors.getColor(f2, 5)) {
            //
            // @Override
            // public boolean doHighlight(final ExtTable<?> extTable, final int row) {
            //
            // return mouseOverRow == row;
            // }
            // });
            //

            this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<T>(null, new Color(0, 0, 0, 20), null) {
                public int getPriority() {
                    return Integer.MAX_VALUE;
                }

                @Override
                public boolean accept(ExtColumn<T> column, T value, boolean selected, boolean focus, int row) {

                    return mouseOverRow == row;
                }

            });
        }
        initRowHeight();
        // this.addRowHighlighter(new AlternateHighlighter(null, ColorUtils.getAlphaInstance(new JLabel().getForeground(), 6)));

        this.setIntercellSpacing(new Dimension(0, 0));
        if (CFG_GUI.TABLE_ALTERNATE_ROW_HIGHLIGHT_ENABLED.isEnabled()) {

            this.getModel().addExtComponentRowHighlighter(new AlternateHighlighter<T>(null, new Color(0, 0, 0, 6), null));
        }

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
