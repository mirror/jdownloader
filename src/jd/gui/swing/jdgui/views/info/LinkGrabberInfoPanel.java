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

package jd.gui.swing.jdgui.views.info;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.nutils.Formatter;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

public class LinkGrabberInfoPanel extends InfoPanel {

    private static final long serialVersionUID = 2276105693934789404L;

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.info.LinkGrabberInfoPanel.";
    private LinkGrabberController lgi;
    protected long links;
    protected long tot;
    protected ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>();

    public LinkGrabberInfoPanel() {
        super("gui.images.taskpanes.linkgrabber");

        addInfoEntry(JDL.L(JDL_PREFIX + "packages", "Package(s)"), "0", 0, 0);
        addInfoEntry(JDL.L(JDL_PREFIX + "links", "Links(s)"), "0", 0, 1);
        addInfoEntry(JDL.L(JDL_PREFIX + "filteredlinks", "filtered Links(s)"), "0", 1, 1);
        addInfoEntry(JDL.L(JDL_PREFIX + "size", "Total size"), "0", 1, 0);
        addCheckboxes();
        lgi = LinkGrabberController.getInstance();
        Thread updateTimer = new Thread() {
            public void run() {
                this.setName("LinkGrabberView: infoupdate");
                while (true) {
                    update();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };
        updateTimer.start();
    }

    private void addCheckboxes() {
        final JCheckBox topOrBottom = new JCheckBox(JDL.L("gui.taskpanes.download.linkgrabber.config.addattop", "Add at top"));
        topOrBottom.setOpaque(false);
        topOrBottom.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_INSERT_NEW_LINKS_AT, topOrBottom.isSelected());
                GUIUtils.getConfig().save();
            }

        });
        if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
            topOrBottom.setSelected(true);
        }
        topOrBottom.setToolTipText(JDL.L("gui.tooltips.linkgrabber.topOrBottom", "if selected, new links will be added at top of your downloadlist"));
        topOrBottom.setIconTextGap(3);

        final JCheckBox startAfterAdding = new JCheckBox(JDL.L("gui.taskpanes.download.linkgrabber.config.startofter", "Start after adding"));
        startAfterAdding.setOpaque(false);
        startAfterAdding.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS, startAfterAdding.isSelected());
                GUIUtils.getConfig().save();
            }

        });
        if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS, true)) {
            startAfterAdding.setSelected(true);
        }

        startAfterAdding.setToolTipText(JDL.L("gui.tooltips.linkgrabber.startlinksafteradd", "Is selected, download starts after adding new links"));
        startAfterAdding.setIconTextGap(3);

        final JCheckBox autoStart = new JCheckBox(JDL.L("gui.taskpanes.download.linkgrabber.config.autostart", "Start Automatically"));
        autoStart.setOpaque(false);
        autoStart.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS_AUTO, autoStart.isSelected());
                GUIUtils.getConfig().save();
            }

        });
        if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS_AUTO, false)) {
            autoStart.setSelected(true);
        }
        autoStart.setToolTipText(JDL.L("gui.tooltips.linkgrabber.autostart", "if selected, links will get added and started automatically"));
        autoStart.setIconTextGap(3);

        addComponent(topOrBottom, 2, 0);
        addComponent(startAfterAdding, 2, 1);
        addComponent(autoStart, 3, 0);
    }

    private void update() {
        if (!isShown()) return;
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                tot = 0;
                links = 0;
                fps.addAll(lgi.getPackages());
                for (LinkGrabberFilePackage fp : fps) {
                    tot += fp.getDownloadSize(false);
                    links += fp.getDownloadLinks().size();
                }
                updateInfo(JDL.L(JDL_PREFIX + "packages", "Package(s)"), fps.size());
                updateInfo(JDL.L(JDL_PREFIX + "links", "Links(s)"), links);
                updateInfo(JDL.L(JDL_PREFIX + "filteredlinks", "filtered Links(s)"), lgi.getFilterPackage().size());
                updateInfo(JDL.L(JDL_PREFIX + "size", "Total size"), Formatter.formatReadable(tot));
                fps.clear();
                return null;
            }
        }.start();
    }

    @Override
    public void onHide() {
    }

    @Override
    public void onShow() {
    }

}
