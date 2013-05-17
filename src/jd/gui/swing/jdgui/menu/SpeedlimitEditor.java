package jd.gui.swing.jdgui.menu;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.SizeSpinner;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.locale._AWU;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class SpeedlimitEditor extends MenuEditor {

    /**
	 * 
	 */
    private static final long serialVersionUID = 5406904697287119514L;
    private JLabel            lbl;
    private SizeSpinner       spinner;

    public SpeedlimitEditor() {
        this(false);
    }

    public SpeedlimitEditor(boolean b) {
        super(b);
        setLayout(new MigLayout("ins 0", "6[grow,fill][][]", "[]"));

        setOpaque(false);

        lbl = getLbl(_GUI._.SpeedlimitEditor_SpeedlimitEditor_(), NewTheme.I().getIcon("speed", 18));
        spinner = new SizeSpinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT)) {
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
        add(new ExtCheckBox(org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED, lbl, spinner), "width 20!");
        add(spinner, "height " + Math.max(spinner.getEditor().getPreferredSize().height, 20) + "!,width " + getEditorWidth() + "!");

    }
}
