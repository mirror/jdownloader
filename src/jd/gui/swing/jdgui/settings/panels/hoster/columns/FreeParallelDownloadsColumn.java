package jd.gui.swing.jdgui.settings.panels.hoster.columns;

import jd.HostPluginWrapper;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextTableColumn;
import jd.plugins.HosterInfo;

public class FreeParallelDownloadsColumn extends JDTextTableColumn {

    private static final long serialVersionUID = -5007673302920966471L;

    public FreeParallelDownloadsColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    protected String getStringValue(Object value) {
        HosterInfo hi = ((HostPluginWrapper) value).getPlugin().getHosterInfo();
        if (hi == null) return "~";
        return String.valueOf(hi.getFreeParallelDownloads());
    }

    @Override
    public boolean defaultVisible() {
        return false;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

}
