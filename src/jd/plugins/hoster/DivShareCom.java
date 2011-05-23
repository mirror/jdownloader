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

package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.PluginUtils;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "divshare.com" }, urls = { "http://[\\w\\.]*?divshare\\.com/(download|image|direct)/\\d+-.+" }, flags = { 0 })
public class DivShareCom extends PluginForHost {

    public DivShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.divshare.com/page/terms";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(true);
        if (link.getDownloadURL().contains("direct")) {
            String dllink;
            dllink = br.getURL();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(null);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
            dl.startDownload();

        } else {
            String infolink = link.getDownloadURL();
            // Check ob ein Passwort verlangt wird, wenn keins verlangt wird
            // wird der Link sofort geändert und geladen (unten ists dann
            // infolink2)!
            if (br.containsHTML("This file is password protected.")) {
                Form form = br.getForm(0);
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String passCode = null;
                if (link.getStringProperty("pass", null) == null) {
                    passCode = getUserInput(null, link);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = link.getStringProperty("pass", null);
                }
                form.put("gallery_password", passCode);
                br.submitForm(form);
                if (br.containsHTML("This file is password protected.")) {
                    logger.warning("Wrong password!");
                    link.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (passCode != null) {
                    link.setProperty("pass", passCode);
                }
            }
            String infolink2 = infolink.replaceAll("divshare\\.com/(download|image)", "divshare.com/download/launch");
            br.getPage(infolink2);
            String dllink = br.getRegex("class=\"download_message\">Your download will start momentarily\\. If it doesn\\'t, <a href=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://storagestart\\.divshare\\.com/launch\\.php\\?f=.*?)\"").getMatch(0);
            if (dllink == null && br.containsHTML("application/mp3")) {
                String id = br.getRegex("divshare.com/download/([0-9a-f]+)").getMatch(0);
                String id2 = br.getRegex("divshare.com/download/.*?-([0-9a-f]+)").getMatch(0);
                dllink = "http://storagestart2.divshare.com/launch.php?f=" + id + "&s=" + id2;
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();

        }

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<title>DivShare \\- Password-Protected File</title>")) {
            Form pw = br.getForm(0);
            String password = null;
            // first look plugin
            password = this.getPluginConfig().getStringProperty("pass", password);
            // then downloadlink
            password = downloadLink.getStringProperty("pass", password);
            if (password == null) password = PluginUtils.askPassword(this);
            pw.put("gallery_password", password);
            br.submitForm(pw);
            pw = br.getFormbyKey("gallery_password");
            if (pw != null) {
                password = PluginUtils.askPassword(this);
                pw.put("gallery_password", password);
                br.submitForm(pw);
                pw = br.getFormbyKey("gallery_password");
                if (pw != null) {
                    logger.warning("Wrong Password for " + downloadLink + ". You will need the correct password to download this file.");
                } else {
                    downloadLink.setProperty("pass", password);
                    getPluginConfig().setProperty("pass", password);
                    getPluginConfig().save();
                }
            } else {
                downloadLink.setProperty("pass", password);
                getPluginConfig().setProperty("pass", password);
                getPluginConfig().save();
            }
        }
        // handling für die links, die "direct" beinhalten (diese starten
        // sofort)
        if (downloadLink.getDownloadURL().contains("direct")) {
            String redirect = br.getRegex("If it doesn\\'t\\, <a href=\"(.*?)\">click here<").getMatch(0);
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(redirect);
                if (con.isContentDisposition()) {
                    downloadLink.setFinalFileName(Plugin.getFileNameFromDispositionHeader(con.getHeaderField("Content-Disposition")).replace("_", "."));
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    return AvailableStatus.TRUE;
                }
                if (br.containsHTML("This file is unavailable until")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                if (br.containsHTML("Sorry, we couldn't find this file.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                if (br.containsHTML("This file is secured by Divshare.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
                br.setFollowRedirects(false);
            }
        } else {
            // handling für die anderen Links (mit "download" bzw. "image" im
            // Link
            if (br.containsHTML("Sorry, we couldn't find this file.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("This file is secured by Divshare.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // alte Methode um an den filename der Downloadseite zu kommen
            // br.getRegex("<title>(.*?) - DivShare</title>").getMatch(0);
            String filename0 = downloadLink.getDownloadURL().replaceAll("divshare.com/(download|image)", "divshare.com/sharing");
            Browser br2 = br.cloneBrowser();
            br2.getPage(filename0);
            String filename = br2.getRegex("no-repeat left center;\">(.*?)</span>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(filename.replaceAll("(\\)|\\()", "").replace("_", "."));
            String id = br2.getRegex("divshare.com/download/([0-9a-f\\-]+)").getMatch(0);
            if (id != null) br2.postPage("http://www.divshare.com/scripts/ajax/v5/fileStats.php", "code=" + id);
            String size = br2.getRegex("File Size:.*?>.*?([0-9\\.]+)").getMatch(0);
            String sizef = br2.getRegex("tiny.*?>([KGTBM]+)").getMatch(0);
            if (sizef != null) size = size + " " + sizef;
            downloadLink.setDownloadSize(SizeFormatter.getSize(size.replaceAll(",", "\\.")));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}