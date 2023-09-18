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
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.requests.PutRequest;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.FromsmashComFolder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FromsmashCom extends PluginForHost {
    public FromsmashCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://en.fromsmash.com/terms-policies";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fromsmash.com" });
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
            ret.add("https?://(?:[\\w\\-]+\\.)?" + buildHostsPatternPart(domains) + "/([^/]+)#fileid=([A-Za-z0-9\\-_]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                       = true;
    private static final int     FREE_MAXCHUNKS                    = 0;
    private static final int     FREE_MAXDOWNLOADS                 = 20;
    public static final String   PROPERTY_DIRECTURL                = "directurl";
    public static final String   PROPERTY_STATIC_DOWNLOAD_PASSWORD = "static_download_password";

    @Override
    public String getLinkID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null) {
            return this.getHost() + "://" + getFolderID(link) + "_" + getFileID(link);
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFolderID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        }
    }

    private String getFileID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        }
    }

    private String getRegion(final DownloadLink link) {
        return link.getStringProperty("region", "transfer.eu-central-1.fromsmash.co");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String directurl = link.getStringProperty(PROPERTY_DIRECTURL);
        if (directurl != null) {
            final String expireTimestampStr = UrlQuery.parse(directurl).get("Expires");
            final long expireTimestamp = Long.parseLong(expireTimestampStr) * 1000;
            if (expireTimestamp > System.currentTimeMillis()) {
                /* Trust given timestamp */
                return AvailableStatus.TRUE;
            } else {
                logger.info("Directurl needs to be refreshed");
            }
        }
        logger.info("Obtaining fresh directurl");
        final Browser brc = br.cloneBrowser();
        final String token = FromsmashComFolder.getToken(this, brc);
        brc.getHeaders().put("Authorization", "Bearer " + token);
        if (link.hasProperty(PROPERTY_STATIC_DOWNLOAD_PASSWORD)) {
            FromsmashComFolder.setPasswordHeader(brc, link.getStringProperty(PROPERTY_STATIC_DOWNLOAD_PASSWORD));
        }
        FromsmashComFolder.prepBR(brc);
        final String region = getRegion(link);
        final PutRequest put = new PutRequest("https://" + region + "/transfer/" + getFolderID(link) + "/urls?version=07-2020");
        final String reqData = "{\"files\":[{\"id\":\"" + getFileID(link) + "\"}]}";
        put.setPostBytes(reqData.getBytes());
        brc.getPage(put);
        if (brc.getHttpConnection().getResponseCode() == 404) {
            /*
             * E.g. {"code":404,"error":"Transfer <folderID> not found","requestId":"<someHash>","details":{"name":"Transfer","primary":
             * "<folderID>"}}
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
        /* Valid for 10 minutes, IP-independent! */
        final String freshDirecturl = JavaScriptEngineFactory.walkJson(entries, "transfer/files/{0}/url").toString();
        if (StringUtils.isEmpty(freshDirecturl)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(PROPERTY_DIRECTURL, freshDirecturl);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(dllink)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to file");
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
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