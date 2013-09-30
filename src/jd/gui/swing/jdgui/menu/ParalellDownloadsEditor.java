package jd.gui.swing.jdgui.menu;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JSpinner.DefaultEditor;
import javax.swing.SwingUtilities;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtSpinner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class ParalellDownloadsEditor extends MenuEditor {

    /**
	 * 
	 */
    private static final long serialVersionUID = -2226927990185644213L;
    private ExtSpinner        spinner;
    private GeneralSettings   config;

    public ParalellDownloadsEditor() {
        this(false);
    }

    public ParalellDownloadsEditor(boolean b) {
        super(b);
        config = JsonConfig.create(GeneralSettings.class);
        add(getLbl(_GUI._.ParalellDownloadsEditor_ParalellDownloadsEditor_(), NewTheme.I().getIcon("paralell", 18)));
        spinner = new ExtSpinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS));
        try {
            ((DefaultEditor) spinner.getEditor()).getTextField().addFocusListener(new FocusListener() {

                @Override
                public void focusLost(FocusEvent e) {
                }

                @Override
                public void focusGained(FocusEvent e) {
                    // requires invoke later!
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            ((DefaultEditor) spinner.getEditor()).getTextField().selectAll();
                        }
                    });

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            // too much fancy Casting.
        }
        add(spinner, "height " + Math.max(spinner.getEditor().getPreferredSize().height, 20) + "!,width " + getEditorWidth() + "!");
    }
}
