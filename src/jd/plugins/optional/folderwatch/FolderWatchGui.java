package jd.plugins.optional.folderwatch;

import javax.swing.JScrollPane;

import jd.config.SubConfiguration;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("unused")
public class FolderWatchGui extends SwitchPanel {

    private static final long serialVersionUID = -4451556977039313203L;

    private static final String JDL_PREFIX = "jd.plugins.optional.folderwatch.FolderWatchGui.";

    private final FolderWatchTable table;
    private SubConfiguration config;

    public FolderWatchGui(SubConfiguration config) {
        table = new FolderWatchTable();
        this.config = config;
        initActions();
        initGUI();
    }

    private void initGUI() {
        this.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[][grow,fill]"));
        this.add(new JScrollPane(table), "grow");
    }

    private void initActions() {
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
        table.getModel().refreshModel();
        table.getModel().fireTableDataChanged();
    }

}
