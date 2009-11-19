package jd.plugins.optional.routerdbeditor;

import jd.gui.swing.components.table.JDTable;

public class RouterTable extends JDTable {

    /**
     * 
     */
    private static final long serialVersionUID = 4748473166225850119L;

    public RouterTable(RouterList router) {
        super(new RouterTableModel("router", router));
    }

    @Override
    public RouterTableModel getModel() {
        return (RouterTableModel) super.getModel();
    }

}
