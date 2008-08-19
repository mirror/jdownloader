//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDLowSpeed extends PluginOptional {
    private static final String PROPERTY_ENABLED = "PROPERTY_ENABLED";

    private static final String PROPERTY_MAXSPEED = "PROPERTY_MAXSPEED";
    private static final String PROPERTY_MINSPEED = "PROPERTY_MINSPEED";
    private static final String PROPERTY_RAPIDSHAREONLY = "PROPERTY_RAPIDSHAREONLY";

    public static int getAddonInterfaceVersion() {
        return 1;
    }

    private boolean isRunning = false;
    private Thread pluginThread = null;
    private SubConfiguration subConfig = JDUtilities.getSubConfig("ADDONS_JDLOWSPEED");

    public JDLowSpeed() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_RAPIDSHAREONLY, JDLocale.L("plugins.optional.jdlowspeed.rsonly", "Nur Raidshare überwachen")));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_MAXSPEED, JDLocale.L("plugins.optional.jdlowspeed.maxspeed", "Maximale Internetgeschwindigkeit in kb/s (DSL 3000≈350kb/s)"), 1, 1000000));
        cfg.setDefaultValue("350");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_MINSPEED, JDLocale.L("plugins.optional.jdlowspeed.minspeed", "Mindestgeschwindigkeit für einen Download in kb/s"), 1, 100000));
        cfg.setDefaultValue("40");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuItem mi = (MenuItem) e.getSource();
        if (mi.getActionID() == 0) {
            subConfig.setProperty(PROPERTY_ENABLED, true);
            if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
                initPlugin();
            }
            subConfig.save();
        } else {
            subConfig.setProperty(PROPERTY_ENABLED, false);
            stopPlugin();
            subConfig.save();

        }
    }

    @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        if (subConfig.getBooleanProperty(PROPERTY_ENABLED, false)) {
            if (event.getID() == ControlEvent.CONTROL_DOWNLOAD_START) {
                initPlugin();
            } else if (event.getID() == ControlEvent.CONTROL_DOWNLOAD_STOP) {
                stopPlugin();
            } else if (event.getID() == ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED) {
                stopPlugin();
            }
        }

    }

    private void controllDownload(final DownloadLink downloadLink, final ArrayList<DownloadLink> other, final int minspeed, final int maxspeed) {
        if (downloadLink.getSpeedMeter().getSpeed() < minspeed) {
            new Thread(new Runnable() {

                public void run() {
                    for (int i = 0; i < 20; i++) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {

                            e.printStackTrace();
                        }
                        int speed = downloadLink.getSpeedMeter().getSpeed();
                        long size = downloadLink.getDownloadSize();
                        if (!downloadLink.getLinkStatus().isPluginActive() || downloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS) || speed > minspeed) { return; }
                        if (size != 0) {
                            try {
                                if ((size - downloadLink.getDownloadCurrent()) / speed < size / maxspeed) { return; }
                            } catch (Exception e) {
                                return;
                            }
                            Iterator<DownloadLink> iter = other.iterator();
                            long othersSpeed = 0;
                            while (iter.hasNext()) {
                                DownloadLink downloadLink2 = iter.next();
                                if (downloadLink2.getLinkStatus().isPluginActive() || downloadLink2.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                                    othersSpeed += downloadLink2.getSpeedMeter().getSpeed();
                                }
                            }
                            if (othersSpeed != 0 && (size - downloadLink.getDownloadCurrent()) / speed < size / (maxspeed * 5 / 4 - othersSpeed)) { return; }
                        }
                    }
                    logger.info("reset download: " + downloadLink.getName());
                    downloadLink.getLinkStatus().setStatus(LinkStatus.TODO);
                    downloadLink.getLinkStatus().setStatusText("");
                    downloadLink.reset();

                }

            }).start();
        }
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;

        if (!subConfig.getBooleanProperty(PROPERTY_ENABLED, false)) {

            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdlowspeed.statusmessage.enabled", "Low Speederkennung einschalten"), 0).setActionListener(this));
            m.setSelected(false);
        } else {
            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdlowspeed.statusmessage.disabled", "Low Speederkennung ausschalten"), 1).setActionListener(this));
            m.setSelected(true);
        }
        return menu;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.jdlowspeed.name", "LowSpeed Detection");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {

        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        logger.info("JDLowSpeed detection ok");
        return true;
    }

    private void initPlugin() {
        if (!isRunning) {
            logger.info("start");
            isRunning = true;
            pluginThread = new Thread(new Runnable() {

                public void run() {
                    while (isRunning) {
                        int maxspeed = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
                        if (maxspeed == 0) {
                            maxspeed = subConfig.getIntegerProperty(PROPERTY_MAXSPEED, 350);
                        }
                        maxspeed = maxspeed * 4 / 5;
                        maxspeed *= 1024;
                        Boolean rsOnly = subConfig.getBooleanProperty(PROPERTY_RAPIDSHAREONLY, true);
                        if (JDUtilities.getController().getSpeedMeter() < maxspeed) {
                            int minspeed = subConfig.getIntegerProperty(PROPERTY_MINSPEED, 350) * 1024;

                            Iterator<DownloadLink> ff = JDUtilities.getController().getDownloadLinks().iterator();
                            ArrayList<DownloadLink> dls = new ArrayList<DownloadLink>();
                            ArrayList<DownloadLink> dlsToCheck = new ArrayList<DownloadLink>();
                            while (ff.hasNext()) {
                                DownloadLink dl = ff.next();
                                if (dl.getLinkStatus().isPluginActive() || dl.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                                    dls.add(dl);
                                    if (!rsOnly || dl.getPlugin().getHost().equals("rapidshare.com")) {
                                        dlsToCheck.add(dl);

                                    }
                                }
                            }
                            ff = dlsToCheck.iterator();
                            while (ff.hasNext()) {
                                DownloadLink downloadLink = ff.next();
                                ArrayList<DownloadLink> other = new ArrayList<DownloadLink>();
                                Iterator<DownloadLink> it = dls.iterator();
                                while (it.hasNext()) {
                                    DownloadLink downloadLink2 = it.next();
                                    if (!downloadLink2.equals(downloadLink)) {
                                        other.add(downloadLink);
                                    }
                                }

                                controllDownload(downloadLink, other, minspeed, maxspeed);
                            }
                        }
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {

                            e.printStackTrace();
                        }
                    }

                }
            });
            pluginThread.start();
        }
    }

    @Override
    public void onExit() {

    }

    private void stopPlugin() {
        logger.info("stop");
        isRunning = false;

    }

}