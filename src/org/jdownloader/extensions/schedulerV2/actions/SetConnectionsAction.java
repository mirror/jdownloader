package org.jdownloader.extensions.schedulerV2.actions;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.appwork.swing.MigPanel;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("SET_CONNECTIONS")
public class SetConnectionsAction extends AbstractScheduleAction<SetConnectionsActionConfig> {

    public SetConnectionsAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_setConnections();
    }

    @Override
    public void execute() {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS.setValue(getConfig().getConnections());
    }

    public JPanel getConfigPanel() {
        MigPanel actionParameterPanelInt = new MigPanel("ins 0", "", "");
        actionParameterPanelInt.add(new JLabel(T._.addScheduleEntryDialog_number() + ":"), "growx,width 18%");
        final SpinnerNumberModel model;
        JSpinner intParameterSpinner = new JSpinner(model = new SpinnerNumberModel(0, 0, 25, 1));
        intParameterSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Object value = model.getValue();
                if (value instanceof Number) {
                    getConfig().setConnections(((Number) value).intValue());
                }
            }
        });
        actionParameterPanelInt.add(intParameterSpinner, "growx, width 30%");
        return actionParameterPanelInt;
    };

    @Override
    public String getReadableParameter() {
        return String.valueOf(getConfig().getConnections());
    }

}
