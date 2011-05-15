package jd.gui.swing.jdgui.views.settings.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.Dialog.FileChooserType;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.translate._JDT;

public class FolderChooser extends JPanel implements SettingsComponent, ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JTextField        txt;
    private JButton           btn;
    private String            id;

    public FolderChooser(String id) {
        super(new MigLayout("ins 0", "[grow,fill][]"));
        this.id = id;
        txt = new JTextField();
        btn = new JButton(_JDT._.basics_browser_folder());
        btn.addActionListener(this);

        add(txt);
        add(btn);
    }

    @Override
    public void setEnabled(boolean enabled) {
        txt.setEnabled(enabled);
        btn.setEnabled(enabled);
    }

    public String getConstraints() {
        return null;
    }

    public void setText(String text) {
        txt.setText(text);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            File[] ret = Dialog.getInstance().showFileChooser(id, _JDT._.gui_setting_folderchooser_title(), FileChooserSelectionMode.DIRECTORIES_ONLY, null, false, FileChooserType.SAVE_DIALOG, null);

            txt.setText(ret[0].getAbsolutePath());

        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        }

    }

    public boolean isMultiline() {
        return false;
    }

}
