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

package jd.gui.swing.jdgui.views.linkgrabber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.views.InfoPanel;
import jd.nutils.Formatter;
import jd.plugins.LinkGrabberFilePackage;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.translate._GUI;

public class LinkGrabberInfoPanel extends InfoPanel {

    private static final long                   serialVersionUID = 2276105693934789404L;

    private LinkGrabberController               lgi;
    protected long                              links;
    protected long                              tot;
    protected ArrayList<LinkGrabberFilePackage> fps              = new ArrayList<LinkGrabberFilePackage>();

    public LinkGrabberInfoPanel() {
        super("linkgrabber");

        addInfoEntry(_GUI._.jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_packages(), "0", 0, 0);
        addInfoEntry(_GUI._.jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_links(), "0", 0, 1);
        addInfoEntry(_GUI._.jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_filteredlinks(), "0", 1, 1);
        addInfoEntry(_GUI._.jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_size(), "0", 1, 0);
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
        final JCheckBox topOrBottom = new JCheckBox(_GUI._.gui_taskpanes_download_linkgrabber_config_addattop());
        topOrBottom.setOpaque(false);
        topOrBottom.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                JsonConfig.create(LinkgrabberSettings.class).setAddNewLinksOnTop(topOrBottom.isSelected());
            }

        });
        topOrBottom.setSelected(JsonConfig.create(LinkgrabberSettings.class).isAddNewLinksOnTop());

        topOrBottom.setToolTipText(_GUI._.gui_tooltips_linkgrabber_topOrBottom());
        topOrBottom.setIconTextGap(3);

        final JCheckBox startAfterAdding = new JCheckBox(_GUI._.gui_taskpanes_download_linkgrabber_config_startofter());
        startAfterAdding.setOpaque(false);
        startAfterAdding.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                JsonConfig.create(LinkgrabberSettings.class).setAutoDownloadStartAfterAddingEnabled(startAfterAdding.isSelected());
            }

        });
        startAfterAdding.setSelected(JsonConfig.create(LinkgrabberSettings.class).isAutoDownloadStartAfterAddingEnabled());

        startAfterAdding.setToolTipText(_GUI._.gui_tooltips_linkgrabber_startlinksafteradd());
        startAfterAdding.setIconTextGap(3);

        final JCheckBox autoStart = new JCheckBox(_GUI._.gui_taskpanes_download_linkgrabber_config_autostart());
        autoStart.setOpaque(false);
        autoStart.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JsonConfig.create(LinkgrabberSettings.class).setAutoaddLinksAfterLinkcheck(autoStart.isSelected());
            }

        });
        autoStart.setSelected(JsonConfig.create(LinkgrabberSettings.class).isAutoaddLinksAfterLinkcheck());

        autoStart.setToolTipText(_GUI._.gui_tooltips_linkgrabber_autostart());
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
                updateInfo(_GUI._.jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_packages(), fps.size());
                updateInfo(_GUI._.jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_links(), links);
                updateInfo(_GUI._.jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_filteredlinks(), lgi.getFilterPackage().size());
                updateInfo(_GUI._.jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_size(), Formatter.formatReadable(tot));
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