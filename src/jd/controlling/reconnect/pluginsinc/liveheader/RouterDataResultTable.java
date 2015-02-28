package jd.controlling.reconnect.pluginsinc.liveheader;

import java.util.List;

import javax.swing.ListSelectionModel;

import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.gui.swing.jdgui.BasicJDTable;

public class RouterDataResultTable extends BasicJDTable<RouterData> {

    public RouterDataResultTable() {
        super(new RouterDataResultTableModel());
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public void update(List<RouterData> list) {
        ((RouterDataResultTableModel) getModel()).update(list);
    }

    @Override
    protected void onSelectionChanged() {
        super.onSelectionChanged();
    }

}
