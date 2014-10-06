package org.jdownloader.extensions.schedulerV2.actions;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.appwork.swing.MigPanel;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("SET_CHUNKS")
public class SetChunksAction extends AbstractScheduleAction<SetChunksActionConfig> {

    public SetChunksAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_setChunks();
    }

    @Override
    public JPanel getConfigPanel() {
        MigPanel actionParameterPanelInt = new MigPanel("ins 0,wrap 2", "", "");
        actionParameterPanelInt.add(new JLabel(T._.addScheduleEntryDialog_number() + ":"), "growx, width 18%");
        final SpinnerNumberModel model;
        JSpinner intParameterSpinner = new JSpinner(model = new SpinnerNumberModel(getConfig().getChunks(), 0, 25, 1));
        intParameterSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {

                Object value = model.getValue();
                if (value instanceof Number) {
                    getConfig().setChunks(((Number) value).intValue());
                }
            }
        });
        actionParameterPanelInt.add(intParameterSpinner, "growx, width 30%");
        return actionParameterPanelInt;
    }

    @Override
    public void execute() {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_CHUNKS_PER_FILE.setValue(getConfig().getChunks());
    }

    @Override
    public String getReadableParameter() {
        return String.valueOf(getConfig().getChunks());
    }
}
