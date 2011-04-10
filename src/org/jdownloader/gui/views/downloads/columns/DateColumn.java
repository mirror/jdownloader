package org.jdownloader.gui.views.downloads.columns;


 import org.jdownloader.gui.translate.*;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import jd.plugins.PackageLinkNode;
import jd.utils.locale.JDL;

import org.appwork.utils.swing.table.ExtDefaultRowSorter;
import org.appwork.utils.swing.table.columns.ExtTextColumn;

public abstract class DateColumn extends ExtTextColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = -8841119846403017974L;
    SimpleDateFormat          dateFormat       = null;
    Date                      date             = null;
    private StringBuffer      sb               = null;

    public DateColumn(String id) {
        super(id);
        date = new Date();
        sb = new StringBuffer();
        try {
            dateFormat = new SimpleDateFormat(T._.org_jdownloader_gui_views_downloads_columns_DateColumn_dateFormat()) {

                /**
                 * 
                 */
                private static final long serialVersionUID = 1L;

                public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
                    sb.setLength(0);
                    return super.format(date, sb, pos);
                }
            };
        } catch (Exception e) {
            dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm") {

                /**
                 * 
                 */
                private static final long serialVersionUID = 1L;

                public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
                    sb.setLength(0);
                    return super.format(date, sb, pos);
                }
            };
        }

        this.setRowSorter(new ExtDefaultRowSorter<PackageLinkNode>() {
            @Override
            public int compare(PackageLinkNode o1, PackageLinkNode o2) {
                long l1 = getDate(o1);
                long l2 = getDate(o2);
                if (l1 == l2) return 0;
                if (this.isSortOrderToggle()) {
                    return l1 > l2 ? -1 : 1;
                } else {
                    return l1 < l2 ? -1 : 1;
                }
            }

        });
    }

    @Override
    protected String getStringValue(PackageLinkNode value) {
        long dateLong = getDate(value);
        if (dateLong <= 0) return "";
        date.setTime(dateLong);
        return dateFormat.format(date).toString();

    }

    public abstract long getDate(PackageLinkNode node);
}