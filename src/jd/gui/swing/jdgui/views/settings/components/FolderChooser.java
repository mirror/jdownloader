package jd.gui.swing.jdgui.views.settings.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.translate._JDT;

public class FolderChooser extends JPanel implements SettingsComponent, ActionListener {
    /**
     * 
     */
    private static final long                     serialVersionUID = 1L;
    private JTextField                            txt;
    private JButton                               btn;

    private StateUpdateEventSender<FolderChooser> eventSender;
    private boolean                               setting;

    public FolderChooser() {
        super(new MigLayout("ins 0", "[grow,fill][]", "[26!,fill]"));

        txt = new JTextField();
        btn = new JButton(_JDT._.basics_browser_folder());
        btn.addActionListener(this);
        add(txt);
        add(btn);
        eventSender = new StateUpdateEventSender<FolderChooser>();

        this.txt.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<FolderChooser>(FolderChooser.this));
            }

            public void insertUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<FolderChooser>(FolderChooser.this));
            }

            public void changedUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<FolderChooser>(FolderChooser.this));
            }
        });
    }

    public JButton getBtn() {
        return btn;
    }

    public JTextField getTxt() {
        return txt;
    }

    @Override
    public void setEnabled(boolean enabled) {
        txt.setEnabled(enabled);
        btn.setEnabled(enabled);
    }

    public String getConstraints() {
        return null;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        eventSender.addListener(listener);
    }

    public void setText(String t) {
        setting = true;
        try {
            txt.setText(t);
        } finally {
            setting = false;
        }
    }

    public void actionPerformed(ActionEvent e) {
        try {

            File ret = DownloadFolderChooserDialog.open(new File(txt.getText()), getPackageSubFolderEnabled(), _JDT._.gui_setting_folderchooser_title());
            txt.setText(ret.getAbsolutePath());

        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        }

    }

    protected boolean getPackageSubFolderEnabled() {
        return false;
    }

    public boolean isMultiline() {
        return false;
    }

    public String getText() {
        return txt.getText();
    }

}
