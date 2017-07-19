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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.Regex;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision: 35559 $", interfaceVersion = 3, names = { "hqq.tv" }, urls = { "https?://(?:www\\.)?hqq\\.(?:tv|watch)/.+" })
public class HqqTv extends antiDDoSForHost {
    public HqqTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://hqq.tv";
    }

    /* Connection stuff */
    private static final int FREE_MAXDOWNLOADS = 1;
    private String           hls_master        = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String jsWise = br.getRegex(";eval\\((function\\(w,i,s,e.+?)\\);\\s*</").getMatch(0);
        String decode = decodeWise(jsWise);
        String[] data = new Regex(decode, "var vid=\"([^\"]*).*?var at=\"([^\"]*).*?var autoplayed=\"([^\"]*).*?var referer=\"([^\"]*).*var http_referer=\"([^\"]*).*var pass=\"([^\"]*).*var embed_from=\"([^\"]*).*var need_captcha=\"([^\"]*).*var hash_from=\"([^\"]*)").getRow(0);
        if (data == null || data.length == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Browser ajax = br.cloneBrowser();
        // ip
        getPage(ajax, "//hqq.watch/player/ip.php?type=json");
        String ip = PluginJSonUtils.getJsonValue(ajax, "ip");
        String ipBlacklist = PluginJSonUtils.getJsonValue(ajax, "ip_blacklist");
        String vid = data[0];
        String at = data[1];
        StringBuffer sb = new StringBuffer();
        sb.append("/sec/player/embed_player.php?iss=");
        sb.append(ip);
        sb.append("&vid=");
        sb.append(vid);
        sb.append("&at=");
        sb.append(at);
        sb.append("&autoplayed=");
        sb.append(data[2]);
        sb.append("&referer=");
        sb.append(data[3]);
        sb.append("&http_referer=");
        sb.append(data[4]);
        sb.append("&pass=");
        sb.append(data[5]);
        sb.append("&embed_from=");
        sb.append(data[6]);
        sb.append("&need_captcha=");
        if (ipBlacklist.equals("1")) {
            sb.append("1");
        } else {
            sb.append(data[7]);
        }
        sb.append("&hash_from=");
        sb.append(data[8]);
        // html
        getPage(sb.toString());
        //
        String filename = br.getRegex("title\":\\s*\"([^\"]+)").getMatch(0);
        link.setFinalFileName(filename + ".mp4");
        String[] escapedString = br.getRegex("document.write\\(unescape\\(\"([^\"]*)").getColumn(0);
        StringBuffer unescapedString = new StringBuffer();
        for (String es : escapedString) {
            unescapedString.append(Encoding.htmlDecode(es));
        }
        String[] keyNames = new Regex(unescapedString.toString(), "link_1: ([^,]+), server_1: ([^,]+),").getRow(0);
        jsWise = br.getRegex(";eval\\((function\\(w,i,s,e.+?)\\);\\s*</").getMatch(0);
        if (jsWise == null) {
            /* 2017-07-18 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        decode = decodeWise(jsWise);
        String linkl = new Regex(decode, "var " + keyNames[0] + " = \"([^\"]+)").getMatch(0);
        String serverl = new Regex(decode, "var " + keyNames[1] + " = \"([^\"]+)").getMatch(0);
        sb = new StringBuffer();
        sb.append("/player/get_md5.php?at=");
        sb.append(at);
        sb.append("&adb=0%2F&b=1&link_1=");
        sb.append(linkl);
        sb.append("&server_1=");
        sb.append(serverl);
        sb.append("&vid=");
        sb.append(vid);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // json
        getPage(sb.toString());
        String obfLink = PluginJSonUtils.getJsonValue(br, "obf_link");
        if (obfLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        hls_master = decodeLink(obfLink);
        return AvailableStatus.TRUE;
    }

    private String decodeLink(final String obfLink) {
        String result = obfLink;
        if (obfLink.indexOf('.') == -1) {
            String obfLink2 = obfLink.substring(1);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < obfLink2.length(); i += 3) {
                sb.append("%u0");
                sb.append(obfLink2.substring(i, i + 3));
            }
            result = Encoding.unicodeDecode(sb.toString());
        }
        return result;
    }

    private String decodeWise(final String s) {
        String src = s;
        String result = null;
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval("var result=" + src);
            String jsDecode = engine.get("result").toString();
            String jsWise = new Regex(jsDecode, ";eval\\((function\\(w,i,s,e.+)\\)").getMatch(0);
            engine.eval("var result=" + jsWise);
            jsDecode = engine.get("result").toString();
            jsWise = new Regex(jsDecode, ";;\\s*;eval\\((function\\(w,i,s,e.+)\\)").getMatch(0);
            engine.eval("var result=" + jsWise);
            result = engine.get("result").toString();
        } catch (final Exception e) {
            logger.info(e.toString());
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // br.getPage(hls_master);
        // final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        // if (hlsbest == null) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        final String url_hls = hls_master; // hlsbest.getDownloadurl();
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, url_hls);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}