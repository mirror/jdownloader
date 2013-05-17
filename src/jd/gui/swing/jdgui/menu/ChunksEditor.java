package jd.gui.swing.jdgui.menu;

import javax.swing.JLabel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtSpinner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class ChunksEditor extends MenuEditor {

    /**
	 * 
	 */
    private static final long serialVersionUID = 4058537656898670477L;
    private ExtSpinner        spinner;
    private GeneralSettings   config;
    private JLabel            lbl;

    public ChunksEditor() {
        this(false);
    }

    public ChunksEditor(boolean b) {
        super(b);

        add(lbl = getLbl(_GUI._.ChunksEditor_ChunksEditor_(), NewTheme.I().getIcon("chunks", 18)));
        config = JsonConfig.create(GeneralSettings.class);
        spinner = new ExtSpinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_CHUNKS_PER_FILE));

        // new SpinnerNumberModel(config.getMaxChunksPerFile(), 1, 20, 1)

        add(spinner, "height " + Math.max(spinner.getEditor().getPreferredSize().height, 20) + "!,width " + getEditorWidth() + "!");
    }

}
