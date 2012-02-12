package jd.gui.swing.jdgui.menu;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.SizeSpinner;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class SpeedlimitEditor extends MenuEditor {

    private JLabel      lbl;
    private SizeSpinner spinner;

    public SpeedlimitEditor() {
        super();
        setLayout(new MigLayout("ins 0", "6[grow,fill][][]", "[grow,fill]"));

        setOpaque(false);

        lbl = getLbl(_GUI._.SpeedlimitEditor_SpeedlimitEditor_(), NewTheme.I().getIcon("speed", 18));
        spinner = new SizeSpinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.GENERAL.DOWNLOAD_SPEED_LIMIT)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            protected String longToText(long longValue) {

                return _GUI._.SpeedlimitEditor_format(SizeFormatter.formatBytes(longValue));
            }
        };

        add(lbl);
        add(new ExtCheckBox(org.jdownloader.settings.staticreferences.GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED, lbl, spinner), "width 20!");
        add(spinner, "height 20!,width 100!");

    }

}
