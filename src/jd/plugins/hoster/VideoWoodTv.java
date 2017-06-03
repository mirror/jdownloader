//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.util.HashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videowood.tv" }, urls = { "http://(www\\.)?videowood\\.tv/(embed|video)/[A-Za-z0-9]+" })
public class VideoWoodTv extends antiDDoSForHost {

    public VideoWoodTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://videowood.tv/terms-of-service";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/video/", "/embed/"));
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Browser prepBrowser(Browser prepBr, String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.getHeaders().put("Cache-Control", null);
            prepBr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getDownloadURL());
        if (br.containsHTML(">This video doesn't exist|>Was deleted by user")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("This video is not ready yet")) {
            link.getLinkStatus().setStatusText("Host says 'This video is not ready yet'");
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("style=\"vertical-align: middle\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("title:\\s*(\"|')(.*?)\\1").getMatch(1);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("This video is not ready yet")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host says 'This video is not ready yet'", 30 * 60 * 1000l);
        }
        final String decoded = new org.jdownloader.encoding.AADecoder(br.toString()).decode();
        String dllink = decoded != null ? new Regex(decoded, "('|\")(http.*?)\\1").getMatch(1) : null;
        if (dllink == null) {
            br.getRegex("file:\\s*(\"|')(http.*?)\\1").getMatch(1);
            if (dllink == null) {
                // packed at this time is missing components within url structure.
                final String js = br.getRegex("eval\\((function\\(p,a,c,k,e,d\\)[^\r\n]+\\)\\))\\)").getMatch(0);
                final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                final ScriptEngine engine = manager.getEngineByName("javascript");
                String result = null;
                try {
                    engine.eval("var res = " + js);
                    result = (String) engine.get("res");
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                if (result == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // mp4 is under the second file, my simple jsonutils wont work well.
                final HashMap<String, Object> entries = (HashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(result);
                dllink = (String) entries.get("file");
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}