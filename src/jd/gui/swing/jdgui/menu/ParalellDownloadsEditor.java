package jd.gui.swing.jdgui.menu;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.appwork.swing.components.ExtSpinner;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class ParalellDownloadsEditor extends MenuEditor implements ChangeListener, ConfigEventListener {

    private ExtSpinner      spinner;
    private GeneralSettings config;

    public ParalellDownloadsEditor() {
        super();
        config = JsonConfig.create(GeneralSettings.class);
        add(getLbl(_GUI._.ParalellDownloadsEditor_ParalellDownloadsEditor_(), NewTheme.I().getIcon("paralell", 18)));
        spinner = new ExtSpinner(new SpinnerNumberModel(config.getMaxSimultaneDownloads(), 1, 20, 1));
        spinner.addChangeListener(this);
        config.getStorageHandler().getEventSender().addListener(this);
        add(spinner, "height 20!");
    }

    public void onConfigValidatorError(ConfigInterface config, Throwable validateException, KeyHandler methodHandler) {
    }

    public void onConfigValueModified(ConfigInterface config, final String key, final Object newValue) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if ("MaxSimultaneDownloads".equalsIgnoreCase(key)) {
                    spinner.setValue(newValue);
                }
            }
        };

    }

    public void stateChanged(ChangeEvent e) {
        System.out.println(e);
        config.setMaxSimultaneDownloads(spinner.getIntValue());
    }

}
