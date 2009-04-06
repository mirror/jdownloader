package jd.gui.skins.simple.tasks;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSeparator;

import jd.event.JDEvent;
import jd.event.JDListener;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2Event;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2TreeTableAction;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberTaskPane extends TaskPanel implements ActionListener, JDListener {

    private static final long serialVersionUID = -7720749076951577192L;
    private JButton panel_add_links;
    private JButton panel_add_containers;
    private JButton lg_add_all;
    private JButton lg_add_selected;
    private JButton lg_clear;

    private JSeparator sep = new JSeparator();
    boolean lg_buttons_visible = false;

    public LinkGrabberTaskPane(String string, ImageIcon ii) {
        super(string, ii, "linkgrabber");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill]"));
        lg_buttons_visible = false;
        initGUI();
    }

    private void initGUI() {
        this.panel_add_links = addButton(this.createButton(JDLocale.L("gui.linkgrabberv2.addlinks", "Add Links"), JDTheme.II("gui.images.add", 16, 16)));
        this.panel_add_containers = addButton(this.createButton(JDLocale.L("gui.linkgrabberv2.addcontainers", "Add Containers"), JDTheme.II("gui.images.load", 16, 16)));
        add(sep, 2);
        lg_add_all = addButton(createButton(JDLocale.L("gui.linkgrabberv2.lg.addall", "Add all packages"), JDTheme.II("gui.images.add_all", 16, 16)));
        lg_add_selected = addButton(createButton(JDLocale.L("gui.linkgrabberv2.lg.addselected", "Add selected package(s)"), JDTheme.II("gui.images.add_package", 16, 16)));
        lg_clear = addButton(createButton(JDLocale.L("gui.linkgrabberv2.lg.clear", "Clear List"), JDTheme.II("gui.images.clear", 16, 16)));
        lg_add_all.setEnabled(false);
        lg_add_selected.setEnabled(false);
        lg_clear.setEnabled(false);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == panel_add_links) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.GUI_ADD, null));
            return;
        }

        if (e.getSource() == panel_add_containers) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.GUI_LOAD, null));
            return;
        }

        if (e.getSource() == lg_add_all) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.ADD_ALL, null));
            return;
        }
        if (e.getSource() == lg_add_selected) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.ADD_SELECTED, null));
            return;
        }
        if (e.getSource() == lg_clear) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.CLEAR, null));
            return;
        }
    }

    public void receiveJDEvent(JDEvent event) {
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
                    revalidate();
                }
            });
        }

    }

}
