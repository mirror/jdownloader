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
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class ConfigTaskPane extends TaskPanel implements ActionListener, ControlListener {

    private static final long serialVersionUID = -7720749076951577192L;

    private JButton general;
    private JButton download;
    private JButton gui;
    private JButton reconnect;
    private JButton captcha;
    private JButton host;
    private JButton addons;
    private JButton eventmanager;
    private JButton sav;
    private boolean changes;
    public static final int ACTION_GENERAL = 1;
    public static final int ACTION_DOWNLOAD = 2;
    public static final int ACTION_GUI = 3;
    public static final int ACTION_RECONNECT = 4;
    public static final int ACTION_CAPTCHA = 5;
    public static final int ACTION_HOST = 6;
    public static final int ACTION_ADDONS = 7;
    public static final int ACTION_EVENTMANAGER = 8;
    public static final int ACTION_SAVE = 9;

    public ConfigTaskPane(String string, ImageIcon ii) {
        super(string, ii, "config");
        JDUtilities.getController().addControlListener(this);
        initGUI();
    }

    private void initGUI() {
        this.general = (this.createButton(JDLocale.L("gui.config.tabLables.general", "general"), JDTheme.II("gui.images.config.home", 16, 16)));
        this.download = (this.createButton(JDLocale.L("gui.config.tabLables.download", "download"), JDTheme.II("gui.images.config.network_local", 16, 16)));
        this.gui = (this.createButton(JDLocale.L("gui.config.tabLables.gui", "gui"), JDTheme.II("gui.images.config.gui", 16, 16)));
        this.reconnect = (this.createButton(JDLocale.L("gui.config.tabLables.reconnect", "reconnect"), JDTheme.II("gui.images.config.reconnect", 16, 16)));
        this.captcha = (this.createButton(JDLocale.L("gui.config.tabLables.jac", "jac"), JDTheme.II("gui.images.config.ocr", 16, 16)));
        this.host = (this.createButton(JDLocale.L("gui.config.tabLables.hostPlugin", "hostPlugin"), JDTheme.II("gui.images.config.host", 16, 16)));
        this.addons = (this.createButton(JDLocale.L("gui.config.tabLables.addons", "addons"), JDTheme.II("gui.images.config.packagemanager", 16, 16)));
        this.eventmanager = (this.createButton(JDLocale.L("gui.config.tabLables.eventManager", "eventManager"), JDTheme.II("gui.images.config.eventmanager", 16, 16)));
        this.sav = (this.createButton(JDLocale.L("gui.task.config.save", "Save changes"), JDTheme.II("gui.images.save", 16, 16)));

        add(general, D1_BUTTON_ICON);
        add(download, D1_BUTTON_ICON);
        add(gui, D1_BUTTON_ICON);
        add(reconnect, D1_BUTTON_ICON);
        add(host, D1_BUTTON_ICON);
        add(captcha, D1_BUTTON_ICON);
        add(addons, D1_BUTTON_ICON);
        add(eventmanager, D1_BUTTON_ICON);

        general.setName(JDLocale.L("quickhelp.config.general","configuration general"));
        download.setName(JDLocale.L("quickhelp.config.download","configuration download"));
        gui.setName(JDLocale.L("quickhelp.config.gui","configuration gui"));
        reconnect.setName(JDLocale.L("quickhelp.config.reconnect","configuration reconnect"));
        host.setName(JDLocale.L("quickhelp.config.host","configuration host"));
        captcha.setName(JDLocale.L("quickhelp.config.captcha","configuration captcha"));
        addons.setName(JDLocale.L("quickhelp.config.addons","configuration addons"));
        eventmanager.setName(JDLocale.L("quickhelp.config.eventmanager","configuration eventmanager"));
        add(new JSeparator());

        add(sav, D1_BUTTON_ICON);
    }

    public void actionPerformed(ActionEvent e) {
        SimpleGUI.CURRENTGUI.setWaiting(true);
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
            ContentPanel contentPanel = SimpleGUI.CURRENTGUI.getContentPane();
            if (contentPanel != null && contentPanel.getRightPanel() instanceof ConfigPanel) {
                if (((ConfigPanel) contentPanel.getRightPanel()).hasChanges() != PropertyType.NONE) {
                    this.changes = true;
                    if (((ConfigPanel) contentPanel.getRightPanel()).hasChanges() == PropertyType.NEEDS_RESTART) {
                        System.out.println("RESTART !");
                    }
                }
                if (changes) {
                    sav.setEnabled(true);
                }
            }
        }
    }

}
