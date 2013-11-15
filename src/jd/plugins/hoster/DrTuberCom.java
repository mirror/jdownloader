//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drtuber.com" }, urls = { "http://(www\\.)?drtuber\\.com/(video/\\d+|player/config_embed3\\.php\\?vkey=[a-z0-9]+|embed/\\d+)" }, flags = { 0 })
public class DrTuberCom extends PluginForHost {

    private String DLLINK = null;

    public DrTuberCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.drtuber.com/static/terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().toLowerCase());
    }

    private String getContinueLink(String fun) {
        if (fun == null) { return null; }
        fun = fun.replaceAll("s1\\.addVariable\\(\\'config\\',", "var result = ").replaceAll("params\\);", "params;");
        Object result = new Object();
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval(fun);
            result = engine.get("result");
        } catch (Throwable e) {
            return null;
        }
        return result.toString();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        String continueLink = null, filename = null;
        // Check if link is an embedded link e.g. from a decrypter

        /* embed v3 */
        String vk = new Regex(downloadLink.getDownloadURL(), "vkey=(\\w+)").getMatch(0);
        if (vk != null) {
            br.getPage(downloadLink.getDownloadURL() + "&pkey=" + JDHash.getMD5(vk + Encoding.Base64Decode("S0s2Mml5aUliWFhIc2J3")));
            if (br.containsHTML("Invalid video key\\!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String finallink = br.getRegex("type=video_click\\&amp;target_url=(http.*?)</url>").getMatch(0);
            if (finallink == null) {
                logger.warning("Failed to find original link for: " + downloadLink.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setUrlDownload(Encoding.htmlDecode(finallink));
        }

        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("This video was deleted") || br.getURL().contains("missing=true")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // No account support -> No support for private videos
        if (br.containsHTML("Sorry\\.\\.\\. Video is private")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        /* normal links */
        if (new Regex(downloadLink.getDownloadURL(), Pattern.compile("http://(www\\.)?drtuber\\.com/video/\\d+", Pattern.CASE_INSENSITIVE)).matches()) {
            filename = br.getRegex("<title>(.*?) \\- Free Porn.*?DrTuber\\.com</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1 class=\"name\">(.*?)</h1>").getMatch(0);
            }
            br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
            continueLink = getContinueLink(br.getRegex("(var configPath.*?addVariable\\(\\'config\\',.*?;)").getMatch(0));
            String vKey = new Regex(continueLink, "vkey=(\\w+)").getMatch(0);
            if (continueLink == null || vKey == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (!continueLink.startsWith("http://"))
                continueLink = "http://drtuber.com" + Encoding.htmlDecode(continueLink) + "&pkey=" + JDHash.getMD5(vKey + Encoding.Base64Decode("UFQ2bDEzdW1xVjhLODI3"));
            else
                continueLink = Encoding.htmlDecode(continueLink) + "&pkey=" + JDHash.getMD5(vKey + Encoding.Base64Decode("UFQ2bDEzdW1xVjhLODI3"));
        }

        /* embed v4 */
        if (downloadLink.getDownloadURL().matches("http://(www\\.)?drtuber\\.com/embed/\\d+")) {
            String nextUrl = br.getRegex("flashvars=\"embed=1\\&config=([^\"]+)\"").getMatch(0);
            if (nextUrl == null) {
                String[] hashEncValues = br.getRegex("flashvars=\"id_video=(\\d+)\\&t=(\\d+)").getRow(0);
                if (hashEncValues == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                continueLink = "/player/config_embed4.php?id_video=" + hashEncValues[0] + "&t=" + hashEncValues[1] + "&pkey=" + JDHash.getMD5(hashEncValues[0] + hashEncValues[1] + Encoding.Base64Decode("RXMxaldDemZOQmRsMlk4"));
            } else {
                nextUrl = Encoding.htmlDecode(nextUrl);
                vk = new Regex(nextUrl, "vkey=(\\w+)").getMatch(0);
                continueLink = nextUrl + "&pkey=" + JDHash.getMD5(vk + Encoding.Base64Decode("UFQ2bDEzdW1xVjhLODI3"));
            }
            filename = br.getRegex("<title>(.*?)\\s+\\-\\s+Free Porn Videos").getMatch(0);
        }
        if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        br.getPage(continueLink);
        DLLINK = br.getRegex("<video_file>(<\\!\\[CDATA\\[)?(http://.*?)(\\]\\]>)?</video_file>").getMatch(1);
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK.trim());
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
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