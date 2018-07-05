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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.Regex;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: $", interfaceVersion = 3, names = { "javusb.com" }, urls = { "https?://(?:www\\.)?javusb\\.com/.+" })
public class JavusbCom extends PluginForHost {
    public JavusbCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // this host use JuicyCodes.Run (https://juicycodes.com)
    private static final String  DEFAULT_EXTENSION = ".mp4";
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://javusb.com/terms-conditions/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>([^<>\"]*?) - ").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = Encoding.htmlDecode(title.trim());
        String filename = title;
        if (!filename.endsWith(DEFAULT_EXTENSION)) {
            filename += DEFAULT_EXTENSION;
        }
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        String iframeUrl = br.getRegex("<iframe [^>]+?src=\"([^\"]+)\"").getMatch(0);
        if (iframeUrl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(iframeUrl);
        String juicy = br.getRegex("JuicyCodes.Run\\(([^)]+)\\);").getMatch(0);
        dllink = decodeJuicyCodes(juicy);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("range", "bytes=0-");
        Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html") && con.isOK()) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void getPage(String page) throws Exception {
        br.getPage(page);
    }

    // decode JuicyCodes.Run
    private String decodeJuicyCodes(String juicy) {
        String result = null;
        StringBuilder sb = new StringBuilder();
        sb.append("var atob=function(f){var g={},b=65,d=0,a,c=0,h,e='',k=String.fromCharCode,l=f.length;for(a='';91>b;)a+=k(b++);a+=a.toLowerCase()+'0123456789+/';for(b=0;64>b;b++)g[a.charAt(b)]=b;for(a=0;a<l;a++)for(b=g[f.charAt(a)],d=(d<<6)+b,c+=6;8<=c;)((h=d>>>(c-=8)&255)||a<l-2)&&(e+=k(h));return e};");
        sb.append(String.format("var result=atob(%s);", juicy));
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        String jsResult = null;
        try {
            engine.eval(sb.toString());
            jsResult = engine.get("result").toString();
            String fnc = new Regex(jsResult, "eval\\((.+?)\\)$").getMatch(0);
            engine.eval(String.format("var result=%s;", fnc));
            jsResult = engine.get("result").toString();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        result = jsResult != null ? new Regex(jsResult, "src\":\"([^\"]+)\"").getMatch(0) : null;
        return result;
    }

    @Override
    public String encodeUnicode(final String input) {
        if (input != null) {
            String output = input;
            output = output.replace(":", "：");
            output = output.replace("|", "｜");
            output = output.replace("<", "＜");
            output = output.replace(">", "＞");
            output = output.replace("/", "⁄");
            output = output.replace("\\", "∖");
            output = output.replace("*", "＊");
            output = output.replace("?", "？");
            output = output.replace("!", "！");
            output = output.replace("\"", "”");
            return output;
        }
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}