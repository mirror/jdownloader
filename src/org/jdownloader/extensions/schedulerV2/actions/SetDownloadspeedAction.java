package org.jdownloader.extensions.schedulerV2.actions;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.appwork.swing.MigPanel;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.extensions.schedulerV2.gui.SpeedSpinner;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("SET_DOWNLOADSPEED")
public class SetDownloadspeedAction extends AbstractScheduleAction<SetDownloadspeedActionConfig> {

    public SetDownloadspeedAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_setDownloadspeed();
    }

    @Override
    public void execute() {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(true);
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.setValue(getConfig().getDownloadspeed());
    }

    @Override
    public JPanel getConfigPanel() {
        MigPanel actionParameterPanelSpeed = new MigPanel("ins 0,wrap 2", "", "");

        actionParameterPanelSpeed.add(new JLabel(T._.addScheduleEntryDialog_speed() + ":"), "width 18%,growx");

        final SpeedSpinner downloadspeedSpinner = new SpeedSpinner(0l, 100 * 1024 * 1024 * 1024l, 1l);
        downloadspeedSpinner.setValue(getConfig().getDownloadspeed());
        downloadspeedSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Object value = downloadspeedSpinner.getValue();
                if (value instanceof Number) {
                    getConfig().setDownloadspeed(((Number) value).intValue());
                }
            }
        });
        actionParameterPanelSpeed.add(downloadspeedSpinner, "width 30%,growx");
        return actionParameterPanelSpeed;
    }

    @Override
    public String getReadableParameter() {
        return SizeFormatter.formatBytes(Long.valueOf(getConfig().getDownloadspeed())) + "/s";
    }
}
