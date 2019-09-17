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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.Regex;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hqq.tv" }, urls = { "https?://(?:www\\.)?hqq\\.(?:tv|watch)/.+|https?://waaw\\.tv/watch_video\\.php\\?v=[A-Za-z0-9]+" })
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
    private String           hls_downloadurl   = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        final String videoURL;
        if (link.getDownloadURL().matches(".+waaw\\.tv.+")) {
            final String videoID = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
            videoURL = "https://hqq.watch/player/embed_player.php?vid=" + videoID;
        } else {
            videoURL = link.getPluginPatternMatcher();
        }
        getPage(videoURL);
        final String url_name = new Regex(videoURL, "https?://[^/]+/(.+)").getMatch(0);
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        /* Set this temporary name so that offline URLs have 'ok'-names. */
        link.setName(url_name);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String jsWise = br.getRegex(";eval\\((function\\(w,i,s,e.+?)\\);\\s*</").getMatch(0);
        String decode = decodeWise(jsWise);
        String[] data = new Regex(decode, "var vid=\"([^\"]*).*?var at=\"([^\"]*).*?var autoplayed=\"([^\"]*).*?var referer=\"([^\"]*).*var http_referer=\"([^\"]*).*var pass=\"([^\"]*).*var embed_from=\"([^\"]*).*var need_captcha=\"([^\"]*).*var hash_from=\"([^\"]*)").getRow(0);
        if (data == null || data.length == 0) {
            data = new Regex(decode, "vid=([^&]*)&at=([^&]*)&autoplayed=([^&]*)&referer=([^&]*)&http_referer=([^&]*)&pass=([^&]*)&embed_from=([^&]*)&need_captcha=[^&]*&hash_from=([^&]*)").getRow(0);
        }
        if (data == null || data.length == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Browser ajax = br.cloneBrowser();
        // ip
        getPage(ajax, "//hqq.watch/player/ip.php?type=json&rand=" + System.currentTimeMillis() / 1000L);
        final String ip = PluginJSonUtils.getJsonValue(ajax, "ip");
        final String needCaptcha = PluginJSonUtils.getJsonValue(ajax, "need_captcha");
        final String sitekey = new Regex(decode, "grecaptcha.execute\\('([^']+)'").getMatch(0);
        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, this.br, sitekey) {
            @Override
            public TYPE getType() {
                return TYPE.INVISIBLE;
            }
        }.getToken();
        String vid = data[0];
        String at = data[1];
        StringBuffer sb = new StringBuffer();
        sb.append("/sec/player/embed_player_");
        sb.append(System.currentTimeMillis() / 1000L);
        sb.append(".php?iss=");
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
        sb.append(needCaptcha);
        sb.append("&hash_from=");
        sb.append(data[7]);
        sb.append("&secured=0&gtoken=");
        sb.append(recaptchaV2Response);
        // html
        getPage(sb.toString());
        final Form myForm = br.getFormbyProperty("id", "my_form");
        if (myForm == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        submitForm(myForm); // duplicate URL parameters.
        //
        final String filename = br.getRegex("title\":\\s*\"([^\"]+)").getMatch(0);
        if (filename != null) {
            link.setFinalFileName(filename + ".mp4");
        }
        String[] escapedString = br.getRegex("document.write\\(unescape\\(\"([^\"]*)").getColumn(0);
        StringBuffer unescapedString = new StringBuffer();
        for (String es : escapedString) {
            unescapedString.append(Encoding.htmlDecode(es));
        }
        String[] keyNames = new Regex(unescapedString.toString(), "link_(\\d+): ([^,]+), server_(\\d+): ([^,]+),").getRow(0);
        jsWise = br.getRegex(";eval\\((function\\(w,i,s,e.+?)\\);\\s*</").getMatch(0);
        if (jsWise == null) {
            /* 2017-07-18 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (keyNames == null || keyNames.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decode = decodeWise(jsWise);
        String linkl = new Regex(decode, "var " + keyNames[1] + " = \"([^\"]+)").getMatch(0);
        String serverl = new Regex(decode, "var " + keyNames[3] + " = \"([^\"]+)").getMatch(0);
        sb = new StringBuffer();
        sb.append("/player/get_md5.php?at=");
        sb.append(at);
        sb.append("&adb=0%2F&b=1");
        sb.append(String.format("&link_%s=", keyNames[0]));
        sb.append(linkl);
        sb.append(String.format("&server_%s=", keyNames[2]));
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
        hls_downloadurl = decodeLink(obfLink);
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
        if (hls_downloadurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        /* 404 errors might happen here for broken/offline streams */
        dl = new HLSDownloader(downloadLink, br, hls_downloadurl);
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