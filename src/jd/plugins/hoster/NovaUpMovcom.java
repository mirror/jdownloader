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
import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "novaup.com" }, urls = { "http://(www\\.)?(nova(up|mov)\\.com/(download|sound|video)/[a-z0-9]+|(embed\\.)?novamov\\.com/embed\\.php(\\?width=\\d+\\&height=\\d+\\&|\\?)v=[a-z0-9]+)" }, flags = { 0 })
public class NovaUpMovcom extends PluginForHost {

    private final String TEMPORARYUNAVAILABLE         = "(The file is being transfered to our other servers\\.|This may take few minutes\\.</)";
    private final String TEMPORARYUNAVAILABLEUSERTEXT = "Temporary unavailable";
    private String       DLLINK                       = "";

    public NovaUpMovcom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String videoID = new Regex(link.getDownloadURL(), "novamov\\.com/embed\\.php.*?v=([a-z0-9]+)$").getMatch(0);
        if (videoID != null) {
            link.setUrlDownload("http://www.novamov.com/video/" + videoID);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.novamov.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("This file no longer exists on our servers|The file has failed to convert!") || br.getURL().contains("novamov.com/index.php")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        // onlinecheck für Videolinks
        if (downloadLink.getDownloadURL().contains("video")) {
            String filename = br.getRegex("name=\"title\" content=\"Watch(.*?)online\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>Watch(.*?)online \\| NovaMov - Free and reliable flash video hosting</title>").getMatch(0);
            }
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            filename = filename.trim();
            downloadLink.setFinalFileName(filename.replace(filename.substring(filename.length() - 4, filename.length()), "") + ".flv");
            getVideoLink();
            if (br.containsHTML("error_msg=The video is being transfered")) {
                downloadLink.getLinkStatus().setStatusText("Not downloadable at the moment, try again later...");
                return AvailableStatus.TRUE;
            }
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = Encoding.urlDecode(DLLINK, false);
            final URLConnectionAdapter con = br.openGetConnection(DLLINK);
            try {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } finally {
                con.disconnect();
            }

        } else {
            // Onlinecheck für "nicht"-video Links
            String filename = br.getRegex("<h3><a href=\"#\"><h3>(.*?)</h3></a></h3>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("style=\"text-indent:0;\"><h3>(.*?)</h3></h5>").getMatch(0);
            }
            final String filesize = br.getRegex("strong>File size :</strong>(.*?)</div>").getMatch(0);
            if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "")));
        }

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        final String addedlink = link.getDownloadURL();
        if (link.getDownloadURL().contains("video")) {
            requestFileInformation(link);
            if (br.containsHTML("error_msg=The video is being transfered")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable at the moment, try again later...", 60 * 60 * 1000l);
        } else {
            // handling für "nicht"-video Links
            if (br.containsHTML(TEMPORARYUNAVAILABLE)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.novaupmovcom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT), 30 * 60 * 1000l); }
            br.setFollowRedirects(false);
            br.getPage(addedlink);
            DLLINK = br.getRegex("class= \"click_download\"><a href=\"(http://.*?)\"").getMatch(0);
            if (DLLINK == null) {
                DLLINK = br.getRegex("\"(http://e\\d+\\.novaup\\.com/dl/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
            }
            if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (!DLLINK.contains("http://")) {
                DLLINK = "http://novaup.com" + DLLINK;
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void getVideoLink() throws PluginException, IOException {
        String result = unWise();
        final String fileId = new Regex(result, "flashvars\\.file=\"(.*?)\"").getMatch(0);
        final String fileKey = new Regex(result, "flashvars\\.filekey=\"(.*?)\"").getMatch(0);
        final String fileCid = new Regex(result, "flashvars\\.cid=\"(.*?)\"").getMatch(0);
        if (fileId == null || fileKey == null || fileCid == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.getPage("http://www.novamov.com/api/player.api.php?user=undefined&codes=" + fileCid + "&file=" + fileId + "&pass=undefined&key=" + fileKey);
        DLLINK = br.getRegex("url=(.*?)\\&").getMatch(0);
    }

    private String unWise() {
        String result = null;
        String fn = br.getRegex("eval\\((function\\(.*?\'\\))\\);").getMatch(0);
        if (fn == null) return null;
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("ECMAScript");
        try {
            engine.eval("var res = " + fn);
            result = (String) engine.get("res");
            result = new Regex(result, "eval\\((.*?)\\);$").getMatch(0);
            engine.eval("res = " + result);
            result = (String) engine.get("res");
            String res[] = result.split(";\\s;");
            engine.eval("res = " + new Regex(res[res.length - 1], "eval\\((.*?)\\);$").getMatch(0));
            result = (String) engine.get("res");
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}