package jd.plugins.optional.routerdbeditor;

import jd.gui.swing.components.table.JDTableModel;
import jd.utils.locale.JDL;

public class RouterTableModel extends JDTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 1411827173660950838L;
    private static final String JDL_PREFIX = "jd.plugins.optional.JDRouterEditor.";
    private RouterList router;
    
    public RouterTableModel(String configname, RouterList router) {
        super(configname);
        this.router = router;
    }

    @Override
    protected void initColumns() {
        this.addColumn(new NameColumn(JDL.L(JDL_PREFIX + "router", "Router Name"), this));
        
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            synchronized (RouterList.LOCK) {
                list.clear();
                if(router != null)
                    list.addAll(router.getRouter());
            }
        }
        
    }
   

}
