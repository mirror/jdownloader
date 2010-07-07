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

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "remixshare.com" }, urls = { "http://[\\w\\.]*?remixshare\\.com/(.*?\\?file=|download/)[a-z0-9]+" }, flags = { 0 })
public class RemixShareCom extends PluginForHost {

    public RemixShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(3000l);
    }

    public String getAGBLink() {
        return "http://remixshare.com/information/";
    }

    public static final String BLOCKED = "(class=\"blocked\"|>Von Deiner IP Adresse kamen zu viele Anfragen innerhalb kurzer Zeit\\.)";
    public static final Object LOCK = new Object();

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        synchronized (LOCK) {
            if (this.isAborted(downloadLink)) return AvailableStatus.TRUE;
            /* wait 1.5 seconds between filechecks */
            Thread.sleep(1500);
        }
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://remixshare.com", "lang_en", "english");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(BLOCKED)) return AvailableStatus.UNCHECKABLE;
        br.setFollowRedirects(false);
        if (br.containsHTML("Error Code: 500.") || br.containsHTML("Please check the downloadlink")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<span title='([0-9]{10}_)?(.*?)'>", Pattern.CASE_INSENSITIVE)).getMatch(1));
        if (filename == null) filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<title>(.*?)Download at remiXshare Filehosting", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex("(>|\\.\\.\\.)\\&nbsp;\\((.*?)\\)<").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            filesize = filesize.replace("&nbsp;", " ");
            downloadLink.setDownloadSize(Regex.getSize(filesize.replace(",", ".")));
        }
        String md5Hash = br.getRegex("/>MD5:(.*?)</span>").getMatch(0);
        if (md5Hash != null) {
            downloadLink.setMD5Hash(md5Hash.trim());
        }
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (br.containsHTML(BLOCKED)) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 10 * 1000l);
        if (br.containsHTML("Download password")) {
            Form pw = br.getFormbyProperty("name", "pass");
            String pass = downloadLink.getStringProperty("pass", null);
            if (pass == null) pass = Plugin.getUserInput("Password?", downloadLink);
            pw.put("passwd", pass);
            br.submitForm(pw);
            br.getPage(br.getRedirectLocation());
            if (br.containsHTML("Incorrect password entered")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                downloadLink.setProperty("pass", pass);
            }
        }
        String fCKU = br.getRegex("'(/downloadfinal/.*?/)'").getMatch(0);
        if (fCKU == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        fCKU = fCKU.replace("break0", "a0");
        fCKU = "http://remixshare.com" + fCKU;
        br.getPage(fCKU);
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
