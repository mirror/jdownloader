package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.ListSelectionModel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.exttable.ExtTable;

public class FilterTable extends ExtTable<Filter> {

    public FilterTable() {
        super(new FilterTableModel());

        this.setShowVerticalLines(false);
        this.setShowGrid(false);
        this.setShowHorizontalLines(false);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setRowHeight(22);

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
        // this.addRowHighlighter(new SelectionHighlighter(null, b2));
        // this.getExtTableModel().addExtComponentRowHighlighter(new
        // ExtComponentRowHighlighter<T>(f2, b2, null) {
        //
        // @Override
        // public boolean accept(ExtColumn<T> column, T value, boolean selected,
        // boolean focus, int row) {
        // return selected;
        // }
        //
        // });

        // this.addRowHighlighter(new AlternateHighlighter(null,
        // ColorUtils.getAlphaInstance(new JLabel().getForeground(), 6)));
        this.setIntercellSpacing(new Dimension(0, 0));
    }

}
