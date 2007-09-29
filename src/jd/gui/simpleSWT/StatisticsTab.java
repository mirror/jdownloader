package jd.gui.simpleSWT;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;

public class StatisticsTab {

    private MainGui mainGui;
    public StatisticsTab(MainGui mainGui) {
        this.mainGui = mainGui;
        initStatistics();
    }
    public void initStatistics() {
        final CTabItem tbStatistics = new CTabItem(mainGui.folder, SWT.NONE);
        tbStatistics.setText(JDSWTUtilities.getSWTResourceString("StatisticsTab.name"));
        tbStatistics.setImage(JDSWTUtilities.getImageSwt("statistic"));

    }

}
