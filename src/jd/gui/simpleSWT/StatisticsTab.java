package jd.gui.simpleSWT;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;

public class StatisticsTab {
    
    private static CTabItem tbStatistics;

    public static CTabItem initStatistics() {
        final CTabFolder folder = MainGui.getFolder();
        tbStatistics = new CTabItem(folder, SWT.NONE);
        tbStatistics.setText(JDSWTUtilities.getSWTResourceString("StatisticsTab.name"));
        tbStatistics.setImage(JDSWTUtilities.getImageSwt("statistic"));

        return tbStatistics;

    }

}
