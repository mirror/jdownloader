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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerRule;
import jd.controlling.reconnect.ipcheck.IP;
import jd.http.Authentication;
import jd.http.AuthenticationFactory;
import jd.http.Browser;
import jd.http.Browser.BlockedByException;
import jd.http.CallbackAuthenticationFactory;
import jd.http.Cookies;
import jd.http.DefaultAuthenticanFactory;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.URLUserInfoAuthentication;
import jd.nutils.SimpleFTP;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadConnectionVerifier;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.HashInfo;
import jd.plugins.download.HashResult;
import jd.plugins.download.raf.HTTPDownloader;
import jd.utils.locale.JDL;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.DispositionHeader;
import org.jdownloader.auth.AuthenticationController;
import org.jdownloader.auth.AuthenticationInfo;
import org.jdownloader.auth.AuthenticationInfo.Type;
import org.jdownloader.auth.Login;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;

/**
 * TODO: remove after next big update of core to use the public static methods!
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "DirectHTTP", "http links" }, urls = { "directhttp://.+",
        "https?(viajd)?://[^/]+/.*\\.((jdeatme|3gp|7zip|7z|abr|ac3|ace|aiff|aifc|aif|ai|au|avi|avif|appimage|apk|azw3|azw|adf|asc|bin|ape|ass|bmp|bat|bz2|cbr|csv|cab|cbz|ccf|chm|cr2|cso|cue|cpio|cvd|c\\d{2,4}|chd|dta|deb|diz|divx|djvu|dlc|dmg|dms|doc|docx|dot|dx2|eps|epub|exe|ff|flv|flac|f4v|gsd|gif|gpg|gz|hqx|iwd|idx|iso|ipa|ipsw|java|jar|jpe?g|jp2|load|lha|lzh|m2ts|m4v|m4a|md5|midi?|mkv|mp2|mo3|mp3|mp4|mobi|mov|movie|mpeg|mpe|mpg|mpq|msi|msu|msp|mv|mws|nfo|npk|nsf|oga|ogg|ogm|ogv|otrkey|par2|pak|pkg|png|pdf|pptx?|ppsx?|ppz|pdb|pot|psd|ps|qt|rmvb|rm|rar|ra|rev|rnd|rpm|run|rsdf|reg|rtf|shnf|sh(?!tml)|ssa|smi|sig|sub|srt|snd|sfv|sfx|swf|swc|sid|sit|tar\\.(gz|bz2|xz)|tar|tgz|tiff?|ts|txt|viv|vivo|vob|vtt|webm|webp|wav|wad|wmv|wma|wpt|xla|xls|xpi|xtm|zeno|zip|[r-z]\\d{2}|_?[_a-z]{2}|\\d{1,4}$)(\\.\\d{1,4})?(?=\\?|$|#|\"|\r|\n|;))" })
public class DirectHTTP extends antiDDoSForHost implements DownloadConnectionVerifier {
    public static final String  ENDINGS                                      = "\\.(jdeatme|3gp|7zip|7z|abr|ac3|ace|aiff|aifc|aif|ai|au|avi|avif|appimage|apk|azw3|azw|adf|asc|ape|bin|ass|bmp|bat|bz2|cbr|csv|cab|cbz|ccf|chm|cr2|cso|cue|cpio|cvd|c\\d{2,4}|chd|dta|deb|diz|divx|djvu|dlc|dmg|dms|doc|docx|dot|dx2|eps|epub|exe|ff|flv|flac|f4v|gsd|gif|gpg|gz|hqx|iwd|idx|iso|ipa|ipsw|java|jar|jpe?g|jp2|load|lha|lzh|m2ts|m4v|m4a|md5|midi?|mkv|mp2|mo3|mp3|mp4|mobi|mov|movie|mpeg|mpe|mpg|mpq|msi|msu|msp|mv|mws|nfo|npk|nfs|oga|ogg|ogm|ogv|otrkey|par2|pak|pkg|png|pdf|pptx?|ppsx?|ppz|pdb|pot|psd|ps|qt|rmvb|rm|rar|ra|rev|rnd|rpm|run|rsdf|reg|rtf|shnf|sh(?!tml)|ssa|smi|sig|sub|srt|snd|sfv|sfx|swf|swc|sid|sit|tar\\.(gz|bz2|xz)|tar|tgz|tiff?|ts|txt|viv|vivo|vob|vtt|webm|webp|wav|wad|wmv|wma|wpt|xla|xls|xpi|xtm|zeno|zip|[r-z]\\d{2}|_?[_a-z]{2}|\\d{1,4}(?=\\?|$|#|\"|\r|\n|;))";
    public static final String  NORESUME                                     = "nochunkload";
    public static final String  NOCHUNKS                                     = "nochunk";
    /**
     * Set this property on DownloadLink objects if you want to force a filename which also survives if the user resets a DownloadLink.
     * Otherwise, Content-Disposition filename will be used (or filename from inside URL as fallback).
     */
    public static final String  FIXNAME                                      = "fixName";
    public static final String  FORCE_NORESUME                               = "forcenochunkload";                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   // TODO:
    public static final String  FORCE_NOCHUNKS                               = "forcenochunk";
    public static final String  FORCE_NOVERIFIEDFILESIZE                     = "forcenoverifiedfilesize";
    public static final String  TRY_ALL                                      = "tryall";
    public static final String  POSSIBLE_URLPARAM                            = "POSSIBLE_GETPARAM";
    public static final String  BYPASS_CLOUDFLARE_BGJ                        = "bpCfBgj";                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            // TODO:
    public static final String  PROPERTY_COOKIES                             = "COOKIES";
    public static final String  PROPERTY_HEADERS                             = "customHeader";
    public static final String  PROPERTY_MAX_CONCURRENT                      = "PROPERTY_MAX_CONCURRENT";
    public static final String  PROPERTY_RATE_LIMIT                          = "PROPERTY_RATE_LIMIT";
    public static final String  PROPERTY_RATE_LIMIT_TLD                      = "PROPERTY_RATE_LIMIT_TLD";
    public static final String  PROPERTY_CUSTOM_HOST                         = "PROPERTY_CUSTOM_HOST";
    public static final String  PROPERTY_REQUEST_TYPE                        = "requestType";
    private static final String PROPERTY_DISABLE_PREFIX                      = "disable_";
    private static final String PROPERTY_AVOID_HEAD_REQUEST                  = "PROPERTY_AVOID_HEAD_REQUEST";
    private static final String PROPERTY_ENABLE_PREFIX                       = "enable_";
    private static final String PROPERTY_OPTION_SET                          = "optionSet";
    public static final String  PROPERTY_ServerComaptibleForByteRangeRequest = "ServerComaptibleForByteRangeRequest";
    private final String        PROPERTY_LAST_REFERER                        = "lastRefURL";

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.GENERIC };
    }

    @Override
    public ArrayList<DownloadLink> getDownloadLinks(CrawledLink source, final String data, final FilePackage fp) {
        final ArrayList<DownloadLink> ret = super.getDownloadLinks(source, data, fp);
        if (ret != null && ret.size() == 1) {
            preProcessDirectHTTP(ret.get(0), data);
        }
        return ret;
    }

    @Override
    public PluginForHost assignPlugin(final PluginFinder pluginFinder, final DownloadLink link) {
        if (pluginFinder != null && DirectHTTP.class.equals(getClass()) && getHost().equals(link.getHost())) {
            try {
                final List<LazyHostPlugin> assignPlugins = pluginFinder.listAssignPlugins();
                for (final LazyHostPlugin lazyAssignPlugin : assignPlugins) {
                    try {
                        PluginForHost assignPlugin = pluginFinder.getPlugin(lazyAssignPlugin);
                        if ((assignPlugin = assignPlugin.assignPlugin(pluginFinder, link)) != null) {
                            return assignPlugin;
                        }
                    } catch (final Throwable e) {
                        pluginFinder.getLogger().log(e);
                    }
                }
            } catch (final Throwable e) {
                pluginFinder.getLogger().log(e);
            }
        }
        return super.assignPlugin(pluginFinder, link);
    }

    private void preProcessDirectHTTP(final DownloadLink downloadLink, final String data) {
        try {
            String modifiedData = null;
            final boolean isDirect;
            final boolean tryAll = StringUtils.containsIgnoreCase(data, ".jdeatme");
            if (data.startsWith("directhttp://")) {
                isDirect = true;
                modifiedData = data.replace("directhttp://", "");
            } else {
                isDirect = false;
                modifiedData = data.replace("httpsviajd://", "https://");
                modifiedData = modifiedData.replace("httpviajd://", "http://");
                modifiedData = modifiedData.replace(".jdeatme", "");
            }
            final DownloadLink link = downloadLink;
            if (tryAll) {
                link.setProperty(TRY_ALL, Boolean.TRUE);
            }
            correctDownloadLink(link);// needed to fixup the returned url
            CrawledLink currentLink = getCurrentLink();
            String autoReferer = null;
            while (currentLink != null) {
                if (StringUtils.equals(currentLink.getURL(), modifiedData)) {
                    // no autoReferer as the link is result from DeepDecrypt(Rule) or DirectHTTPRule
                    autoReferer = null;
                    break;
                } else if (!StringUtils.equals(currentLink.getURL(), data)) {
                    if (autoReferer == null) {
                        autoReferer = currentLink.getURL();
                    }
                }
                currentLink = currentLink.getSourceLink();
            }
            if (autoReferer != null) {
                link.setProperty(LinkCrawler.PROPERTY_AUTO_REFERER, autoReferer);
            }
            /* single link parsing in svn/jd2 */
            final String url = link.getPluginPatternMatcher();
            final int idx = modifiedData.indexOf(url);
            if (!isDirect && idx >= 0 && modifiedData.length() >= idx + url.length()) {
                String param = modifiedData.substring(idx + url.length());
                if (param != null) {
                    param = new Regex(param, "(.*?)(\r|\n|$)").getMatch(0);
                    if (param != null && param.trim().length() != 0) {
                        link.setProperty(DirectHTTP.POSSIBLE_URLPARAM, new String(param));
                    }
                }
            }
        } catch (final Throwable e) {
            if (logger != null) {
                this.logger.log(e);
            }
        }
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        final String mirrorID = link != null ? link.getStringProperty("mirrorID", null) : null;
        return mirrorID;
    }

    @Override
    public boolean isValidURL(String url) {
        if (url != null) {
            if (StringUtils.startsWithCaseInsensitive(url, "directhttp")) {
                return true;
            } else {
                url = url.toLowerCase(Locale.ENGLISH);
                if (url.contains("facebook.com/l.php")) {
                    return false;
                } else if (url.contains("facebook.com/ajax/sharer/")) {
                    return false;
                } else if (url.contains("youtube.com/videoplayback") && url.startsWith("http")) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isResumeable(DownloadLink link, Account account) {
        if (link != null) {
            if (link.getBooleanProperty(DirectHTTP.NORESUME, false) || link.getBooleanProperty(DirectHTTP.FORCE_NORESUME, false)) {
                return false;
            } else if (link.getBooleanProperty(DownloadLink.PROPERTY_RESUMEABLE, true)) {
                return true;
            } else {
                return false;
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
            link.setUrlDownload(link.getDownloadURL().replaceAll("(?i)^directhttp://", ""));
        } else {
            link.setUrlDownload(link.getDownloadURL().replaceAll("(?i)httpviajd://", "http://").replaceAll("(?i)httpsviajd://", "https://"));
            /* this extension allows to manually add unknown extensions */
            link.setUrlDownload(link.getDownloadURL().replaceAll("(?i)\\.jdeatme$", ""));
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
    protected int getMaxSimultanDownload(DownloadLink link, Account account) {
        final int max = link != null ? link.getIntegerProperty(PROPERTY_MAX_CONCURRENT, -1) : -1;
        if (max > 0) {
            return max;
        } else {
            return super.getMaxSimultanDownload(link, account);
        }
    }

    @Override
    public Downloadable newDownloadable(final DownloadLink downloadLink, final Browser br) {
        return new DownloadLinkDownloadable(downloadLink, br) {

            @Override
            public boolean isHashCheckEnabled() {
                if (!super.isHashCheckEnabled()) {
                    return false;
                } else if (!isValidHashInfo(getHashInfo())) {
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public HashResult getHashResult(HashInfo hashInfo, File outputPartFile) {
                final HashResult ret = super.getHashResult(hashInfo, outputPartFile);
                if (ret == null || ret.match() || !downloadLink.hasProperty(BYPASS_CLOUDFLARE_BGJ)) {
                    return ret;
                } else {
                    return new HashResult(HashInfo.parse(ret.getFileHash()), ret.getFileHash());
                }
            }

        };
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        final Set<String> optionSet = getOptionSet(downloadLink);
        logger.info("OptionSet:" + optionSet);
        if (this.requestFileInformation(downloadLink, 0, optionSet) == AvailableStatus.UNCHECKABLE) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 15 * 60 * 1000l);
        }
        final Cookies cookies = br.getCookies(getDownloadURL(downloadLink));
        br.setCurrentURL(null);
        br.setRequest(null);
        br.setCookies(getDownloadURL(downloadLink), cookies);
        this.br.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity");
        br.setDefaultSSLTrustALL(isSSLTrustALL());
        /* Workaround to clear referer */
        this.br.setFollowRedirects(true);
        this.br.setDebug(true);
        boolean resume = true;
        int chunks = 0;
        if (downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) || downloadLink.getBooleanProperty(DirectHTTP.FORCE_NORESUME, false)) {
            logger.info("Disable Resume:" + downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) + "|" + downloadLink.getBooleanProperty(DirectHTTP.FORCE_NORESUME, false));
            resume = false;
        }
        if (downloadLink.getBooleanProperty(DirectHTTP.NOCHUNKS, false) || downloadLink.getBooleanProperty(DirectHTTP.FORCE_NOCHUNKS, false) || resume == false) {
            logger.info("Disable Chunks:" + downloadLink.getBooleanProperty(DirectHTTP.NOCHUNKS, false) + "|" + downloadLink.getBooleanProperty(DirectHTTP.FORCE_NOCHUNKS, false) + "|" + resume);
            chunks = 1;
        }
        if (downloadLink.getBooleanProperty(FORCE_NOVERIFIEDFILESIZE, false)) {
            logger.info("Forced NoVerifiedFilesize! Disable Chunks/Resume!");
            chunks = 1;
            resume = false;
        }
        final String streamMod = downloadLink.getStringProperty("streamMod", null);
        if (streamMod != null) {
            logger.info("Apply streamMod handling:" + streamMod);
            resume = true;
            downloadLink.setProperty(PROPERTY_ServerComaptibleForByteRangeRequest, true);
        }
        this.setCustomHeaders(this.br, downloadLink, optionSet);
        if (resume && downloadLink.getVerifiedFileSize() > 0) {
            downloadLink.setProperty(PROPERTY_ServerComaptibleForByteRangeRequest, true);
        } else {
            downloadLink.setProperty(PROPERTY_ServerComaptibleForByteRangeRequest, Property.NULL);
        }
        if (optionSet.contains("avoidOpenRange")) {
            logger.info("Avoid open range workaround!");
            downloadLink.setProperty(PROPERTY_ServerComaptibleForByteRangeRequest, Property.NULL);
        }
        long downloadCurrentRaw = downloadLink.getDownloadCurrentRaw();
        if (downloadLink.getProperty(BYPASS_CLOUDFLARE_BGJ) != null) {
            logger.info("Apply Cloudflare BGJ bypass");
            resume = false;
            chunks = 1;
        }
        resume = isResumable(downloadLink, optionSet, resume);
        chunks = getMaxChunks(downloadLink, optionSet, chunks);
        logger.info("Resumable:" + resume + "|MaxChunks:" + chunks + "|ServerComaptibleForByteRangeRequest:" + downloadLink.getProperty(PROPERTY_ServerComaptibleForByteRangeRequest));
        boolean instantRetryFlag = true;
        instantRetry: while (instantRetryFlag) {
            instantRetryFlag = false;
            try {
                if (downloadLink.getStringProperty("post", null) != null) {
                    this.dl = new jd.plugins.BrowserAdapter().openDownload(this.br, downloadLink, getDownloadURL(downloadLink), downloadLink.getStringProperty("post", null), resume, chunks);
                } else {
                    this.dl = new jd.plugins.BrowserAdapter().openDownload(this.br, downloadLink, getDownloadURL(downloadLink), resume, chunks);
                }
            } catch (final IllegalStateException e) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable ignore) {
                }
                logger.log(e);
                if (StringUtils.containsIgnoreCase(e.getMessage(), "Range Error. Requested bytes=0- Got range: bytes 0-")) {
                    logger.info("Workaround for Cloudflare-Cache transparent image compression!");
                    downloadLink.setVerifiedFileSize(-1);
                    throw new PluginException(LinkStatus.ERROR_RETRY, null, -1, e);
                } else {
                    throw e;
                }
            }
            if (this.dl.getConnection().getResponseCode() == 416 && dl.getConnection().getRequestProperty(HTTPConstants.HEADER_REQUEST_RANGE) != null) {
                followURLConnection(br, dl.getConnection());
                downloadLink.setProperty(DirectHTTP.NORESUME, Boolean.TRUE);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (this.dl.getConnection().getResponseCode() == 403 && dl.getConnection().getRequestProperty(HTTPConstants.HEADER_REQUEST_RANGE) != null) {
                followURLConnection(br, dl.getConnection());
                downloadLink.setProperty(DirectHTTP.NORESUME, Boolean.TRUE);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (dl.getConnection().getResponseCode() >= 500) {
                followURLConnection(br, dl.getConnection());
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 15 * 60 * 1000l);
            } else if ((dl.getConnection().getResponseCode() == 200 || dl.getConnection().getResponseCode() == 206) && dl.getConnection().getCompleteContentLength() == -1 && downloadLink.getVerifiedFileSize() > 0) {
                logger.info("Workaround for missing Content-Length!");
                dl.getConnection().disconnect();
                downloadLink.setVerifiedFileSize(-1);
                downloadLink.setProperty(DirectHTTP.FORCE_NOVERIFIEDFILESIZE, Boolean.TRUE);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            try {
                if (!this.dl.startDownload()) {
                    if (this.dl.externalDownloadStop()) {
                        return;
                    }
                }
            } catch (Exception e) {
                if (e instanceof PluginException) {
                    final PluginException pE = (PluginException) e;
                    switch (pE.getLinkStatus()) {
                    case LinkStatus.ERROR_ALREADYEXISTS:
                    case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                        throw e;
                    case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
                        if (StringUtils.equals(streamMod, "limitedRangeLength")) {
                            final long nowCurrentRaw = downloadLink.getDownloadCurrent();
                            if (nowCurrentRaw > downloadCurrentRaw) {
                                logger.info("InstantRetry for limitedRangeLength:" + nowCurrentRaw + ">" + downloadCurrentRaw);
                                downloadCurrentRaw = nowCurrentRaw;
                                instantRetryFlag = true;
                                dl.close();
                                continue instantRetry;
                            }
                        }
                        break;
                    default:
                        break;
                    }
                } else if (e instanceof SkipReasonException) {
                    throw e;
                } else if (e instanceof InterruptedException) {
                    throw e;
                }
                if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload")) || this.dl.getConnection().getResponseCode() == 400 && this.br.getRequest().getHttpConnection().getHeaderField("server").matches("HFS.+")) {
                    if (downloadLink.getBooleanProperty(DirectHTTP.NORESUME, false) == false) {
                        /* clear chunkProgress and disable resume(ranges) and retry */
                        downloadLink.setChunksProgress(null);
                        downloadLink.setProperty(DirectHTTP.NORESUME, Boolean.TRUE);
                        throw new PluginException(LinkStatus.ERROR_RETRY, null, -1, e);
                    }
                } else {
                    if (downloadLink.getBooleanProperty(DirectHTTP.NOCHUNKS, false) == false) {
                        if (downloadLink.getDownloadCurrent() > downloadCurrentRaw + (128 * 1024l)) {
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
    }

    protected int getMaxChunks(DownloadLink downloadLink, Set<String> optionSet, int chunks) {
        return chunks;
    }

    protected boolean isResumable(DownloadLink downloadLink, Set<String> optionSet, boolean resume) {
        return resume;
    }

    private String customDownloadURL = null;

    private void setDownloadURL(String newURL, DownloadLink link) throws IOException {
        if (link != null && !StringUtils.equals(link.getDownloadURL(), newURL)) {
            if (StringUtils.isEmpty(new URL(newURL).getFile())) {
                throw new IOException("invalid url:" + newURL);
            }
            link.setUrlDownload(newURL);
            this.customDownloadURL = null;
        } else {
            this.customDownloadURL = newURL;
        }
    }

    protected boolean hasCustomDownloadURL() {
        return customDownloadURL != null;
    }

    protected String getDownloadURL(DownloadLink downloadLink) throws IOException {
        String ret = customDownloadURL;
        if (ret == null) {
            ret = downloadLink.getDownloadURL();
        }
        if (downloadLink.getProperty(BYPASS_CLOUDFLARE_BGJ) != null) {
            ret = URLHelper.parseLocation(new URL(ret), "&bpcfbgj=" + System.nanoTime());
        }
        return ret;
    }

    private URLConnectionAdapter prepareConnection(final Browser br, final DownloadLink downloadLink, final Set<String> optionSet) throws Exception {
        final String requestType = downloadLink.getStringProperty(PROPERTY_REQUEST_TYPE, isPreferHeadRequest(optionSet) ? "HEAD" : "GET");
        return prepareConnection(br, downloadLink, 1, requestType, optionSet);
    }

    private final String SESSION_TRUST_HEAD    = "SESSION_TRUST_HEAD";
    private final String SESSION_AUTHORIZATION = "SESSION_AUTHORIZATION";

    private URLConnectionAdapter prepareConnection(final Browser br, final DownloadLink downloadLink, final int round, final String requestType, Set<String> optionSet) throws Exception {
        URLConnectionAdapter urlConnection = null;
        br.setRequest(null);
        this.setCustomHeaders(br, downloadLink, optionSet);
        boolean rangeHeader = false;
        try {
            String downloadURL = getDownloadURL(downloadLink);
            if (downloadLink.getProperty("streamMod") != null || optionSet.contains("streamMod")) {
                if (!optionSet.contains("avoidOpenRange")) {
                    rangeHeader = true;
                    br.getHeaders().put(OPEN_RANGE_REQUEST);
                }
            }
            Request request = null;
            try {
                final Boolean trustHeadRequest = getSessionValue(downloadLink, SESSION_TRUST_HEAD, Boolean.class, null);
                if (downloadLink.getStringProperty("post", null) != null || "POST".equals(requestType)) {
                    // PROPERTY_REQUEST_TYPE is set to POST
                    request = br.createPostRequest(downloadURL, downloadLink.getStringProperty("post", null));
                    urlConnection = openAntiDDoSRequestConnection(br, request);
                } else if ("GET".equals(requestType)) {
                    // PROPERTY_REQUEST_TYPE is set to GET
                    request = br.createGetRequest(downloadURL);
                    urlConnection = openAntiDDoSRequestConnection(br, request);
                } else if (!Boolean.FALSE.equals(trustHeadRequest) && "HEAD".equals(requestType)) {
                    // PROPERTY_REQUEST_TYPE is set to HEAD or preferHeadRequest
                    request = br.createHeadRequest(downloadURL);
                    urlConnection = openAntiDDoSRequestConnection(br, request);
                } else {
                    request = br.createGetRequest(downloadURL);
                    urlConnection = openAntiDDoSRequestConnection(br, request);
                }
                final ResponseCode responseCode = ResponseCode.get(urlConnection.getResponseCode());
                if (responseCode != null) {
                    switch (responseCode) {
                    case ERROR_FORBIDDEN:
                    case ERROR_NOT_FOUND:
                    case METHOD_NOT_ALLOWED:
                        boolean retry = false;
                        if (downloadLink.getStringProperty(LinkCrawler.PROPERTY_AUTO_REFERER) != null && optionSet.add(PROPERTY_DISABLE_PREFIX + LinkCrawler.PROPERTY_AUTO_REFERER)) {
                            retry = true;
                        } else if (optionSet.add(PROPERTY_ENABLE_PREFIX + "selfReferer")) {
                            retry = true;
                        }
                        if (retry) {
                            followURLConnection(br, urlConnection);
                            return prepareConnection(br, downloadLink, round + 1, requestType, optionSet);
                        }
                        break;
                    default:
                        break;
                    }
                }
                if (RequestMethod.HEAD.equals(urlConnection.getRequestMethod())) {
                    if (Boolean.TRUE.equals(trustHeadRequest)) {
                        return urlConnection;
                    } else if (ResponseCode.ERROR_NOT_FOUND.equals(responseCode)) {
                        /*
                         * && StringUtils.contains(urlConnection.getHeaderField("Cache-Control"), "must-revalidate") &&
                         * urlConnection.getHeaderField("Via") != null
                         */
                        followURLConnection(br, urlConnection);
                        return prepareConnection(br, downloadLink, round + 1, "GET", optionSet);
                    } else if ((!ResponseCode.ERROR_UNAUTHORIZED.equals(responseCode) && !ResponseCode.TOO_MANY_REQUESTS.equals(responseCode)) && urlConnection.getResponseCode() >= 300) {
                        // no head support?
                        followURLConnection(br, urlConnection);
                        return prepareConnection(br, downloadLink, round + 1, "GET", optionSet);
                    }
                }
                return urlConnection;
            } catch (final IOException e) {
                followURLConnection(br, urlConnection);
                if (request != null && RequestMethod.HEAD.equals(request.getRequestMethod())) {
                    /* some servers do not allow head requests */
                    try {
                        urlConnection = openAntiDDoSRequestConnection(br, br.createGetRequest(downloadURL));
                        downloadLink.setProperty(PROPERTY_REQUEST_TYPE, "GET");
                        putSessionValue(downloadLink, SESSION_TRUST_HEAD, Boolean.FALSE);
                        return urlConnection;
                    } catch (IOException e2) {
                        followURLConnection(br, urlConnection);
                        if (StringUtils.startsWithCaseInsensitive(downloadURL, "http://")) {
                            setDownloadURL(downloadURL.replaceFirst("(?i)^http://", "https://"), null);
                            return prepareConnection(br, downloadLink, round + 1, requestType, optionSet);
                        } else {
                            throw e2;
                        }
                    }
                } else {
                    if (StringUtils.startsWithCaseInsensitive(downloadURL, "http://")) {
                        setDownloadURL(downloadURL.replaceFirst("(?i)^http://", "https://"), null);
                        return prepareConnection(br, downloadLink, round + 1, requestType, optionSet);
                    } else {
                        throw e;
                    }
                }
            }
        } finally {
            if (rangeHeader) {
                br.getHeaders().remove(HTTPConstants.HEADER_REQUEST_RANGE);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        final Set<String> optionSet = getOptionSet(downloadLink);
        logger.info("OptionSet:" + optionSet);
        return requestFileInformation(downloadLink, 0, optionSet);
    }

    private void followURLConnection(Browser br, URLConnectionAdapter urlConnection) throws IOException {
        if (urlConnection != null) {
            try {
                br.followConnection(true);
            } catch (BlockedByException e) {
                logger.log(e);
                throw e;
            } catch (final IOException e) {
                logger.log(e);
            } finally {
                urlConnection.disconnect();
            }
        }
    }

    @Override
    public Boolean verifyDownloadableContent(Set<LazyHostPlugin> plugins, URLConnectionAdapter urlConnection) {
        if (plugins == null) {
            plugins = new HashSet<LazyHostPlugin>();
        }
        plugins.add(getLazyP());
        final String host = Browser.getHost(urlConnection.getURL());
        final LazyHostPlugin lazyHostPlugin = new PluginFinder()._assignHost(host);
        if (lazyHostPlugin != null && plugins.add(lazyHostPlugin)) {
            try {
                final PluginClassLoaderChild pluginClassLoaderChild = PluginClassLoader.getThreadPluginClassLoaderChild();
                final PluginForHost plugin = Plugin.getNewPluginInstance(this, lazyHostPlugin, pluginClassLoaderChild);
                if (plugin instanceof DownloadConnectionVerifier) {
                    return ((DownloadConnectionVerifier) plugin).verifyDownloadableContent(plugins, urlConnection);
                }
            } catch (PluginException e) {
                getLogger().log(e);
            }
        }
        return null;
    }

    private String getPossibleURLParams(DownloadLink downloadLink) {
        return downloadLink.getStringProperty(DirectHTTP.POSSIBLE_URLPARAM, null);
    }

    private boolean retryConnection(final DownloadLink downloadLink, final URLConnectionAdapter con) {
        switch (con.getResponseCode()) {
        case 200:
            /*
             * for example HTTP/1.1 200 OK, Content-Disposition: inline; filename=error.html
             * 
             * we retry without HEAD in order to get full html response
             */
            return RequestMethod.HEAD.equals(con.getRequest().getRequestMethod()) && Boolean.FALSE.equals(verifyDownloadableContent(null, con));
        case 400:// Bad Request
        case 401:// Unauthorized
        case 403:// Forbidden
        case 404:// Not found
        case 410:// Gone
        case 470:// special response code, see thread 81171
            return getPossibleURLParams(downloadLink) != null || RequestMethod.HEAD.equals(con.getRequest().getRequestMethod());
        case 500:
            return getPossibleURLParams(downloadLink) != null;
        default:
            return false;
        }
    }

    private LinkCrawlerRule getDirectHTTPRule(final DownloadLink downloadLink) {
        final long linkCrawlerRuleID = downloadLink.getLongProperty("lcrID", -1);
        if (linkCrawlerRuleID != -1) {
            final LinkCrawlerRule rule = LinkCrawler.getLinkCrawlerRule(linkCrawlerRuleID);
            if (rule != null && LinkCrawlerRule.RULE.DIRECTHTTP.equals(rule.getRule())) {
                return rule;
            }
        }
        return null;
    }

    private void applyRateLimits(final DownloadLink downloadLink, Browser br) {
        int limit = downloadLink != null ? downloadLink.getIntegerProperty(PROPERTY_RATE_LIMIT_TLD) : -1;
        if (limit > 0) {
            final String host = Browser.getHost(downloadLink.getPluginPatternMatcher(), false);
            logger.info("Apply RateLimit:" + host + "|" + limit);
            Browser.setRequestIntervalLimitGlobal(host, false, limit);
        }
        limit = downloadLink != null ? downloadLink.getIntegerProperty(PROPERTY_RATE_LIMIT) : -1;
        if (limit > 0) {
            final String host = Browser.getHost(downloadLink.getPluginPatternMatcher(), true);
            logger.info("Apply RateLimit:" + host + "|" + limit);
            Browser.setRequestIntervalLimitGlobal(host, true, limit);
        }
    }

    private String preSetFinalName = null;
    private String preSetFIXNAME   = null;

    private boolean isPreferHeadRequest(final Set<String> optionSet) {
        return optionSet != null && !optionSet.contains(PROPERTY_AVOID_HEAD_REQUEST);
    }

    private void setPreferHeadRequest(Set<String> optionSet, boolean preferHeadRequest) {
        if (optionSet != null) {
            if (preferHeadRequest) {
                optionSet.remove(PROPERTY_AVOID_HEAD_REQUEST);
            } else {
                optionSet.add(PROPERTY_AVOID_HEAD_REQUEST);
            }
        }
    }

    private static Object OPTIONSETLOCK = new Object();

    private Set<String> getOptionSet(final DownloadLink downloadLink) {
        synchronized (OPTIONSETLOCK) {
            Set<String> optionSet = downloadLink.getObjectProperty(PROPERTY_OPTION_SET, TypeRef.STRING_SET);
            if (optionSet != null && !(optionSet instanceof CopyOnWriteArraySet)) {
                optionSet = new CopyOnWriteArraySet<String>(optionSet);
                downloadLink.setProperty(PROPERTY_OPTION_SET, optionSet);
            } else if (optionSet == null) {
                optionSet = new CopyOnWriteArraySet<String>();
                downloadLink.setProperty(PROPERTY_OPTION_SET, optionSet);
            }
            return optionSet;
        }
    }

    protected URLConnectionAdapter handleRateLimit(final DownloadLink downloadLink, Set<String> optionSet, Browser br, URLConnectionAdapter urlConnection) throws Exception {
        if (HTTPConstants.ResponseCode.TOO_MANY_REQUESTS.matches(urlConnection.getResponseCode())) {
            followURLConnection(br, urlConnection);
            final String waitSecondsStr = br.getRequest().getResponseHeader(HTTPConstants.HEADER_RESPONSE_RETRY_AFTER);
            final int waitSeconds = Math.max(1, waitSecondsStr != null && waitSecondsStr.matches("^\\s*\\d+\\s*$") ? Integer.parseInt(waitSecondsStr.trim()) : -1);
            final String host = getHost(downloadLink, null, true);
            final int requestInterval = Math.min(waitSeconds * 1000, 3000) + 1000;
            logger.info("Auto set session requestInterval:" + host + "->" + requestInterval);
            Browser.setRequestIntervalLimitGlobal(host, true, requestInterval);
            if (waitSeconds < 5 || ((Thread.currentThread() instanceof SingleDownloadController) && waitSeconds < 60)) {
                sleep(waitSeconds * 1000l, downloadLink);
                urlConnection = this.prepareConnection(this.br, downloadLink, optionSet);
            }
            if (HTTPConstants.ResponseCode.TOO_MANY_REQUESTS.matches(urlConnection.getResponseCode())) {
                followURLConnection(br, urlConnection);
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Error 429 rate limit reached", 5 * 60 * 1000l);
            }
        }
        return urlConnection;
    }

    protected AvailableStatus requestFileInformation(final DownloadLink downloadLink, int retry, Set<String> optionSet) throws Exception {
        if (retry == 0) {
            preSetFinalName = downloadLink.getFinalFileName();
            preSetFIXNAME = downloadLink.getStringProperty(FIXNAME, null);
            final Authentication auth = getSessionValue(downloadLink, SESSION_AUTHORIZATION, Authentication.class, null);
            if (auth != null) {
                logger.info("Auto set session authorization:" + auth.getUsername() + "@" + auth.getRealm() + "@" + auth.getHost());
                br.addAuthentication(auth);
            }
        }
        if (downloadLink.getBooleanProperty("OFFLINE", false) || downloadLink.getBooleanProperty("offline", false)) {
            // used to make offline links for decrypters. To prevent 'Checking online status' and/or prevent downloads of downloadLink.
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (retry == 5) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (optionSet == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        applyRateLimits(downloadLink, br);
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
            protected Authentication remember           = null;
            protected boolean        allowRequestLogins = true;

            protected Authentication askAuthentication(Browser browser, Request request, final String realm) {
                try {
                    final Login login = allowRequestLogins ? requestLogins(org.jdownloader.translate._JDT.T.DirectHTTP_getBasicAuth_message(), realm, downloadLink) : null;
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
                    if (e.getLinkStatus() == LinkStatus.ERROR_FATAL) {
                        allowRequestLogins = false;
                    }
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
            for (final AuthenticationFactory authenticationFactory : authenticationFactories) {
                br.setCustomAuthenticationFactory(authenticationFactory);
                urlConnection = this.prepareConnection(this.br, downloadLink, optionSet);
                urlConnection = handleRateLimit(downloadLink, optionSet, this.br, urlConnection);
                logger.info("looksLikeDownloadableContent result(" + retry + ",1):" + looksLikeDownloadableContent(urlConnection));
                if (isCustomOffline(urlConnection)) {
                    followURLConnection(br, urlConnection);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, urlConnection.getResponseCode() + "-" + urlConnection.getResponseMessage());
                }
                final String server = urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_SERVER);
                if (server != null && server.matches("(?i).*Apache/2.2.22.*")) {
                    /* server has issues with open end range requests */
                    optionSet.add("avoidOpenRange");
                }
                if (retryConnection(downloadLink, urlConnection) || (StringUtils.contains(urlConnection.getContentType(), "image") && (urlConnection.getLongContentLength() < 1024) || StringUtils.containsIgnoreCase(getFileNameFromConnection(urlConnection), "expired"))) {
                    final String possibleURLParams = getPossibleURLParams(downloadLink);
                    if (possibleURLParams != null || RequestMethod.HEAD.equals(urlConnection.getRequest().getRequestMethod())) {
                        followURLConnection(br, urlConnection);
                        if (possibleURLParams != null) {
                            /*
                             * check if we need the URLPARAMS to download the file
                             */
                            final String newURL = getDownloadURL(downloadLink) + possibleURLParams;
                            downloadLink.removeProperty(DirectHTTP.POSSIBLE_URLPARAM);
                            setDownloadURL(newURL, downloadLink);
                        } else if (RequestMethod.HEAD.equals(urlConnection.getRequest().getRequestMethod())) {
                            setPreferHeadRequest(optionSet, false);
                        }
                        br.setRequest(null);
                        urlConnection = this.prepareConnection(this.br, downloadLink, optionSet);
                        urlConnection = handleRateLimit(downloadLink, optionSet, this.br, urlConnection);
                        logger.info("looksLikeDownloadableContent result(" + retry + ",2):" + looksLikeDownloadableContent(urlConnection));
                        if (isCustomOffline(urlConnection)) {
                            followURLConnection(br, urlConnection);
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, urlConnection.getResponseCode() + "-" + urlConnection.getResponseMessage());
                        }
                    }
                }
                if (ResponseCode.ERROR_UNAUTHORIZED.matches(urlConnection.getResponseCode())) {
                    followURLConnection(br, urlConnection);
                    if (urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_WWW_AUTHENTICATE) == null) {
                        /* no basic auth */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, urlConnection.getResponseCode() + "-" + urlConnection.getResponseMessage());
                    }
                } else {
                    break;
                }
            }
            final ResponseCode responseCode = ResponseCode.get(urlConnection.getResponseCode());
            if (responseCode != null) {
                switch (responseCode) {
                case SERVERERROR_SERVICE_UNAVAILABLE:
                case GATEWAY_TIMEOUT:
                    followURLConnection(br, urlConnection);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, urlConnection.getResponseCode() + "-" + urlConnection.getResponseMessage(), 15 * 60 * 1000l);
                case SUCCESS_OK:
                    if (Boolean.FALSE.equals(verifyDownloadableContent(null, urlConnection))) {
                        followURLConnection(br, urlConnection);
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, urlConnection.getResponseCode() + "-" + urlConnection.getResponseMessage());
                    } else {
                        break;
                    }
                case SUCCESS_PARTIAL_CONTENT:
                    if (Boolean.FALSE.equals(verifyDownloadableContent(null, urlConnection))) {
                        followURLConnection(br, urlConnection);
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, urlConnection.getResponseCode() + "-" + urlConnection.getResponseMessage());
                    } else {
                        break;
                    }
                default:
                    followURLConnection(br, urlConnection);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, urlConnection.getResponseCode() + "-" + urlConnection.getResponseMessage());
                }
            } else {
                followURLConnection(br, urlConnection);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, urlConnection.getResponseCode() + "-" + urlConnection.getResponseMessage());
            }
            final String contentType = urlConnection.getContentType();
            if (contentType != null) {
                if (StringUtils.startsWithCaseInsensitive(contentType, "application/pls") && StringUtils.endsWithCaseInsensitive(urlConnection.getURL().getPath(), ".mp3")) {
                    followURLConnection(br, urlConnection);
                    final String mp3URL = this.br.getRegex("(https?://.*?\\.mp3)").getMatch(0);
                    if (hasCustomDownloadURL()) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (mp3URL != null) {
                        setDownloadURL(mp3URL, null);
                        return this.requestFileInformation(downloadLink, retry + 1, optionSet);
                    }
                }
            }
            final long length = urlConnection.getCompleteContentLength();
            if (length == 0 && RequestMethod.HEAD.equals(urlConnection.getRequest().getRequestMethod())) {
                setPreferHeadRequest(optionSet, false);
                followURLConnection(br, urlConnection);
                return this.requestFileInformation(downloadLink, retry + 1, optionSet);
            }
            if (urlConnection.getHeaderField("cf-bgj") != null && !downloadLink.hasProperty(BYPASS_CLOUDFLARE_BGJ)) {
                // TODO: add support for "Cf-Polished: status=not_needed"
                if (RequestMethod.HEAD.equals(urlConnection.getRequest().getRequestMethod())) {
                    followURLConnection(br, urlConnection);
                } else {
                    urlConnection.disconnect();
                }
                downloadLink.setProperty(BYPASS_CLOUDFLARE_BGJ, Boolean.TRUE);
                return this.requestFileInformation(downloadLink, retry + 1, optionSet);
            }
            String streamMod = null;
            for (Entry<String, List<String>> header : urlConnection.getHeaderFields().entrySet()) {
                if (StringUtils.startsWithCaseInsensitive(header.getKey(), "X-Mod-H264-Streaming")) {
                    streamMod = header.getKey();
                    break;
                } else if (StringUtils.startsWithCaseInsensitive(header.getKey(), "x-swarmify")) {
                    streamMod = header.getKey();
                    break;
                }
            }
            if (ResponseCode.SUCCESS_PARTIAL_CONTENT.matches(urlConnection.getResponseCode())) {
                final long[] responseRange = urlConnection.getRange();
                if (responseRange[1] < responseRange[2] - 1) {
                    streamMod = "limitedRangeLength";
                }
            }
            if (streamMod != null && downloadLink.getProperty("streamMod") == null) {
                downloadLink.setProperty("streamMod", streamMod);
                if (RequestMethod.HEAD.equals(urlConnection.getRequest().getRequestMethod())) {
                    followURLConnection(br, urlConnection);
                } else {
                    urlConnection.disconnect();
                }
                return this.requestFileInformation(downloadLink, retry + 1, optionSet);
            }
            if (contentType != null && (contentType.startsWith("text/html") || contentType.startsWith("application/json")) && urlConnection.isContentDisposition() == false && downloadLink.getBooleanProperty(DirectHTTP.TRY_ALL, false) == false && getDirectHTTPRule(downloadLink) == null) {
                try {
                    String possibleURLParams = getPossibleURLParams(downloadLink);
                    if (possibleURLParams != null) {
                        followURLConnection(br, urlConnection);
                        /*
                         * check if we need the URLPARAMS to download the file
                         */
                        final Browser br = this.br;
                        try {
                            this.br = br.cloneBrowser();
                            downloadLink.setProperty(DirectHTTP.POSSIBLE_URLPARAM, Property.NULL);
                            final String newURL = getDownloadURL(downloadLink) + possibleURLParams;
                            setDownloadURL(newURL, null);
                            final AvailableStatus ret = this.requestFileInformation(downloadLink, retry + 1, optionSet);
                            if (AvailableStatus.TRUE.equals(ret)) {
                                possibleURLParams = null;
                                setDownloadURL(newURL, downloadLink);
                                return ret;
                            }
                        } finally {
                            this.br = br;
                            downloadLink.setProperty(DirectHTTP.POSSIBLE_URLPARAM, possibleURLParams);
                        }
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
                /* jd does not want to download html content! */
                /* if this page does redirect via js/html, try to follow */
                if (RequestMethod.HEAD.equals(urlConnection.getRequest().getRequestMethod())) {
                    setPreferHeadRequest(optionSet, false);
                    followURLConnection(br, urlConnection);
                    return this.requestFileInformation(downloadLink, retry + 1, optionSet);
                } else {
                    final String pageContent = this.br.followConnection(true);
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
                    } else {
                        final String refreshRedirect = urlConnection.getRequest().getHTMLRefresh();
                        if (refreshRedirect != null) {
                            setDownloadURL(refreshRedirect, null);
                            return this.requestFileInformation(downloadLink, retry + 1, optionSet);
                        }
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
                                        break;
                                    } else {
                                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                                    }
                                }
                            }
                        }
                        if (embeddedLink == null) {
                            final String possibleURLParams = getPossibleURLParams(downloadLink);
                            if (possibleURLParams != null) {
                                downloadLink.setProperty(DirectHTTP.POSSIBLE_URLPARAM, Property.NULL);
                                final String newURL = getDownloadURL(downloadLink) + possibleURLParams;
                                setDownloadURL(newURL, downloadLink);
                                return this.requestFileInformation(downloadLink, retry + 1, optionSet);
                            }
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                    }
                    /* found one valid url */
                    setDownloadURL(embeddedLink, downloadLink);
                    return this.requestFileInformation(downloadLink, retry + 1, optionSet);
                }
            } else {
                if (RequestMethod.HEAD.equals(urlConnection.getRequest().getRequestMethod())) {
                    followURLConnection(br, urlConnection);
                } else {
                    urlConnection.disconnect();
                }
            }
            /* if final filename already set, do not change */
            if (downloadLink.getFinalFileName() != null && parseDispositionHeader(urlConnection) == null) {
                final String oldFinalFilename = downloadLink.getFinalFileName();
                final String newFinalFilename = correctOrApplyFileNameExtension(oldFinalFilename, null, urlConnection);
                if (!StringUtils.equals(oldFinalFilename, newFinalFilename)) {
                    logger.info("Updated finalFilenames' file extension | Old: " + oldFinalFilename + " | New: " + newFinalFilename);
                    downloadLink.setFinalFileName(newFinalFilename);
                }
            } else if (downloadLink.getFinalFileName() == null) {
                /* Restore filename from property */
                boolean allowFileExtensionCorrection = true;
                String fileName = downloadLink.getStringProperty(FIXNAME, null);
                if (StringUtils.isEmpty(fileName)) {
                    final DispositionHeader dispositionHeader = parseDispositionHeader(urlConnection);
                    if (dispositionHeader != null) {
                        // trust given filename extension via Content-Disposition header
                        allowFileExtensionCorrection = false;
                        fileName = dispositionHeader.getFilename();
                    }
                    if (StringUtils.isEmpty(fileName)) {
                        fileName = Plugin.extractFileNameFromURL(urlConnection.getRequest().getUrl());
                        if (StringUtils.isNotEmpty(fileName)) {
                            // TODO: Check if this is still needed
                            if (StringUtils.equalsIgnoreCase("php", Files.getExtension(fileName)) || fileName.matches(IP.IP_PATTERN)) {
                                fileName = null;
                            }
                        }
                    }
                    if (StringUtils.isNotEmpty(fileName) && downloadLink.getBooleanProperty("urlDecodeFinalFileName", true)) {
                        fileName = SimpleFTP.BestEncodingGuessingURLDecode(fileName);
                    }
                }
                if (StringUtils.isEmpty(fileName)) {
                    /* Get any cached name */
                    fileName = downloadLink.getName();
                }
                if (fileName != null) {
                    if (allowFileExtensionCorrection) {
                        fileName = correctOrApplyFileNameExtension(fileName, null, urlConnection);
                    }
                    downloadLink.setFinalFileName(fileName);
                    /* save filename in property so we can restore in reset case */
                    downloadLink.setProperty(FIXNAME, fileName);
                }
            }
            if (length >= 0) {
                downloadLink.setDownloadSize(length);
                final HashInfo connectionHashInfo = HTTPDownloader.getHashInfoFromHeaders(getLogger(), urlConnection);
                final HashInfo knownHashInfo = downloadLink.getHashInfo();
                if (connectionHashInfo != null && (knownHashInfo == null || (!knownHashInfo.isForced() && connectionHashInfo.isStrongerThan(knownHashInfo)))) {
                    downloadLink.setHashInfo(connectionHashInfo);
                }
                final String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
                if (urlConnection.getHeaderField("X-Mod-H264-Streaming") == null && (contentEncoding == null || "none".equalsIgnoreCase(contentEncoding))) {
                    if (downloadLink.getBooleanProperty(FORCE_NOVERIFIEDFILESIZE, false)) {
                        logger.info("Forced NoVerifiedFileSize:" + length);
                        downloadLink.setVerifiedFileSize(-1);
                    } else {
                        downloadLink.setVerifiedFileSize(length);
                    }
                }
            } else {
                downloadLink.setDownloadSize(-1);
                downloadLink.setVerifiedFileSize(-1);
            }
            final Authentication auth = br.getRequest().getAuthentication();
            if (auth != null) {
                putSessionValue(downloadLink, SESSION_AUTHORIZATION, auth);
            }
            final String referer = urlConnection.getRequestProperty(HTTPConstants.HEADER_REQUEST_REFERER);
            downloadLink.setProperty(PROPERTY_LAST_REFERER, referer);
            final RequestMethod requestMethod = urlConnection.getRequestMethod();
            downloadLink.setProperty("allowOrigin", urlConnection.getHeaderField("access-control-allow-origin"));
            downloadLink.removeProperty(IOEXCEPTIONS);
            AvailableStatus status = AvailableStatus.TRUE;
            if (RequestMethod.HEAD.equals(requestMethod)) {
                if (Boolean.TRUE.equals(getSessionValue(downloadLink, SESSION_TRUST_HEAD, Boolean.class, false))) {
                    logger.info("Trust head request(history)!");
                } else if (downloadLink.getStringProperty(PROPERTY_REQUEST_TYPE, null) != null) {
                    logger.info("Trust head request(stored)!");
                } else {
                    final String headFinalFileName = downloadLink.getFinalFileName();
                    final String headFIXNAME = downloadLink.getStringProperty(FIXNAME, null);
                    final long headFileSize = downloadLink.getVerifiedFileSize();
                    boolean trustHeadRequest = true;
                    final boolean preferHeadRequest = isPreferHeadRequest(optionSet);
                    try {
                        // trust preset FinalFileName
                        downloadLink.setFinalFileName(preSetFinalName);
                        // trust preset FIXNAME property
                        downloadLink.setProperty(FIXNAME, preSetFIXNAME);
                        downloadLink.setVerifiedFileSize(-1);
                        setPreferHeadRequest(optionSet, false);
                        status = this.requestFileInformation(downloadLink, retry + 1, optionSet);
                        if (AvailableStatus.TRUE.equals(status)) {
                            if (headFileSize != downloadLink.getVerifiedFileSize()) {
                                logger.info("Don't trust head request: contentLength mismatch! head:" + headFileSize + "!=get:" + downloadLink.getVerifiedFileSize());
                                trustHeadRequest = false;
                            }
                            if (preSetFinalName == null && !StringUtils.equals(headFinalFileName, downloadLink.getFinalFileName())) {
                                logger.info("Don't trust head request: name mismatch! head:" + headFinalFileName + "!=get:" + downloadLink.getFinalFileName());
                                trustHeadRequest = false;
                            }
                        }
                    } finally {
                        setPreferHeadRequest(optionSet, preferHeadRequest);
                        if (trustHeadRequest) {
                            logger.info("Trust head request(validated)!");
                            downloadLink.setProperty(PROPERTY_REQUEST_TYPE, requestMethod.name());
                            downloadLink.setVerifiedFileSize(headFileSize);
                            if (preSetFinalName == null) {
                                downloadLink.setFinalFileName(headFinalFileName);
                            }
                            if (preSetFIXNAME == null) {
                                downloadLink.setProperty(FIXNAME, headFIXNAME);
                            }
                            putSessionValue(downloadLink, SESSION_TRUST_HEAD, Boolean.TRUE);
                        } else {
                            putSessionValue(downloadLink, SESSION_TRUST_HEAD, Boolean.FALSE);
                        }
                    }
                }
            } else {
                downloadLink.setProperty(PROPERTY_REQUEST_TYPE, requestMethod.name());
            }
            final String acceptRanges = urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_ACCEPT_RANGES);
            if (StringUtils.containsIgnoreCase(acceptRanges, "none")) {
                downloadLink.setProperty(DirectHTTP.NORESUME, Boolean.TRUE);
            }
            return status;
        } catch (final PluginException e2) {
            final String finalFileName = downloadLink.getFinalFileName();
            resetDownloadlink(downloadLink);
            if (finalFileName != null) {
                downloadLink.setFinalFileName(finalFileName);
            }
            throw e2;
        } catch (IOException e) {
            logger.log(e);
            final String finalFileName = downloadLink.getFinalFileName();
            resetDownloadlink(downloadLink);
            if (finalFileName != null) {
                downloadLink.setFinalFileName(finalFileName);
            }
            if (!isConnectionOffline(e)) {
                final int nextIOExceptions = ioExceptions + 1;
                if (nextIOExceptions > 2) {
                    if (e instanceof java.net.ConnectException || e.getCause() instanceof java.net.ConnectException) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, -1, e);
                    } else if (e instanceof java.net.UnknownHostException || e.getCause() instanceof java.net.UnknownHostException) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, -1, e);
                    }
                }
                downloadLink.setProperty(IOEXCEPTIONS, nextIOExceptions);
            }
            String message = e.getMessage();
            if (message == null && e.getCause() != null) {
                message = e.getCause().getMessage();
            }
            if (e instanceof BlockedByException) {
                throw e;
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Network problem: " + message, 30 * 60 * 1000l, e);
            }
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

    protected static HashMap<String, Map<String, Object>> SESSION_MAP = new HashMap<String, Map<String, Object>>();

    protected <T> T getSessionValue(DownloadLink downloadLink, final String key, Class<T> clazz, T defaultValue) {
        synchronized (SESSION_MAP) {
            final String host = getHost(downloadLink, null, true);
            final Map<String, Object> map = SESSION_MAP.get(host);
            if (map == null || !map.containsKey(key)) {
                return defaultValue;
            } else {
                return (T) map.get(key);
            }
        }
    }

    protected void putSessionValue(DownloadLink downloadLink, final String key, Object value) {
        synchronized (SESSION_MAP) {
            final String host = getHost(downloadLink, null, true);
            if (key == null) {
                SESSION_MAP.remove(host);
            } else {
                Map<String, Object> map = SESSION_MAP.get(host);
                if (map == null) {
                    map = new HashMap<String, Object>();
                    SESSION_MAP.put(host, map);
                }
                map.put(key, value);
            }
        }
    }

    private final String IOEXCEPTIONS = "IOEXCEPTIONS";

    @Override
    public void reset() {
        customDownloadURL = null;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        setPreferHeadRequest(getOptionSet(link), true);
        link.removeProperty(IOEXCEPTIONS);
        link.removeProperty(DirectHTTP.NORESUME);
        link.removeProperty(DirectHTTP.NOCHUNKS);
        link.removeProperty(PROPERTY_LAST_REFERER);
        link.removeProperty(PROPERTY_REQUEST_TYPE);
        link.removeProperty("streamMod");
        link.removeProperty("allowOrigin");
        synchronized (OPTIONSETLOCK) {
            link.removeProperty(PROPERTY_OPTION_SET);
        }
        link.removeProperty(FORCE_NOVERIFIEDFILESIZE);
        link.removeProperty(BYPASS_CLOUDFLARE_BGJ);
        putSessionValue(link, null, null);
        /* E.g. filename set in crawler --> We don't want to lose that. */
        final String fixName = link.getStringProperty(FIXNAME, null);
        if (StringUtils.isNotEmpty(fixName)) {
            link.setFinalFileName(fixName);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }

    private String getRefererURL(final DownloadLink link) {
        /* Last referer used by this plugin with given DownloadLink. */
        final String lastReferer = link.getStringProperty(PROPERTY_LAST_REFERER);
        if (this.isValidReferer(lastReferer)) {
            return lastReferer;
        } else {
            /* This was used in some older plugins. */
            final String oldRefProperty1 = link.getStringProperty("Referer", link.getStringProperty("referer"));
            if (this.isValidReferer(oldRefProperty1)) {
                return oldRefProperty1;
            } else {
                /* This was used in some older plugins and by Flashgot(?) */
                final String oldRefProperty2 = link.getStringProperty("refURL");
                if (this.isValidReferer(oldRefProperty2)) {
                    return oldRefProperty2;
                } else {
                    final String pluginReferer = link.getReferrerUrl();
                    if (this.isValidReferer(pluginReferer)) {
                        return pluginReferer;
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    private boolean isValidReferer(final String referer) {
        if (StringUtils.startsWithCaseInsensitive(referer, "http://") || StringUtils.startsWithCaseInsensitive(referer, "https://")) {
            try {
                URLHelper.verifyURL(new URL(referer));
                return true;
            } catch (IOException ignore) {
                return false;
            }
        } else {
            return false;
        }
    }

    /** Sets custom headers (including cookie header). */
    private void setCustomHeaders(final Browser br, final DownloadLink downloadLink, Set<String> optionSet) throws IOException {
        /* allow customized headers, eg useragent */
        final Object customRet = downloadLink.getProperty(PROPERTY_HEADERS);
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
        if (downloadLink.getStringProperty(DirectHTTP.PROPERTY_COOKIES, null) != null) {
            br.getCookies(getDownloadURL(downloadLink)).add(Cookies.parseCookies(downloadLink.getStringProperty(DirectHTTP.PROPERTY_COOKIES, null), Browser.getHost(getDownloadURL(downloadLink)), null));
        }
        final LinkCrawlerRule rule = getDirectHTTPRule(downloadLink);
        if (rule != null && rule.isEnabled()) {
            /* Check for cookies by LinkCrawler rule */
            final String url = getDownloadURL(downloadLink);
            rule.applyCookies(br, url, false);
        }
        final String refererUrl = this.getRefererURL(downloadLink);
        if (refererUrl != null) {
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, refererUrl);
        } else if (optionSet == null || !optionSet.contains(PROPERTY_DISABLE_PREFIX + LinkCrawler.PROPERTY_AUTO_REFERER)) {
            final String autoRefererUrl = downloadLink.getStringProperty(LinkCrawler.PROPERTY_AUTO_REFERER);
            if (this.isValidReferer(autoRefererUrl)) {
                br.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, autoRefererUrl);
            }
        }
        if (optionSet != null && optionSet.contains(PROPERTY_ENABLE_PREFIX + "selfReferer")) {
            final String referer = getDownloadURL(downloadLink);
            br.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, referer);
        }
        this.downloadWorkaround(br, downloadLink);
        if (downloadLink.hasProperty("allowOrigin")) {
            final String referer = br.getHeaders().get(HTTPConstants.HEADER_REQUEST_REFERER);
            if (referer != null) {
                final URL refURL = new URL(referer);
                br.getHeaders().put("Origin", refURL.getProtocol() + "://" + refURL.getHost());
            } else {
                br.getHeaders().put("Origin", "*");
            }
        }
    }

    protected void downloadWorkaround(final Browser br, final DownloadLink downloadLink) throws IOException {
        // we shouldn't potentially over right setCustomHeaders..
        if (br.getHeaders().getValue(HTTPConstants.HEADER_REQUEST_REFERER) == null) {
            final String link = getDownloadURL(downloadLink);
            if (link.contains("sites.google.com")) {
                /*
                 * It seems google checks referer and ip must have called the page lately.
                 *
                 * TODO: 2021-12-07 Check if this is still required
                 */
                br.getHeaders().put(HTTPConstants.HEADER_REQUEST_REFERER, "https://sites.google.com");
            }
        }
    }

    @Override
    protected void updateDownloadLink(final CheckableLink checkableLink, final String url) {
        final DownloadLink downloadLink = checkableLink != null ? checkableLink.getDownloadLink() : null;
        if (downloadLink != null) {
            final List<DownloadLink> downloadLinks = getDownloadLinks(null, url, null);
            if (downloadLinks != null && downloadLinks.size() == 1) {
                downloadLink.setPluginPatternMatcher(downloadLinks.get(0).getPluginPatternMatcher());
            } else {
                downloadLink.setPluginPatternMatcher(url);
            }
            downloadLink.setDomainInfo(null);
            downloadLink.resume(Arrays.asList(new PluginForHost[] { this }));
            preProcessDirectHTTP(downloadLink, url);
            final LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
            linkChecker.check(checkableLink);
        }
    }

    /**
     * custom offline references based on conditions found within previous URLConnectionAdapter request.
     *
     * @author raztoki
     */
    private boolean isCustomOffline(URLConnectionAdapter urlConnection) {
        return false;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    protected boolean supportsUpdateDownloadLink(CheckableLink checkableLink) {
        return checkableLink != null && checkableLink.getDownloadLink() != null;
    }

    @Override
    public boolean isHandlingMultipleHosts() {
        return true;
    }

    @Override
    public String getHost(final DownloadLink link, final Account account, final boolean includeSubdomain) {
        if (link != null) {
            final String customHost = link.getStringProperty(PROPERTY_CUSTOM_HOST, null);
            if (StringUtils.isNotEmpty(customHost)) {
                return customHost;
            } else {
                final String contentURL = link.getContentUrl();
                String ret;
                if (contentURL != null) {
                    ret = Browser.getHost(contentURL, includeSubdomain);
                } else {
                    /* prefer domain via public suffic list */
                    ret = Browser.getHost(link.getPluginPatternMatcher(), includeSubdomain);
                }
                if (includeSubdomain) {
                    // we don't want www. subdomain
                    ret = ret != null ? ret.replaceFirst("(?i)^www\\.", "") : ret;
                }
                return ret;
            }
        } else if (account != null) {
            return account.getHoster();
        } else {
            return null;
        }
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public static String createURLForThisPlugin(final String url) {
        return url == null ? null : "directhttp://" + url;
    }
}