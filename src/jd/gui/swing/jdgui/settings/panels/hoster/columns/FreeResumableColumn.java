package jd.gui.swing.jdgui.settings.panels.hoster.columns;

import jd.HostPluginWrapper;
import jd.gui.swing.components.table.JDCheckBoxTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.HosterInfo;

public class FreeResumableColumn extends JDCheckBoxTableColumn {

    private static final long serialVersionUID = -5268800711316993530L;

    public FreeResumableColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    protected boolean getBooleanValue(Object value) {
        HosterInfo hi = ((HostPluginWrapper) value).getPlugin().getHosterInfo();
        if (hi == null) return false;
        return hi.isFreeResumable();
    }

    @Override
    protected void setBooleanValue(boolean value, Object object) {
    }

    @Override
    public boolean defaultVisible() {
        return false;
    }

    @Override
    public boolean isEditable(Object obj) {
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
