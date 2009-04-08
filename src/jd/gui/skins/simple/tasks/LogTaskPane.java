package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class LogTaskPane extends TaskPanel implements ActionListener {

    private static final long serialVersionUID = 1828963496367613790L;
    public static final int ACTION_SAVE = 1;
    public static final int ACTION_UPLOAD = 2;
    public static final int ACTION_LEVEL = 3;
    private JButton save;
    private JButton upload;
    private JComboBox box;

    public LogTaskPane(String string, ImageIcon ii) {
        super(string, ii, "logtask");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill,grow]", "[fill][grow]"));
        initGui();
    }

    private void initGui() {
        this.addButton(save = this.createButton(JDLocale.L("gui.taskpanels.log.save", "Save as"), JDTheme.II("gui.images.save", 16, 16)));
        this.addButton(upload = this.createButton(JDLocale.L("gui.taskpanels.log.upload", "Upload log"), JDTheme.II("gui.images.upload", 16, 16)));
        add(new JSeparator());
        add(new JLabel(JDLocale.L("gui.config.general.loggerLevel", "Level für's Logging")));
        add(box = new JComboBox(new Level[] { Level.ALL, Level.FINER, Level.INFO, Level.WARNING, Level.SEVERE }), "spanx,alignx leading,gapleft 13,gapright 5,gaptop 2");
        box.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == save) {
            this.broadcastEvent(new ActionEvent(this, ACTION_SAVE, e.getActionCommand()));
        } else if (e.getSource() == box) {
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_LOGGER_LEVEL, box.getSelectedItem());
            JDLogger.getLogger().setLevel((Level) box.getSelectedItem());
            this.broadcastEvent(new ActionEvent(this, ACTION_LEVEL, e.getActionCommand()));
        } else if (e.getSource() == upload) {
            this.broadcastEvent(new ActionEvent(this, ACTION_UPLOAD, e.getActionCommand()));
        }

    }

}
