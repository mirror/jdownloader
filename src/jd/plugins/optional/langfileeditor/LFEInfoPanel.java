package jd.plugins.optional.langfileeditor;

import jd.gui.swing.components.pieapi.ChartAPIEntity;
import jd.gui.swing.components.pieapi.PieChartAPI;
import jd.gui.swing.jdgui.views.info.InfoPanel;
import jd.utils.locale.JDL;

public class LFEInfoPanel extends InfoPanel {

    private static final long serialVersionUID = 1314727641431663267L;
    private static final String LOCALE_PREFIX = "plugins.optional.langfileeditor.";
    private static LFEInfoPanel INSTANCE = null;

    private PieChartAPI keyChart;
    private ChartAPIEntity entDone, entMissing, entOld;

    public static LFEInfoPanel getInstance() {
        if (INSTANCE == null) INSTANCE = new LFEInfoPanel();
        return INSTANCE;
    }

    private LFEInfoPanel() {
        super("gui.splash.languages");

        keyChart = new PieChartAPI(350, 50);
        keyChart.addEntity(entDone = new ChartAPIEntity(JDL.L(LOCALE_PREFIX + "keychart.done", "Done"), 0, LFEGui.COLOR_DONE));
        keyChart.addEntity(entMissing = new ChartAPIEntity(JDL.L(LOCALE_PREFIX + "keychart.missing", "Missing"), 0, LFEGui.COLOR_MISSING));
        keyChart.addEntity(entOld = new ChartAPIEntity(JDL.L(LOCALE_PREFIX + "keychart.old", "Old"), 0, LFEGui.COLOR_OLD));

        this.addComponent(keyChart, 0, 0);
    }

    public void updateInfo(int done, int missing, int old) {
        entDone.setData(done);
        entDone.setCaption(JDL.L(LOCALE_PREFIX + "keychart.done", "Done") + " [" + entDone.getData() + "]");
        entMissing.setData(missing);
        entMissing.setCaption(JDL.L(LOCALE_PREFIX + "keychart.missing", "Missing") + " [" + entMissing.getData() + "]");
        entOld.setData(old);
        entOld.setCaption(JDL.L(LOCALE_PREFIX + "keychart.old", "Old") + " [" + entOld.getData() + "]");
        keyChart.fetchImage();
    }

}
