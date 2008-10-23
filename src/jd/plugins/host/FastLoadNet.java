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

import java.net.MalformedURLException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.gui.skins.simple.components.JLinkButton;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class FastLoadNet extends PluginForHost {

    private static int SIM = 20;

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
        String first= pid.substring(0, pid.length()-4);
        String last=pid.substring(pid.length()-4);
        return last+first;
    }

    public void prepareLink(DownloadLink downloadLink) throws Exception {
        String uui = JDUtilities.getMD5(System.currentTimeMillis() + "_" + (Math.random() * Integer.MAX_VALUE));
        String id = this.getModifiedID(downloadLink);
        if (downloadLink.getProperty("ONEWAYLINK", null) != null) { return; }
        downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.host.fastload.prepare","Wait for linkconfirmation"));
        downloadLink.requestGuiUpdate();
        try {
            JLinkButton.openURL("http://www.fast-load.net/getdownload.php?fid=" + id + "&jid=" + uui);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        int i = 30;
        while (i-- > 0) {

            Thread.sleep(1000);

            br.getPage("http://www.fast-load.net/system/checkconfirmation.php?fid=" + id + "&jid=" + uui);

            if (br.containsHTML("wrong link")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

            if (br.containsHTML("not confirmed")) {
                continue;

            } else {

                downloadLink.setProperty("ONEWAYLINK", (br + "").trim());
                break;
            }
        }
        downloadLink.getLinkStatus().setStatusText(null);
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    public ArrayList<MenuItem> createMenuitems() {

        ArrayList<MenuItem> menuList = super.createMenuitems();
        // if (menuList == null) menuList = new ArrayList<MenuItem>();
        //
        // MenuItem m = new MenuItem(MenuItem.NORMAL,
        // JDLocale.L("plugins.menu.fastload", "Get Freeticket"), 1);
        // m.setActionListener(this);
        // menuList.add(m);
        return menuList;

    }

    public void handleFree(final DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);

        // if (this.throttled && !this.speedTicket && !TICKET_MESSAGE) {
        // if (JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.L(
        // "plugins.host.fastload.ticketmessage",
        // "Get a Fastload.net Speedticket to boost speed up to 300kb/s"), 15))
        // {
        // try {
        // JLinkButton.openURL("http://www.fast-load.net/getticket.php");
        // } catch (MalformedURLException e1) {
        // e1.printStackTrace();
        // }
        // downloadLink.getLinkStatus().setStatusText(JDLocale.L(
        // "plugins.host.fastload.wait_ticketmessage", "Wait for Speedticket"));
        // downloadLink.requestGuiUpdate();
        // int i = 0;
        // while (!speedTicket && i < 90000) {
        // Thread.sleep(1000);
        // getFileInformation(downloadLink);
        // i += 1000;
        //
        // }
        // }
        // TICKET_MESSAGE = true;
        // }
        br.setDebug(true);
this.prepareLink(downloadLink);
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
        // return
        // Math.min(SIM,JDUtilities.getController().getRunningDownloadNumByHost
        // (this)+1);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}