package jd.gui.skins.simple.components.treetable;

import java.util.List;

import javax.swing.Action;
import javax.swing.table.TableColumn;

import jd.config.SubConfiguration;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.ColumnControlButton;

public class JColumnControlButton extends ColumnControlButton {

    /**
     * 
     */
    private static final long serialVersionUID = 3234874733654121548L;
    private SubConfiguration config;
    private JXTable tab;

    public JColumnControlButton(JXTable table) {
        super(table);
this.tab=table;
    }

//    protected ColumnVisibilityAction createColumnVisibilityAction(TableColumn column) {
//        if (config == null) {
//            config = JDUtilities.getSubConfig("gui");
//        }
//        TableColumnExt col;
//        if (column instanceof TableColumnExt) {
//            col = ((TableColumnExt) column);
//            int id = col.getModelIndex();
//            col.setVisible(config.getBooleanProperty("VISABLE_COL_" + id, true));
//           
//        }
//
//        return new JColumnVisibilityAction(column);
//
//    }

    /**
     * 
     */
//    protected ColumnControlPopup createColumnControlPopup() {
//        return new DefaultColumnControlPopup(){
//            
//        };
//    }
    protected List<Action> getAdditionalActions() {
        List<Action> actions = super.getAdditionalActions();
        actions.remove(0);
        return actions;
    }
    public class JColumnVisibilityAction extends ColumnVisibilityAction {

   
        private int id;

        public JColumnVisibilityAction(TableColumn column) {
            super(column);

        }

        public void setActionCommand(Object key) {
      
            this.id = DownloadTreeTableModel.getIDFormHeaderLabel(key.toString());
            putValue(Action.ACTION_COMMAND_KEY, key);
        }

        public synchronized void setSelected(boolean newValue) {
            super.setSelected(newValue);
            if (config.getBooleanProperty("VISABLE_COL_" + id, true) != newValue) {
                config.setProperty("VISABLE_COL_" + id, newValue);
                config.save();
            }

        }

        /**
         * 
         */
        private static final long serialVersionUID = -3849908321036020341L;

    }
}
