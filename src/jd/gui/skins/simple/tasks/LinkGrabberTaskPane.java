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

import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.gui.skins.simple.Factory;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.SubPane;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackage;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberTableAction;
import jd.nutils.Formatter;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class LinkGrabberTaskPane extends TaskPanel implements ActionListener, LinkGrabberControllerListener {

    private static final long serialVersionUID = -7720749076951577192L;
    private static final String JDL_PREFIX = "jd.gui.skins.simple.tasks.linkgrabbertaskpane.";
    private JButton panelAddLinks;
    private JButton panelAddContainers;
    private JButton lgAddAll;
    private JButton lgAddSelected;
    private JButton lgClear;

    private boolean linkgrabberButtonsEnabled = false;

    private SubPane linkgrabber;

    private JLabel downloadlinks;
    private JLabel filteredlinks;
    private JLabel packages;
    private JLabel totalsize;
    private Thread fadeTimer;
    private LinkGrabberController lgi;
    private JCheckBox topOrBottom;
    private JCheckBox startAfterAdding;
    private ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>();
    protected boolean updateinprogress = false;

    private long tot = 0;
    private long links = 0;
    private SubPane addLinks;
    private SubPane confirmLinks;
    private SubPane settingsLinks;

    public LinkGrabberTaskPane(String string, ImageIcon ii) {
        super(string, ii, "linkgrabber");
        lgi = LinkGrabberController.getInstance();
        lgi.addListener(this);
        linkgrabberButtonsEnabled = false;
        initGUI();
        fadeTimer = new Thread() {
            public void run() {
                this.setName("LinkGrabberTask: infoupdate");
                while (true) {
                    if (isActiveTab()) {

                        update();
                    }
                    try {
                        Thread.sleep(2000);
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

        linkgrabber = Factory.getSubPane(JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16), JDL.L("gui.taskpanes.download.linkgrabber", "Packagestats"));

        packages = new JLabel(JDL.LF("gui.taskpanes.download.linkgrabber.packages", "%s Package(s)", 0));
        downloadlinks = new JLabel(JDL.LF("gui.taskpanes.download.linkgrabber.downloadLinks", "%s Link(s)", 0));
        filteredlinks = new JLabel(JDL.LF("gui.taskpanes.download.linkgrabber.filteredLinks", "%s filtered Link(s)", 0));
        totalsize = new JLabel(JDL.LF("gui.taskpanes.download.linkgrabber.size", "Total size: %s", 0));
String gapleft = "gapleft 14";
        linkgrabber.add(packages,gapleft);
        linkgrabber.add(downloadlinks,gapleft);
        linkgrabber.add(filteredlinks,gapleft);
        linkgrabber.add(totalsize,gapleft);
        add(linkgrabber, "shrinky 100");
    }

    /**
     * TODO: soll man über events aktuallisiert werden
     */
    private void update() {
        tot = 0;
        links = 0;
        fps.clear();
        fps.addAll(lgi.getPackages());
        for (LinkGrabberFilePackage fp : fps) {
            tot += fp.getDownloadSize(false);
            links += fp.getDownloadLinks().size();
        }
        fps.clear();
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                packages.setText(JDL.LF("gui.taskpanes.download.downloadlist.packages", "%s Packages", fps.size()));
                downloadlinks.setText(JDL.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Links", links));
                filteredlinks.setText(JDL.LF("gui.taskpanes.download.downloadlist.filteredLinks", "%s filtered Link(s)", lgi.getFILTERPACKAGE().size()));
                totalsize.setText(JDL.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", Formatter.formatReadable(tot)));
                return null;
            }
        }.start();
    }

    private void initGUI() {

        addLinks = Factory.getSubPane(JDTheme.II("gui.images.add", 16, 16), JDL.L(JDL_PREFIX + "link", "Add Links"));
        // com.jtattoo.plaf.BaseButtonUI
        this.panelAddLinks = this.createButton(JDL.L("gui.linkgrabberv2.addlinks", "Add Links"), JDTheme.II("gui.images.add", 16, 16));
        this.panelAddContainers = this.createButton(JDL.L("gui.linkgrabberv2.addcontainers", "Open Containers"), JDTheme.II("gui.images.load", 16, 16));

        lgAddAll = createButton(JDL.L("gui.linkgrabberv2.lg.addall", "Add all packages"), JDTheme.II("gui.images.add_all", 16, 16));
        lgAddAll.setName("addAllPackages");

        lgAddSelected = createButton(JDL.L("gui.linkgrabberv2.lg.addselected", "Add selected package(s)"), JDTheme.II("gui.images.add_package", 16, 16));
        lgClear = createButton(JDL.L("gui.linkgrabberv2.lg.clear", "Clear List"), JDTheme.II("gui.images.clear", 16, 16));

        addLinks.add(panelAddLinks);
        addLinks.add(panelAddContainers);
        add(addLinks);

        confirmLinks = Factory.getSubPane(JDTheme.II("gui.images.add", 16, 16), JDL.L(JDL_PREFIX + "accept", "Confirm Links"));
        confirmLinks.add(lgAddAll);
        confirmLinks.add(lgAddSelected);
        confirmLinks.add(lgClear);
        lgAddAll.setEnabled(false);
        lgAddSelected.setEnabled(false);
        lgClear.setEnabled(false);
        add(confirmLinks);

        initListStatGUI();

        initQuickConfig();
    }

    private void initQuickConfig() {

        settingsLinks = Factory.getSubPane(JDTheme.II("gui.images.taskpanes.configuration", 16, 16), JDL.L("gui.taskpanes.download.linkgrabber.config", "Settings"));

        topOrBottom = new JCheckBox(JDL.L("gui.taskpanes.download.linkgrabber.config.addattop", "Add at top"));
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

        startAfterAdding = new JCheckBox(JDL.L("gui.taskpanes.download.linkgrabber.config.startofter", "Start after adding"));
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

        startAfterAdding.setToolTipText(JDL.L("gui.tooltips.linkgrabber.startlinksafteradd", "Is selected, download starts after adding new links"));
        startAfterAdding.setIconTextGap(3);
        settingsLinks.add(startAfterAdding,"gapleft 5");
        startAfterAdding.setToolTipText(JDL.L("gui.tooltips.linkgrabber.topOrBottom", "if selected, new links will be added at top of your downloadlist"));
        topOrBottom.setIconTextGap(3);
        settingsLinks.add(topOrBottom,"gapleft 5");
        add(settingsLinks, "shrinky 100,growy,pushy");
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == panelAddLinks) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTableAction.GUI_ADD, null));
            return;
        }

        if (e.getSource() == panelAddContainers) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTableAction.GUI_LOAD, null));
            return;
        }

        if (e.getSource() == lgAddAll) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTableAction.ADD_ALL, null));
            return;
        }
        if (e.getSource() == lgAddSelected) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTableAction.ADD_SELECTED_PACKAGES, null));
            return;
        }
        if (e.getSource() == lgClear) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTableAction.CLEAR, null));
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
        // lgAddAll.setEnabled(false);
        // lgAddSelected.setEnabled(false);
        // lgClear.setEnabled(false);
        // panelAddLinks.setEnabled(false);
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
        // panelAddLinks.setEnabled(true);
        // if (linkgrabberButtonsEnabled) {
        // lgAddAll.setEnabled(true);
        // lgAddSelected.setEnabled(true);
        // lgClear.setEnabled(true);
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
                    lgAddAll.setEnabled(true);
                    lgAddSelected.setEnabled(true);
                    lgClear.setEnabled(true);
                    revalidate();
                }
            });
            break;
        case LinkGrabberControllerEvent.EMPTY:
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    lgAddAll.setEnabled(false);
                    lgAddSelected.setEnabled(false);
                    lgClear.setEnabled(false);
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
