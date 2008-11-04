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

    private static Semaphore USERIO_SEMAPHORE = new Semaphore(1);
    private static boolean REFRESH_ALL_FLAG = false;
    private static TicketRefresher REFRESH_THREAD = null;

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

    public void prepare(DownloadLink downloadLink, boolean use_semaphore) throws Exception {
        String uui = JDUtilities.getMD5(System.currentTimeMillis() + "_" + (Math.random() * Integer.MAX_VALUE));
        String id = this.getModifiedID(downloadLink);
        if (downloadLink.getProperty("ONEWAYLINK", null) != null) { return; }
        downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.host.fastload.prepare", "Wait for linkconfirmation"));
        downloadLink.requestGuiUpdate();
        if (use_semaphore) acquireUserIO();
        try {
            JLinkButton.openURL("http://www.fast-load.net/getdownload.php?fid=" + id + "&jid=" + uui);
        } catch (Exception e1) {
            e1.printStackTrace();
            throw new PluginException(LinkStatus.ERROR_FATAL,JDLocale.L("plugins.host.fastload.errors","Browserlauncher missconfigured"));
        }
        Thread.sleep(5000);
        int i = 30;
        // http://www.fast-load.net/getdownload.php?fid=
        // 174eebac7bd433c09e348cb8ab7d8410&jid=ffe52e498265a32e53e1986750f944b6
        boolean confirmed = false;
        br.setDebug(true);
        while (i-- > 0) {
            Thread.sleep(1000);
            downloadLink.getLinkStatus().setStatusText("Waiting for Ticket: " + i + " secs");
            downloadLink.requestGuiUpdate();
            br.getPage("http://www.fast-load.net/system/checkconfirmation.php?fid=" + id + "&jid=" + uui);

            if (br.containsHTML("wrong link")) {
                downloadLink.getLinkStatus().setStatusText(null);
                if (use_semaphore) releaseUserIO();
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
            if (use_semaphore) releaseUserIO();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        downloadLink.getLinkStatus().setStatusText(null);
        if (use_semaphore) releaseUserIO();
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handleFree(final DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setDebug(true);
        prepare(downloadLink, true);
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
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    public static void acquireUserIO() throws InterruptedException {
        try {
            USERIO_SEMAPHORE.acquire();
        } catch (InterruptedException e) {
            USERIO_SEMAPHORE.drainPermits();
            USERIO_SEMAPHORE.release(1);
            throw e;
        }
    }

    public static void releaseUserIO() {
        USERIO_SEMAPHORE.release();
    }

    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;
        menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.menu.fastload_refresh", "Refresh all Tickets"), 0).setActionListener(this));
        m.setSelected(REFRESH_ALL_FLAG);
        m.setActionListener(this);
        return menu;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem) {
            switch (((MenuItem) e.getSource()).getActionID()) {
            case 0:
                REFRESH_ALL_FLAG = !REFRESH_ALL_FLAG;
                if (REFRESH_ALL_FLAG == true) {
                    ArrayList<DownloadLink> links = JDUtilities.getController().getDownloadLinks(this);
                    if (REFRESH_THREAD != null) {
                        REFRESH_THREAD.stop_running();
                        if (REFRESH_THREAD.isAlive()) REFRESH_THREAD.interrupt();
                        REFRESH_THREAD = null;
                    }
                    REFRESH_THREAD = new TicketRefresher(links);
                    REFRESH_THREAD.start();
                } else {
                    if (REFRESH_THREAD != null) {
                        REFRESH_THREAD.stop_running();
                        if (REFRESH_THREAD.isAlive()) REFRESH_THREAD.interrupt();
                        REFRESH_THREAD = null;
                    }
                }
                break;
            default:
                break;
            }
        }
    }

    public class TicketRefresher extends Thread {
        private ArrayList<DownloadLink> links;
        private boolean running = false;
        ProgressController progress;

        public TicketRefresher(ArrayList<DownloadLink> links) {
            this.links = links;
            running = true;
            this.setName("FastLoad_Refresh_all_Tickets");
            progress = new ProgressController("Refresh FastLoad Tickets");
            progress.setRange(0);
            for (DownloadLink link : links) {
                if (link.isEnabled()) {
                    progress.addToMax(1l);
                }
            }
        }

        public void stop_running() {
            running = false;
        }

        public void run() {
            Iterator<DownloadLink> iter = links.iterator();
            while (running && iter.hasNext()) {
                DownloadLink link = iter.next();
                if (link.isEnabled() && !link.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    try {
                        prepare(link, false);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        progress.finalize();
                        REFRESH_ALL_FLAG = false;
                        break;
                    } catch (PluginException e) {
                        link.getLinkStatus().setStatusText(e.getErrorMessage());
                        link.requestGuiUpdate();
                        continue;
                    } catch (Exception e) {
                        e.printStackTrace();
                        
                    }
                    progress.increase(1l);
                    link.getLinkStatus().setStatusText(null);
                    link.requestGuiUpdate();
                }
            }
            REFRESH_ALL_FLAG = false;
            progress.finalize();
        }
    }

}