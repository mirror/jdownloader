package jd.gui.swing.jdgui.menu;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtSpinner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ParallelDownloadsPerHostEditor extends MenuEditor {

    /**
	 * 
	 */
    private static final long serialVersionUID = 8113594595061609525L;

    private ExtSpinner        spinner;

    private JLabel            lbl;

    public ParallelDownloadsPerHostEditor() {
        this(false);
    }

    public ParallelDownloadsPerHostEditor(boolean b) {
        super(b);
        setLayout(new MigLayout("ins 0", "6[grow,fill][][]", "[grow,fill]"));

        lbl = getLbl(_GUI._.ParalellDownloadsEditor_ParallelDownloadsPerHostEditor_(), NewTheme.I().getIcon("batch", 18));
        spinner = new ExtSpinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST));

        add(lbl);
        add(new ExtCheckBox(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED, lbl, spinner), "width 20!");
        add(spinner, "height " + Math.max(spinner.getEditor().getPreferredSize().height, 20) + "!,width " + getEditorWidth() + "!");
    }

}
