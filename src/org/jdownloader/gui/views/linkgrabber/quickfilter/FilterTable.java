package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.ListSelectionModel;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.exttable.AlternateHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.utils.ColorUtils;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;

public class FilterTable extends ExtTable<Filter> implements PackageControllerTableModelFilter<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long   serialVersionUID = -5917220196056769905L;
    protected ArrayList<Filter> filters          = new ArrayList<Filter>();
    protected volatile boolean  enabled          = false;
    protected static final long REFRESH_MIN      = 200l;
    protected static final long REFRESH_MAX      = 2000l;

    public FilterTable() {
        super(new FilterTableModel());

        this.setShowVerticalLines(false);
        this.setShowGrid(false);
        this.setShowHorizontalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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

        this.getExtTableModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<Filter>(f2, b2, null) {

            @Override
            public boolean accept(ExtColumn<Filter> column, Filter value, boolean selected, boolean focus, int row) {
                return selected;
            }

        });

        this.addRowHighlighter(new AlternateHighlighter(null, ColorUtils.getAlphaInstance(new JLabel().getForeground(), 6)));

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

    public boolean isFiltered(CrawledLink e) {
        if (enabled == false) return false;
        ArrayList<Filter> lfilters = filters;
        for (Filter filter : lfilters) {
            if (filter.isEnabled()) {
                if (filter.isFiltered(e)) {
                    filter.setMatchCounter(filter.getMatchCounter() + 1);
                }
                continue;
            }
            if (filter.isFiltered(e)) {
                filter.setMatchCounter(filter.getMatchCounter() + 1);
                return true;
            }
        }
        return false;
    }

    public boolean isFiltered(CrawledPackage v) {

        return false;
    }

    public void reset() {
        ArrayList<Filter> lfilters = filters;
        for (Filter filter : lfilters) {
            filter.setMatchCounter(0);
            filter.setCounter(0);
        }
    }

}
