package jd.gui.swing.jdgui.settings.panels.hoster.columns;

import jd.HostPluginWrapper;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextTableColumn;
import jd.nutils.Formatter;
import jd.plugins.HosterInfo;

public class FreeIPBlockWaittimeColumn extends JDTextTableColumn {

    private static final long serialVersionUID = -6268922254927727963L;

    public FreeIPBlockWaittimeColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    protected String getStringValue(Object value) {
        HosterInfo hi = ((HostPluginWrapper) value).getPlugin().getHosterInfo();
        if (hi == null) return "~";
        return Formatter.formatSeconds(hi.getFreeIPBlockWaittime() / 1000);
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
