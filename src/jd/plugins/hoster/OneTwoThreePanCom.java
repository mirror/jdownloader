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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.OneTwoThreePanComFolder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { OneTwoThreePanComFolder.class })
public class OneTwoThreePanCom extends PluginForHost {
    public OneTwoThreePanCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://www.123pan.com/UserAgreement";
    }

    private static List<String[]> getPluginDomains() {
        return OneTwoThreePanComFolder.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/s/([A-Za-z0-9\\-_]+)(#fileID=(\\d+))?");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean      FREE_RESUME             = true;
    private final int          FREE_MAXCHUNKS          = 0;
    private final int          FREE_MAXDOWNLOADS       = -1;
    // private final boolean ACCOUNT_FREE_RESUME = true;
    // private final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private final int ACCOUNT_FREE_MAXDOWNLOADS = -1;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private final int ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    public static final String PROPERTY_FILENAME       = "webapifilename";
    public static final String PROPERTY_ETAG           = "etag";
    public static final String PROPERTY_S3KEYFLAG      = "s3keyflag";
    public static final String PROPERTY_SIZEBYTES      = "sizebytes";
    public static final String PROPERTY_PARENT_FILE_ID = "parent_file_id";
    public static final String PROPERTY_DIRECTURL      = "directurl";

    @Override
    public String getLinkID(final DownloadLink link) {
        return this.getHost() + "://" + getShareKey(link) + "_" + getFileID(link);
    }

    private String getShareKey(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getFileID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        final String shareKey = getShareKey(link);
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(shareKey + "_ " + getFileID(link));
        }
        this.setBrowserExclusive();
        final String etag = link.getStringProperty(PROPERTY_ETAG);
        final String s3keyflag = link.getStringProperty(PROPERTY_S3KEYFLAG);
        final long sizebytes = link.getLongProperty(PROPERTY_SIZEBYTES, -1);
        if (etag == null || s3keyflag == null || sizebytes == -1) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fileidStr = getFileID(link);
        final Object fileidO;
        if (fileidStr.matches("\\d+")) {
            fileidO = Integer.parseInt(fileidStr);
        } else {
            fileidO = fileidStr;
        }
        final Map<String, Object> postdata = new HashMap<String, Object>();
        postdata.put("Etag", etag);
        postdata.put("FileID", fileidO);
        postdata.put("S3keyFlag", s3keyflag);
        postdata.put("ShareKey", shareKey);
        postdata.put("Size", sizebytes);
        final boolean tryExperimentalAPIRequest = false;
        if (tryExperimentalAPIRequest && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* 2023-11-20: Testing as some parts of their website were changed. */
            br.getHeaders().put("App-Version", "3");
            br.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
            br.getHeaders().put("Platform", "web"); // Important! (even case-sensitive!)
            br.getHeaders().put("Origin", "https://www." + this.getHost());
            // br.getHeaders().put("Loginuuid", "\"<someHash>\"");
            // br.postPageRaw(OneTwoThreePanComFolder.API_BASE_2 + "/share/download/info", JSonStorage.serializeToJson(postdata));
            /*
             * TODO: Some ID / signature is missing here, leading to this response: "message":"签名错误,请检查您的本地时间是否为东八区时间-dykey illegality 1001"
             */
            br.postPageRaw("https://www.123pan.com/b/api/share/download/info?" + System.currentTimeMillis(), JSonStorage.serializeToJson(postdata));
        } else {
            String url = OneTwoThreePanComFolder.API_BASE + "/share/download/info?";
            url += System.currentTimeMillis() + "=" + System.currentTimeMillis();
            br.postPageRaw(url, JSonStorage.serializeToJson(postdata));
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Object dataO = entries.get("data");
        final Map<String, Object> data = dataO instanceof Map ? (Map<String, Object>) dataO : null;
        if (data == null) {
            /* E.g. {"code":400,"message":"非法请求,源文件不存在","data":null} */
            // TODO: 2023-11-20: Update errorhandling
            final int code = Integer.parseInt(entries.get("code").toString());
            final String message = entries.get("message").toString();
            if (code == 429) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 429 rate limit reached", 3 * 60 * 1000l);
            } else if (code == 5112) {
                /* Account required to download this file */
                throw new AccountRequiredException();
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, message);
            }
        }
        final String url = data.get("DownloadURL").toString();
        if (StringUtils.isEmpty(url)) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(PROPERTY_DIRECTURL, url);
        final String directurl = getStoredDirecturl(link);
        final String filenameFromWebAPI = link.getStringProperty(PROPERTY_FILENAME);
        final UrlQuery query2 = UrlQuery.parse(directurl);
        final String filenameFromURL = query2.get("filename");
        if (filenameFromURL != null) {
            link.setFinalFileName(Encoding.htmlDecode(filenameFromURL).trim());
        } else {
            link.setFinalFileName(filenameFromWebAPI);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS);
    }

    public static void setEtag(final DownloadLink link, final String etag) {
        link.setProperty(PROPERTY_ETAG, etag);
        link.setMD5Hash(etag);
    }

    private String getStoredDirecturl(final DownloadLink link) throws MalformedURLException {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        final UrlQuery query = UrlQuery.parse(url);
        final String urlBase64 = query.get("params");
        if (urlBase64 != null) {
            return Encoding.Base64Decode(urlBase64);
        } else {
            /* This might potentially lead to a failure. */
            logger.warning("Couldn't find expected base64 String inside stored DownloadUrl");
            return url;
        }
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks) throws Exception, PluginException {
        final String storedDirecturl = getStoredDirecturl(link);
        String dllink;
        if (storedDirecturl != null) {
            dllink = storedDirecturl;
        } else {
            requestFileInformation(link, true);
            dllink = getStoredDirecturl(link);
            if (StringUtils.isEmpty(dllink)) {
                /* This should never happen! */
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(PROPERTY_DIRECTURL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired?", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
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