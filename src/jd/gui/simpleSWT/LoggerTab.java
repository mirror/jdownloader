package jd.gui.simpleSWT;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Text;

public class LoggerTab {

    private static Text Logger;
    private static CTabItem tbLogger;

    public static CTabItem initLogger() {
        final CTabFolder folder = MainGui.getFolder();
        tbLogger = new CTabItem(folder, SWT.NONE);
        tbLogger.setText(JDSWTUtilities.getSWTResourceString("LoggerTab.name"));
        {
            Logger = new Text(folder, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            Logger.addKeyListener(GuiListeners.getKeyListener("mainGui"));
            tbLogger.setControl(Logger);
        }
        tbLogger.setImage(JDSWTUtilities.getImageSwt("log"));

        return tbLogger;

    }

}
