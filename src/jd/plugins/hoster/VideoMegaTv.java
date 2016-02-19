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

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogJob;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashInfo.TYPE;
import jd.plugins.download.HashResult;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "videomega.tv" }, urls = { "http://(www\\.)?videomega\\.tv/(?:(?:(?:iframe|cdn|view)\\.php)?\\?ref=|validatehash\\.php\\?hashkey=)[A-Za-z0-9]+" }, flags = { 0 })
public class VideoMegaTv extends antiDDoSForHost {

    public VideoMegaTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://videomega.tv/terms.html";
    }

    private static final String TYPE_HASH   = "http://(www\\.)?videomega\\.tv/(?:view\\.php\\?ref=|validatehash\\.php\\?hashkey=)[A-Za-z0-9]+";
    private static final String TYPE_NORMAL = "http://(www\\.)?videomega\\.tv/(?:(?:iframe|cdn)\\.php)?\\?ref=[A-Za-z0-9]+";

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final String fuid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        if (link.getDownloadURL().matches(TYPE_HASH)) {
            link.setLinkID(getHost() + "://hash:" + fuid);
        } else {
            link.setLinkID(getHost() + "://id:" + fuid);
        }
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private String               fuid              = null;
    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        if (link.getBooleanProperty("offlineByHash", false)) {
            return AvailableStatus.FALSE;
        }
        br = new Browser();
        fuid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        if (link.getDownloadURL().matches(TYPE_HASH)) {
            // lets set all referrer to home page
            br.getHeaders().put("Referer", "http://videomega.tv/");
            getPage("http://videomega.tv/iframe.php?ref=" + fuid + "&width=800&height=400");
            if (br.getHttpConnection().getResponseCode() == 404 | br.containsHTML("<source src=\"http:///v/.mp4?")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            br.getHeaders().put("Referer", "http://videomega.tv/");
            final String page = "http://videomega.tv/?ref=" + fuid + "&width=800&height=400";
            getPage(page);
            String redirect = br.getRedirectLocation();
            if (redirect != null) {
                if (redirect.contains("google.com/")) {
                    // without referer it will most likely redirect to google
                    br.getHeaders().put("Referer", page);
                    getPage(page);
                    redirect = br.getRedirectLocation();
                }
                if (!redirect.contains("videomega.tv/")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            if (br.containsHTML(">VIDEO NOT FOUND")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String fileName = br.getRegex("class=\"center\">Videomega\\.tv - (.*?)</div").getMatch(0);
        if (link.getFinalFileName() == null) {
            if (StringUtils.isEmpty(fileName)) {
                fileName = br.getRegex("<title>Videomega\\.tv - ((?!Disable adblock|Host and share your videos free, subtitles supported).*?)</title").getMatch(0);
            }
            if (StringUtils.isNotEmpty(fileName)) {
                link.setFinalFileName(fileName + ".mp4");
            } else {
                link.setFinalFileName(fuid + ".mp4");
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        String dllink = null;
        requestFileInformation(downloadLink);
        // cdn
        Browser js = br.cloneBrowser();
        js.getHeaders().put("Accept", "*/*");
        getPage(js, "/cdn.js");
        final String javascript = br.getRegex("<script[^\r\n]+ref=\"" + fuid + ".*?</script>").getMatch(-1);
        final String width = new Regex(javascript, "width=\"(\\d+)%?\"").getMatch(0);
        final String height = new Regex(javascript, "height=\"(\\d+)%?\"").getMatch(0);
        getPage("/view.php?ref=" + fuid + "&width=" + (width == null ? "800" : width) + "&height=" + (height == null ? "400" : height));
        if (br.containsHTML(">Sorry an error has occurred converting this video\\.<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster issue converting video.", 30 * 60 * 1000l);
        }
        String[] escaped = br.getRegex("document\\.write\\(unescape\\(\"([^<>\"]*?)\"").getColumn(0);
        if (escaped != null && escaped.length > 0) {
            /* Old way */
            for (String escape : escaped) {
                Browser br2 = br.cloneBrowser();
                escape = Encoding.htmlDecode(escape);
                dllink = new Regex(escape, "file:\\s*\"(https?://[^<>\"]*?)\"").getMatch(0);
                if (inValidateHost(dllink)) {
                    dllink = new Regex(escape, "\"(https?://([a-z0-9]+\\.){1,}videomega\\.tv/vid(?:eo)?s/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+\\.mp4)\"").getMatch(0);
                }
                if (inValidateHost(dllink)) {
                    if (!escaped[escaped.length - 1].equals(escape)) {
                        // this tests if link is last in array
                        continue;
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                try {
                    dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
                } catch (final Exception t) {
                    if (!escaped[escaped.length - 1].equals(escape)) {
                        // this tests if link is last in array
                        continue;
                    }
                    throw t;
                }
                if (dl.getConnection().getContentType().contains("html")) {
                    if (!escaped[escaped.length - 1].equals(escape)) {
                        // this tests if link is last in array
                        continue;
                    }
                    handleServerErrors();
                    br2.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl.startDownload();
                break;
            }
        } else {
            /* New way 20160121 */
            Boolean hasLinkButInvalid = null;
            {
                // packed
                String packed = br.getRegex("eval\\s*\\((function\\(p,a,c,k,e,d\\).*?\\}{2}.*?\\))\\)").getMatch(0);
                if (packed != null) {
                    final ScriptEngineManager manager = jd.plugins.hoster.DummyScriptEnginePlugin.getScriptEngineManager(null);
                    final ScriptEngine engine = manager.getEngineByName("javascript");
                    String result = null;
                    try {
                        engine.eval("var res = " + packed);
                        result = (String) engine.get("res");
                    } catch (final Exception e) {
                    }
                    dllink = new Regex(result, "(\"|')(https?://.*?\\.mp4.*?)\\1").getMatch(1);
                    hasLinkButInvalid = inValidateHost(dllink);
                }
            }
            if (inValidateHost(dllink)) {
                dllink = br.getRegex("<source src=\"(http://[^<>\"]*?)\"").getMatch(0);
                hasLinkButInvalid = inValidateHost(dllink);
                if (Boolean.TRUE.equals(hasLinkButInvalid)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (inValidateHost(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            // this is needed
            String ran = "";
            while (ran.length() < 12) {
                ran = new Random().nextLong() + "";
            }
            ran = ran.substring(1, 11);
            br.setCookie(this.getHost(), "_ga", "GA1.2.62223824" + ran + "." + System.currentTimeMillis());
            br.setCookie(this.getHost(), "_gat", "1");
            Browser adbr = br.cloneBrowser();
            adbr.getHeaders().put("Accept", "*/*");
            final String[] adlinks = br.getRegex("\"(/[A-Z0-9]+/ad\\.php[^<>\"]*?)\"").getColumn(0);
            final HashSet<String> dupe = new HashSet<String>();
            if (adlinks != null && adlinks.length > 0) {
                getPage(adbr, adlinks[0]);
                adbr = br.cloneBrowser();
                adbr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                getPage(adbr, adlinks[0]);
                adbr = br.cloneBrowser();
                adbr.getHeaders().put("Accept", "*/*");
                getPage(adbr, adlinks[0]);
            }
        }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Encoding", "identity;q=1, *;q=0");
        // purge unexpected cookies
        final Cookies cookies = br.getCookies(this.getHost());
        for (final Cookie c : cookies.getCookies()) {
            if (!c.getKey().matches("__cfduid|_ga.*")) {
                br.getCookies(this.getHost()).remove(c);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
        // the following determine is its empty container.
        if (dl.getDownloadable().getDownloadTotalBytes() == 80896) {
            final Downloadable downloadable = dl.getDownloadable();
            final File file = new File(downloadable.getFileOutput());
            if (file.exists()) {
                final HashInfo hashInfo = new HashInfo("808bb651cc72adc7f91fb443bf11d3fa", TYPE.MD5);
                final HashResult result = downloadable.getHashResult(hashInfo, file);
                if (result.match()) {
                    // set as offline
                    downloadLink.setProperty("offlineByHash", true);
                    // delete method
                    downloadLink.getDownloadLinkController().getJobsAfterDetach().add(new DownloadWatchDogJob() {

                        @Override
                        public void interrupt() {
                        }

                        @Override
                        public void execute(DownloadSession currentSession) {
                            final ArrayList<DownloadLink> delete = new ArrayList<DownloadLink>();
                            delete.add(downloadLink);
                            DownloadWatchDog.getInstance().delete(delete, null);
                        }
                    });
                    // not set as offline! have to throw exception!!
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
        }
    }

    private void handleServerErrors() throws PluginException {
        if (dl.getConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
        } else if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (dl.getConnection().getLongContentLength() < 10000l) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: File is too small", 60 * 60 * 1000l);
        }
    }

    protected boolean inValidateHost(final String s) {
        if (inValidate(s)) {
            return true;
        }
        try {
            InetAddress.getAllByName(new URL(s).getHost());
        } catch (Throwable t) {
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("offlineByHash", Property.NULL);
    }

}