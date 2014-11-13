package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtSpinnerColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class TimingTableModel extends ExtTableModel<SolverService> {

    private SolverService mySolver;

    public TimingTableModel(SolverService solver) {
        super("TimingTableModel");
        mySolver = solver;
        update();
    }

    private HashMap<String, Integer>        waitTimesMap;
    private ExtSpinnerColumn<SolverService> timingColumn;

    private void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // make sure that this class is loaded. it contains the logic to restore old settings.
                List<SolverService> lst;
                ArrayList<SolverService> solverList = new ArrayList<SolverService>();

                HashSet<String> dupeMap = new HashSet<String>();
                dupeMap.add(mySolver.getID());
                waitTimesMap = mySolver.getWaitForMap();

                for (SolverService es : ChallengeResponseController.getInstance().listServices()) {
                    if (dupeMap.add(es.getID())) {
                        solverList.add(es);
                    }
                }

                _fireTableStructureChanged(solverList, true);
            }
        };

    }

    @Override
    protected void initColumns() {

        addColumn(new ExtTextColumn<SolverService>(_GUI._.SolverOrderTableModel_initColumns_service()) {

            @Override
            public boolean isSortable(final SolverService obj) {
                return false;
            }

            @Override
            protected Icon getIcon(SolverService value) {
                return value.getIcon(18);
            }

            @Override
            public int getDefaultWidth() {
                return 150;
            }

            @Override
            public boolean isEnabled(SolverService obj) {
                return true;
            }

            @Override
            public String getStringValue(SolverService value) {
                return value.getName();
            }
        });

        addColumn(new ExtTextColumn<SolverService>(_GUI._.SolverOrderTableModel_initColumns_type_()) {

            @Override
            public boolean isSortable(final SolverService obj) {
                return false;
            }

            @Override
            public boolean isEnabled(SolverService obj) {
                return true;
            }

            @Override
            public int getDefaultWidth() {
                return 300;
            }

            @Override
            public String getStringValue(SolverService value) {
                return value.getType();
            }
        });

        addColumn(timingColumn = new ExtSpinnerColumn<SolverService>(_GUI._.SolverOrderTableModel_initColumns_startafter()) {
            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isSortable(final SolverService obj) {
                return false;
            }

            @Override
            public boolean isEditable(SolverService obj) {
                return true;
            }

            @Override
            protected String getTooltipText(SolverService obj) {
                return null;
            }

            @Override
            protected boolean isEditable(SolverService obj, boolean enabled) {
                return true;
            }

            @Override
            public boolean isEnabled(SolverService obj) {
                return true;
            }

            @Override
            public int getDefaultWidth() {
                return 120;
            }

            @Override
            protected Number getNumber(SolverService value) {
                return getWaittimeBySolver(value);
            }

            @Override
            protected void setNumberValue(Number value, SolverService object) {
                waitTimesMap.put(object.getID(), value.intValue() * 1000);
                refreshSort();

            }

            @Override
            public String getStringValue(SolverService value) {
                return (getNumber(value).intValue()) + " seconds";
            }
        });
        this.sortColumn = timingColumn;

    }

    public int getWaittimeBySolver(SolverService value) {
        Integer v = waitTimesMap.get(value.getID());
        if (v == null || v.intValue() < 0) {
            v = 0;
        }
        return v.intValue() / 1000;
    }

    public AbstractAction getResetAction() {
        return new AppAction() {
            {
                setName(_GUI._.lit_reset());
                setIconKey(IconKey.ICON_RESET);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                mySolver.getConfig().setWaitForMap(null);
                update();
            }
        };
    }

}
