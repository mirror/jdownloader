//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.http.Authentication;
import jd.http.AuthenticationFactory;
import jd.http.Browser;
import jd.http.CallbackAuthenticationFactory;
import jd.http.Cookies;
import jd.http.DefaultAuthenticanFactory;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.URLUserInfoAuthentication;
import jd.http.requests.GetRequest;
import jd.http.requests.HeadRequest;
import jd.nutils.SimpleFTP;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.download.Downloadable;
import jd.utils.locale.JDL;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.jdownloader.auth.AuthenticationController;
import org.jdownloader.auth.AuthenticationInfo;
import org.jdownloader.auth.AuthenticationInfo.Type;
import org.jdownloader.auth.Login;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

/**
 * TODO: remove after next big update of core to use the public static methods!
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "DirectHTTP", "http links" }, urls = { "directhttp://.+", "https?viajd://[\\w\\.:\\-@]*/.*\\.((jdeatme|3gp|7zip|7z|abr|ac3|ace|aiff|aifc|aif|ai|au|avi|apk|bin|bmp|bat|bz2|cbr|cab|cbz|ccf|chm|cr2|cso|cue|cpio|cvd|c\\d{2,4}|dta|deb|divx|djvu|dlc|dmg|doc|docx|dot|dx2|eps|epub|exe|ff|flv|flac|f4v|gsd|gif|gpg|gz|hqx|iwd|idx|iso|ipa|ipsw|java|jar|jpe?g|load|lha|lzh|m2ts|m4v|m4a|md5|mkv|mp2|mp3|mp4|mobi|mov|movie|mpeg|mpe|mpg|mpq|msi|msu|msp|mv|mws|nfo|npk|oga|ogg|ogv|otrkey|par2|pak|pkg|png|pdf|pptx?|ppsx?|ppz|pot|psd|ps|qt|rmvb|rm|rar|ra|rev|rnd|rpm|run|rsdf|reg|rtf|shnf|sh(?!tml)|ssa|smi|sub|srt|snd|sfv|sfx|swf|swc|sid|sit|tar\\.(gz|bz2|xz)|tar|tgz|tiff?|ts|txt|viv|vivo|vob|vtt|webm|webp|wav|wad|wmv|wma|wpt|xla|xls|xpi|xtm|zeno|zip|[r-z]\\d{2}|_[_a-z]{2}|\\d{1,4}$)(\\.\\d{1,4})?(?=\\?|$|\"|\r|\n))" })
public class DirectHTTP extends antiDDoSForHost {
    public static final String ENDINGS               = "\\.(jdeatme|3gp|7zip|7z|abr|ac3|ace|aiff|aifc|aif|ai|au|avi|apk|bin|bmp|bat|bz2|cbr|cab|cbz|ccf|chm|cr2|cso|cue|cpio|cvd|c\\d{2,4}|dta|deb|divx|djvu|dlc|dmg|doc|docx|dot|dx2|eps|epub|exe|ff|flv|flac|f4v|gsd|gif|gpg|gz|hqx|iwd|idx|iso|ipa|ipsw|java|jar|jpe?g|load|lha|lzh|m2ts|m4v|m4a|md5|mkv|mp2|mp3|mp4|mobi|mov|movie|mpeg|mpe|mpg|mpq|msi|msu|msp|mv|mws|nfo|npk|oga|ogg|ogv|otrkey|par2|pak|pkg|png|pdf|pptx?|ppsx?|ppz|pot|psd|ps|qt|rmvb|rm|rar|ra|rev|rnd|rpm|run|rsdf|reg|rtf|shnf|sh(?!tml)|ssa|smi|sub|srt|snd|sfv|sfx|swf|swc|sid|sit|tar\\.(gz|bz2|xz)|tar|tgz|tiff?|ts|txt|viv|vivo|vob|vtt|webm|webp|wav|wad|wmv|wma|wpt|xla|xls|xpi|xtm|zeno|zip|[r-z]\\d{2}|_[_a-z]{2}|\\d{1,4}(?=\\?|$|\"|\r|\n))";
    public static final String NORESUME              = "nochunkload";
    public static final String NOCHUNKS              = "nochunk";
    public static final String FIXNAME               = "fixName";
    public static final String FORCE_NORESUME        = "forcenochunkload";
    public static final String FORCE_NOCHUNKS        = "forcenochunk";
    public static final String TRY_ALL               = "tryall";
    public static final String POSSIBLE_URLPARAM     = "POSSIBLE_GETPARAM";
    public static final String BYPASS_CLOUDFLARE_BGJ = "bpCfBgj";

    @Override
    public ArrayList<DownloadLink> getDownloadLinks(final String data, final FilePackage fp) {
        final ArrayList<DownloadLink> ret = super.getDownloadLinks(data, fp);
        try {
            if (ret != null && ret.size() == 1) {
                String modifiedData = null;
                final boolean isDirect;
                if (data.startsWith("directhttp://")) {
                    isDirect = true;
                    modifiedData = data.replace("directhttp://", "");
                } else {
                    isDirect = false;
                    modifiedData = data.replace("httpsviajd://", "https://");
                    modifiedData = modifiedData.replace("httpviajd://", "http://");
                    modifiedData = modifiedData.replace(".jdeatme", "");
                }
                final DownloadLink link = ret.get(0);
                correctDownloadLink(link);// needed to fixup the returned url
                /* single link parsing in svn/jd2 */
                final String url = link.getDownloadURL();
                final int idx = modifiedData.indexOf(url);
                if (!isDirect && idx >= 0 && modifiedData.length() >= idx + url.length()) {
                    String param = modifiedData.substring(idx + url.length());
                    if (param != null) {
                        param = new Regex(param, "(.*?)(\r|\n|$)").getMatch(0);
                        if (param != null && param.trim().length() != 0) {
                            ret.get(0).setProperty(DirectHTTP.POSSIBLE_URLPARAM, new String(param));
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            this.logger.severe(e.getMessage());
        }
        return ret;
    }

    @Override
    public boolean isValidURL(String url) {
        if (url != null) {
            if (StringUtils.startsWithCaseInsensitive(url, "directhttp")) {
                return true;
            }
            url = url.toLowerCase(Locale.ENGLISH);
            if (url.contains("facebook.com/l.php")) {
                return false;
            }
            if (url.contains("facebook.com/ajax/sharer/")) {
                return false;
            }
            if (url.contains("youtube.com/videoplayback") && url.startsWith("http")) {
                return false;
            }
            if (url.matches(".*?://.*?/.*\\?.*\\.\\d+$")) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isResumeable(DownloadLink link, Account account) {
        if (link != null) {
            if (link.getBooleanProperty(DirectHTTP.NORESUME, false) || link.getBooleanProperty(DirectHTTP.FORCE_NORESUME, false)) {
                return false;
            } else {
                return link.getBooleanProperty(DownloadLink.PROPERTY_RESUMEABLE, true);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    /**
     * TODO: Remove with next major-update!
     */
    public static ArrayList<String> findUrls(final String source) {
        /* TODO: better parsing */
        /* remove tags!! */
        final ArrayList<String> ret = new ArrayList<String>();
        try {
            for (final String link : new Regex(source, "((https?|ftp):((//)|(\\\\\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)(\n|\r|$|<|\")").getColumn(0)) {
                try {
                    new URL(link);
                    if (!ret.contains(link)) {
                        ret.add(link);
                    }
                } catch (final MalformedURLException e) {
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return DirectHTTP.removeDuplicates(ret);
    }

    /**
     * TODO: Remove with next major-update!
     */
    public static ArrayList<String> removeDuplicates(final ArrayList<String> links) {
        final ArrayList<String> tmplinks = new ArrayList<String>();
        if (links == null || links.size() == 0) {
            return tmplinks;
        }
        for (final String link : links) {
            if (link.contains("...")) {
                final String check = link.substring(0, link.indexOf("..."));
                String found = link;
                for (final String link2 : links) {
                    if (link2.startsWith(check) && !link2.contains("...")) {
                        found = link2;
                        break;
                    }
                }
                if (!tmplinks.contains(found)) {
                    tmplinks.add(found);
                }
            } else {
                tmplinks.add(link);
            }
        }
        return tmplinks;
    }

    private String contentType = "";

    public DirectHTTP(final PluginWrapper wrapper) {
        super(wrapper);
        if ("DirectHTTP".equalsIgnoreCase(getHost())) {
            setConfigElements();
        }
    }

    private final String  SSLTRUSTALL        = "SSLTRUSTALL";
    private final boolean SSLTRUSTAL_default = true;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SSLTRUSTALL, JDL.L("plugins.hoster.http.ssltrustall", "Ignore SSL issues?")).setDefaultValue(SSLTRUSTAL_default));
    }

    private boolean isSSLTrustALL() {
        return SubConfiguration.getConfig("DirectHTTP").getBooleanProperty(SSLTRUSTALL, SSLTRUSTAL_default);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().startsWith("directhttp")) {
            link.setUrlDownload(link.getDownloadURL().replaceAll("^directhttp://", ""));
        } else {
            link.setUrlDownload(link.getDownloadURL().replaceAll("httpviajd://", "http://").replaceAll("httpsviajd://", "https://"));
            /* this extension allows to manually add unknown extensions */
            link.setUrlDownload(link.getDownloadURL().replaceAll("\\.jdeatme$", ""));
        }
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public Downloadable newDownloadable(DownloadLink downloadLink, Browser br) {
        final String host = Browser.getHost(downloadLink.getPluginPatternMatcher());
        if (StringUtils.contains(host, "mooo.com")) {
            final Browser brc = br.cloneBrowser();
            brc.setRequest(null);
            return super.newDownloadable(downloadLink, brc);
        } else {
            return super.newDownloadable(downloadLink, br);
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        if (this.requestFileInformation(downloadLink) == AvailableStatus.UNCHECKABLE) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 15 * 60 * 1000l);
        }
        /*
         * replace with br.setCurrentURL(null); in future (after 0.9)
         */
        final Cookies cookies = br.getCookies(getDownloadURL(downloadLink));
        br.setCurrentURL(null);
        br.setRequest(null);
        br.setCookies(getDownloadURL(downloadLink), cookies);
        this.br.getHeaders().put("Accept-Encoding", "identity");
        br.setDefaultSSLTrustALL(isSSLTrustALL());
        /* workaround to clear referer */
        this.br.setFollowRedirects(true);
        this.br.setDebug(true);
        boolean resume = true;
        int chunks = 0;
        if (downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) || downloadLink.getBooleanProperty(DirectHTTP.FORCE_NORESUME, false)) {
            resume = false;
        }
        if (downloadLink.getBooleanProperty(DirectHTTP.NOCHUNKS, false) || downloadLink.getBooleanProperty(DirectHTTP.FORCE_NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        if (downloadLink.getProperty("streamMod") != null) {
            resume = true;
            downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);
        }
        this.setCustomHeaders(this.br, downloadLink);
        if (resume && downloadLink.getVerifiedFileSize() > 0) {
            downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);
        } else {
            downloadLink.setProperty("ServerComaptibleForByteRangeRequest", Property.NULL);
        }
        final long downloadCurrentRaw = downloadLink.getDownloadCurrentRaw();
        if (downloadLink.getProperty(BYPASS_CLOUDFLARE_BGJ) != null) {
            resume = false;
            chunks = 1;
        }
        try {
            if (downloadLink.getStringProperty("post", null) != null) {
                this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getDownloadURL(downloadLink), downloadLink.getStringProperty("post", null), resume, chunks);
            } else {
                this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, getDownloadURL(downloadLink), resume, chunks);
            }
        } catch (final IllegalStateException e) {
            logger.log(e);
            if (StringUtils.containsIgnoreCase(e.getMessage(), "Range Error. Requested bytes=0- Got range: bytes 0-")) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e2) {
                }
                logger.info("Workaround for Cloudflare-Cache transparent image compression!");
                downloadLink.setVerifiedFileSize(-1);
                throw new PluginException(LinkStatus.ERROR_RETRY, null, -1, e);
            }
            throw e;
        }
        if (this.dl.getConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 15 * 60 * 1000l);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (this.dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                    // not stable compatible
                }
            }
        } catch (Exception e) {
            if (e instanceof PluginException) {
                if (((PluginException) e).getLinkStatus() == LinkStatus.ERROR_ALREADYEXISTS) {
                    throw e;
                }
            }
            if (e instanceof SkipReasonException) {
                throw e;
            }
            if (e instanceof InterruptedException) {
                throw e;
            }
            logger.log(e);
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || this.dl.getConnection().getResponseCode() == 400 && this.br.getRequest().getHttpConnection().getHeaderField("server").matches("HFS.+")) {
                if (downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) == false) {
                    /* clear chunkProgress and disable resume(ranges) and retry */
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(DirectHTTP.NORESUME, Boolean.TRUE);
                    throw new PluginException(LinkStatus.ERROR_RETRY, null, -1, e);
                }
            } else if (downloadLink.getLinkStatus().hasStatus(1 << 13)) {
                return;
            } else {
                if (downloadLink.getBooleanProperty(DirectHTTP.NOCHUNKS, false) == false) {
                    if (downloadLink.getDownloadCurrent() > downloadCurrentRaw + (1024 * 1024l)) {
                        throw e;
                    } else {
                        /* disable multiple chunks => use only 1 chunk and retry */
                        downloadLink.setProperty(DirectHTTP.NOCHUNKS, Boolean.TRUE);
                        throw new PluginException(LinkStatus.ERROR_RETRY, null, -1, e);
                    }
                } else if (downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) == false) {
                    boolean disableRanges = false;
                    final long[] progress = downloadLink.getChunksProgress();
                    if (progress != null) {
                        if (progress.length > 1) {
                            /* reset chunkProgress to first chunk and retry */
                            downloadLink.setChunksProgress(new long[] { progress[0] });
                            throw new PluginException(LinkStatus.ERROR_RETRY, null, -1, e);
                        } else {
                            if (downloadLink.getDownloadCurrent() == downloadCurrentRaw) {
                                disableRanges = true;
                            }
                        }
                    } else {
                        disableRanges = true;
                    }
                    if (disableRanges) {
                        /* clear chunkProgress and disable resume(ranges) and retry */
                        downloadLink.setChunksProgress(null);
                        downloadLink.setProperty(DirectHTTP.NORESUME, Boolean.valueOf(true));
                        throw new PluginException(LinkStatus.ERROR_RETRY, null, -1, e);
                    }
                }
            }
            throw e;
        }
    }

    private String customDownloadURL = null;

    private void setDownloadURL(String newURL, DownloadLink link) {
        if (link != null && !StringUtils.equals(link.getDownloadURL(), newURL)) {
            link.setUrlDownload(newURL);
        } else {
            this.customDownloadURL = newURL;
        }
    }

    private boolean hasCustomDownloadURL() {
        return customDownloadURL != null;
    }

    private String getDownloadURL(DownloadLink downloadLink) throws IOException {
        String ret = customDownloadURL;
        if (ret == null) {
            ret = downloadLink.getDownloadURL();
        }
        if (downloadLink.getProperty(BYPASS_CLOUDFLARE_BGJ) != null) {
            ret = URLHelper.parseLocation(new URL(ret), "&bpcfbgj=" + System.nanoTime());
        }
        return ret;
    }

    private URLConnectionAdapter prepareConnection(final Browser br, final DownloadLink downloadLink) throws Exception {
        URLConnectionAdapter urlConnection = null;
        this.setCustomHeaders(br, downloadLink);
        boolean rangeHeader = false;
        try {
            if (downloadLink.getProperty("streamMod") != null) {
                rangeHeader = true;
                br.getHeaders().put("Range", "bytes=" + 0 + "-");
            }
            if (downloadLink.getStringProperty("post", null) != null) {
                urlConnection = openAntiDDoSRequestConnection(br, br.createPostRequest(getDownloadURL(downloadLink), downloadLink.getStringProperty("post", null)));
            } else {
                try {
                    if (!preferHeadRequest || "GET".equals(downloadLink.getStringProperty("requestType", null))) {
                        urlConnection = openAntiDDoSRequestConnection(br, br.createGetRequest(getDownloadURL(downloadLink)));
                    } else if (preferHeadRequest || "HEAD".equals(downloadLink.getStringProperty("requestType", null))) {
                        urlConnection = openAntiDDoSRequestConnection(br, br.createHeadRequest(getDownloadURL(downloadLink)));
                        if (urlConnection.getResponseCode() == 404) {
                            /*
                             * && StringUtils.contains(urlConnection.getHeaderField("Cache-Control"), "must-revalidate") &&
                             * urlConnection.getHeaderField("Via") != null
                             */
                            urlConnection.disconnect();
                            urlConnection = openAntiDDoSRequestConnection(br, br.createGetRequest(getDownloadURL(downloadLink)));
                        } else if (urlConnection.getResponseCode() != 404 && urlConnection.getResponseCode() != 401 && urlConnection.getResponseCode() >= 300) {
                            // no head support?
                            urlConnection.disconnect();
                            urlConnection = openAntiDDoSRequestConnection(br, br.createGetRequest(getDownloadURL(downloadLink)));
                        }
                    } else {
                        urlConnection = openAntiDDoSRequestConnection(br, br.createGetRequest(getDownloadURL(downloadLink)));
                    }
                } catch (final IOException e) {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (preferHeadRequest || "HEAD".equals(downloadLink.getStringProperty("requestType", null))) {
                        /* some servers do not allow head requests */
                        urlConnection = openAntiDDoSRequestConnection(br, br.createGetRequest(getDownloadURL(downloadLink)));
                        downloadLink.setProperty("requestType", "GET");
                    } else {
                        throw e;
                    }
                }
            }
        } finally {
            if (rangeHeader) {
                br.getHeaders().remove("Range");
            }
        }
        return urlConnection;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.GENERIC };
    }

    private boolean preferHeadRequest = true;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        return requestFileInformation(downloadLink, 0);
    }

    private AvailableStatus requestFileInformation(final DownloadLink downloadLink, int retry) throws Exception {
        if (downloadLink.getBooleanProperty("OFFLINE", false) || downloadLink.getBooleanProperty("offline", false)) {
            // used to make offline links for decrypters. To prevent 'Checking online status' and/or prevent downloads of downloadLink.
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (retry == 5) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final int ioExceptions = downloadLink.getIntegerProperty(IOEXCEPTIONS, 0);
        downloadLink.removeProperty(IOEXCEPTIONS);
        // if (true) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, 60 * 1000l);
        this.setBrowserExclusive();
        this.br.setDefaultSSLTrustALL(isSSLTrustALL());
        this.br.getHeaders().put("Accept-Encoding", "identity");
        final List<AuthenticationFactory> authenticationFactories = new ArrayList<AuthenticationFactory>();
        final URL url = new URL(getDownloadURL(downloadLink));
        if (url.getUserInfo() != null) {
            authenticationFactories.add(new URLUserInfoAuthentication());
        }
        authenticationFactories.addAll(AuthenticationController.getInstance().getSortedAuthenticationFactories(url, null));
        authenticationFactories.add(new CallbackAuthenticationFactory() {
            protected Authentication remember = null;

            protected Authentication askAuthentication(Browser browser, Request request, final String realm) {
                try {
                    final Login login = requestLogins(org.jdownloader.translate._JDT.T.DirectHTTP_getBasicAuth_message(), realm, downloadLink);
                    if (login != null) {
                        final Authentication ret = new DefaultAuthenticanFactory(request.getURL().getHost(), realm, login.getUsername(), login.getPassword()).buildAuthentication(browser, request);
                        addAuthentication(ret);
                        if (login.isRememberSelected()) {
                            remember = ret;
                        }
                        return ret;
                    }
                } catch (PluginException e) {
                    getLogger().log(e);
                }
                return null;
            }

            @Override
            public boolean retry(Authentication authentication, Browser browser, Request request) {
                if (authentication != null && containsAuthentication(authentication) && request.getAuthentication() == authentication && !requiresAuthentication(request)) {
                    if (remember == authentication) {
                        final AuthenticationInfo auth = new AuthenticationInfo();
                        auth.setRealm(authentication.getRealm());
                        auth.setUsername(authentication.getUsername());
                        auth.setPassword(authentication.getPassword());
                        auth.setHostmask(authentication.getHost());
                        auth.setType(Type.HTTP);
                        AuthenticationController.getInstance().add(auth);
                    } else {
                        try {
                            final String newURL = authentication.getURLWithUserInfo(url);
                            if (newURL != null) {
                                downloadLink.setUrlDownload(newURL);
                            }
                        } catch (IOException e) {
                            getLogger().log(e);
                        }
                    }
                }
                return super.retry(authentication, browser, request);
            }
        });
        this.br.setFollowRedirects(true);
        URLConnectionAdapter urlConnection = null;
        try {
            String basicauth = null;
            for (final AuthenticationFactory authenticationFactory : authenticationFactories) {
                br.setCustomAuthenticationFactory(authenticationFactory);
                urlConnection = this.prepareConnection(this.br, downloadLink);
                if (isCustomOffline(urlConnection)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String urlParams = null;
                if ((urlConnection.getResponseCode() == 401 || urlConnection.getResponseCode() == 400 || urlConnection.getResponseCode() == 404 || urlConnection.getResponseCode() == 403 || (StringUtils.contains(urlConnection.getContentType(), "image") && urlConnection.getLongContentLength() < 1024)) && (urlParams = downloadLink.getStringProperty(DirectHTTP.POSSIBLE_URLPARAM, null)) != null) {
                    /* check if we need the URLPARAMS to download the file */
                    urlConnection.setAllowedResponseCodes(new int[] { urlConnection.getResponseCode() });
                    try {
                        br.followConnection();
                    } catch (final Throwable e) {
                    } finally {
                        urlConnection.disconnect();
                    }
                    final String newURL = getDownloadURL(downloadLink) + urlParams;
                    downloadLink.setProperty(DirectHTTP.POSSIBLE_URLPARAM, Property.NULL);
                    setDownloadURL(newURL, downloadLink);
                    urlConnection = this.prepareConnection(this.br, downloadLink);
                }
                if (urlConnection.getResponseCode() == 401) {
                    if (urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_WWW_AUTHENTICATE) == null) {
                        /* no basic auth */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    urlConnection.setAllowedResponseCodes(new int[] { urlConnection.getResponseCode() });
                    br.followConnection();
                } else {
                    break;
                }
            }
            if (urlConnection.getResponseCode() == 503 || urlConnection.getResponseCode() == 504) {
                return AvailableStatus.UNCHECKABLE;
            }
            if (urlConnection.getResponseCode() == 404 || urlConnection.getResponseCode() == 410 || !urlConnection.isOK()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("auth", basicauth);
            this.contentType = urlConnection.getContentType();
            if (contentType != null) {
                if (StringUtils.equalsIgnoreCase(contentType, "application/pls") && StringUtils.endsWithCaseInsensitive(urlConnection.getURL().getPath(), ".mp3")) {
                    this.br.followConnection();
                    final String mp3URL = this.br.getRegex("(https?://.*?\\.mp3)").getMatch(0);
                    if (hasCustomDownloadURL()) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (mp3URL != null) {
                        setDownloadURL(mp3URL, null);
                        return this.requestFileInformation(downloadLink, retry + 1);
                    }
                }
            }
            final long length = urlConnection.getLongContentLength();
            if (length == 0 && urlConnection.getRequest() instanceof HeadRequest) {
                preferHeadRequest = false;
                br.followConnection();
                return this.requestFileInformation(downloadLink, retry + 1);
            }
            if (urlConnection.getHeaderField("cf-bgj") != null && !downloadLink.hasProperty(BYPASS_CLOUDFLARE_BGJ)) {
                if (urlConnection.getRequest() instanceof HeadRequest) {
                    br.followConnection();
                } else {
                    urlConnection.disconnect();
                }
                downloadLink.setProperty(BYPASS_CLOUDFLARE_BGJ, Boolean.TRUE);
                return this.requestFileInformation(downloadLink, retry + 1);
            }
            final String streamMod = urlConnection.getHeaderField("X-Mod-H264-Streaming");
            if (streamMod != null && downloadLink.getProperty("streamMod") == null) {
                downloadLink.setProperty("streamMod", streamMod);
                if (urlConnection.getRequest() instanceof HeadRequest) {
                    br.followConnection();
                } else {
                    urlConnection.disconnect();
                }
                return this.requestFileInformation(downloadLink, retry + 1);
            }
            if (this.contentType != null && (this.contentType.startsWith("text/html") || this.contentType.startsWith("application/json")) && urlConnection.isContentDisposition() == false && downloadLink.getBooleanProperty(DirectHTTP.TRY_ALL, false) == false) {
                /* jd does not want to download html content! */
                /* if this page does redirect via js/html, try to follow */
                if (urlConnection.getRequest() instanceof HeadRequest) {
                    preferHeadRequest = false;
                    br.followConnection();
                    return this.requestFileInformation(downloadLink, retry + 1);
                } else {
                    final String pageContent = this.br.followConnection();
                    if (StringUtils.endsWithCaseInsensitive(br.getURL(), "mp4")) {
                        final String videoURL = br.getRegex("source type=\"video/mp4\"\\s*src=\"(https?://.*)\"").getMatch(0);
                        if (videoURL != null && !hasCustomDownloadURL()) {
                            setDownloadURL(videoURL, null);
                            return AvailableStatus.TRUE;
                            // return this.requestFileInformation(downloadLink);
                        }
                    }
                    if (hasCustomDownloadURL()) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    /* search urls */
                    final ArrayList<String> embeddedLinks = DirectHTTP.findUrls(pageContent);
                    String embeddedLink = null;
                    if (embeddedLinks.size() == 1) {
                        embeddedLink = embeddedLinks.get(0);
                    } else {
                        final String orginalURL = getDownloadURL(downloadLink);
                        final String extension = Files.getExtension(orginalURL);
                        if (embeddedLinks.contains(orginalURL)) {
                            embeddedLink = orginalURL;
                        } else {
                            for (final String check : embeddedLinks) {
                                if (StringUtils.endsWithCaseInsensitive(check, extension)) {
                                    if (embeddedLink == null) {
                                        embeddedLink = check;
                                    } else {
                                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                                    }
                                }
                            }
                        }
                        if (embeddedLink == null) {
                            final String urlParams = downloadLink.getStringProperty(DirectHTTP.POSSIBLE_URLPARAM, null);
                            if (urlParams != null) {
                                downloadLink.setProperty(DirectHTTP.POSSIBLE_URLPARAM, Property.NULL);
                                final String newURL = getDownloadURL(downloadLink) + urlParams;
                                setDownloadURL(newURL, downloadLink);
                                return this.requestFileInformation(downloadLink, retry + 1);
                            }
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                    }
                    /* found one valid url */
                    setDownloadURL(embeddedLink, downloadLink);
                    return this.requestFileInformation(downloadLink, retry + 1);
                }
            } else {
                if (urlConnection.getRequest() instanceof HeadRequest) {
                    br.followConnection();
                } else {
                    urlConnection.disconnect();
                }
            }
            /* if final filename already set, do not change */
            if (downloadLink.getFinalFileName() == null) {
                /* restore filename from property */
                String fileName = downloadLink.getStringProperty(FIXNAME, null);
                if (fileName == null && downloadLink.getBooleanProperty("MOVIE2K", false)) {
                    final String ext = new Regex(this.contentType, "(audio|video)/(x\\-)?(.*?)$").getMatch(2);
                    fileName = downloadLink.getName() + "." + ext;
                }
                if (fileName == null) {
                    fileName = HTTPConnectionUtils.getFileNameFromDispositionHeader(urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_DISPOSITION));
                    if (fileName == null) {
                        fileName = Plugin.extractFileNameFromURL(urlConnection.getRequest().getUrl());
                        if (StringUtils.equalsIgnoreCase("php", Files.getExtension(fileName))) {
                            fileName = null;
                        }
                    }
                    if (fileName != null && downloadLink.getBooleanProperty("urlDecodeFinalFileName", true)) {
                        fileName = SimpleFTP.BestEncodingGuessingURLDecode(fileName);
                    }
                }
                if (fileName == null) {
                    fileName = downloadLink.getName();
                }
                if (fileName != null) {
                    if (fileName.indexOf(".") < 0) {
                        final String ext = this.getExtensionFromMimeType(this.contentType);
                        if (ext != null) {
                            fileName = fileName + "." + ext;
                        }
                    }
                    downloadLink.setFinalFileName(fileName);
                    /* save filename in property so we can restore in reset case */
                    downloadLink.setProperty(FIXNAME, fileName);
                }
            }
            if (length >= 0) {
                downloadLink.setDownloadSize(length);
                final String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
                if (urlConnection.getHeaderField("X-Mod-H264-Streaming") == null && (contentEncoding == null || "none".equalsIgnoreCase(contentEncoding))) {
                    final String contentMD5 = urlConnection.getHeaderField("Content-MD5");
                    final String contentSHA1 = urlConnection.getHeaderField("Content-SHA1");
                    if (downloadLink.getSha1Hash() == null) {
                        if (contentSHA1 != null) {
                            downloadLink.setSha1Hash(contentSHA1);
                        }
                    } else if (downloadLink.getMD5Hash() == null) {
                        if (contentMD5 != null) {
                            downloadLink.setMD5Hash(contentMD5);
                        }
                    }
                    downloadLink.setVerifiedFileSize(length);
                }
            } else {
                downloadLink.setDownloadSize(-1);
                downloadLink.setVerifiedFileSize(-1);
            }
            final String referer = urlConnection.getRequestProperty(HTTPConstants.HEADER_REQUEST_REFERER);
            downloadLink.setProperty("lastRefURL", referer);
            if (urlConnection.getRequest() instanceof HeadRequest) {
                downloadLink.setProperty("requestType", "HEAD");
            } else if (urlConnection.getRequest() instanceof GetRequest) {
                downloadLink.setProperty("requestType", "GET");
            } else {
                downloadLink.setProperty("requestType", Property.NULL);
            }
            return AvailableStatus.TRUE;
        } catch (final PluginException e2) {
            /* try referer set by flashgot and check if it works then */
            if (downloadLink.getBooleanProperty("tryoldref", false) == false && downloadLink.getStringProperty("referer", null) != null) {
                downloadLink.setProperty("tryoldref", true);
                return this.requestFileInformation(downloadLink, retry + 1);
            } else {
                resetDownloadlink(downloadLink);
                throw e2;
            }
        } catch (IOException e) {
            logger.log(e);
            resetDownloadlink(downloadLink);
            if (!isConnectionOffline(e)) {
                final int nextIOExceptions = ioExceptions + 1;
                if (nextIOExceptions > 2) {
                    if (e instanceof java.net.ConnectException || e.getCause() instanceof java.net.ConnectException) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (e instanceof java.net.UnknownHostException || e.getCause() instanceof java.net.UnknownHostException) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
                downloadLink.setProperty(IOEXCEPTIONS, nextIOExceptions);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Network problem: " + e.getMessage(), 30 * 60 * 1000l);
        } catch (final Exception e) {
            this.logger.log(e);
        } finally {
            try {
                urlConnection.disconnect();
            } catch (final Throwable e) {
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    private final String IOEXCEPTIONS = "IOEXCEPTIONS";

    /**
     * update this map to your needs
     *
     * @param mimeType
     * @return
     */
    public String getExtensionFromMimeType(final String mimeType) {
        if (mimeType == null) {
            return null;
        }
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("image/gif", "gif");
        map.put("image/jpeg", "jpeg");
        map.put("image/png", "png");
        map.put("image/tiff", "tiff");
        map.put("video/mp4", "mp4");
        map.put("audio/mp3", "mp3");
        map.put("application/gzip", "gz");
        map.put("application/pdf", "pdf");
        return map.get(mimeType.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public void reset() {
        preferHeadRequest = true;
        contentType = null;
        customDownloadURL = null;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(IOEXCEPTIONS);
        link.removeProperty(DirectHTTP.NORESUME);
        link.removeProperty(DirectHTTP.NOCHUNKS);
        link.removeProperty("lastRefURL");
        link.removeProperty("requestType");
        link.removeProperty("streamMod");
        link.removeProperty(BYPASS_CLOUDFLARE_BGJ);
        final String fixName = link.getStringProperty(FIXNAME, null);
        if (StringUtils.isNotEmpty(fixName)) {
            link.setFinalFileName(fixName);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setCustomHeaders(final Browser br, final DownloadLink downloadLink) throws IOException {
        /* allow customized headers, eg useragent */
        final Object customRet = downloadLink.getProperty("customHeader");
        List<String[]> custom = null;
        if (customRet != null && customRet instanceof List) {
            custom = (List<String[]>) customRet;
        }
        // Bla
        if (custom != null && custom.size() > 0) {
            for (final Object header : custom) {
                /*
                 * this is needed because we no longer serialize the stuff. We use JSON as storage and it does not differ between String[]
                 * and ArrayList<String>
                 */
                if (header instanceof ArrayList) {
                    br.getHeaders().put((String) ((ArrayList<?>) header).get(0), (String) ((ArrayList<?>) header).get(1));
                } else if (header.getClass().isArray()) {
                    br.getHeaders().put(((String[]) header)[0], ((String[]) header)[1]);
                }
            }
        }
        /*
         * seems like flashgot catches the wrong referer and some downloads do not work then, we do not set referer as a workaround
         */
        if (downloadLink.getStringProperty("refURL", null) != null) {
            /* refURL is for internal use */
            br.getHeaders().put("Referer", downloadLink.getStringProperty("refURL", null));
        }
        /*
         * try the referer set by flashgot, maybe it works
         */
        if (downloadLink.getBooleanProperty("tryoldref", false) && downloadLink.getStringProperty("referer", null) != null) {
            /* refURL is for internal use */
            br.getHeaders().put("Referer", downloadLink.getStringProperty("referer", null));
        }
        if (downloadLink.getStringProperty("cookies", null) != null) {
            br.getCookies(getDownloadURL(downloadLink)).add(Cookies.parseCookies(downloadLink.getStringProperty("cookies", null), Browser.getHost(getDownloadURL(downloadLink)), null));
        }
        if (downloadLink.getStringProperty("Referer", null) != null) {
            // used in MANY plugins!
            br.getHeaders().put("Referer", downloadLink.getStringProperty("Referer", null));
        }
        if (downloadLink.getStringProperty("lastRefURL", null) != null) {
            // used in MANY plugins!
            br.getHeaders().put("Referer", downloadLink.getStringProperty("lastRefURL", null));
        }
        this.downloadWorkaround(br, downloadLink);
    }

    private void downloadWorkaround(final Browser br, final DownloadLink downloadLink) throws IOException {
        // we shouldn't potentially over right setCustomHeaders..
        if (br.getHeaders().get("Referer") == null) {
            final String link = getDownloadURL(downloadLink);
            if (link.contains("fileplanet.com")) {
                /*
                 * it seems fileplanet firewall checks referer and ip must have called the page lately
                 */
                // br.getPage("http://www.fileplanet.com/");
                br.getHeaders().put("Referer", "http://fileplanet.com/");
            } else if (link.contains("sites.google.com")) {
                /*
                 * it seems google checks referer and ip must have called the page lately
                 */
                br.getHeaders().put("Referer", "https://sites.google.com");
            } else if (link.contains("tinypic.com/")) {
                // they seem to block direct link access
                br.getHeaders().put("Referer", link);
            } else if (link.contains("project-gxs.com")) {
                br.getHeaders().put("Referer", "http://sh.st/");
            }
        }
    }

    /**
     * custom offline references based on conditions found within previous URLConnectionAdapter request.
     *
     * @author raztoki
     */
    private boolean isCustomOffline(URLConnectionAdapter urlConnection) {
        final String url = urlConnection.getURL().toString();
        if (url != null) {
            if ("imgchili.net".equals(Browser.getHost(url)) && (url.endsWith("/hotlink.png") || url.endsWith("/404.png"))) {
                return true;
            }
        }
        return false;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public void extendDownloadsTableContextMenu(JComponent parent, PluginView<DownloadLink> pv) {
        if (pv.size() == 1) {
            final JMenuItem changeURLMenuItem = createChangeURLMenuItem(pv.get(0));
            if (changeURLMenuItem != null) {
                parent.add(changeURLMenuItem);
            }
        }
    }

    @Override
    public String getHost(final DownloadLink link, Account account) {
        if (link != null) {
            // prefer domain via public suffic list
            return Browser.getHost(link.getDownloadURL());
        }
        if (account != null) {
            return account.getHoster();
        }
        return null;
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }
}