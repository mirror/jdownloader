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

    private static final String CODER = "JD-Team";

    private String link;

    private static int SIM = 20;

    public FastLoadNet(PluginWrapper wrapper) {
        super(wrapper);

    }

    @Override
    public String getAGBLink() {
        return "http://www.fast-load.net/infos.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws Exception {

        String downloadurl = downloadLink.getDownloadURL();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        String pid = downloadLink.getDownloadURL().substring(downloadurl.indexOf("pid=") + 4).trim();
        br.getPage("http://www.fast-load.net/api/jdownloader/" + pid);
        
        if(br.getRegex("Server Down").matches()){
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,10*60*1000l);
        }
        String[] lines = Regex.getLines(br + "");

        String filename = lines[0].substring(10).trim();
        long fileSize = Long.parseLong(lines[1].substring(10).trim());
        this.link = lines[2].substring(6).toString();

        if (filename != null) {
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(fileSize);
            return true;
        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getID() == 1) {
            try {
                JLinkButton.openURL("http://www.fast-load.net/getticket.php");
            } catch (MalformedURLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        }
    }

    public ArrayList<MenuItem> createMenuitems() {

        ArrayList<MenuItem> menuList = super.createMenuitems();
        if (menuList == null) menuList = new ArrayList<MenuItem>();

        MenuItem m = new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.menu.fastload", "Get Freeticket"), 1);
        m.setActionListener(this);
        menuList.add(m);
        return menuList;

    }

    public void handleFree(final DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setDebug(true);
        String msg = JDLocale.L("plugins.host.fastload.getticketmsg", "Get Fastload-Ticket!");
        JDUtilities.getGUI().displayMiniWarning(msg, msg, 30);
        dl = br.openDownload(downloadLink, this.link);
        if (!dl.getConnection().isContentDisposition()) {
            SIM = 1;
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,10*1000l);
        }
        SIM = 20;
        dl.startDownload();

    }

    public int getMaxSimultanFreeDownloadNum() {
        return SIM;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}