//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.http.RandomUserAgent;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bigandfree.com" }, urls = { "http://[\\w\\.]*?(megaftp|bigandfree)\\.com/[0-9]+" }, flags = { 0 })
public class BigAndFreeCom extends PluginForHost {

    public static final Object LOCK = new Object();

    public BigAndFreeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.bigandfree.com/tos";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(megaftp|bigandfree)", "bigandfree"));
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception, PluginException, InterruptedException {
        synchronized (LOCK) {
            if (this.isAborted(downloadLink)) return AvailableStatus.TRUE;
            /* wait 3 seconds between filechecks */
            Thread.sleep(3000);
        }
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.bigandfree.com", "geoCode", "EN");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The file you requested has been removed")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File Name: </font><font class=.*?>(.*?)</font>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("addthis_open\\(this, '', 'http://.*?', '(.*?)'\\)").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        br.setFollowRedirects(false);
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        synchronized (LOCK) {
            if (this.isAborted(downloadLink)) return;
        }
        if (br.containsHTML("You have exceeded your download limit")) {
            int wait = 60;
            String time = br.getRegex("Please wait (\\d+) Minut").getMatch(0);
            if (time != null) wait = Integer.parseInt(time);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1000l * 60);
        }
        Form freeform = br.getFormbyProperty("name", "chosen");
        if (freeform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        freeform.setAction(downloadLink.getDownloadURL());
        freeform.remove("chosen_prem");
        br.submitForm(freeform);
        // Datei hat Passwortschutz?
        if (br.containsHTML("This file is password-protected")) {
            String passCode;
            DownloadLink link = downloadLink;
            Form form = br.getFormbyProperty("name", "pswcheck");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            if (link.getStringProperty("pass", null) == null) {
                /* Usereingabe */
                passCode = Plugin.getUserInput(null, link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            /* Passwort übergeben */
            form.put("psw", passCode);
            form.setAction(downloadLink.getDownloadURL());
            br.submitForm(form);
            form = br.getFormbyProperty("name", "pswcheck");
            if (form != null && br.containsHTML("Invalid Password")) {
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                link.setProperty("pass", passCode);
            }
        }

        // often they only change this form
        Form downloadForm = br.getForm(1);
        if (downloadForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String current = br.getRegex("name=\"current\" value=\"(.*?)\"").getMatch(0);
        if (current == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String wait = br.getRegex("var x = (\\d+);").getMatch(0);
        if (wait != null) sleep(Long.parseLong(wait.trim()) * 1000, downloadLink);
        downloadForm.put("current", current);
        downloadForm.put("limit_reached", "0");
        downloadForm.put("download_now", "Click+here+to+download");
        downloadForm.setAction(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadForm, false, 1);
        if (!(dl.getConnection().isContentDisposition()) && !dl.getConnection().getContentType().contains("octet")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {

    }

}