package jd.gui.swing.jdgui.menu;

import javax.swing.JLabel;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.SizeSpinner;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class SpeedlimitEditor extends MenuEditor implements DownloadWatchdogListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 5406904697287119514L;
    private JLabel            lbl;
    private SizeSpinner       spinner;
    private ExtCheckBox       checkbox;

    public SpeedlimitEditor() {
        this(false);
    }

    public SpeedlimitEditor(boolean b) {
        super(b);
        setLayout(new MigLayout("ins 0", "6[grow,fill][][]", "[]"));

        setOpaque(false);

        lbl = getLbl(_GUI._.SpeedlimitEditor_SpeedlimitEditor_(), NewTheme.I().getIcon("speed", 18));
        spinner = new SizeSpinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT) {
            @Override
            public void setValue(Object value) {
                if (DownloadWatchDog.getInstance().isPaused()) {
                    try {
                        JsonConfig.create(GeneralSettings.class).setPauseSpeed(((Number) value).intValue());
                    } catch (ValidationException e) {
                        java.awt.Toolkit.getDefaultToolkit().beep();
                    }
                }
                super.setValue(value);
            }
        }) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected Object textToObject(String text) {
                if (text != null && text.trim().matches("^[0-9]+$")) { return super.textToObject(text + " kb/s"); }
                return super.textToObject(text);
            }

            protected String longToText(long longValue) {
                if (longValue <= 0) {
                    return _GUI._.SpeedlimitEditor_format(_AWU.T.literally_kibibyte("0"));
                } else {
                    return _GUI._.SpeedlimitEditor_format(SizeFormatter.formatBytes(longValue));
                }
            }
        };

        add(lbl);
        add(checkbox = new ExtCheckBox(org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED, lbl, spinner), "width 20!");
        DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
        add(spinner, "height " + Math.max(spinner.getEditor().getPreferredSize().height, 20) + "!,width " + getEditorWidth() + "!");
        DownloadWatchDog.getInstance().notifyCurrentState(this);

    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                checkbox.setEnabled(true);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                checkbox.setEnabled(false);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                checkbox.setEnabled(true);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                checkbox.setEnabled(true);
            }
        };
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                checkbox.setEnabled(true);
            }
        };
    }
}
