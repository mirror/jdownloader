package org.jdownloader.extensions.langfileeditor;

import jd.gui.swing.components.pieapi.ChartAPIEntity;
import jd.gui.swing.components.pieapi.PieChartAPI;
import jd.gui.swing.jdgui.views.InfoPanel;

import org.jdownloader.extensions.langfileeditor.translate.T;

public class LFEInfoPanel extends InfoPanel {

    private static final long   serialVersionUID = 1314727641431663267L;
    private static LFEInfoPanel INSTANCE         = null;

    private PieChartAPI         keyChart;
    private ChartAPIEntity      entDone, entMissing, entOld;

    public static LFEInfoPanel getInstance() {
        if (INSTANCE == null) INSTANCE = new LFEInfoPanel();
        return INSTANCE;
    }

    private LFEInfoPanel() {
        super("gui.splash.languages");

        keyChart = new PieChartAPI(350, 50);
        keyChart.addEntity(entDone = new ChartAPIEntity(T._.plugins_optional_langfileeditor_keychart_done(), 0, LFEGui.COLOR_DONE));
        keyChart.addEntity(entMissing = new ChartAPIEntity(T._.plugins_optional_langfileeditor_keychart_missing(), 0, LFEGui.COLOR_MISSING));
        keyChart.addEntity(entOld = new ChartAPIEntity(T._.plugins_optional_langfileeditor_keychart_old(), 0, LFEGui.COLOR_OLD));

        this.addComponent(keyChart, 0, 0);
    }

    public void updateInfo(int done, int missing, int old) {
        entDone.setData(done);
        entDone.setCaption(T._.plugins_optional_langfileeditor_keychart_done() + " [" + entDone.getData() + "]");
        entMissing.setData(missing);
        entMissing.setCaption(T._.plugins_optional_langfileeditor_keychart_missing() + " [" + entMissing.getData() + "]");
        entOld.setData(old);
        entOld.setCaption(T._.plugins_optional_langfileeditor_keychart_old() + " [" + entOld.getData() + "]");
        keyChart.fetchImage();
    }

}