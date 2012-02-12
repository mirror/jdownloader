package jd.gui.swing.jdgui.menu;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtSpinner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class ParalellDownloadsEditor extends MenuEditor {

    private ExtSpinner      spinner;
    private GeneralSettings config;

    public ParalellDownloadsEditor() {
        super();
        config = JsonConfig.create(GeneralSettings.class);
        add(getLbl(_GUI._.ParalellDownloadsEditor_ParalellDownloadsEditor_(), NewTheme.I().getIcon("paralell", 18)));
        spinner = new ExtSpinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.GENERAL.MAX_SIMULTANE_DOWNLOADS));
        add(spinner, "height 20!");
    }
}
