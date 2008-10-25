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

package jd.plugins.host;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.components.JLinkButton;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class FastLoadNet extends PluginForHost {

    private static final String PROPERTY_TICKET = "FASTLOAD_TICKET";
    private static Semaphore userio_sem = new Semaphore(1);
    private static boolean Refresh_ALL = false;
    private static Refresh_Tickets refresh_thread = null;

    public FastLoadNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fast-load.net/infos.php";
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws Exception {

        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());

        String id = getModifiedID(downloadLink);
        br.getPage("http://www.fast-load.net/system/jd.php?fid=" + id);
        if (br.getRegex("No Such File").matches()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getRegex("Server Down").matches()) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
        String[] lines = Regex.getLines(br + "");

        String filename = lines[0].substring(10).trim();
        long fileSize = Long.parseLong(lines[1].substring(10).trim());
        if (filename != null) {
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(fileSize);
            return true;
        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

    }

    private String getModifiedID(DownloadLink downloadLink) {
        String pid = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=") + 4).trim();
        String first = pid.substring(0, pid.length() - 4);
        String last = pid.substring(pid.length() - 4);
        return last + first;
    }

    public void prepareLink(DownloadLink downloadLink) throws Exception {
        if (getPluginConfig().getBooleanProperty(PROPERTY_TICKET, false)) {
            prepareLink2(downloadLink, true);
        }
    }

    public void prepareLink2(DownloadLink downloadLink, boolean use_semaphore) throws Exception {
        String uui = JDUtilities.getMD5(System.currentTimeMillis() + "_" + (Math.random() * Integer.MAX_VALUE));
        String id = this.getModifiedID(downloadLink);
        if (downloadLink.getProperty("ONEWAYLINK", null) != null) { return; }
        downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.host.fastload.prepare", "Wait for linkconfirmation"));
        downloadLink.requestGuiUpdate();
        if (use_semaphore) acquireUserIO_Semaphore();
        try {
            JLinkButton.openURL("http://www.fast-load.net/getdownload.php?fid=" + id + "&jid=" + uui);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        int i = 30;
        boolean confirmed = false;
        while (i-- > 0) {
            Thread.sleep(1000);
            downloadLink.getLinkStatus().setStatusText("Waiting for Ticket: " + i + " secs");
            downloadLink.requestGuiUpdate();
            br.getPage("http://www.fast-load.net/system/checkconfirmation.php?fid=" + id + "&jid=" + uui);

            if (br.containsHTML("wrong link")) {
                downloadLink.getLinkStatus().setStatusText(null);
                if (use_semaphore) releaseUserIO_Semaphore();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            if (br.containsHTML("not confirmed")) {
                continue;
            } else {
                downloadLink.setProperty("ONEWAYLINK", (br + "").trim());
                confirmed = true;
                break;
            }
        }
        if (!confirmed) {
            logger.warning("Kein Ticket geholt");
            downloadLink.setProperty("ONEWAYLINK", null);
            if (use_semaphore) releaseUserIO_Semaphore();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        downloadLink.getLinkStatus().setStatusText(null);
        if (use_semaphore) releaseUserIO_Semaphore();
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handleFree(final DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setDebug(true);
        this.prepareLink2(downloadLink, true);
        dl = br.openDownload(downloadLink, downloadLink.getStringProperty("ONEWAYLINK", null));
        if (!dl.getConnection().isContentDisposition()) {
            String page = br.loadConnection(dl.getConnection()).trim();
            if (page.equals("wrong link")) {
                logger.warning("Ticket abgelaufen");
                downloadLink.setProperty("ONEWAYLINK", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 1000l);
        }
        dl.startDownload();

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    public static void acquireUserIO_Semaphore() throws InterruptedException {
        try {
            userio_sem.acquire();
        } catch (InterruptedException e) {
            userio_sem.drainPermits();
            userio_sem.release(1);
            throw e;
        }
    }

    public static void releaseUserIO_Semaphore() {
        userio_sem.release();
    }

    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;
        menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.menu.fastload_ticket", "Get Ticket immediately"), 0).setActionListener(this));
        m.setSelected(getPluginConfig().getBooleanProperty(PROPERTY_TICKET, false));
        menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.menu.fastload_refresh", "Refresh all Tickets"), 1).setActionListener(this));
        m.setSelected(Refresh_ALL);
        m.setActionListener(this);
        return menu;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem) {
            switch (((MenuItem) e.getSource()).getActionID()) {
            case 0:
                getPluginConfig().setProperty(PROPERTY_TICKET, !getPluginConfig().getBooleanProperty(PROPERTY_TICKET, false));
                getPluginConfig().save();
                break;
            case 1:
                Refresh_ALL = !Refresh_ALL;
                if (Refresh_ALL == true) {
                    ArrayList<DownloadLink> links = JDUtilities.getController().getDownloadLinks(this);
                    if (refresh_thread != null) {
                        refresh_thread.stop_running();
                        if (refresh_thread.isAlive()) refresh_thread.interrupt();
                        refresh_thread = null;
                    }
                    refresh_thread = new Refresh_Tickets(links);
                    refresh_thread.start();
                } else {
                    if (refresh_thread != null) {
                        refresh_thread.stop_running();
                        if (refresh_thread.isAlive()) refresh_thread.interrupt();
                        refresh_thread = null;
                    }
                }
                break;
            default:
                break;
            }
        }
    }

    public class Refresh_Tickets extends Thread {
        private ArrayList<DownloadLink> links;
        private boolean running = false;
        ProgressController progress;

        public Refresh_Tickets(ArrayList<DownloadLink> links) {
            this.links = links;
            running = true;
            this.setName("FastLoad_Refresh_all_Tickets");
            progress = new ProgressController("Refresh FastLoad Tickets");
            long counter = 0;
            for (DownloadLink link : links) {
                if (link.isEnabled()) {
                    counter++;
                }
            }
            progress.setRange(counter);
        }

        public void stop_running() {
            running = false;
        }

        public void run() {
            Iterator<DownloadLink> iter = links.iterator();
            while (running && iter.hasNext()) {
                DownloadLink link = iter.next();
                if (link.isEnabled()) {
                    try {
                        prepareLink2(link, false);
                    } catch (InterruptedException e) {
                        progress.finalize();
                        Refresh_ALL = false;
                        break;
                    } catch (Exception e) {
                    }
                    progress.increase(1l);
                    link.getLinkStatus().setStatusText(null);
                    link.requestGuiUpdate();
                }
            }
            Refresh_ALL = false;
            progress.finalize();
        }
    }

}