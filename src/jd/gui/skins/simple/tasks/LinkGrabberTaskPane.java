//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.tasks;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackage;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberTreeTableAction;
import jd.nutils.Formatter;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class LinkGrabberTaskPane extends TaskPanel implements ActionListener, LinkGrabberControllerListener {

    private static final long serialVersionUID = -7720749076951577192L;
    private JButton panel_add_links;
    private JButton panel_add_containers;
    private JButton lg_add_all;
    private JButton lg_add_selected;
    private JButton lg_clear;

    private boolean linkgrabberButtonsEnabled = false;

    private JLabel linkgrabber;

    private JLabel downloadlinks;
    private JLabel filteredlinks;
    private JLabel packages;
    private JLabel totalsize;
    private Thread fadeTimer;
    private ArrayList<LinkGrabberFilePackage> fps;
    private LinkGrabberController lgi;
    private JCheckBox topOrBottom;
    private JCheckBox startAfterAdding;
    protected boolean updateinprogress = false;

    private long tot = 0;
    private long links = 0;

    public LinkGrabberTaskPane(String string, ImageIcon ii) {
        super(string, ii, "linkgrabber");
        lgi = LinkGrabberController.getInstance();
        fps = lgi.getPackages();
        lgi.addListener(this);
        linkgrabberButtonsEnabled = false;
        initGUI();
        fadeTimer = new Thread() {
            public void run() {
                while (true) {
                    if (!isCollapsed()) update();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };
        fadeTimer.start();
    }

    private void initListStatGUI() {

        linkgrabber = (new JLabel(JDLocale.L("gui.taskpanes.download.linkgrabber", "Packagestats")));
        linkgrabber.setIcon(JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16));

        packages = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.packages", "%s Package(s)", 0)));
        downloadlinks = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.downloadLinks", "%s Link(s)", 0)));
        filteredlinks = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.filteredLinks", "%s filtered Link(s)", 0)));
        totalsize = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.size", "Total size: %s", 0)));
        add(linkgrabber, D1_LABEL_ICON);
        add(packages, D2_LABEL);
        add(downloadlinks, D2_LABEL);
        add(filteredlinks, D2_LABEL);
        add(totalsize, D2_LABEL);
    }

    /**
     * TODO: soll man über events aktuallisiert werden
     */
    private void update() {
        tot = 0;
        links = 0;
        synchronized (fps) {
            for (LinkGrabberFilePackage fp : fps) {
                tot += fp.getDownloadSize(false);
                links += fp.getDownloadLinks().size();
            }
        }
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                packages.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.packages", "%s Packages", fps.size()));
                downloadlinks.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Links", links));
                filteredlinks.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.filteredLinks", "%s filtered Link(s)", lgi.getFILTERPACKAGE().size()));
                totalsize.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", Formatter.formatReadable(tot)));
                return null;
            }
        }.start();
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
        add(new JSeparator());
        initQuickConfig();
    }

    private void initQuickConfig() {
        JLabel config = (new JLabel(JDLocale.L("gui.taskpanes.download.linkgrabber.config", "Settings")));
        config.setIcon(JDTheme.II("gui.images.taskpanes.configuration", 16, 16));

        topOrBottom = new JCheckBox(JDLocale.L("gui.taskpanes.download.linkgrabber.config.addattop", "Add at top"));
        topOrBottom.setOpaque(false);
        topOrBottom.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SimpleGuiConstants.GUI_CONFIG.setProperty(SimpleGuiConstants.PARAM_INSERT_NEW_LINKS_AT, topOrBottom.isSelected());
                SimpleGuiConstants.GUI_CONFIG.save();
            }

        });
        if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
            topOrBottom.setSelected(true);
        }

        startAfterAdding = new JCheckBox(JDLocale.L("gui.taskpanes.download.linkgrabber.config.startofter", "Start after adding"));
        startAfterAdding.setOpaque(false);
        startAfterAdding.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SimpleGuiConstants.GUI_CONFIG.setProperty(SimpleGuiConstants.PARAM_START_AFTER_ADDING_LINKS, startAfterAdding.isSelected());
                SimpleGuiConstants.GUI_CONFIG.save();
            }

        });
        if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_START_AFTER_ADDING_LINKS, true)) {
            startAfterAdding.setSelected(true);
        }
        add(config, D1_LABEL_ICON);

        startAfterAdding.setToolTipText(JDLocale.L("gui.tooltips.linkgrabber.startlinksafteradd", "Is selected, download starts after adding new links"));
        add(startAfterAdding, TaskPanel.D2_CHECKBOX);
        startAfterAdding.setToolTipText(JDLocale.L("gui.tooltips.linkgrabber.topOrBottom", "if selected, new links will be added at top of your downloadlist"));

        add(topOrBottom, TaskPanel.D2_CHECKBOX);

    }

    public void actionPerformed(ActionEvent e) {
       
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
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.ADD_SELECTED_PACKAGES, null));
            return;
        }
        if (e.getSource() == lg_clear) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.CLEAR, null));
            return;
        }
    }

    public void setPanelID(final int i) {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                SimpleGUI.CURRENTGUI.getContentPane().display(getPanel(i));
                return null;
            }
        }.start();
        // switch (i) {
        // case 0:
        // lg_add_all.setEnabled(false);
        // lg_add_selected.setEnabled(false);
        // lg_clear.setEnabled(false);
        // panel_add_links.setEnabled(false);
        //
        // linkgrabber.setEnabled(false);
        // packages.setEnabled(false);
        // downloadlinks.setEnabled(false);
        // totalsize.setEnabled(false);
        // break;
        // case 1:
        // linkgrabber.setEnabled(true);
        // packages.setEnabled(true);
        // downloadlinks.setEnabled(true);
        // totalsize.setEnabled(true);
        // panel_add_links.setEnabled(true);
        // if (linkgrabberButtonsEnabled) {
        // lg_add_all.setEnabled(true);
        // lg_add_selected.setEnabled(true);
        // lg_clear.setEnabled(true);
        // }
        // break;
        // }

    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        switch (event.getID()) {
        case LinkGrabberControllerEvent.REFRESH_STRUCTURE:
            if (linkgrabberButtonsEnabled) return;
            linkgrabberButtonsEnabled = true;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    lg_add_all.setEnabled(true);
                    lg_add_selected.setEnabled(true);
                    lg_clear.setEnabled(true);
                    revalidate();
                }
            });
            break;
        case LinkGrabberControllerEvent.EMPTY:
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    lg_add_all.setEnabled(false);
                    lg_add_selected.setEnabled(false);
                    lg_clear.setEnabled(false);
                    revalidate();
                    linkgrabberButtonsEnabled = false;
                }
            });
            break;
        default:
            break;
        }
    }

}
