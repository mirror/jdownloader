package jd.gui.simpleSWT;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Text;

public class LoggerTab {

    private Text Logger;
    private CTabItem tbLogger;
    private MainGui mainGui;
    public LoggerTab(MainGui mainGui) {
        this.mainGui = mainGui;
        initLogger();
    }
    private void initLogger() {
        final CTabFolder folder = mainGui.folder;
        tbLogger = new CTabItem(folder, SWT.NONE);
        tbLogger.setText(JDSWTUtilities.getSWTResourceString("LoggerTab.name"));
        {
            Logger = new Text(folder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            Logger.addListener(SWT.KeyDown, mainGui.guiListeners.getListener("mainGuiKey"));
            tbLogger.setControl(Logger);
        }
        tbLogger.setImage(JDSWTUtilities.getImageSwt("log"));

    }

}
