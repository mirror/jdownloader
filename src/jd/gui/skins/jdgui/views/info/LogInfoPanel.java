package jd.gui.skins.jdgui.views.info;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import jd.gui.skins.simple.Factory;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class LogInfoPanel extends InfoPanel implements ActionListener {

    private static final long serialVersionUID = -1910950245889164423L;
    private static final String JDL_PREFIX = "jd.gui.skins.jdgui.views.info.LogInfoPanel.";

    public static final int ACTION_SAVE = 1;
    public static final int ACTION_UPLOAD = 2;

    private JButton btnSave;
    private JButton btnUpload;

    public LogInfoPanel() {
        super();
        this.setIcon(JDTheme.II("gui.images.taskpanes.log", 32, 32));

        btnSave = Factory.createButton(JDL.L(JDL_PREFIX + "save", "Save Log As"), JDTheme.II("gui.images.save", 16, 16), this);
        btnUpload = Factory.createButton(JDL.L(JDL_PREFIX + "upload", "Upload Log"), JDTheme.II("gui.images.upload", 16, 16), this);

        addComponent(btnSave, 0, 0);
        addComponent(btnUpload, 0, 1);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            this.broadcastEvent(new ActionEvent(this, ACTION_SAVE, e.getActionCommand()));
        } else if (e.getSource() == btnUpload) {
            this.broadcastEvent(new ActionEvent(this, ACTION_UPLOAD, e.getActionCommand()));
        }
    }
}
