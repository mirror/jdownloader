package jd.gui.swing.jdgui.menu;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.appwork.storage.config.swing.models.ConfigIntSpinnerModel;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtSpinner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import net.miginfocom.swing.MigLayout;

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
        setLayout(new MigLayout("ins " + getInsetsString(), "6[grow,fill][][]", "[" + getComponentHeight() + "!]"));

        lbl = getLbl(_GUI.T.ParalellDownloadsEditor_ParallelDownloadsPerHostEditor_(), new AbstractIcon(IconKey.ICON_BATCH, 18));
        spinner = new ExtSpinner(new ConfigIntSpinnerModel(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS_PER_HOST));
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
        add(lbl);
        ExtCheckBox checkbox;
        add(checkbox = new ExtCheckBox(org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_DOWNLOADS_PER_HOST_ENABLED, lbl, spinner), "width 20!");
        checkbox.setVerticalAlignment(SwingConstants.CENTER);

        add(spinner, "width " + getEditorWidth() + "!");
    }

}
