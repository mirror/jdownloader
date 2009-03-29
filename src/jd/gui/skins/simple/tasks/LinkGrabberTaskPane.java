package jd.gui.skins.simple.tasks;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSeparator;

import jd.config.Configuration;
import jd.controlling.ClipboardHandler;
import jd.controlling.EventSystem.JDEvent;
import jd.controlling.EventSystem.JDListener;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.JDAction;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SingletonPanel;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2Event;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2TreeTableAction;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberTaskPane extends TaskPanel implements ActionListener, ControlListener, JDListener {

    private JButton panel_add_links;
    private JButton panel_add_containers;
    private JButton lg_add_all;
    private JButton lg_add_selected;
    private JButton lg_clear;
    private JButton panel_clipboard;
    private JSeparator sep = new JSeparator();
    private SimpleGUI simplegui;
    boolean lg_buttons_visible = false;

    public LinkGrabberTaskPane(String string, ImageIcon ii) {
        super(string, ii, "linkgrabber");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill]"));
        JDUtilities.getController().addControlListener(this);
        simplegui = SimpleGUI.CURRENTGUI;
        lg_buttons_visible = false;
        initGUI();
    }

    private void initGUI() {
        this.panel_add_links = addButton(this.createButton(JDLocale.L("gui.linkgrabberv2.addlinks", "Add Links"), JDTheme.II("gui.images.add", 16, 16)));
        this.panel_add_containers = addButton(this.createButton(JDLocale.L("gui.linkgrabberv2.addcontainers", "Add Containers"), JDTheme.II("gui.images.load", 16, 16)));
        add(sep, 2);
        lg_add_all = addButton(createButton(JDLocale.L("gui.linkgrabberv2.lg.addall", "Add all packages"), JDTheme.II("gui.images.add_all", 16, 16)), 3);
        lg_add_selected = addButton(createButton(JDLocale.L("gui.linkgrabberv2.lg.addselected", "Add selected package(s)"), JDTheme.II("gui.images.add_package", 16, 16)), 4);
        lg_clear = addButton(createButton(JDLocale.L("gui.linkgrabberv2.lg.clear", "Clear List"), JDTheme.II("gui.images.clear", 16, 16)), 5);
        lg_add_all.setEnabled(false);
        lg_add_selected.setEnabled(false);
        lg_clear.setEnabled(false);
        
        add(new JSeparator());
        this.panel_clipboard = addButton(this.createButton(JDLocale.L("gui.linkgrabberv2.clipboard", "Clipboard Watching"), JDTheme.II(getClipBoardImage(), 16, 16)));

    }

    private String getClipBoardImage() {
        if (ClipboardHandler.getClipboard().isEnabled()) {
            return JDTheme.V("gui.images.clipboardon");
        } else {
            return JDTheme.V("gui.images.clipboardoff");
        }
    }

    private JButton addButton(JButton bt) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "alignx leading");
        return bt;
    }

    private JButton addButton(JButton bt, int pos) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "alignx leading", pos);
        return bt;
    }

    private void removeButton(JButton bt) {
        if (bt == null) return;
        bt.removeActionListener(this);
        remove(bt);
    }

    /**
     * 
     */
    private static final long serialVersionUID = -7720749076951577192L;

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == panel_add_links) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.ITEMS_ADD, null));
            return;
        }
        if (e.getSource() == panel_add_containers) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_LOAD_DLC, null));
            return;
        }
        if (e.getSource() == panel_clipboard) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_CLIPBOARD, null));
            return;
        }
        if (e.getSource() == lg_add_all) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.ADD_ALL, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == lg_add_selected) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.ADD_SELECTED, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == lg_clear) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.CLEAR, ((JButton) e.getSource()).getName()));
            return;
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_JDPROPERTY_CHANGED && event.getParameter() instanceof String && ((String) event.getParameter()).equalsIgnoreCase(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE)) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    panel_clipboard.setIcon(JDTheme.II(getClipBoardImage(), 16, 16));
                }
            });
        }
    }
    public void recieveJDEvent(JDEvent event) {
        if (!(event instanceof LinkGrabberV2Event)) return;
        if (event.getID() == LinkGrabberV2Event.EMPTY_EVENT) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    lg_add_all.setEnabled(false);
                    lg_add_selected.setEnabled(false);
                    lg_clear.setEnabled(false);
                    revalidate();
                    lg_buttons_visible = false;
                }
            });
        }
        if (event.getID() == LinkGrabberV2Event.UPDATE_EVENT && lg_buttons_visible == false) {
            lg_buttons_visible = true;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    lg_add_all.setEnabled(true);
                    lg_add_selected.setEnabled(true);
                    lg_clear.setEnabled(true);
                  
                }
            });
        }

    }

 
}
