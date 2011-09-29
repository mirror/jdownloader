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

public class ChunksEditor extends MenuEditor implements ConfigEventListener, ChangeListener {

    private ExtSpinner      spinner;
    private GeneralSettings config;

    public ChunksEditor() {
        super();

        add(getLbl(_GUI._.ChunksEditor_ChunksEditor_(), NewTheme.I().getIcon("chunks", 18)));
        config = JsonConfig.create(GeneralSettings.class);
        spinner = new ExtSpinner(new SpinnerNumberModel(config.getMaxChunksPerFile(), 1, 20, 1));
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
                if ("MaxChunksPerFile".equalsIgnoreCase(key)) {
                    spinner.setValue(newValue);
                }
            }
        };

    }

    public void stateChanged(ChangeEvent e) {

        config.setMaxChunksPerFile(spinner.getIntValue());
    }

}
