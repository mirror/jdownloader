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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PixeldrainCom extends PluginForHost {
    public PixeldrainCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://pixeldrain.com/about";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pixeldrain.com", "pixeldra.in" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:api/file/|u/)?([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fid = this.getFID(link);
        if (fid != null) {
            final String newURL = "https://" + this.getHost() + "/u/" + fid;
            link.setPluginPatternMatcher(newURL);
            link.setContentUrl(newURL);
        }
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;
    public static final String   API_BASE          = "https://pixeldrain.com/api";

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    /** Using API according to https://pixeldrain.com/api */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBR(this.br);
        /* 2020-04-14: According to API docs, multiple files can be checked with one request but I was unable to make this work. */
        br.getPage(API_BASE + "/file/" + this.getFID(link) + "/info");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /*
             * 2020-04-14: E.g. {"success":false,"value":"not_found","message":"The entity you requested could not be found"}
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> data = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        setDownloadLinkInfo(link, data);
        return AvailableStatus.TRUE;
    }

    public static void setDownloadLinkInfo(final DownloadLink link, final Map<String, Object> data) {
        final boolean success = ((Boolean) data.get("success")).booleanValue();
        if (!success) {
            link.setAvailable(false);
        } else {
            link.setAvailable(true);
            final String filename = (String) data.get("name");
            final long filesize = JavaScriptEngineFactory.toLong(data.get("size"), 0);
            if (!StringUtils.isEmpty(filename)) {
                link.setFinalFileName(filename);
            }
            if (filesize > 0) {
                link.setVerifiedFileSize(filesize);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        /* 2020-04-14: Content-type check is not necessary, we rely on content-disposition only. */
        // final String expected_content_type = PluginJSonUtils.getJson(br, "mime_type");
        final String dllink = API_BASE + "/file/" + this.getFID(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        // final String real_content_type = dl.getConnection().getContentType();
        // final boolean content_type_mismatch = expected_content_type != null && real_content_type != null &&
        // !real_content_type.equalsIgnoreCase(expected_content_type);
        // if (!dl.getConnection().isContentDisposition() || content_type_mismatch) {
        if (!dl.getConnection().isContentDisposition()) {
            if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.followConnection();
            /*
             * 2020-04-14: E.g. {"success": false,"value": "file_rate_limited_captcha_required","message":
             * "This file is using too much bandwidth. For anonymous downloads a captcha is required now. The captcha entry is available on the download page"
             * } ------> Was never able to get this
             */
            final String msg = PluginJSonUtils.getJson(br, "message");
            if (!StringUtils.isEmpty(msg)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        /* 2020-04-14: No captcha at all */
        return false;
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