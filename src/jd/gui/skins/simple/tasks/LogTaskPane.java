package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import jd.utils.JDLocale;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class LogTaskPane extends TaskPanel implements ActionListener {

    private static final long serialVersionUID = 1828963496367613790L;
    public static final int ACTION_SAVE = 1;
    public static final int ACTION_UPLOAD = 2;
    private JButton save;
    private JButton upload;

    public LogTaskPane(String string, ImageIcon ii) {
        super(string, ii, "logtask");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]", "[fill][grow]"));
        initGui();
    }

    private void initGui() {
        this.addButton(save = this.createButton(JDLocale.L("gui.taskpanels.log.save", "Save as"), JDTheme.II("gui.images.save", 16, 16)));
        this.addButton(upload = this.createButton(JDLocale.L("gui.taskpanels.log.upload", "Upload log"), JDTheme.II("gui.images.upload", 16, 16)));

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == save) {
            this.broadcastEvent(new ActionEvent(this, ACTION_SAVE, e.getActionCommand()));
        } else if (e.getSource() == upload) {
            this.broadcastEvent(new ActionEvent(this, ACTION_UPLOAD, e.getActionCommand()));
        }

    }

}
