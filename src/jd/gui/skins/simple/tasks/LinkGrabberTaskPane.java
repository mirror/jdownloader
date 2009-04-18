package jd.gui.skins.simple.tasks;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.Timer;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberEvent;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackage;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberListener;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberPanel;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberTreeTableAction;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class LinkGrabberTaskPane extends TaskPanel implements ActionListener, LinkGrabberListener {

    private static final long serialVersionUID = -7720749076951577192L;
    private JButton panel_add_links;
    private JButton panel_add_containers;
    private JButton lg_add_all;
    private JButton lg_add_selected;
    private JButton lg_clear;

    private boolean linkgrabberButtonsEnabled = false;

    private JLabel linkgrabber;

    private JLabel downloadlinks;
    private JLabel packages;
    private JLabel totalsize;
    private Timer fadeTimer;

    public LinkGrabberTaskPane(String string, ImageIcon ii) {
        super(string, ii, "linkgrabber");

        linkgrabberButtonsEnabled = false;
        initGUI();
        fadeTimer = new Timer(2000, this);
        fadeTimer.setInitialDelay(0);
        fadeTimer.start();
    }

    private void initListStatGUI() {

        linkgrabber = (new JLabel(JDLocale.L("gui.taskpanes.download.linkgrabber", "Packagestats")));
        linkgrabber.setIcon(JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16));

        packages = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.packages", "%s Package(s)", 0)));
        downloadlinks = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.downloadLinks", "%s Link(s)", 0)));
        totalsize = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.size", "Total size: %s", 0)));
        add(linkgrabber, D1_LABEL_ICON);
        add(packages, D2_LABEL);
        add(downloadlinks, D2_LABEL);
        add(totalsize, D2_LABEL);
    }

    private void update() {/* TODO: soll man über events aktuallisiert werden */
        LinkGrabberPanel lg = LinkGrabberPanel.getLinkGrabber();
        packages.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.packages", "%s Packages", lg.getPackages().size()));
        long tot = 0;
        long links = 0;
        synchronized (lg.getPackages()) {
            for (LinkGrabberFilePackage fp : lg.getPackages()) {
                for (DownloadLink l : fp.getDownloadLinks()) {
                    tot += l.getDownloadSize();
                    links++;
                }
            }
        }
        downloadlinks.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Links", links));
        totalsize.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", JDUtilities.formatKbReadable(tot / 1024)));
    }

    private void initGUI() {

        this.panel_add_links = (this.createButton(JDLocale.L("gui.linkgrabberv2.addlinks", "Add Links"), JDTheme.II("gui.images.add", 16, 16)));
        this.panel_add_containers = (this.createButton(JDLocale.L("gui.linkgrabberv2.addcontainers", "Open Containers"), JDTheme.II("gui.images.load", 16, 16)));

        lg_add_all = (createButton(JDLocale.L("gui.linkgrabberv2.lg.addall", "Add all packages"), JDTheme.II("gui.images.add_all", 16, 16)));
        lg_add_selected = (createButton(JDLocale.L("gui.linkgrabberv2.lg.addselected", "Add selected package(s)"), JDTheme.II("gui.images.add_package", 16, 16)));
        lg_clear = (createButton(JDLocale.L("gui.linkgrabberv2.lg.clear", "Clear List"), JDTheme.II("gui.images.clear", 16, 16)));

        add(panel_add_links, D1_BUTTON_ICON);
        add(panel_add_containers, D1_BUTTON_ICON);
        add(new JSeparator());
        add(lg_add_all, D1_BUTTON_ICON);
        add(lg_add_selected, D1_BUTTON_ICON);
        add(lg_clear, D1_BUTTON_ICON);
        lg_add_all.setEnabled(false);
        lg_add_selected.setEnabled(false);
        lg_clear.setEnabled(false);

        add(new JSeparator());
        initListStatGUI();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == fadeTimer) {
            update();
            return;
        }
        if (e.getSource() == panel_add_links) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.GUI_ADD, null));
            return;
        }

        if (e.getSource() == panel_add_containers) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.GUI_LOAD, null));
            return;
        }

        if (e.getSource() == lg_add_all) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.ADD_ALL, null));
            return;
        }
        if (e.getSource() == lg_add_selected) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.ADD_SELECTED, null));
            return;
        }
        if (e.getSource() == lg_clear) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.CLEAR, null));
            return;
        }
    }

    public void onLinkgrabberEvent(LinkGrabberEvent event) {
        if (event.getID() == LinkGrabberEvent.EMPTY_EVENT) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    lg_add_all.setEnabled(false);
                    lg_add_selected.setEnabled(false);
                    lg_clear.setEnabled(false);
                    revalidate();
                    linkgrabberButtonsEnabled = false;
                }
            });
        }
        if (event.getID() == LinkGrabberEvent.UPDATE_EVENT && linkgrabberButtonsEnabled == false) {
            linkgrabberButtonsEnabled = true;
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

    public void setPanelID(int i) {
        SimpleGUI.CURRENTGUI.getContentPane().display(getPanel(i));
        switch (i) {
        case 0:
            lg_add_all.setEnabled(false);
            lg_add_selected.setEnabled(false);
            lg_clear.setEnabled(false);
            panel_add_links.setEnabled(false);

            linkgrabber.setEnabled(false);
            packages.setEnabled(false);
            downloadlinks.setEnabled(false);
            totalsize.setEnabled(false);
            break;
        case 1:
            linkgrabber.setEnabled(true);
            packages.setEnabled(true);
            downloadlinks.setEnabled(true);
            totalsize.setEnabled(true);
            panel_add_links.setEnabled(true);
            if (linkgrabberButtonsEnabled) {
                lg_add_all.setEnabled(true);
                lg_add_selected.setEnabled(true);
                lg_clear.setEnabled(true);
            }
            break;
        }

    }

}
