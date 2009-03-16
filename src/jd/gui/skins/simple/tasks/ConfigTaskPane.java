package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSeparator;

import jd.config.ConfigEntry.PropertyType;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.ContentPanel;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class ConfigTaskPane extends TaskPanel implements ActionListener, ControlListener {

    private JButton general;
    private JButton download;
    private JButton gui;
    private JButton reconnect;
    private JButton captcha;
    private JButton host;
    private JButton decrypt;
    private JButton addons;
    private JButton eventmanager;
    private JButton sav;
    private boolean changes;
    private boolean restart;
    public static final int ACTION_GENERAL = 1;
    public static final int ACTION_DOWNLOAD = 2;
    public static final int ACTION_GUI = 3;
    public static final int ACTION_RECONNECT = 4;
    public static final int ACTION_CAPTCHA = 5;
    public static final int ACTION_HOST = 6;
    public static final int ACTION_DECRYPT = 7;
    public static final int ACTION_ADDONS = 8;
    public static final int ACTION_EVENTMANAGER = 9;
    public static final int ACTION_SAVE = 10;

    public ConfigTaskPane(String string, ImageIcon ii) {
        super(string, ii, "config");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill]"));
        JDUtilities.getController().addControlListener(this);
        initGUI();
    }

    private void initGUI() {
        this.general = addButton(this.createButton(JDLocale.L("gui.config.tabLables.general", "general"), JDTheme.II("gui.images.config.home", 16, 16)));
        this.download = addButton(this.createButton(JDLocale.L("gui.config.tabLables.download", "download"), JDTheme.II("gui.images.config.network_local", 16, 16)));
        this.gui = addButton(this.createButton(JDLocale.L("gui.config.tabLables.gui", "gui"), JDTheme.II("gui.images.config.gui", 16, 16)));
        this.reconnect = addButton(this.createButton(JDLocale.L("gui.config.tabLables.reconnect", "reconnect"), JDTheme.II("gui.images.config.reconnect", 16, 16)));
        this.captcha = addButton(this.createButton(JDLocale.L("gui.config.tabLables.jac", "jac"), JDTheme.II("gui.images.config.ocr", 16, 16)));
        this.host = addButton(this.createButton(JDLocale.L("gui.config.tabLables.hostPlugin", "hostPlugin"), JDTheme.II("gui.images.config.host", 16, 16)));
        this.decrypt = addButton(this.createButton(JDLocale.L("gui.config.tabLables.decryptPlugin", "decryptPlugin"), JDTheme.II("gui.images.config.decrypt", 16, 16)));
        this.addons = addButton(this.createButton(JDLocale.L("gui.config.tabLables.addons", "addons"), JDTheme.II("gui.images.config.packagemanager", 16, 16)));
        this.eventmanager = addButton(this.createButton(JDLocale.L("gui.config.tabLables.eventManager", "eventManager"), JDTheme.II("gui.images.config.eventmanager", 16, 16)));

       add(new JSeparator());

        this.sav = addButton(this.createButton(JDLocale.L("gui.task.config.save", "Save changes"), JDTheme.II("gui.images.save", 16, 16)));
     
    }

    private JButton addButton(JButton bt) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "alignx leading");
        return bt;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -7720749076951577192L;


    public void actionPerformed(ActionEvent e) {
     
        if (e.getSource() == general) {
            this.broadcastEvent(new ActionEvent(this, ACTION_GENERAL, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == download) {
            this.broadcastEvent(new ActionEvent(this, ACTION_DOWNLOAD, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == gui) {
            this.broadcastEvent(new ActionEvent(this, ACTION_GUI, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == reconnect) {
            this.broadcastEvent(new ActionEvent(this, ACTION_RECONNECT, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == captcha) {
            this.broadcastEvent(new ActionEvent(this, ACTION_CAPTCHA, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == host) {
            this.broadcastEvent(new ActionEvent(this, ACTION_HOST, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == decrypt) {
            this.broadcastEvent(new ActionEvent(this, ACTION_DECRYPT, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == addons) {
            this.broadcastEvent(new ActionEvent(this, ACTION_ADDONS, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == eventmanager) {
            this.broadcastEvent(new ActionEvent(this, ACTION_EVENTMANAGER, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == sav) {
            this.broadcastEvent(new ActionEvent(this, ACTION_SAVE, ((JButton) e.getSource()).getName()));
            return;
        }
       

    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED) {
            if (ContentPanel.PANEL.getRightPanel() instanceof ConfigPanel) {
                if (((ConfigPanel) ContentPanel.PANEL.getRightPanel()).hasChanges() != PropertyType.NONE) {
                    this.changes = true;
                    System.out.println("CHANGES !");
                    if (((ConfigPanel) ContentPanel.PANEL.getRightPanel()).hasChanges() == PropertyType.NEEDS_RESTART) {
                        System.out.println("RESTART !");
                        this.restart = true;
                    }
                }
                if(changes){
                    sav.setEnabled(true);
                }

            }
        }

    }

}
