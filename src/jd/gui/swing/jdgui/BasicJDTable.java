package jd.gui.swing.jdgui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.exttable.AlternateHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.ColorUtils;

public class BasicJDTable<T> extends ExtTable<T> {

    private static final long serialVersionUID = -9181860215412270250L;

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

        this.setRowHeight(prefHeight + 3);

        this.addRowHighlighter(new AlternateHighlighter(null, ColorUtils.getAlphaInstance(new JLabel().getForeground(), 6)));
        this.setIntercellSpacing(new Dimension(0, 0));

    }

}
