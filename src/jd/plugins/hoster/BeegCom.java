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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "beeg.com" }, urls = { "https?://(?:www\\.)?beeg\\.com/((?!section|static|tag)[a-z0-9\\-]+/[a-z0-9\\-]+|\\d+)" })
public class BeegCom extends PluginForHost {
    /* DEV NOTES */
    /* Porn_plugin */
    private String DLLINK = null;

    public BeegCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://beeg.com/contacts/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String INVALIDLINKS = "http://(www\\.)?beeg\\.com/generator.+";
    private boolean             server_issue = false;

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fuid = this.getFID(link);
        if (fuid != null) {
            /*
             * 2019-08-16: Users may sometimes add URLs which are offline via browser but work fine in JD so let's correct these URLs so
             * that if the user copies them inside JD, they will work fine via browser too!
             */
            link.setContentUrl("https://" + this.getHost() + "/" + fuid);
            link.setLinkID(this.getHost() + "://" + fuid);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        server_issue = false;
        final String videoid = getFID(link);
        if (link.getDownloadURL().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        String[] match = br.getRegex("script src=\"([^\"]+/(\\d+)\\.js)").getRow(0);
        String jsurl = null;
        String beegVersion = null;
        if (match != null) {
            jsurl = match[0];
            beegVersion = match[1];
        }
        /* 2019-01-16: Salt is not always given/required */
        String salt = null;
        if (beegVersion == null) {
            beegVersion = br.getRegex("var beeg_version = (\\d+);").getMatch(0);
        }
        if (beegVersion == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (jsurl != null) {
            final Browser cbr = br.cloneBrowser();
            cbr.getPage(jsurl);
            salt = cbr.getRegex("beeg_salt=\"([^\"]+)").getMatch(0);
            if (salt == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        LinkedHashMap<String, Object> entries;
        /* 2019-07-16: This basically loads the whole website - we then need to find the element the user wants to download. */
        br.getPage("//beeg.com/api/v6/" + beegVersion + "/index/main/0/pc");
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("videos");
        boolean useApiV1 = true;
        for (final Object videoO : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoO;
            final String videoidTemp = Long.toString(JavaScriptEngineFactory.toLong(entries.get("svid"), -1));
            final String videoidTemp2 = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), -1));
            if ((videoidTemp != null && videoid.equals(videoidTemp)) || (videoidTemp2 != null && videoid.equals(videoidTemp2))) {
                useApiV1 = false;
                break;
            }
        }
        if (!useApiV1) {
            /* Example v2video: 1059800872 */
            if (!entries.containsKey("start") && !entries.containsKey("end")) {
                /* 2019-08-14: They can be null but they should be present in their json! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String get_parameters = "?v=2";
            final long v_start = JavaScriptEngineFactory.toLong(entries.get("start"), -1);
            final long v_end = JavaScriptEngineFactory.toLong(entries.get("end"), -1);
            if (v_start > -1 && v_end > -1) {
                /* 2019-07-16: These values are required to call the API to access single objects! */
                get_parameters += "&s=" + v_start + "&e=" + v_end;
            }
            br.getPage("/api/v6/" + beegVersion + "/video/" + videoid + get_parameters);
            if (br.getHttpConnection().getResponseCode() == 404) {
                /* 2019-08-20: Ultimate fallback */
                logger.info("Video is offline according to APIv2 --> Trying APIv1");
                useApiV1 = true;
            } else {
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            }
        }
        if (useApiV1) {
            /* Example v1video: 6471530 */
            logger.info("Failed to find extra data for desired content --> Falling back to apiv1");
            br.getPage("/api/v6/" + beegVersion + "/video/" + videoid + "?v=1");
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = (String) entries.get("title");
        final String[] qualities = { "2160", "1080", "720", "480", "360", "240" };
        for (final String quality : qualities) {
            DLLINK = (String) entries.get(quality + "p");
            if (DLLINK != null) {
                break;
            }
        }
        if (filename == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (DLLINK.startsWith("//")) {
            DLLINK = "https:" + DLLINK;
        }
        DLLINK = DLLINK.replace("{DATA_MARKERS}", "data=pc.XX");
        final String key = new Regex(this.DLLINK, "/key=([^<>\"=]+)%2Cend=").getMatch(0);
        if (key != null && salt != null) {
            String deckey = decryptKey(key, salt);
            DLLINK = DLLINK.replace(key, deckey).replace("%2C", ",");
        }
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".flv";
        }
        filename = filename.trim();
        if (filename.endsWith(".")) {
            filename = filename.substring(0, filename.length() - 1);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        br.setFollowRedirects(true);
        br.getHeaders().put("Referer", link.getDownloadURL());
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (con.isOK() && !con.getContentType().contains("html") && !con.getContentType().contains("text")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                server_issue = true;
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issue) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server issue", 30 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html") || dl.getConnection().getContentType().contains("text")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String decryptKey(final String key, final String salt) {
        String decodeKey = Encoding.htmlDecode(key);
        int s = salt.length();
        StringBuffer t = new StringBuffer();
        for (int o = 0; o < decodeKey.length(); o++) {
            char l = decodeKey.charAt(o);
            int n = o % s;
            int i = salt.charAt(n) % 21;
            t.append(String.valueOf(Character.toChars(l - i)));
        }
        String result = t.toString();
        result = strSplitReverse(result, 3, true);
        return result;
    }

    private String strSplitReverse(final String key, final int e, final boolean t) {
        String n = key;
        StringBuffer r = new StringBuffer();
        if (t) {
            int a = n.length() % e;
            if (a > 0) {
                r.append(new StringBuffer(n.substring(0, a)).reverse());
                n = n.substring(a);
            }
        }
        for (; n.length() > e;) {
            r.append(new StringBuffer(n.substring(0, e)).reverse());
            n = n.substring(e);
        }
        r.append(new StringBuffer(n).reverse());
        return r.reverse().toString();
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