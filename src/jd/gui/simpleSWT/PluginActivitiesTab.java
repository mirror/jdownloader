package jd.gui.simpleSWT;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;


public class PluginActivitiesTab {

    public static CTabItem initPluginActivities() {
        final CTabFolder folder = MainGui.getFolder();
        CTabItem tbPluginActivities = new CTabItem(folder, SWT.NONE);
        tbPluginActivities.setText(JDSWTUtilities.getSWTResourceString("PluginActivitiesTab.name"));
        tbPluginActivities.setImage(JDSWTUtilities.getImageSwt("plugins"));
       // Shell shell = folder.getShell();
        final Table tablePluginActivities = new Table(folder, SWT.BORDER);
        tbPluginActivities.setControl(tablePluginActivities);
        tablePluginActivities.setHeaderVisible(true);
        tablePluginActivities.setLinesVisible(false);
        for (int i = 0; i < 3; i++) {
          new TableColumn(tablePluginActivities, SWT.NONE).setText(JDSWTUtilities.getSWTResourceString("PluginActivitiesTab.column"+i+".name"));
        }
        folder.addControlListener(GuiListeners.setFolderControlListener(tablePluginActivities));
        return tbPluginActivities;
    }
    
}
