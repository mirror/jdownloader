package jd.gui.simpleSWT;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class PluginActivitiesTab {
    private MainGui mainGui;
    public PluginActivitiesTab(MainGui mainGui) {
        this.mainGui = mainGui;
        initPluginActivities();
    }
    private void initPluginActivities() {
        CTabItem tbPluginActivities = new CTabItem(mainGui.folder, SWT.NONE);
        tbPluginActivities.setText(JDSWTUtilities.getSWTResourceString("PluginActivitiesTab.name"));
        tbPluginActivities.setImage(JDSWTUtilities.getImageSwt("plugins"));
        // Shell shell = folder.getShell();
        final Table tablePluginActivities = new Table(mainGui.folder, SWT.BORDER);
        tbPluginActivities.setControl(tablePluginActivities);
        tablePluginActivities.setHeaderVisible(true);
        tablePluginActivities.setLinesVisible(false);
        for (int i = 0; i < 3; i++) {
            new TableColumn(tablePluginActivities, SWT.NONE).setText(JDSWTUtilities.getSWTResourceString("PluginActivitiesTab.column" + i + ".name"));
        }
        mainGui.folder.addControlListener(mainGui.guiListeners.setFolderControlListener(tablePluginActivities));
    }

}
