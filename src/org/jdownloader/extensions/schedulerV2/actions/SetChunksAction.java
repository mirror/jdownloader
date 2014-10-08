package org.jdownloader.extensions.schedulerV2.actions;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
    protected void createPanel() {
        panel.put(new JLabel(T._.addScheduleEntryDialog_chunks() + ":"), "gapleft 10,");
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
        panel.put(intParameterSpinner, "");
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
