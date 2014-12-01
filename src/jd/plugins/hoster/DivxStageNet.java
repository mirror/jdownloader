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

import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "divxstage.to", "divxstage.net" }, urls = { "http://(www\\.)?(divxstage\\.(net|eu|to)/video/|embed\\.divxstage\\.(net|eu|to)/embed\\.php\\?v=)[a-z0-9]+", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" }, flags = { 0, 0 })
public class DivxStageNet extends PluginForHost {

    /* Similar plugins: NovaUpMovcom, VideoWeedCom, NowVideoEu, MovShareNet, DivxStageNet */
    private static final String DOMAIN = "divxstage.to";

    public DivxStageNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://divxstage.to/video/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
    }

    @Override
    public String getAGBLink() {
        return "http://www.divxstage.net/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String rewriteHost(String host) {
        if ("divxstage.net".equals(getHost()) || "divxstage.eu".equals(getHost())) {
            if (host == null || "divxstage.net".equals(host) || "divxstage.eu".equals(host)) {
                return "divxstage.to";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We need you to prove you're human")) {
            Form IAmAHuman = br.getForm(0);
            if (IAmAHuman == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.submitForm(IAmAHuman);
        }
        if (br.containsHTML("The file is beeing transfered to our other servers")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        if (br.containsHTML("This file no longer exists on our servers")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"video_det\">.*?<strong>(.*?)</strong>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename.trim().equals("Untitled")) {
            downloadLink.setFinalFileName("Video " + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + ".avi");
        } else {
            downloadLink.setFinalFileName(filename + ".flv");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "divxstagedirectlink");
        if (dllink == null) {
            if (br.containsHTML("error_msg=The video is being transfered")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable at the moment, try again later...", 60 * 60 * 1000l);
            } else if (br.containsHTML("error_msg=The video has failed to convert")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable at the moment, try again later...", 60 * 60 * 1000l);
            } else if (br.containsHTML("error_msg=The video is converting")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server says: This video is converting", 60 * 60 * 1000l);
            }
            String cid2 = br.getRegex("flashvars\\.cid2=\"(\\d+)\";").getMatch(0);
            String key = br.getRegex("flashvars\\.filekey=\"(.*?)\"").getMatch(0);
            if (key == null && br.containsHTML("w,i,s,e")) {
                String result = unWise();
                key = new Regex(result, "(\"\\d+{1,3}\\.\\d+{1,3}\\.\\d+{1,3}\\.\\d+{1,3}-[a-f0-9]{32})\"").getMatch(0);
            }
            if (key == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (cid2 == null) {
                cid2 = "undefined";
            }
            key = Encoding.urlEncode(key);
            final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            String lastdllink = null;
            boolean success = false;
            for (int i = 0; i <= 3; i++) {
                if (i > 0) {
                    br.getPage("http://www." + DOMAIN + "/api/player.api.php?user=undefined&errorUrl=" + Encoding.urlEncode(lastdllink) + "&pass=undefined&cid3=undefined&errorCode=404&cid=1&cid2=" + cid2 + "&key=" + key + "&file=" + fid + "&numOfErrors=" + i);
                } else {
                    br.getPage("http://www." + DOMAIN + "/api/player.api.php?cid2=" + cid2 + "&numOfErrors=0&user=undefined&cid=1&pass=undefined&key=" + key + "&file=" + fid + "&cid3=undefined");
                }
                dllink = br.getRegex("url=(http://.*?)\\&title").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                try {
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
                    if (!dl.getConnection().getContentType().contains("html")) {
                        success = true;
                        break;
                    } else {
                        lastdllink = dllink;
                        continue;
                    }
                } finally {
                    try {
                        dl.getConnection().disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            if (!success) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        downloadLink.setProperty("divxstagedirectlink", dllink);
        dl.startDownload();
    }

    private String unWise() {
        String result = null;
        String fn = br.getRegex("eval\\((function\\(.*?\'\\))\\);").getMatch(0);
        if (fn == null) {
            return null;
        }
        final ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
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

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}