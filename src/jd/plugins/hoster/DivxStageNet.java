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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
import jd.plugins.components.SiteType.SiteTemplate;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cloudtime.to", "divxstage.to", "divxstage.net" }, urls = { "http://(?:www\\.)?(?:(?:divxstage\\.(?:net|eu|to)|cloudtime\\.(?:co|to))/video/|embed\\.(?:divxstage\\.(?:net|eu|to)|cloudtime\\.(?:co|to))/embed\\.php\\?v=)[a-z0-9]+", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133", "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsfs2133" })
public class DivxStageNet extends PluginForHost {

    /* Similar plugins: NovaUpMovcom, VideoWeedCom, NowVideoEu, MovShareNet, DivxStageNet */
    // cloudtime.to and nowvideo are the same provider, finallink servers are identical hostnames!

    private static final String DOMAIN = "cloudtime.to";

    @SuppressWarnings("deprecation")
    public DivxStageNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String newurl = "http://www.cloudtime.to/video/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        link.setPluginPatternMatcher(newurl);
        link.setContentUrl(newurl);
    }

    @Override
    public String getAGBLink() {
        return "http://www.cloudtime.to/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String rewriteHost(String host) {
        if ("divxstage.net".equals(getHost()) || "divxstage.eu".equals(getHost()) || "divxstage.to".equals(getHost())) {
            if (host == null || "divxstage.net".equals(host) || "divxstage.eu".equals(host) || "divxstage.to".equals(host)) {
                return "cloudtime.to";
            }
        }
        return super.rewriteHost(host);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /* Important to handle domainchanges... */
        correctDownloadLink(downloadLink);
        br.setFollowRedirects(true);
        setBrowserExclusive();
        // final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        br.getPage(downloadLink.getDownloadURL());
        final Form humanform = br.getFormbyKey("stepkey");
        if (humanform != null) {
            br.submitForm(humanform);
        }
        if (br.containsHTML("The file is beeing transfered to our other servers")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        if (br.containsHTML("This file no longer exists on our servers|This video is not yet ready")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"video_det\">.*?<strong>(.*?)</strong>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)( - CloudTime)?</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename.trim().equals("Untitled")) {
            downloadLink.setFinalFileName("Video " + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + ".avi");
        } else {
            // Check filename from decrypter first
            if (downloadLink.getFinalFileName() == null) {
                downloadLink.setFinalFileName(filename + ".mp4");
            }
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "divxstagedirectlink");
        if (dllink != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            dl.startDownload();
            return;
        }
        if (br.containsHTML("error_msg=The video is being transfered")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable at the moment, try again later...", 60 * 60 * 1000l);
        } else if (br.containsHTML("error_msg=The video has failed to convert")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable at the moment, try again later...", 60 * 60 * 1000l);
        } else if (br.containsHTML("error_msg=The video is converting")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server says: This video is converting", 60 * 60 * 1000l);
        }
        // 20160805-raztoki
        final String[] sources = br.getRegex("<source src=\"(.*?)\"").getColumn(0);
        if (sources != null && sources.length > 0) {
            final ArrayList<String> s = new ArrayList<String>(Arrays.asList(sources));
            Collections.shuffle(s);
            Browser br2 = null;
            for (final String d : s) {
                br2 = br.cloneBrowser();
                try {
                    dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLink, d, true, 0);
                } catch (final Throwable e) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (final Throwable ee) {
                    }
                    logger.info("Download attempt failed:\r\n");
                    e.printStackTrace();
                    continue;
                }
                if (!dl.getConnection().getContentType().contains("html")) {
                    downloadLink.setProperty("divxstagedirectlink", d);
                    dl.startDownload();
                    return;
                }
            }
            if (dl.getConnection().getContentType().contains("html")) {
                br2.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        }
        // old
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
        Browser br2 = br.cloneBrowser();
        for (int i = 0; i <= 5; i++) {
            if (this.isAbort()) {
                return;
            }
            if (i > 0) {
                br2.getPage("http://www." + DOMAIN + "/api/player.api.php?cid=1&user=undefined&pass=undefined&key=" + key + "&cid3=undefined&numOfErrors=" + i + "&cid2=" + cid2 + "&file=" + fid + "&errorUrl=" + Encoding.urlEncode(dllink) + "&errorCode=404");
            } else {
                br2.getPage("http://www." + DOMAIN + "/api/player.api.php?cid2=" + cid2 + "&numOfErrors=0&user=undefined&cid=1&pass=undefined&key=" + key + "&file=" + fid + "&cid3=undefined");
            }
            dllink = br2.getRegex("url=(http://.*?)\\&title").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br2 = br.cloneBrowser();
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLink, dllink, true, 0);
            } catch (final Throwable e) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable ee) {
                }
                logger.info("Download attempt failed:\r\n");
                logger.log(e);
                continue;
            }
            if (!dl.getConnection().getContentType().contains("html")) {
                downloadLink.setProperty("divxstagedirectlink", dllink);
                dl.startDownload();
                return;
            }
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br2.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private String unWise() {
        String result = null;
        String fn = br.getRegex("eval\\((function\\(.*?\'\\))\\);").getMatch(0);
        if (fn == null) {
            return null;
        }
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
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
            logger.log(e);
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_VideoHosting;
    }
}