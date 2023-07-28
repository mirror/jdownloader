package org.jdownloader.plugins.components;

import java.awt.Color;
//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.jdownloader.captcha.v2.CaptchaHosterHelperInterface;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.components.config.XFSConfig;
import org.jdownloader.plugins.components.config.XFSConfigVideo;
import org.jdownloader.plugins.components.config.XFSConfigVideo.DownloadMode;
import org.jdownloader.plugins.components.config.XFSConfigVideo.PreferredDownloadQuality;
import org.jdownloader.plugins.components.config.XFSConfigVideo.PreferredStreamQuality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DefaultEditAccountPanel;
import jd.plugins.DownloadConnectionVerifier;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class XFileSharingProBasic extends antiDDoSForHost implements DownloadConnectionVerifier {
    public XFileSharingProBasic(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(super.getPurchasePremiumURL());
    }

    // public static List<String[]> getPluginDomains() {
    // final List<String[]> ret = new ArrayList<String[]>();
    // // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
    // ret.add(new String[] { "imgdew.com" });
    // return ret;
    // }
    //
    // public static String[] getAnnotationNames() {
    // return buildAnnotationNames(getPluginDomains());
    // }
    //
    // @Override
    // public String[] siteSupportedNames() {
    // return buildSupportedNames(getPluginDomains());
    // }
    //
    // public static String[] getAnnotationUrls() {
    // return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    // }
    // @Override
    // public String rewriteHost(final String host) {
    // return this.rewriteHost(getPluginDomains(), host);
    // }
    public static final String getDefaultAnnotationPatternPart() {
        return "/(?:d/[A-Za-z0-9]+|(?:embed-|e/)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?)";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?::\\d+)?" + XFileSharingProBasic.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
    }

    /**
     * Override this and add dead domains so upper handling can auto update added URLs and change domain if it contains a dead domain. This
     * way a lot of "old" URLs will continue to work in JD while they may fail in browser. </br>
     * TODO: Make use of this.
     */
    protected List<String> getDeadDomains() {
        return null;
    }

    /* Used variables */
    @Deprecated
    public String                             correctedBR                                                       = "";
    protected WeakHashMap<Request, String>    correctedBrowserRequestMap                                        = new WeakHashMap<Request, String>();
    /* Don't touch the following! */
    private static Map<String, AtomicInteger> freeRunning                                                       = new HashMap<String, AtomicInteger>();
    protected static final String             PROPERTY_captcha_required                                         = "captcha_requested_by_website";
    protected static final String             PROPERTY_ACCOUNT_apikey                                           = "apikey";
    private static final String               PROPERTY_PLUGIN_api_domain_with_protocol                          = "apidomain";
    private static final String               PROPERTY_PLUGIN_REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_TIMESTAMP = "REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_TIMESTAMP";
    private static final String               PROPERTY_PLUGIN_REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_VERSION   = "REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_VERSION";
    private static final String               PROPERTY_PLUGIN_ALT_AVAILABLECHECK_LAST_FAILURE_TIMESTAMP         = "ALT_AVAILABLECHECK_LAST_FAILURE_TIMESTAMP";
    private static final String               PROPERTY_PLUGIN_ALT_AVAILABLECHECK_LAST_FAILURE_VERSION           = "ALT_AVAILABLECHECK_LAST_FAILURE_VERSION";
    private static final String               PROPERTY_PLUGIN_ALT_AVAILABLECHECK_LAST_WORKING                   = "ALT_AVAILABLECHECK_LAST_WORKING";
    protected static final String             PROPERTY_ACCOUNT_INFO_TRUST_UNLIMITED_TRAFFIC                     = "trust_unlimited_traffic";

    public static enum URL_TYPE {
        EMBED_VIDEO,
        FILE,
        IMAGE,
        NORMAL,
        SHORT,
        OFFICIAL_VIDEO_DOWNLOAD
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        final List<LazyPlugin.FEATURE> ret = new ArrayList<LazyPlugin.FEATURE>();
        if (requiresCookieLogin()) {
            ret.add(LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY);
        } else {
            ret.add(LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL);
        }
        if (isImagehoster()) {
            ret.add(LazyPlugin.FEATURE.IMAGE_HOST);
        }
        if (isVideohoster()) {
            ret.add(LazyPlugin.FEATURE.VIDEO_STREAMING);
        }
        return ret.toArray(new LazyPlugin.FEATURE[0]);
    }

    /* TODO: Maybe add thumbnail -> Fullsize support for all XFS plugins, see ImagetwistCom and ImgSpiceCom */
    // private static final String TYPE_DIRECT_IMAGE_FULLSIZE = "(?i)https?:///i/\\d+/[a-z0-9]{12}\\.jpg";
    // private static final String TYPE_DIRECT_IMAGE_THUMBNAIL = "(?i)https?:///th/\\d+/[a-z0-9]{12}\\.jpg";
    /**
     * DEV NOTES XfileSharingProBasic Version 4.4.3.8<br />
     * See official changelogs for upcoming XFS changes: https://sibsoft.net/xfilesharing/changelog.html |
     * https://sibsoft.net/xvideosharing/changelog.html <br/>
     * limit-info:<br />
     * captchatype-info: null 4dignum solvemedia reCaptchaV2, hcaptcha<br />
     */
    @Override
    public String getAGBLink() {
        return this.getMainPage() + "/tos.html";
    }

    public String getPurchasePremiumURL() {
        return this.getMainPage() + "/premium.html";
    }

    /**
     * Returns whether resume is supported or not for current download mode based on account availability and account type. <br />
     * Override this function to set resume settings!
     */
    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    /**
     * Returns how many max. chunks per file are allowed for current download mode based on account availability and account type. <br />
     * Override this function to set chunks settings!
     */
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    protected AtomicInteger getFreeRunning() {
        synchronized (freeRunning) {
            AtomicInteger ret = freeRunning.get(getHost());
            if (ret == null) {
                ret = new AtomicInteger(0);
                freeRunning.put(getHost(), ret);
            }
            return ret;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        final int max = getMaxSimultaneousFreeAnonymousDownloads();
        if (max == -1) {
            return -1;
        } else {
            final int running = getFreeRunning().get();
            final int ret = Math.min(running + 1, max);
            return ret;
        }
    }

    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /** Returns property to store generated directurl -> Depends on current download mode and account account type. */
    protected static String getDownloadModeDirectlinkProperty(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return "freelink2";
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return "premlink";
        } else {
            /* Free(anonymous) and unknown account type */
            return "freelink";
        }
    }

    protected boolean useHTTPS() {
        return websiteSupportsHTTPS() && userPrefersHTTPS();
    }

    /**
     * @return true: Website supports https and plugin will prefer https. <br />
     *         false: Website does not support https - plugin will avoid https. <br />
     *         default: true
     */
    protected boolean websiteSupportsHTTPS() {
        return true;
    }

    protected boolean userPrefersHTTPS() {
        final Class<? extends XFSConfig> cfgO = this.getConfigInterface();
        if (cfgO != null) {
            return !PluginJsonConfig.get(cfgO).isPreferHTTP();
        } else {
            return true;
        }
    }

    /**
     * Relevant for premium accounts.
     *
     * @return A list of possible 'paymentURLs' which may contain an exact premium account expire-date down to the second. Return null to
     *         disable this feature!
     */
    protected String[] supportsPreciseExpireDate() {
        return new String[] { "/?op=payments", "/upgrade" };
    }

    /**
     * <b> Enabling this leads to at least one additional http-request! </b> <br />
     * Enable this for websites using <a href="https://sibsoft.net/xvideosharing.html">XVideosharing</a>. <br />
     * Demo-Website: <a href="http://xvideosharing.com">xvideosharing.com</a> DO NOT CALL THIS DIRECTLY - ALWAYS USE
     * {@link #internal_isVideohosterEmbed()}!!!<br />
     *
     * @return true: Try to find final downloadlink via '/embed-<fuid>.html' request. <br />
     *         false: Skips this part. <br />
     *         default: false
     */
    protected boolean isVideohosterEmbed() {
        return false;
    }

    /**
     * Checks whether current html code contains embed code for current fuid which would indicate that we have a videohost and it looks like
     * we can access the embed URL to stream/download our video content. </br>
     * </b> Attention! Browser can be null! </b>
     */
    protected boolean isVideohosterEmbedHTML(final Browser br) {
        if (br == null) {
            return false;
        } else if (br.containsHTML("/embed-" + this.getFUIDFromURL(this.getDownloadLink()) + "\\.html")) {
            return true;
        } else if (br.containsHTML("/e/" + this.getFUIDFromURL(this.getDownloadLink()))) {
            /* A lot of newer XFS templates got such embed URLs. */
            return true;
        } else if (br.containsHTML("(?i)This video can be watched as embed only\\s*<")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Keep in mind: Most videohosters will allow embedding their videos thus a "video filename" should be enforced but they may also
     * sometimes NOT support embedding videos while a "video filename" should still be enforced - then this trigger might be useful! </br>
     * DO NOT CALL THIS FUNCTION DIRECTLY! Use {@link #internal_isVideohoster_enforce_video_filename()} instead!!
     *
     * @return true: Implies that the hoster only allows video-content to be uploaded. Enforces .mp4 extension for all URLs. Also sets
     *         mime-hint via CompiledFiletypeFilter.VideoExtensions.MP4. <br />
     *         false: Website is just a normal filehost and their filenames should contain the fileextension. <br />
     *         default: false
     */
    protected boolean isVideohoster_enforce_video_filename() {
        return false;
    }

    /**
     * Enable this for websites using <a href="https://sibsoft.net/ximagesharing.html">XImagesharing</a>. <br />
     * Demo-Website: <a href="http://ximagesharing.com">ximagesharing.com</a>
     *
     * @return true: Implies that the hoster only allows photo-content to be uploaded. Enabling this will make plugin try to find
     *         picture-downloadlinks. Also sets mime-hint via CompiledFiletypeFilter.ImageExtensions.JPG. <br />
     *         false: Website is just an usual filehost, use given fileextension if possible. <br />
     *         default: false
     */
    protected boolean isImagehoster() {
        return false;
    }

    /** Indicates that this website is hosting video content only. */
    private boolean isVideohoster() {
        return isVideohosterEmbed() || isVideohosterEmbedHTML(br);
    }

    /**
     * See also function {@link #getFilesizeViaAvailablecheckAlt()} ! <br />
     * <b> Enabling this will eventually lead to at least one additional website-request! </b> <br/>
     * <b>DO NOT CALL THIS DIRECTLY, USE {@link #internal_supports_availablecheck_alt()} </b>
     *
     * @return true: Implies that website supports {@link #getFilesizeViaAvailablecheckAlt()} call as an alternative source for
     *         filesize-parsing.<br />
     *         false: Implies that website does NOT support {@link #getFilesizeViaAvailablecheckAlt()}. <br />
     *         default: true
     */
    protected boolean supports_availablecheck_alt() {
        return true;
    }

    /**
     * Only works when {@link #getFilesizeViaAvailablecheckAlt(Browser, DownloadLink))} returns true! See
     * {@link #getFilesizeViaAvailablecheckAlt()}!
     *
     * @return true: Implies that website supports {@link #getFilesizeViaAvailablecheckAlt(Browser, DownloadLink))} call without
     *         Form-handling (one call less than usual) as an alternative source for filesize-parsing. <br />
     *         false: Implies that website does NOT support {@link #getFilesizeViaAvailablecheckAlt(Browser, DownloadLink))} without
     *         Form-handling. <br />
     *         default: true
     */
    protected boolean supports_availablecheck_filesize_alt_fast() {
        return true;
    }

    /**
     * See also function getFilesizeViaAvailablecheckAlt!
     *
     * @return true: Website uses old version of getFilesizeViaAvailablecheckAlt. Old will be tried first, then new if it fails. <br />
     *         false: Website uses current version of getFilesizeViaAvailablecheckAlt - it will be used first and if it fails, old call will
     *         be tried. <br />
     *         2019-07-09: Do not override this anymore - this code will auto-detect this situation!<br/>
     *         default: false
     */
    @Deprecated
    protected boolean prefer_availablecheck_filesize_alt_type_old() {
        return false;
    }

    /**
     * See also function {@link #getFnameViaAbuseLink()}!<br />
     * <b> Enabling this will eventually lead to at least one additional website-request! </b> <br/>
     * DO NOT CALL THIS DIRECTLY - ALWAYS USE {@link #internal_supports_availablecheck_filename_abuse()}!!<br />
     *
     * @return true: Implies that website supports {@link #getFnameViaAbuseLink() } call as an alternative source for filename-parsing.
     *         <br />
     *         false: Implies that website does NOT support {@link #getFnameViaAbuseLink()}. <br />
     *         default: true
     */
    protected boolean supports_availablecheck_filename_abuse() {
        return true;
    }

    /**
     * @return true: Try to RegEx filesize from normal html code. If this fails due to static texts on a website or even fake information,
     *         all links of a filehost may just get displayed with the same/wrong filesize. <br />
     *         false: Do not RegEx filesize from normal html code. </br>
     *         Plugin will still be able to find filesize if {@link #supports_availablecheck_alt()} or
     *         {@link #supports_availablecheck_alt_fast()} is enabled (=default)! <br />
     *         default: true
     */
    protected boolean supports_availablecheck_filesize_html() {
        return true;
    }

    /**
     * This is designed to find the filesize during availablecheck for videohosts based on checking their directURLs! Videohosts usually
     * don't display the filesize anywhere! <br />
     * CAUTION: Only set this to true if a filehost: <br />
     * 1. Allows users to embed videos via '/embed-<fuid>.html'. <br />
     * 2. Does not display a filesize anywhere inside html code or other calls where we do not have to do an http request on a directurl.
     * <br />
     * 3. Allows a lot of simultaneous connections. <br />
     * 4. Is FAST - if it is not fast, this will noticably slow down the linkchecking procedure! <br />
     * 5. Allows using a generated direct-URL at least two times.
     *
     * @return true: {@link #requestFileInformation(DownloadLink)} will use '/embed' to do an additional offline-check and find the
     *         filesize. <br />
     *         false: Disable this.<br />
     *         default: false
     */
    protected boolean supports_availablecheck_filesize_via_embedded_video() {
        return false;
    }

    /**
     * A correct setting increases linkcheck-speed as unnecessary redirects will be avoided. <br />
     * Also in some cases, you may get 404 errors or redirects to other websites if this setting is not correct.
     *
     * @return true: Implies that website requires 'www.' in all URLs. <br />
     *         false: Implies that website does NOT require 'www.' in all URLs. <br />
     *         default: false
     */
    protected boolean requiresWWW() {
        return false;
    }

    /**
     * Use HEAD or GET request for checking directurls? </br>
     * Example HEAD request unsupported: 2022-11-25: no example anymore :(
     *
     * @return default: true
     *
     */
    protected boolean supportsHEADRequestForDirecturlCheck() {
        return true;
    }

    /**
     * Implies that a host supports login via 'API Mod'[https://sibsoft.net/xfilesharing/mods/api.html] via one of these APIs:
     * https://xvideosharing.docs.apiary.io/ OR https://xfilesharingpro.docs.apiary.io/ <br />
     * Enabling this will do the following: </br>
     * - Change login process to accept apikey instead of username & password </br>
     * - Use API for single- and mass linkchecking </br>
     * - Enforce API usage on account downloads: Never download via website, does NOT fallback to website! </br>
     * Sadly, it seems like their linkcheck function often only works for self uploaded conent. </br>
     * API docs: https://xvideosharing.docs.apiary.io/#reference/file/file-info/get-info/check-file(s) <br />
     * 2019-08-20: Some XFS websites are supported via another API via play.google.com/store/apps/details?id=com.zeuscloudmanager --> This
     * has nothing to do with the official XFS API! </br>
     * Example: xvideosharing.com, clicknupload.co <br />
     * default: false
     */
    protected boolean enableAccountApiOnlyMode() {
        return false;
    }

    /** If needed, this can be used to enforce cookie login e.g. if an unsupported captcha type is required for login. */
    protected boolean requiresCookieLogin() {
        return false;
    }

    protected boolean allowAPIDownloadIfApikeyIsAvailable(final DownloadLink link, final Account account) {
        final boolean apikey_is_available = this.getAPIKeyFromAccount(account) != null;
        /* Enable this switch to be able to use this in dev mode. Default = off as we do not use the API by default! */
        final boolean allow_api_premium_download = false;
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && apikey_is_available && allow_api_premium_download) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean allowAPIAvailablecheckInPremiumModeIfApikeyIsAvailable(final Account account) {
        final boolean apikey_is_available = this.getAPIKeyFromAccount(account) != null;
        /* Enable this switch to be able to use this in dev mode. Default = off as we do not use the API by default! */
        final boolean allow_api_availablecheck_in_premium_mode = false;
        return DebugMode.TRUE_IN_IDE_ELSE_FALSE && apikey_is_available && allow_api_availablecheck_in_premium_mode;
    }

    /**
     * If enabled, API will be used to import (public) files into users' account and download them from there. </br>
     * This may sometimes be the only way to download via API because until now (2019-10-31) the XFS API can only be used to download files
     * which the user itself uploaded (= files which are in his account). </br>
     * Warning! The imported files may be PUBLIC as well by default! </br>
     * So far this exists for development purposes ONLY!!
     */
    protected boolean requiresAPIGetdllinkCloneWorkaround(final Account account) {
        /* Enable this switch to be able to use this in dev mode. Default = off as we do not use this workaround by default! */
        final boolean allow_dllink_clone_workaround = false;
        return DebugMode.TRUE_IN_IDE_ELSE_FALSE && allow_dllink_clone_workaround;
    }

    /**
     * @return: Skip pre-download waittime or not. See waitTime function below. <br />
     *          default: false <br />
     *          example true: uploadrar.com
     */
    protected boolean preDownloadWaittimeSkippable() {
        return false;
    }

    /**
     * This is especially useful if a website e.g. provides URLs in this style by default:
     * https://website.com/[a-z0-9]{12}/filename.ext.html --> Then we already have the filename which is perfect as the website mass
     * linkchecker will only return online status (and filesize if the XFS website is up-to-date). </br>
     * You should really only use this if the mass-linkchecker returns filesizes!
     *
     * @default false
     */
    protected boolean supportsMassLinkcheckOverWebsite() {
        return false;
    }

    /**
     * Set this to false if a website is using links that look like short URLs but are not short URLs. </br>
     * Example: streamhide.com
     */
    protected boolean supportsShortURLs() {
        // TODO: 2023-07-25: Change this to false by default
        return true;
    }

    @Override
    public String getLinkID(DownloadLink link) {
        final String fuid = getFUIDFromURL(link);
        if (fuid != null) {
            return getHost() + "://" + fuid;
        } else {
            return super.getLinkID(link);
        }
    }

    protected boolean isEmbedURL(final DownloadLink link) {
        return URL_TYPE.EMBED_VIDEO.equals(getURLType(link));
    }

    protected boolean isEmbedURL(final String url) {
        return URL_TYPE.EMBED_VIDEO.equals(getURLType(url));
    }

    protected String buildEmbedURLPath(DownloadLink link, final String fuid) {
        return buildURLPath(link, fuid, URL_TYPE.EMBED_VIDEO);
    }

    protected String buildNormalURLPath(DownloadLink link, final String fuid) {
        return buildURLPath(link, fuid, URL_TYPE.NORMAL);
    }

    protected String buildImageURLPath(DownloadLink link, final String fuid) {
        return buildURLPath(link, fuid, URL_TYPE.IMAGE);
    }

    protected String buildNormalFileURLPath(DownloadLink link, final String fuid) {
        return buildURLPath(link, fuid, URL_TYPE.FILE);
    }

    protected String buildShortURLPath(DownloadLink link, final String fuid) {
        return buildURLPath(link, fuid, URL_TYPE.SHORT);
    }

    protected String buildURLPath(final DownloadLink link, final String fuid, final URL_TYPE type) {
        switch (type) {
        case EMBED_VIDEO:
            return "/embed-" + fuid + ".html";
        case FILE:
            return "/file/" + fuid;
        case IMAGE:
            return "/" + fuid;
        case NORMAL:
            return "/" + fuid;
        case SHORT:
            return "/d/" + fuid;
        case OFFICIAL_VIDEO_DOWNLOAD:
            return "/d/" + fuid;
        default:
            throw new IllegalArgumentException("Unsupported type:" + type + "|" + fuid);
        }
    }

    /**
     * Returns the desired host. Override is required in some cases where given host can contain unwanted subdomains e.g. imagetwist.com.
     */
    protected String getPreferredHost(final DownloadLink link, URL url) {
        if (isImagehoster()) {
            return getHost();
        } else {
            return url.getHost();
        }
    }

    protected boolean allowGetProtocolHttpsAutoHandling(final String url) {
        return true;
    }

    /**
     * Returns URL to content. </br>
     * Uses original domain whenever possible. </br>
     * TODO add custom support to keep custom port, eg vidspeeds.com
     */
    protected String getContentURL(final DownloadLink link) {
        if (link == null) {
            return null;
        }
        final String originalURL = link.getPluginPatternMatcher();
        if (originalURL == null) {
            return null;
        }
        final String fuid = getFUIDFromURL(link);
        if (fuid == null) {
            return null;
        }
        /* link cleanup, prefer https if possible */
        try {
            final URL url = new URL(originalURL);
            final String urlHost = getPreferredHost(link, url);
            final String protocol;
            if ("https".equalsIgnoreCase(url.getProtocol()) && allowGetProtocolHttpsAutoHandling(originalURL)) {
                protocol = "https://";
            } else if (this.useHTTPS()) {
                protocol = "https://";
            } else {
                protocol = "http://";
            }
            /* Get full host with subdomain and correct base domain. */
            final String pluginHost = this.getHost();
            final List<String> deadDomains = this.getDeadDomains();
            final String host;
            if (deadDomains != null && deadDomains.contains(urlHost)) {
                /* Fallback to plugin domain */
                /* e.g. down.xx.com -> down.yy.com, keep subdomain(s) */
                host = urlHost.replaceFirst("(?i)" + Pattern.quote(Browser.getHost(url, false)) + "$", pluginHost);
            } else {
                /* Use preferred host */
                host = urlHost;
            }
            final String hostCorrected = this.appendWWWIfRequired(host);
            final URL_TYPE type = getURLType(link);
            if (type != null) {
                switch (type) {
                case SHORT:
                    /*
                     * Important! Do not change the domain in shorturls! Some host have extra domains for shortURLs which means they may not
                     * work with the current main domain of the filehost!!
                     */
                    return URLHelper.parseLocation(new URL(protocol + urlHost), buildShortURLPath(link, fuid));
                case EMBED_VIDEO:
                    /*
                     * URL displayed to the user. We correct this as we do not catch the ".html" part but we don't care about the host
                     * inside this URL!
                     */
                    link.setContentUrl(URLHelper.parseLocation(new URL(url.getProtocol() + "://" + urlHost), buildEmbedURLPath(link, fuid)));
                    return URLHelper.parseLocation(new URL(protocol + hostCorrected), buildNormalURLPath(link, fuid));
                case FILE:
                    return URLHelper.parseLocation(new URL(protocol + hostCorrected), buildNormalFileURLPath(link, fuid));
                case NORMAL:
                    return URLHelper.parseLocation(new URL(protocol + hostCorrected), buildNormalURLPath(link, fuid));
                case IMAGE:
                    return URLHelper.parseLocation(new URL(protocol + hostCorrected), buildImageURLPath(link, fuid));
                default:
                    return URLHelper.parseLocation(new URL(protocol + hostCorrected), buildURLPath(link, fuid, type));
                }
            }
        } catch (final MalformedURLException e) {
            logger.log(e);
        }
        /* Return unmodified url. */
        return originalURL;
    }

    @Override
    public String buildExternalDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        return getContentURL(downloadLink);
    }

    @Override
    public Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* Define custom browser headers and preferred language */
            prepBr.setCookie(getMainPage(), "lang", "english");
            prepBr.setAllowedResponseCodes(new int[] { 500 });
        }
        return prepBr;
    }

    /**
     * Returns https?://host.tld ATTENTION: On override, make sure that current browsers' host still gets preferred over plugin host. </br>
     * If a subdomain is required, do not use this method before making a browser request!!
     */
    @Deprecated
    protected String getMainPage() {
        return getMainPage(this.br);
    }

    protected String getMainPage(final DownloadLink link) {
        // TODO: Improve & simplify this
        final URL url;
        try {
            url = new URL(link.getPluginPatternMatcher());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        final String urlHost = this.getPreferredHost(link, url);
        final List<String> deadDomains = this.getDeadDomains();
        final String domainToUse;
        if (deadDomains != null && deadDomains.contains(urlHost)) {
            domainToUse = this.getHost();
        } else {
            domainToUse = urlHost;
        }
        final String protocol;
        if ("https".equalsIgnoreCase(url.getProtocol()) && allowGetProtocolHttpsAutoHandling(url.toExternalForm())) {
            protocol = "https://";
        } else if (this.useHTTPS()) {
            protocol = "https://";
        } else {
            protocol = "http://";
        }
        final String finalDomainToUse = this.appendWWWIfRequired(domainToUse);
        return protocol + finalDomainToUse;
    }

    /** Adds "www." to given host if it doesn't already contain that and if it doesn't contain any other subdomain. */
    protected String appendWWWIfRequired(final String host) {
        if (!requiresWWW() || StringUtils.startsWithCaseInsensitive(host, "www.")) {
            // do not modify host
            return host;
        } else {
            final String hostTld = Browser.getHost(host, false);
            if (!StringUtils.equalsIgnoreCase(host, hostTld)) {
                // keep subdomain
                return host;
            } else {
                // add www.
                return "www." + host;
            }
        }
    }

    protected String getMainPage(final Browser br) {
        final Request request = br != null ? br.getRequest() : null;
        final String host;
        if (request != null) {
            /* Has a browser request been done before? Use this domain as it could e.g. differ from the plugin set main domain. */
            host = request.getURL().getHost();
        } else {
            /* Return current main domain */
            /* 2019-07-25: This may not be correct out of the box e.g. for imgmaze.com */
            host = this.getHost();
        }
        final String protocol;
        if (request != null && "https".equalsIgnoreCase(request.getURL().getProtocol()) && allowGetProtocolHttpsAutoHandling(request.getUrl())) {
            protocol = "https://";
        } else if (this.useHTTPS()) {
            protocol = "https://";
        } else {
            protocol = "http://";
        }
        final String finalHost = this.appendWWWIfRequired(host);
        return protocol + finalHost;
    }

    /**
     * @return true: Link is password protected <br />
     *         false: Link is not password protected
     */
    public boolean isPasswordProtectedHTML(final Browser br, final Form pwForm) {
        final String pattern = "<br>\\s*<b>\\s*Passwor(d|t)\\s*:\\s*</b>\\s*(<input|</div)";
        boolean ret = br.containsHTML(pattern);
        if (ret) {
            /* Double-check in cleaned HTML */
            ret = new Regex(correctBR(br), pattern).matches();
        }
        return ret;
    }

    /**
     * Checks premiumonly status based on current Browser-URL.
     *
     * @return true: Link only downloadable for premium users (sometimes also for registered users). <br />
     *         false: Link is downloadable for all users.
     */
    private boolean isPremiumOnlyURL(final Browser br) {
        return br != null && br.getURL() != null && br.getURL().contains("/?op=login&redirect=");
    }

    /**
     * Checks premiumonly status via current Browser-HTML AND URL via isPremiumOnlyURL.
     *
     * @return true: Link only downloadable for premium users (sometimes also for registered users). <br />
     *         false: Link is downloadable for all users.
     */
    public boolean isPremiumOnly(final Browser br) {
        final boolean premiumonly_by_url = isPremiumOnlyURL(br);
        final String premiumonly_filehost_regex = "( can download files up to |>\\s*Upgrade your account to download (?:larger|bigger) files|>\\s*The file you requested reached max downloads limit for Free Users|Please Buy Premium To download this file\\s*<|>\\s*This file reached max downloads limit|>\\s*This file is available for\\s*(<[^>]*>)?\\s*Premium Users only|>\\s*Available Only for Premium Members|>\\s*File is available only for Premium users|>(\\s*Sorry\\s*,)?\\s*This file (can|only can|can only) be downloaded by)";
        final String premiumonly_videohost_regex = ">\\s*This video is available for Premium users only";
        final boolean premiumonly_filehost = br != null && br.getRegex(premiumonly_filehost_regex).matches();
        final String corrected = getCorrectBR(br);
        final boolean premiumonly_filehost_corrected = new Regex(corrected, premiumonly_filehost_regex).matches();
        /* 2019-05-30: Example: xvideosharing.com */
        final boolean premiumonly_videohost = br != null && br.containsHTML(premiumonly_videohost_regex);
        final boolean premiumonly_videohost_corrected = new Regex(corrected, premiumonly_videohost_regex).matches();
        return premiumonly_by_url || premiumonly_filehost || premiumonly_filehost_corrected || premiumonly_videohost || premiumonly_videohost_corrected;
    }

    /**
     * @return true: Downloadserver is in maintenance mode - downloads are not possible but linkcheck may be possible. <br />
     *         false: Downloadserver is not in maintenance mode and should be possible.
     */
    protected boolean isServerUnderMaintenance(final Browser br) {
        final String pattern = "(?i)>\\s*This server is in maintenance mode";
        return br.getHttpConnection().getResponseCode() == 500 || (br.containsHTML(pattern) && new Regex(correctBR(br), pattern).matches());
    }

    protected boolean isOffline(final DownloadLink link, final Browser br, final String correctedBR) {
        /* 2020-12-11:e.g. "video you are looking for is not found": dood.to | doodstream.com */
        return br.getHttpConnection().getResponseCode() == 404 || new Regex(correctedBR, "(?i)(No such file|>\\s*File Not Found\\s*<|>\\s*The file was removed by|Reason for deletion:\n|File Not Found|>\\s*The file expired|>\\s*Sorry, we can't find the page you're looking for|>\\s*File could not be found due to expiration or removal by the file owner|>\\s*The file of the above link no longer exists|>\\s*video you are looking for is not found|>\\s*The file you were looking for doesn't exist)").matches();
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        final String apiKey = this.getAPIKey();
        if ((isAPIKey(apiKey) && this.supportsAPIMassLinkcheck()) || enableAccountApiOnlyMode()) {
            return massLinkcheckerAPI(urls, apiKey);
        } else if (supportsMassLinkcheckOverWebsite()) {
            return this.massLinkcheckerWebsite(urls);
        } else {
            /* No mass linkchecking possible */
            return false;
        }
    }

    @Override
    public boolean enoughTrafficFor(final DownloadLink link, final Account account) throws Exception {
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        final String dllink = link.getStringProperty(directlinkproperty);
        /*
         * E.g. account doesn't have enough traffic left but we still got a stored directurl from a previous downloadstart --> Allow
         * download attempt anyways as we can be quite sure that it will still be valid.
         */
        if (StringUtils.isNotEmpty(dllink)) {
            return true;
        } else {
            return super.enoughTrafficFor(link, account);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String apikey = getAPIKey();
        if (this.supportsAPISingleLinkcheck() && apikey != null) {
            /* API linkcheck */
            return this.requestFileInformationAPI(link, apikey);
        } else {
            /* Website linkcheck */
            return requestFileInformationWebsite(link, null, false);
        }
    }

    protected boolean probeDirectDownload(final DownloadLink link, final Account account, final Browser br, final Request request, final boolean setFilesize) throws Exception {
        final URLConnectionAdapter con = openAntiDDoSRequestConnection(br, request);
        try {
            if (this.looksLikeDownloadableContent(con)) {
                final long completeContentLength = con.getCompleteContentLength();
                if (completeContentLength >= 0 && completeContentLength < 100) {
                    br.followConnection();
                    runPostRequestTask(br);
                    correctBR(br);
                    return false;
                }
                if (setFilesize && completeContentLength > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String headerFilename = Plugin.getFileNameFromDispositionHeader(con);
                if (!StringUtils.isEmpty(headerFilename)) {
                    link.setFinalFileName(headerFilename);
                } else {
                    final String filenameFromURL = Plugin.getFileNameFromURL(con.getURL());
                    if (filenameFromURL != null) {
                        /* Fallback */
                        link.setFinalFileName(filenameFromURL);
                    }
                }
                storeDirecturl(link, account, con.getURL().toString());
                return true;
            } else {
                br.followConnection();
                runPostRequestTask(br);
                correctBR(br);
                return false;
            }
        } finally {
            con.disconnect();
        }
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        this.resolveShortURL(this.br.cloneBrowser(), link, account);
        /* First, set fallback-filename */
        if (!link.isNameSet()) {
            setWeakFilename(link, null);
        }
        final boolean isFollowRedirect = br.isFollowingRedirects();
        try {
            br.setFollowRedirects(true);
            if (probeDirectDownload(link, account, br, br.createGetRequest(this.getContentURL(link)), true)) {
                return AvailableStatus.TRUE;
            }
        } finally {
            br.setFollowRedirects(isFollowRedirect);
        }
        if (isOffline(link, this.br, getCorrectBR(br))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] fileInfo = internal_getFileInfoArray();
        final Browser altbr = br.cloneBrowser();
        if (isPremiumOnlyURL(this.br)) {
            /*
             * Hosts whose urls are all premiumonly usually don't display any information about the URL at all - only maybe online/offline.
             * There are 2 alternative ways to get this information anyways!
             */
            logger.info("PREMIUMONLY linkcheck: Trying alternative linkcheck");
            /* Find filename */
            if (this.internal_supports_availablecheck_filename_abuse()) {
                fileInfo[0] = this.getFnameViaAbuseLink(altbr, link);
            }
            /* Find filesize */
            if (this.internal_supports_availablecheck_alt()) {
                getFilesizeViaAvailablecheckAlt(altbr, link);
            }
        } else {
            /* Normal handling */
            scanInfo(fileInfo);
            {
                /**
                 * Two possible reasons to use fallback handling to find filename: </br>
                 * 1. Filename abbreviated over x chars long (common serverside XFS bug) --> Use getFnameViaAbuseLink as a workaround to
                 * find the full-length filename! </br>
                 * 2. Missing filename.
                 */
                if (!StringUtils.isEmpty(fileInfo[0]) && fileInfo[0].trim().endsWith("&#133;") && this.internal_supports_availablecheck_filename_abuse()) {
                    logger.warning("Found filename is crippled by website -> Looking for full length filename");
                    final String betterFilename = this.getFnameViaAbuseLink(altbr, link);
                    if (betterFilename != null) {
                        logger.info("Found full length filename: " + betterFilename);
                        fileInfo[0] = betterFilename;
                    } else {
                        logger.info("Failed to find full length filename");
                    }
                } else if (StringUtils.isEmpty(fileInfo[0]) && this.internal_supports_availablecheck_filename_abuse()) {
                    /* We failed to find the filename via html --> Try getFnameViaAbuseLink as workaround */
                    logger.info("Failed to find any filename, trying to obtain filename via getFnameViaAbuseLink");
                    final String betterFilename = this.getFnameViaAbuseLink(altbr, link);
                    if (betterFilename != null) {
                        logger.info("Found filename: " + betterFilename);
                        fileInfo[0] = betterFilename;
                    } else {
                        logger.info("Failed to find any filename -> Fallback will be used");
                    }
                }
            }
            /* Filesize fallback */
            if (StringUtils.isEmpty(fileInfo[1]) && this.internal_supports_availablecheck_alt()) {
                /* Failed to find filesize? Try alternative way! */
                getFilesizeViaAvailablecheckAlt(altbr, link);
            }
        }
        processFileInfo(fileInfo, altbr, link);
        if (!StringUtils.isEmpty(fileInfo[0])) {
            /* Correct- and set filename */
            setFilename(fileInfo[0], link, br);
        } else {
            /* Fallback. Do this again as now we got the html code available so we can e.g. know if this is a video-filehoster or not. */
            this.setWeakFilename(link, br);
        }
        {
            /* Set filesize */
            if (!StringUtils.isEmpty(fileInfo[1])) {
                link.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
            } else if (this.internal_isVideohosterEmbed(this.br) && supports_availablecheck_filesize_via_embedded_video() && !isDownload) {
                /*
                 * Special case for some videohosts to determine the filesize: Last chance to find filesize - do NOT execute this when used
                 * has started the download of our current DownloadLink as this could lead to "Too many connections" errors!
                 */
                requestFileInformationVideoEmbed(br.cloneBrowser(), link, account, true);
            }
        }
        /* Set md5hash - most times there is no md5hash available! */
        if (!StringUtils.isEmpty(fileInfo[2])) {
            link.setMD5Hash(fileInfo[2].trim());
        }
        return AvailableStatus.TRUE;
    }

    protected String removeHostNameFromFilename(final String filename) {
        String ret = filename;
        if (StringUtils.isNotEmpty(ret)) {
            final LinkedHashSet<String> hosts = new LinkedHashSet<String>();
            hosts.add(getHost());
            final String[] siteSupportedNames = siteSupportedNames();
            if (siteSupportedNames != null) {
                hosts.addAll(Arrays.asList(siteSupportedNames));
            }
            for (final String host : hosts) {
                final String host_tag = new Regex(ret, Pattern.compile("(_?" + Pattern.quote(host) + ")", Pattern.CASE_INSENSITIVE)).getMatch(0);
                if (host_tag != null) {
                    ret = ret.replace(host_tag, "");
                }
            }
        }
        return ret;
    }

    /**
     * Wrapper. </br>
     * Does some corrections on given name string and sets it as filename on given DownloadLink.
     */
    protected void setFilename(String name, final DownloadLink link, final Browser br) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        /* Correct- and set filename */
        if (Encoding.isHtmlEntityCoded(name)) {
            name = Encoding.htmlDecode(name);
        }
        /* Remove some html tags - in most cases not necessary! */
        name = name.replaceAll("(</b>|<b>|\\.html)", "").trim();
        final URL_TYPE urltype = this.getURLType(br.getURL());
        if (this.internal_isVideohoster_enforce_video_filename(link) || this.isVideohosterEmbedHTML(br) || URL_TYPE.OFFICIAL_VIDEO_DOWNLOAD.equals(urltype) || URL_TYPE.EMBED_VIDEO.equals(urltype)) {
            /* For videohosts we often get ugly filenames such as 'some_videotitle.avi.mkv.mp4' --> Correct that! */
            name = this.correctOrApplyFileNameExtension(name, ".mp4");
        }
        link.setName(name);
    }

    protected void processFileInfo(String[] fileInfo, Browser altbr, DownloadLink link) {
    }

    protected boolean isShortURL(final DownloadLink link) {
        return URL_TYPE.SHORT.equals(getURLType(link));
    }

    protected URL_TYPE getURLType(final DownloadLink link) {
        return link != null ? getURLType(link.getPluginPatternMatcher()) : null;
    }

    protected URL_TYPE getURLType(final String url) {
        if (url != null) {
            if (isImagehoster() && url.matches("(?i)^https?://[^/]+/(?:th|i)/\\d+/([a-z0-9]{12}).*")) {
                return URL_TYPE.IMAGE;
            } else if (this.supportsShortURLs() && url.matches("(?i)^https?://[^/]+/d/([a-z0-9]+).*")) {
                return URL_TYPE.SHORT;
            } else if (url.matches("(?i)^https?://[^/]+/d/([a-z0-9]{12}).*")) {
                return URL_TYPE.OFFICIAL_VIDEO_DOWNLOAD;
            } else if (url.matches("(?i)^https?://[^/]+/([a-z0-9]{12}).*")) {
                return URL_TYPE.NORMAL;
            } else if (url.matches("(?i)^https?://[^/]+/file/([a-z0-9]{12}).*")) {
                return URL_TYPE.FILE;
            } else if (url.matches("(?i)^https?://[A-Za-z0-9\\-\\.:]+/embed-([a-z0-9]{12}).*") || url.matches("(?i)^https?://[A-Za-z0-9\\-\\.:]+/e/([a-z0-9]{12}).*")) {
                return URL_TYPE.EMBED_VIDEO;
            } else {
                logger.info("Unknown URL_TYPE:" + url);
            }
        }
        return null;
    }

    protected String getFUID(final String url, URL_TYPE type) {
        if (url != null && type != null) {
            try {
                switch (type) {
                case IMAGE:
                    if (isImagehoster()) {
                        return new Regex(new URL(url).getPath(), "/(?:th|i)/\\d+/([a-z0-9]{12})").getMatch(0);
                    } else {
                        throw new IllegalArgumentException("Unsupported type:" + type + "|" + url);
                    }
                case EMBED_VIDEO:
                    return new Regex(new URL(url).getPath(), "/(?:embed-|e/)?([a-z0-9]{12})").getMatch(0);
                case FILE:
                    return new Regex(new URL(url).getPath(), "/file/([a-z0-9]{12})").getMatch(0);
                case SHORT:
                    return new Regex(new URL(url).getPath(), "/d/([a-z0-9]+)").getMatch(0);
                case NORMAL:
                    return new Regex(new URL(url).getPath(), "/([a-z0-9]{12})").getMatch(0);
                default:
                    throw new IllegalArgumentException("Unsupported type:" + type + "|" + url);
                }
            } catch (MalformedURLException e) {
                logger.log(e);
            }
        }
        return null;
    }

    protected String getFUID(final DownloadLink link, final URL_TYPE type) {
        return link != null ? getFUID(link.getPluginPatternMatcher(), type) : null;
    }

    /**
     * Handles URLs matching TYPE_SHORTURL and ensures that we get one of TYPE_NORMAL (or Exception). </br>
     * There are multiple reasons for us to handle this here instead of using a separate crawler plugin. Do NOT move this handling into a
     * separate crawler plugin!!
     */
    protected void resolveShortURL(final Browser br, final DownloadLink link, final Account account) throws Exception {
        synchronized (link) {
            if (isShortURL(link)) {
                final String contentURL = this.getContentURL(link);
                /* Short URLs -> We need to find the long FUID! */
                br.setFollowRedirects(true);
                if (probeDirectDownload(link, account, br, br.createGetRequest(contentURL), true)) {
                    return;
                } else if (this.isOffline(link, br, br.toString())) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                URL_TYPE type = getURLType(br.getURL());
                final String realFUID;
                if (type != null && !URL_TYPE.SHORT.equals(type)) {
                    realFUID = getFUID(br.getURL(), type);
                } else {
                    final Form form = br.getFormbyProperty("name", "F1");
                    final InputField id = form != null ? form.getInputFieldByName("id") : null;
                    realFUID = id != null ? id.getValue() : null;
                    type = URL_TYPE.NORMAL;
                }
                if (realFUID == null || !realFUID.matches("[A-Za-z0-9]{12}")) {
                    /**
                     * The usual XFS errors can happen here in which case we won't be able to find the long FUID. </br>
                     * Even while a limit is reached, such URLs can sometimes be checked via: "/?op=check_files" but we won't do this for
                     * now!
                     */
                    this.checkErrors(br, br.toString(), link, account, false);
                    /* Assume that this URL is offline */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "realFUID:" + realFUID);
                } else {
                    /* Success! */
                    final String urlNew;
                    if (URL_TYPE.FILE.equals(type)) {
                        urlNew = URLHelper.parseLocation(new URL(this.getMainPage(link)), buildNormalFileURLPath(link, realFUID));
                    } else {
                        urlNew = URLHelper.parseLocation(new URL(this.getMainPage(link)), buildNormalURLPath(link, realFUID));
                    }
                    logger.info("resolve URL|old: " + contentURL + "|new:" + urlNew);
                    link.setPluginPatternMatcher(urlNew);
                }
            }
        }
    }

    /**
     * 2019-05-15: This can check availability via '/embed' URL. <br />
     * Only call this if internal_isVideohosterEmbed returns true. </br>
     *
     * @return final downloadurl
     */
    protected String requestFileInformationVideoEmbed(final Browser br, final DownloadLink link, final Account account, final boolean findFilesize) throws Exception {
        /*
         * Some video sites contain their directurl right on the first page - let's use this as an indicator and assume that the file is
         * online if we find a directurl. This also speeds-up linkchecking! Example: uqload.com
         */
        String dllink = getDllink(link, account, br, getCorrectBR(br));
        if (StringUtils.isEmpty(dllink)) {
            final URL_TYPE type = this.getURLType(br.getURL());
            if (type != URL_TYPE.EMBED_VIDEO) {
                final String embed_access = this.getMainPage(br) + this.buildEmbedURLPath(link, this.getFUIDFromURL(link));
                getPage(br, embed_access);
                /**
                 * 2019-07-03: Example response when embedding is not possible (deactivated or it is not a video-file): "Can't create video
                 * code" OR "Video embed restricted for this user"
                 */
            }
            /*
             * Important: Do NOT use 404 as offline-indicator here as the website-owner could have simply disabled embedding while it was
             * enabled before --> This would return 404 for all '/embed' URLs! Only rely on precise errormessages!
             */
            if (br.toString().equalsIgnoreCase("File was deleted")) {
                /* Should be valid for all XFS hosts e.g. speedvideo.net, uqload.com */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = getDllink(link, account, br, br.toString());
            // final String url_thumbnail = getVideoThumbnailURL(br.toString());
        }
        if (findFilesize && !StringUtils.isEmpty(dllink) && !dllink.contains(".m3u8")) {
            /* Get- and set filesize from directurl */
            if (checkDirectLinkAndSetFilesize(link, dllink, true) != null) {
                /* Directurl is valid -> Store it */
                storeDirecturl(link, account, dllink);
            }
        }
        return dllink;
    }

    /**
     * Tries to find filename, filesize and md5hash inside html. On Override, make sure to first use your special RegExes e.g.
     * fileInfo[0]="bla", THEN, if needed, call super.scanInfo(fileInfo). <br />
     * fileInfo[0] = filename, fileInfo[1] = filesize, fileInfo[2] = md5hash (rarely used, 2019-05-21: e.g. md5 hash available and special
     * case: filespace.com)
     */
    public String[] scanInfo(final String[] fileInfo) {
        return scanInfo(correctedBR, fileInfo);
    }

    public String[] scanInfo(final String html, final String[] fileInfo) {
        final DownloadLink link = this.getDownloadLink();
        final String urlFUID = this.getFUIDFromURL(link);
        /*
         * 2019-04-17: TODO: Improve sharebox RegExes (also check if we can remove/improve sharebox0 and sharebox1 RegExes) as this may save
         * us from having to use other time-comsuming fallbacks such as getFilesizeViaAvailablecheckAlt or getFnameViaAbuseLink. E.g. new
         * XFS often has good information in their shareboxes!
         */
        final String sharebox0 = "copy\\(this\\);.+>(.+) - ([\\d\\.]+ (?:B|KB|MB|GB))</a></textarea>\\s*</div>";
        final String sharebox1 = "copy\\(this\\);.+\\](.+) - ([\\d\\.]+ (?:B|KB|MB|GB))\\[/URL\\]";
        /* 2019-05-08: 'Forum Code': Sharebox with filename & filesize (bytes), example: brupload.net, qtyfiles.com */
        final String sharebox2 = "\\[URL=https?://(?:www\\.)?[^/\"]+/" + urlFUID + "[^\\]]*?\\]([^\"]*?)\\s*\\-\\s*(\\d+)\\[/URL\\]";
        /* First found for pixroute.com URLs */
        final String sharebox2_without_filesize = "\\[URL=https?://(?:www\\.)?[^/\"]+/" + urlFUID + "/([^<>\"/\\]]*?)(?:\\.html)?\\]";
        /*
         * 2019-05-21: E.g. uqload.com, vidoba.net - this method will return a 'cleaner' filename than in other places - their titles will
         * often end with " mp4" which we have to correct later!
         */
        final String sharebox3_videohost = "\\[URL=https?://[^/]+/" + urlFUID + "[^/<>\\]]*?\\]\\[IMG\\][^<>\"\\[\\]]+\\[/IMG\\]([^<>\"]+)\\[/URL\\]";
        /* standard traits from base page */
        if (StringUtils.isEmpty(fileInfo[0])) {
            /* 2019-06-12: TODO: Update this RegEx for e.g. up-4ever.org */
            fileInfo[0] = new Regex(html, "You have requested.*?https?://(?:www\\.)?[^/]+/" + urlFUID + "/([^<>\"]+)<").getMatch(0);
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = new Regex(html, "name=\"fname\" (?:type=\"hidden\" )?value=\"(.*?)\"").getMatch(0);
                if (StringUtils.isEmpty(fileInfo[0])) {
                    fileInfo[0] = new Regex(html, "<h2>\\s*Download File\\s*(?:<(?:span|b)[^>]*>)?\\s*(.+?)\\s*(</(?:span|b|h2)>)").getMatch(0);
                    /* traits from download1 page below */
                    if (StringUtils.isEmpty(fileInfo[0])) {
                        fileInfo[0] = new Regex(html, "Filename:?\\s*(<[^>]+>\\s*)+?([^<>\"]+)").getMatch(1);
                    }
                }
            }
        }
        if (StringUtils.isEmpty(fileInfo[0])) {
            /* 2023-07-28: For new style XFS videohosts when on official video download page "/d/<fuid>" */
            fileInfo[0] = new Regex(html, "(?i)<h4 [^>]*>\\s*Download\\s*([^<]*?)\\s*</h\\d+>").getMatch(0);
        }
        final String downloadFileTable = new Regex(html, "<h\\d+>\\s*Download\\s*File\\s*</h\\d+>\\s*<table[^>]*>(.*?)</table>").getMatch(0);
        if (downloadFileTable != null) {
            // eg rarefile.net
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = new Regex(downloadFileTable, "<td>\\s*<font[^>]*>\\s*(.*?)\\s*</font>").getMatch(0);
            }
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = new Regex(downloadFileTable, ">\\s*Size\\s*:\\s*([0-9\\.]+\\s*(TB|GB|MB|KB|B))").getMatch(0);
            }
        }
        final String downloadFileTable2 = new Regex(html, "<table[^>]*>.*?<h\\d+[^>]*>\\s*Download\\s*File\\s*</h\\d+>\\s*(.*?)</table>").getMatch(0);
        if (downloadFileTable2 != null) {
            // eg dailyuploads.net
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = new Regex(downloadFileTable2, "<td\\s*style\\s*=\\s*\"font[^>]*>\\s*(.*?)\\s*(</|<br)").getMatch(0);
            }
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = scanGenericFileSize(downloadFileTable2);
            }
        }
        /* Next - RegExes for specified types of websites e.g. imagehosts */
        if (StringUtils.isEmpty(fileInfo[0]) && this.isImagehoster()) {
            fileInfo[0] = regexImagehosterFilename(br);
        }
        /* Next - details from sharing boxes (new RegExes to old) */
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = new Regex(html, sharebox2).getMatch(0);
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = new Regex(html, sharebox2_without_filesize).getMatch(0);
            }
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = new Regex(html, sharebox1).getMatch(0);
                if (StringUtils.isEmpty(fileInfo[0])) {
                    fileInfo[0] = new Regex(html, sharebox0).getMatch(0);
                }
                if (StringUtils.isEmpty(fileInfo[0])) {
                    /* Link of the box without filesize */
                    fileInfo[0] = new Regex(html, "onFocus=\"copy\\(this\\);\">https?://(?:www\\.)?[^/]+/" + urlFUID + "/([^<>\"]*?)</textarea").getMatch(0);
                }
            }
        }
        /* Next - RegExes for videohosts */
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = new Regex(html, sharebox3_videohost).getMatch(0);
            if (StringUtils.isEmpty(fileInfo[0])) {
                /* 2017-04-11: Typically for XVideoSharing sites */
                fileInfo[0] = new Regex(html, Pattern.compile("<title>\\s*Watch(?:ing)?\\s*([^<>\"]+)\\s*</title>", Pattern.CASE_INSENSITIVE)).getMatch(0);
            }
            if (StringUtils.isEmpty(fileInfo[0]) && isImagehoster()) {
                /* Imagehoster site title */
                final String websiteName = Browser.getHost(getHost()).replaceAll("(\\..+)$", "");
                fileInfo[0] = new Regex(html, Pattern.compile("<title>\\s*(.*?\\.(png|jpe?g|gif))\\s*-\\s*(" + Pattern.quote(getHost()) + "|" + Pattern.quote(websiteName) + ")\\s*</title>", Pattern.CASE_INSENSITIVE)).getMatch(0);
            }
        }
        if (StringUtils.isEmpty(fileInfo[0])) {
            fileInfo[0] = new Regex(html, "class\\s*=\\s*\"dfilename\">\\s*(?:<div>)?\\s*([^<>\"]*?)</").getMatch(0);
            if (StringUtils.isEmpty(fileInfo[0])) {
                fileInfo[0] = new Regex(html, "<div[^>]*id\\s*=\\s*\"dfilename\">\\s*(?:<div>)?\\s*([^<>\"]*?)</").getMatch(0);
            }
        }
        if (internal_isVideohosterEmbed(this.br) && (StringUtils.isEmpty(fileInfo[0]) || StringUtils.equalsIgnoreCase("No title", fileInfo[0]))) {
            /* 2019-10-15: E.g. vidoza.net */
            final String curFileName = br.getRegex("var\\s*curFileName\\s*=\\s*\"(.*?)\"").getMatch(0);
            if (StringUtils.isNotEmpty(curFileName)) {
                fileInfo[0] = curFileName;
            }
        }
        /*
         * 2019-05-16: Experimental RegEx to find 'safe' filesize traits which can always be checked, regardless of the
         * 'supports_availablecheck_filesize_html' setting:
         */
        if (StringUtils.isEmpty(fileInfo[1])) {
            fileInfo[1] = new Regex(html, sharebox2).getMatch(1);
        }
        /* 2019-07-12: Example: Katfile.com */
        if (StringUtils.isEmpty(fileInfo[1])) {
            fileInfo[1] = new Regex(html, "id\\s*=\\s*\"fsize[^\"]*\"\\s*>\\s*([0-9\\.]+\\s*[MBTGK]+)\\s*<").getMatch(0);
        }
        if (StringUtils.isEmpty(fileInfo[1])) {
            /* 2019-07-12: Example: Katfile.com */
            fileInfo[1] = new Regex(html, "class\\s*=\\s*\"statd\"\\s*>\\s*size\\s*</span>\\s*<span>\\s*([0-9\\.]+\\s*[MBTGK]+)\\s*<").getMatch(0);
        }
        if (StringUtils.isEmpty(fileInfo[1])) {
            /* 2020-08-10: E.g. myqloud.org */
            try {
                fileInfo[1] = getDllinkViaOfficialVideoDownload(this.br.cloneBrowser(), link, null, true);
            } catch (final Throwable e) {
                /* This should never happen */
                logger.log(e);
            }
        }
        if (this.supports_availablecheck_filesize_html() && StringUtils.isEmpty(fileInfo[1])) {
            /** TODO: Clean this up */
            /* Starting from here - more unsafe attempts */
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = new Regex(html, "\\((\\d+\\s*bytes)\\)").getMatch(0);
                if (StringUtils.isEmpty(fileInfo[1])) {
                    fileInfo[1] = new Regex(html, "</font>[ ]+\\(([^<>\"'/]+)\\)(.*?)</font>").getMatch(0);
                }
            }
            /* Next - unsafe details from sharing box */
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = new Regex(html, sharebox0).getMatch(1);
                if (StringUtils.isEmpty(fileInfo[1])) {
                    fileInfo[1] = new Regex(html, sharebox1).getMatch(1);
                }
            }
            /* Generic failover */
            if (StringUtils.isEmpty(fileInfo[1])) {
                fileInfo[1] = scanGenericFileSize(html);
            }
        }
        /* MD5 is only available in very very rare cases! */
        if (StringUtils.isEmpty(fileInfo[2])) {
            fileInfo[2] = new Regex(html, "<b>\\s*MD5.*?</b>.*?nowrap>\\s*(.*?)\\s*<").getMatch(0);
        }
        return fileInfo;
    }

    protected String scanGenericFileSize(final String html) {
        // sync with YetiShareCore.scanInfo- Generic failover
        String ret = new Regex(html, "(?:>\\s*|\\(\\s*|\"\\s*|\\[\\s*|\\s+)([0-9\\.]+(?:\\s+|\\&nbsp;)?(bytes)(?!ps|/s|\\w|\\s*Storage|\\s*Disk|\\s*Space|\\s*traffic))").getMatch(0);
        if (StringUtils.isEmpty(ret)) {
            ret = new Regex(html, "(?:>\\s*|\\(\\s*|\"\\s*|\\[\\s*|\\s+)([0-9\\.]+(?:\\s+|\\&nbsp;)?(TB|GB|MB|KB)(?!ps|/s|\\w|\\s*Storage|\\s*Disk|\\s*Space|\\s*traffic))").getMatch(0);
        }
        return ret;
    }

    /** Check single URL via mass-linkchecker. Throws PluginException if URL has been detected as offline. */
    public AvailableStatus requestFileInformationWebsiteMassLinkcheckerSingle(final DownloadLink link) throws IOException, PluginException {
        massLinkcheckerWebsite(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return AvailableStatus.TRUE;
        }
    }

    /**
     * Use this to Override 'checkLinks(final DownloadLink[])' in supported plugins. <br />
     * Used by getFilesizeViaAvailablecheckAlt <br />
     * <b>Use this only if:</b> <br />
     * - You have verified that the filehost has a mass-linkchecker and it is working fine with this code. <br />
     * - The contentURLs contain a filename as a fallback e.g. https://host.tld/<fuid>/someFilename.png.html </br>
     * - If used for single URLs inside 'normal linkcheck' (e.g. inside requestFileInformation), call with setWeakFilename = false <br/>
     * - If the normal way via website is blocked somehow e.g. 'site-verification' captcha </br>
     * <b>- If used to check multiple URLs (mass-linkchecking feature), call with setWeakFilename = true!! </b>
     */
    public boolean massLinkcheckerWebsite(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        boolean linkcheckerHasFailed = false;
        String checkTypeCurrent = null;
        /* Checks linkchecking via: examplehost.com/?op=checkfiles AND examplehost.com/?op=check_files */
        final String checkTypeOld = "checkfiles";
        final String checkTypeNew = "check_files";
        final SubConfiguration cfg = this.getPluginConfig();
        final String checkType_last_used_and_working = cfg.getStringProperty(PROPERTY_PLUGIN_ALT_AVAILABLECHECK_LAST_WORKING, null);
        String checkURL = null;
        int linkcheckTypeTryCount = 0;
        try {
            final Browser br = new Browser();
            this.prepBrowser(br, getMainPage());
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            Form checkForm = null;
            while (true) {
                links.clear();
                while (true) {
                    /* We test max 50 links at once. 2020-05-28: Checked to up to 100 but let's use max. 50. */
                    if (index == urls.length || links.size() == 50) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink link : links) {
                    try {
                        this.resolveShortURL(br.cloneBrowser(), link, null);
                    } catch (final PluginException e) {
                        logger.log(e);
                        if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                            link.setAvailable(false);
                        } else {
                            link.setAvailable(true);
                        }
                        if (!link.isNameSet()) {
                            setWeakFilename(link, null);
                        }
                        /*
                         * We cannot check shortLinks via API so if we're unable to convert them to TYPE_NORMAL we basically already checked
                         * them here. Also we have to avoid sending wrong fileIDs to the API otherwise linkcheck WILL fail!
                         */
                        continue;
                    }
                    sb.append(URLEncode.encodeURIComponent(this.getContentURL(link)));
                    sb.append("%0A");
                }
                {
                    /* Check if the mass-linkchecker works and which check we have to use */
                    while (linkcheckTypeTryCount <= 1) {
                        if (checkTypeCurrent != null) {
                            /* No matter which checkType we tried first - it failed and we need to try the other one! */
                            if (checkTypeCurrent.equals(checkTypeNew)) {
                                checkTypeCurrent = checkTypeOld;
                            } else {
                                checkTypeCurrent = checkTypeNew;
                            }
                        } else if (this.prefer_availablecheck_filesize_alt_type_old()) {
                            /* Old checkType forced? */
                            checkTypeCurrent = checkTypeOld;
                        } else if (checkType_last_used_and_working != null) {
                            /* Try to re-use last working method */
                            checkTypeCurrent = checkType_last_used_and_working;
                        } else {
                            /* First launch */
                            checkTypeCurrent = checkTypeNew;
                        }
                        /*
                         * Sending the Form without a previous request might e.g. fail if the website requires "www." but
                         * supports_availablecheck_filesize_alt_fast returns false.
                         */
                        if (br.getURL() != null) {
                            checkURL = "/?op=" + checkTypeCurrent;
                        } else {
                            checkURL = getMainPage() + "/?op=" + checkTypeCurrent;
                        }
                        /* Get- and prepare Form */
                        if (this.supports_availablecheck_filesize_alt_fast()) {
                            /* Quick way - we do not access the page before and do not need to parse the Form. */
                            checkForm = new Form();
                            checkForm.setMethod(MethodType.POST);
                            checkForm.setAction(checkURL);
                            checkForm.put("op", checkTypeCurrent);
                            checkForm.put("process", "Check+URLs");
                        } else {
                            /* Try to get the Form IF NEEDED as it can contain tokens which would otherwise be missing. */
                            getPage(br, checkURL);
                            checkForm = br.getFormByInputFieldKeyValue("op", checkTypeCurrent);
                            if (checkForm == null) {
                                logger.info("Failed to find Form for checkType: " + checkTypeCurrent);
                                linkcheckTypeTryCount++;
                                continue;
                            }
                        }
                        checkForm.put("list", sb.toString());
                        this.submitForm(br, checkForm);
                        /*
                         * Some hosts will not display any errorpage but also we will not be able to find any of our checked file-IDs inside
                         * the html --> Use this to find out about non-working linkchecking method!
                         */
                        final String example_fuid = this.getFUIDFromURL(links.get(0));
                        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(checkTypeCurrent) || !br.containsHTML(example_fuid)) {
                            /*
                             * This method of linkcheck is not supported - increase the counter by one to find out if ANY method worked in
                             * the end.
                             */
                            logger.info("Failed to find check_files Status via checkType: " + checkTypeCurrent);
                            linkcheckTypeTryCount++;
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                for (final DownloadLink link : links) {
                    if (massLinkcheckerParseFileInfo(br, link) == AvailableStatus.UNCHECKED) {
                        logger.warning("Failed to find any information for current DownloadLink --> Possible mass-linkchecker failure");
                        linkcheckerHasFailed = true;
                        continue;
                    }
                    if (!link.isNameSet()) {
                        /*
                         * Fallback! We cannot get 'good' filenames via this call so we have to rely on our fallback-filenames (fuid or
                         * filename inside URL)!
                         */
                        setWeakFilename(link, null);
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            return false;
        } finally {
            if (linkcheckerHasFailed) {
                logger.info("Seems like checkfiles availablecheck is not supported by this host");
                cfg.setProperty(PROPERTY_PLUGIN_ALT_AVAILABLECHECK_LAST_FAILURE_TIMESTAMP, System.currentTimeMillis());
                cfg.setProperty(PROPERTY_PLUGIN_ALT_AVAILABLECHECK_LAST_FAILURE_VERSION, getPluginVersionHash());
            } else {
                cfg.setProperty(PROPERTY_PLUGIN_ALT_AVAILABLECHECK_LAST_WORKING, checkTypeCurrent);
            }
        }
        if (linkcheckerHasFailed) {
            return false;
        } else {
            return true;
        }
    }

    /** Parses and sets file info returned after doing a mass-linkchecking request to a an XFS website. */
    protected AvailableStatus massLinkcheckerParseFileInfo(final Browser br, final DownloadLink link) {
        final String fuid = this.getFUIDFromURL(link);
        boolean isNewLinkchecker = true;
        String html_for_fuid = br.getRegex("<tr>((?!</?tr>).)*?" + fuid + "((?!</?tr>).)*?</tr>").getMatch(-1);
        if (html_for_fuid == null) {
            /*
             * 2019-07-10: E.g. for old linkcheckers which only return online/offline status in a single line and not as a html table.
             */
            html_for_fuid = br.getRegex("<font color=\\'(?:green|red)\\'>[^>]*?" + fuid + "[^>]*?</font>").getMatch(-1);
            isNewLinkchecker = false;
        }
        if (html_for_fuid == null) {
            return AvailableStatus.UNCHECKED;
        }
        final boolean isOffline;
        if (isNewLinkchecker) {
            isOffline = new Regex(html_for_fuid, "Not found").matches();
        } else {
            isOffline = new Regex(html_for_fuid, "<font color='red").matches();
        }
        if (isOffline) {
            link.setAvailable(false);
            return AvailableStatus.FALSE;
        } else {
            /* We know that the file is online - let's try to find the filesize ... */
            link.setAvailable(true);
            try {
                final String[] tabla_data = new Regex(html_for_fuid, "<td>?(.*?)</td>").getColumn(0);
                final String size = tabla_data.length >= 2 ? tabla_data[2] : null;
                if (size != null) {
                    /*
                     * Filesize should definitly be given - but at this stage we are quite sure that the file is online so let's not throw a
                     * fatal error if the filesize cannot be found.
                     */
                    link.setDownloadSize(SizeFormatter.getSize(size));
                }
            } catch (final Throwable ignore) {
                logger.log(ignore);
            }
            return AvailableStatus.TRUE;
        }
    }

    /**
     * Try to find filename via '/?op=report_file&id=<fuid>'. Only call this function if internal_supports_availablecheck_filename_abuse()
     * returns true!<br />
     * E.g. needed if officially only logged in users can see filename or filename is missing in html code for whatever reason.<br />
     * Often needed for <b><u>IMAGEHOSTER</u> ' s</b>.<br />
     * Important: Only call this if <b><u>SUPPORTS_AVAILABLECHECK_ABUSE</u></b> is <b>true</b> (meaning omly try this if website supports
     * it)!<br />
     *
     * @throws Exception
     */
    protected String getFnameViaAbuseLink(final Browser br, final DownloadLink link) throws Exception {
        getPage(br, getMainPage() + "/?op=report_file&id=" + this.getFUIDFromURL(link), false);
        /*
         * 2019-07-10: ONLY "No such file" as response might always be wrong and should be treated as a failure! Example: xvideosharing.com
         */
        if (br.containsHTML("(?i)>\\s*No such file<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = regexFilenameAbuse(br);
        if (filename != null) {
            logger.info("Successfully found filename via report_file");
            return filename;
        } else {
            logger.info("Failed to find filename via report_file");
            final boolean fnameViaAbuseUnsupported = br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500 || !br.getURL().contains("report_file") || br.toString().trim().equals("No such file");
            if (fnameViaAbuseUnsupported) {
                logger.info("Seems like report_file availablecheck seems not to be supported by this host");
                final SubConfiguration config = this.getPluginConfig();
                config.setProperty(PROPERTY_PLUGIN_REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_TIMESTAMP, System.currentTimeMillis());
                config.setProperty(PROPERTY_PLUGIN_REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_VERSION, getPluginVersionHash());
            }
            return null;
        }
    }

    /** Part of {@link #getFnameViaAbuseLink() getFnameViaAbuseLink} */
    public String regexFilenameAbuse(final Browser br) {
        String filename = null;
        final String filename_src = br.getRegex("(?i)<b>Filename\\s*:?\\s*<[^\n]+</td>").getMatch(-1);
        if (filename_src != null) {
            filename = new Regex(filename_src, ">([^>]+)</td>$").getMatch(0);
        }
        if (filename == null) {
            /* 2020-12-07 e.g. sama-share.com, pandafiles.com */
            filename = br.getRegex("name=\"file_name\"[^>]*value=\"([^<>\"]+)\"").getMatch(0);
        }
        if (filename == null) {
            /* 2021-05-12: New XFS style e.g. userupload.net */
            filename = br.getRegex("(?i)<label>\\s*Filename\\s*</label>\\s*<input[^>]*class=\"form-control form-control-plaintext\"[^>]*value=\"([^\"]+)\"").getMatch(0);
        }
        return filename;
    }

    /** Only use this if it is made sure that the host we're working with is an imagehoster ("ximagesharing")!! */
    public String regexImagehosterFilename(final Browser br) {
        return br.getRegex("class=\"pic\"[^>]*alt=\"([^<>\"]*?)\"").getMatch(0);
    }

    /**
     * Get filesize via massLinkchecker/alternative availablecheck.<br />
     * Wrapper for requestFileInformationWebsiteMassLinkcheckerSingle which contains a bit of extra log output </br>
     * Often used as fallback if e.g. only logged-in users can see filesize or filesize is not given in html code for whatever reason.<br />
     * Often needed for <b><u>IMAGEHOSTER</u>S</b>.<br />
     * Important: Only call this if <b><u>supports_availablecheck_alt</u></b> is <b>true</b> (meaning omly try this if website supports
     * it)!<br />
     * Some older XFS versions AND videohosts have versions of this linkchecker which only return online/offline and NO FILESIZE!</br>
     * In case there is no filesize given, offline status will still be recognized! <br/>
     *
     * @return isOnline
     * @throws IOException
     */
    protected boolean getFilesizeViaAvailablecheckAlt(final Browser br, final DownloadLink link) throws PluginException, IOException {
        logger.info("Trying getFilesizeViaAvailablecheckAlt");
        requestFileInformationWebsiteMassLinkcheckerSingle(link);
        if (link.isAvailabilityStatusChecked()) {
            logger.info("Successfully checked URL via website massLinkcheck | filesize: " + link.getView().getBytesTotal());
            return true;
        } else {
            logger.info("Failed to find filesize via website massLinkcheck");
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        this.resolveShortURL(this.br.cloneBrowser(), link, null);
        doFree(link, null);
    }

    /** Handles pre-download forms & captcha for free (anonymous) + FREE ACCOUNT modes. */
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        if (this.attemptStoredDownloadurlDownload(link, account)) {
            try {
                if (dl.getConnection() != null) {
                    fixFilename(dl.getConnection(), link);
                } else {
                    fixFilenameHLSDownload(link);
                }
            } catch (final Exception ignore) {
                logger.log(ignore);
            }
            logger.info("Using stored directurl");
            try {
                /* add a download slot */
                controlMaxFreeDownloads(account, link, +1);
                /* start the dl */
                dl.startDownload();
            } finally {
                /* remove download slot */
                controlMaxFreeDownloads(account, link, -1);
            }
            return;
        }
        requestFileInformationWebsite(link, account, true);
        String dllink = null;
        String officialVideoDownloadURL = null;
        int download1counter = 0;
        final int download1max = 1;
        final DownloadMode mode = this.getPreferredDownloadModeFromConfig();
        do {
            logger.info(String.format("Handling download1 loop %d / %d", download1counter + 1, download1max + 1));
            officialVideoDownloadURL = getDllinkViaOfficialVideoDownload(this.br.cloneBrowser(), link, account, false);
            if (!StringUtils.isEmpty(officialVideoDownloadURL) && mode == DownloadMode.ORIGINAL) {
                logger.info("Stepping out of download1 loop because: User wants original download && we found original download");
                break;
            }
            /* Check for streaming/direct links on the first page. */
            dllink = getDllink(link, account, br, getCorrectBR(br));
            /* Do they support standard video embedding? */
            if (StringUtils.isEmpty(dllink) && this.internal_isVideohosterEmbed(this.br)) {
                try {
                    logger.info("Trying to get link via embed");
                    dllink = requestFileInformationVideoEmbed(br.cloneBrowser(), link, account, false);
                    if (StringUtils.isEmpty(dllink)) {
                        logger.info("Failed to get link via embed");
                    } else {
                        logger.info("Successfully found link via embed");
                    }
                } catch (final InterruptedException e) {
                    throw e;
                } catch (final Throwable e) {
                    logger.log(e);
                    logger.info("Failed to get link via embed");
                }
            }
            /* Extra handling for imagehosts */
            if (StringUtils.isEmpty(dllink) && this.isImagehoster()) {
                checkErrors(br, getCorrectBR(br), link, account, false);
                Form imghost_next_form = findImageForm(this.br);
                if (imghost_next_form != null) {
                    int counter = -1;
                    final int countermax = 3;
                    do {
                        counter++;
                        logger.info(String.format("imghost_next_form loop %d / %d", counter + 1, countermax));
                        // this.handleCaptcha(link, imghost_next_form);
                        submitForm(imghost_next_form);
                        checkErrors(br, getCorrectBR(br), link, account, false);
                        dllink = getDllink(link, account, br, getCorrectBR(br));
                        /* For imagehosts, filenames are often not given until we can actually see/download the image. */
                        final String imageFilename = regexImagehosterFilename(br);
                        if (imageFilename != null) {
                            link.setName(Encoding.htmlOnlyDecode(imageFilename));
                        }
                        if (!StringUtils.isEmpty(dllink)) {
                            logger.info("Found image directurl: " + dllink);
                            break;
                        } else if (counter >= countermax) {
                            logger.warning("Imagehost handling exceeded max tries");
                            break;
                        } else {
                            imghost_next_form = findImageForm(this.br);
                            if (imghost_next_form == null) {
                                logger.warning("Failed to find next imghost_next_form and no directurl present -> Stepping out of imagehost handling");
                                break;
                            }
                        }
                    } while (true);
                }
            }
            if (!StringUtils.isEmpty(dllink)) {
                logger.info("Stepping out of download1 loop because: Found directurl");
                break;
            }
            /* Check for errors and download1 Form. Only execute this once! */
            if (download1counter == 0) {
                /*
                 * Check errors here because if we don't and a link is premiumonly, download1 Form will be present, plugin will send it and
                 * most likely end up with error "Fatal countdown error (countdown skipped)"
                 */
                checkErrors(br, getCorrectBR(br), link, account, false);
                final Form download1 = findFormDownload1Free(br);
                if (download1 != null) {
                    logger.info("Found download1 Form");
                    submitForm(download1);
                    checkErrors(br, getCorrectBR(br), link, account, false);
                    officialVideoDownloadURL = getDllinkViaOfficialVideoDownload(this.br.cloneBrowser(), link, account, false);
                    if (!StringUtils.isEmpty(officialVideoDownloadURL) && mode == DownloadMode.ORIGINAL) {
                        logger.info("Stepping out of download1 loop because: User wants original download && we found original download");
                        break;
                    }
                    dllink = getDllink(link, account, br, getCorrectBR(br));
                    if (!StringUtils.isEmpty(dllink)) {
                        logger.info("Stepping out of download1 loop because: Found directurl");
                        break;
                    }
                } else {
                    logger.info("Failed to find download1 Form");
                    break;
                }
            }
            download1counter++;
        } while (download1counter <= download1max && StringUtils.isEmpty(dllink));
        if (StringUtils.isEmpty(dllink) && (mode == DownloadMode.STREAM || StringUtils.isEmpty(officialVideoDownloadURL))) {
            Form download2 = findFormDownload2Free(br);
            if (download2 == null) {
                /* Last chance - maybe our errorhandling kicks in here. */
                checkErrors(br, getCorrectBR(br), link, account, false);
                /* Okay we finally have no idea what happened ... */
                logger.warning("Failed to find download2 Form");
                checkErrorsLastResort(br, account);
            }
            logger.info("Found download2 Form");
            /*
             * E.g. html contains text which would lead to error ERROR_IP_BLOCKED --> We're not checking for it as there is a download Form
             * --> Then when submitting it, html will contain another error e.g. 'Skipped countdown' --> In this case we want to prefer the
             * first thrown Exception. Why do we not check errors before submitting download2 Form? Because html could contain faulty
             * errormessages!
             */
            Exception exceptionBeforeDownload2Submit = null;
            try {
                checkErrors(br, getCorrectBR(br), link, account, false);
            } catch (final Exception e) {
                logger.log(e);
                exceptionBeforeDownload2Submit = e;
                logger.info("Found Exception before download2 Form submit");
            }
            /* Define how many forms deep do we want to try? */
            final int download2start = 0;
            final int download2max = 2;
            for (int download2counter = download2start; download2counter <= download2max; download2counter++) {
                logger.info(String.format("Download2 loop %d / %d", download2counter + 1, download2max + 1));
                final long timeBefore = Time.systemIndependentCurrentJVMTimeMillis();
                handlePassword(download2, link);
                handleCaptcha(link, br, download2);
                /* 2019-02-08: MD5 can be on the subsequent pages - it is to be found very rare in current XFS versions */
                if (link.getMD5Hash() == null) {
                    final String md5hash = new Regex(getCorrectBR(br), "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
                    if (md5hash != null) {
                        link.setMD5Hash(md5hash.trim());
                    }
                }
                waitTime(link, timeBefore);
                final URLConnectionAdapter formCon = openAntiDDoSRequestConnection(br, br.createFormRequest(download2));
                if (looksLikeDownloadableContent(formCon)) {
                    /* Very rare case - e.g. tiny-files.com */
                    handleDownload(link, account, null, dllink, formCon.getRequest());
                    return;
                } else {
                    br.followConnection(true);
                    this.correctBR(br);
                    try {
                        formCon.disconnect();
                    } catch (final Throwable e) {
                    }
                }
                logger.info("Submitted Form download2");
                checkErrors(br, getCorrectBR(br), link, account, true);
                /* 2020-03-02: E.g. akvideo.stream */
                officialVideoDownloadURL = getDllinkViaOfficialVideoDownload(this.br.cloneBrowser(), link, account, false);
                if (!StringUtils.isEmpty(officialVideoDownloadURL) && mode == DownloadMode.ORIGINAL) {
                    logger.info("Stepping out of download2 loop because: User wants original download && we found original download");
                    break;
                }
                dllink = getDllink(link, account, br, getCorrectBR(br));
                download2 = findFormDownload2Free(br);
                if (!StringUtils.isEmpty(officialVideoDownloadURL) || !StringUtils.isEmpty(dllink)) {
                    /* Success */
                    validateLastChallengeResponse();
                    break;
                } else if (download2 == null) {
                    /* Failure */
                    logger.info("Stepping out of download2 loop because: download2 form is null");
                    break;
                } else {
                    /* Continue to next round / next pre-download page */
                    invalidateLastChallengeResponse();
                    continue;
                }
            }
            if (StringUtils.isEmpty(officialVideoDownloadURL) && StringUtils.isEmpty(dllink)) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                /* Check if maybe an error happened before stepping in download2 loop --> Throw that */
                if (exceptionBeforeDownload2Submit != null) {
                    logger.info("Throwing exceptionBeforeDownload2Submit");
                    throw exceptionBeforeDownload2Submit;
                } else {
                    checkErrorsLastResort(br, account);
                }
            }
        }
        handleDownload(link, account, officialVideoDownloadURL, dllink, null);
    }

    /**
     * Checks if official video download is possible and returns final downloadurl if possible. </br>
     * This should NOT throw any Exceptions!
     *
     * @param returnFilesize
     *            true = Only return filesize of selected quality. Use this in availablecheck. </br>
     *            false = return final downloadurl of selected quality. Use this in download mode.
     */
    protected String getDllinkViaOfficialVideoDownload(final Browser br, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        if (returnFilesize) {
            logger.info("[FilesizeMode] Trying to find official video downloads");
        } else {
            logger.info("[DownloadMode] Trying to find official video downloads");
        }
        String dllink = null;
        /* Info in table. E.g. xvideosharing.com, watchvideo.us */
        String[] videoQualityHTMLs = br.getRegex("<tr><td>[^\r\t\n]+download_video\\(.*?</td></tr>").getColumn(-1);
        if (videoQualityHTMLs.length == 0) {
            /* Match on line - safe attempt but this may not include filesize! */
            videoQualityHTMLs = br.getRegex("download_video\\([^\r\t\n]+").getColumn(-1);
        }
        if (videoQualityHTMLs == null || videoQualityHTMLs.length == 0) {
            /* Try new handling */
            return getDllinkViaOfficialVideoDownloadNew(br, link, account, returnFilesize);
        }
        /*
         * Internal quality identifiers highest to lowest (inside 'download_video' String): o = original, h = high, n = normal, l=low
         */
        final HashMap<String, Integer> qualityMap = new HashMap<String, Integer>();
        qualityMap.put("l", 20); // low
        qualityMap.put("n", 40); // normal
        qualityMap.put("h", 60); // high
        qualityMap.put("o", 80); // original
        long maxInternalQualityValue = 0;
        String filesizeStr = null;
        String videoQualityStr = null;
        String videoHash = null;
        String targetHTML = null;
        final String userSelectedQualityValue = getPreferredDownloadQualityStr();
        boolean foundUserSelectedQuality = false;
        if (userSelectedQualityValue == null) {
            logger.info("Trying to find highest quality for official video download");
        } else {
            logger.info(String.format("Trying to find user selected quality %s for official video download", userSelectedQualityValue));
        }
        int selectedQualityIndex = 0;
        for (int currentQualityIndex = 0; currentQualityIndex < videoQualityHTMLs.length; currentQualityIndex++) {
            final String videoQualityHTML = videoQualityHTMLs[currentQualityIndex];
            final String filesizeStrTmp = scanGenericFileSize(videoQualityHTML);
            // final String vid = videoinfo.getMatch(0);
            final Regex videoinfo = new Regex(videoQualityHTML, "download_video\\('([a-z0-9]+)','([^<>\"\\']*)','([^<>\"\\']*)'");
            // final String vid = videoinfo.getMatch(0);
            /* Usually this will be 'o' standing for "original quality" */
            final String videoQualityStrTmp = videoinfo.getMatch(1);
            final String videoHashTmp = videoinfo.getMatch(2);
            if (StringUtils.isEmpty(videoQualityStrTmp) || StringUtils.isEmpty(videoHashTmp)) {
                /*
                 * Possible plugin failure but let's skip bad items. Upper handling will fallback to stream download if everything fails!
                 */
                logger.warning("Found unidentifyable video quality");
                continue;
            } else if (!qualityMap.containsKey(videoQualityStrTmp)) {
                /*
                 * 2020-01-18: There shouldn't be any unknown values but we should consider allowing such in the future maybe as final
                 * fallback.
                 */
                logger.info("Skipping unknown quality: " + videoQualityStrTmp);
                continue;
            }
            if (userSelectedQualityValue != null && videoQualityStrTmp.equalsIgnoreCase(userSelectedQualityValue)) {
                logger.info("Found user selected quality: " + userSelectedQualityValue);
                foundUserSelectedQuality = true;
                videoQualityStr = videoQualityStrTmp;
                videoHash = videoHashTmp;
                if (filesizeStrTmp != null) {
                    /*
                     * Usually, filesize for official video downloads will be given but not in all cases. It may also happen that our upper
                     * RegEx fails e.g. for supervideo.tv.
                     */
                    filesizeStr = filesizeStrTmp;
                }
                targetHTML = videoQualityHTML;
                selectedQualityIndex = currentQualityIndex;
                break;
            } else {
                /* Look for best quality */
                final int internalQualityValueTmp = qualityMap.get(videoQualityStrTmp);
                if (internalQualityValueTmp < maxInternalQualityValue) {
                    /* Only continue with qualities that are higher than the highest we found so far. */
                    continue;
                }
                maxInternalQualityValue = internalQualityValueTmp;
                videoQualityStr = videoQualityStrTmp;
                videoHash = videoHashTmp;
                if (filesizeStrTmp != null) {
                    /*
                     * Usually, filesize for official video downloads will be given but not in all cases. It may also happen that our upper
                     * RegEx fails e.g. for supervideo.tv.
                     */
                    filesizeStr = filesizeStrTmp;
                }
                targetHTML = videoQualityHTML;
                selectedQualityIndex = currentQualityIndex;
            }
        }
        if (targetHTML == null || videoQualityStr == null || videoHash == null) {
            if (videoQualityHTMLs != null && videoQualityHTMLs.length > 0) {
                /* This should never happen */
                logger.info(String.format("Failed to find officially downloadable video quality although there are %d qualities available", videoQualityHTMLs.length));
            }
            return null;
        }
        if (filesizeStr == null) {
            /*
             * Last chance attempt to find filesize for selected quality. Only allow units "MB" and "GB" as most filesizes will have one of
             * these units.
             */
            final String[] filesizeCandidates = br.getRegex("(\\d+(?:\\.\\d{1,2})? *(MB|GB))").getColumn(0);
            /* Are there as many filesizes available as there are video qualities --> Chose correct filesize by index */
            if (filesizeCandidates.length == videoQualityHTMLs.length) {
                filesizeStr = filesizeCandidates[selectedQualityIndex];
            }
        }
        if (foundUserSelectedQuality) {
            logger.info("Found user selected quality: " + userSelectedQualityValue);
        } else {
            logger.info("Picked BEST quality: " + videoQualityStr);
        }
        if (filesizeStr == null) {
            /* No dramatic failure */
            logger.info("Failed to find filesize");
        } else {
            logger.info("Found filesize of official video download: " + filesizeStr);
        }
        if (returnFilesize) {
            /* E.g. in availablecheck */
            return filesizeStr;
        }
        /* 2019-08-29: Waittime here is possible but a rare case e.g. deltabit.co */
        this.waitTime(link, Time.systemIndependentCurrentJVMTimeMillis());
        logger.info("Waiting extra wait seconds: " + getDllinkViaOfficialVideoDownloadExtraWaittimeSeconds());
        this.sleep(getDllinkViaOfficialVideoDownloadExtraWaittimeSeconds() * 1000l, link);
        getPage(br, "/dl?op=download_orig&id=" + this.getFUIDFromURL(link) + "&mode=" + videoQualityStr + "&hash=" + videoHash);
        /* 2019-08-29: This Form may sometimes be given e.g. deltabit.co */
        final Form download1 = br.getFormByInputFieldKeyValue("op", "download1");
        if (download1 != null) {
            this.submitForm(br, download1);
            this.checkErrors(br, br.getRequest().getHtmlCode(), link, account, false);
        }
        /*
         * 2019-10-04: TODO: Unsure whether we should use the general 'getDllink' method here as it contains a lot of RegExes (e.g. for
         * streaming URLs) which are completely useless here.
         */
        dllink = this.getDllink(link, account, br, br.toString());
        if (StringUtils.isEmpty(dllink)) {
            /*
             * 2019-05-30: Test - worked for: xvideosharing.com - not exactly required as getDllink will usually already return a result.
             */
            dllink = br.getRegex("<a href=\"(https?[^\"]+)\"[^>]*>Direct Download Link</a>").getMatch(0);
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find dllink via official video download");
            return null;
        } else {
            logger.info("Successfully found dllink via official video download:" + dllink);
            return dllink;
        }
    }

    /** 2023-07-25:For new style XFS websites with links like /d/[a-z0-9]{12} e.g. filelions.to, streamhide.com */
    protected String getDllinkViaOfficialVideoDownloadNew(final Browser br, final DownloadLink link, final Account account, final boolean returnFilesize) throws Exception {
        if (returnFilesize) {
            logger.info("[FilesizeMode] Trying to find official video downloads");
        } else {
            logger.info("[DownloadMode] Trying to find official video downloads");
        }
        final String[][] videoInfo = br.getRegex("href=\"(/d/[a-z0-9]{12}_[a-z]{1})\".*?<small class=\"text-muted\">\\d+x\\d+ ([^<]+)</small>").getMatches();
        if (videoInfo == null || videoInfo.length == 0) {
            logger.info("Failed to find any official video downloads");
            return null;
        }
        /*
         * Internal quality identifiers highest to lowest (inside 'download_video' String): o = original, h = high, n = normal, l=low
         */
        final HashMap<String, Integer> qualityMap = new HashMap<String, Integer>();
        qualityMap.put("l", 20); // low
        qualityMap.put("n", 40); // normal
        qualityMap.put("h", 60); // high
        qualityMap.put("o", 80); // original
        qualityMap.put("x", 100); // download
        long maxInternalQualityValue = 0;
        String filesizeStrBest = null;
        String filesizeStrSelected = null;
        String videoURLBest = null;
        String videoURLSelected = null;
        final String userSelectedQualityValue = getPreferredDownloadQualityStr();
        if (userSelectedQualityValue == null) {
            logger.info("Trying to find highest quality for official video download");
        } else {
            logger.info(String.format("Trying to find user selected quality %s for official video download", userSelectedQualityValue));
        }
        for (final String videoInfos[] : videoInfo) {
            final String videoURL = videoInfos[0];
            final String filesizeStr = videoInfos[1];
            final String videoQualityStrTmp = new Regex(videoURL, "_([a-z]{1})$").getMatch(0);
            if (StringUtils.isEmpty(videoQualityStrTmp)) {
                /*
                 * Possible plugin failure but let's skip bad items. Upper handling will fallback to stream download if everything fails!
                 */
                logger.warning("Found unidentifyable video quality");
                continue;
            } else if (!qualityMap.containsKey(videoQualityStrTmp)) {
                /*
                 * 2020-01-18: There shouldn't be any unknown values but we should consider allowing such in the future maybe as final
                 * fallback.
                 */
                logger.info("Skipping unknown quality: " + videoQualityStrTmp);
                continue;
            }
            /* Look for best quality */
            final int internalQualityValueTmp = qualityMap.get(videoQualityStrTmp);
            if (internalQualityValueTmp > maxInternalQualityValue || videoURLBest == null) {
                maxInternalQualityValue = internalQualityValueTmp;
                videoURLBest = videoURL;
                filesizeStrBest = filesizeStr;
            }
            if (userSelectedQualityValue != null && videoQualityStrTmp.equalsIgnoreCase(userSelectedQualityValue)) {
                logger.info("Found user selected quality: " + userSelectedQualityValue);
                videoURLSelected = videoURL;
                if (filesizeStr != null) {
                    /*
                     * Usually, filesize for official video downloads will be given but not in all cases. It may also happen that our upper
                     * RegEx fails e.g. for supervideo.tv.
                     */
                    filesizeStrSelected = filesizeStr;
                }
                break;
            }
        }
        if (videoURLBest == null && videoURLSelected == null) {
            logger.warning("Video selection handling failed");
            return null;
        }
        final String filesizeStrChosen;
        final String continueURL;
        if (filesizeStrSelected == null) {
            if (userSelectedQualityValue == null) {
                logger.info("Returning BEST quality according to user preference");
            } else {
                logger.info("Returning BEST quality as fallback");
            }
            filesizeStrChosen = filesizeStrBest;
            continueURL = videoURLBest;
        } else {
            logger.info("Returning user selected quality: " + userSelectedQualityValue);
            filesizeStrChosen = filesizeStrSelected;
            continueURL = videoURLSelected;
        }
        if (returnFilesize) {
            /* E.g. in availablecheck */
            return filesizeStrChosen;
        }
        this.getPage(br, continueURL);
        String dllink = null;
        final Form download1 = br.getFormByInputFieldKeyValue("op", "download_orig");
        if (download1 != null) {
            this.handleCaptcha(link, br, download1);
            this.submitForm(br, download1);
            this.checkErrors(br, br.getRequest().getHtmlCode(), link, account, false);
        }
        dllink = this.getDllink(link, account, br, br.toString());
        if (StringUtils.isEmpty(dllink)) {
            /*
             * 2019-05-30: Test - worked for: xvideosharing.com - not exactly required as getDllink will usually already return a result.
             */
            dllink = br.getRegex("<a href\\s*=\\s*\"(https?[^\"]+)\"[^>]*>\\s*Direct Download Link\\s*</a>").getMatch(0);
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find dllink via official video download");
        } else {
            logger.info("Successfully found dllink via official video download");
        }
        return dllink;
    }

    /**
     * 2020-05-22: Workaround attempt for unnerving class="err">Security error< which can sometimes appear if you're too fast in this
     * handling. </br>
     * This issue may have solved in newer XFS versions so we might be able to remove this extra wait in the long run.
     */
    protected int getDllinkViaOfficialVideoDownloadExtraWaittimeSeconds() {
        return 5;
    }

    /**
     * @return User selected video download quality for official video download. </br>
     *         h = high </br>
     *         n = normal </br>
     *         l = low </br>
     *         null = No selection/Grab BEST available
     */
    protected String getPreferredDownloadQualityStr() {
        final Class<? extends XFSConfigVideo> cfgO = getVideoConfigInterface();
        if (cfgO != null) {
            final XFSConfigVideo cfg = PluginJsonConfig.get(cfgO);
            final PreferredDownloadQuality quality = cfg.getPreferredDownloadQuality();
            switch (quality) {
            case HIGH:
                return "h";
            case NORMAL:
                return "n";
            case LOW:
                return "l";
            case BEST:
            default:
                return null;
            }
        } else {
            return null;
        }
    }

    protected void setCaptchaResponse(final Browser br, CaptchaHosterHelperInterface captchaHosterHelper, final Form form, final String response) {
        if (captchaHosterHelper instanceof CaptchaHelperHostPluginHCaptcha) {
            form.put("h-captcha-response", Encoding.urlEncode(response));
            if (containsRecaptchaV2Class(br)) {
                /*
                 * E.g. novafile.com, filefox.cc - some use this as legacy handling, some will even send both, h-captcha-response AND
                 * g-recaptcha-response
                 */
                form.put("g-recaptcha-response", Encoding.urlEncode(response));
            }
        } else {
            form.put("g-recaptcha-response", Encoding.urlEncode(response));
        }
    }

    /**
     * Admins may sometimes setup waittimes that are higher than the interactive captcha timeout so lets say they set up 180 seconds of
     * pre-download-waittime --> User solves captcha immediately --> Captcha-solution times out after 120 seconds --> User has to re-enter
     * it in browser (and it would fail in JD)! </br>
     * If admins set it up in a way that users can solve the captcha via the waittime counts down, this failure may even happen via browser!
     * </br>
     * This is basically a workaround which avoids running into said timeout: Make sure that we wait less than 120 seconds after the user
     * has solved the captcha by waiting some of this time in beforehand.
     */
    protected void waitBeforeInteractiveCaptcha(final DownloadLink link, final int captchaTimeoutMillis) throws PluginException {
        if (!this.preDownloadWaittimeSkippable()) {
            final String waitStr = regexWaittime();
            if (waitStr != null && waitStr.matches("\\d+")) {
                final int preDownloadWaittimeMillis = Integer.parseInt(waitStr) * 1000;
                if (preDownloadWaittimeMillis > captchaTimeoutMillis) {
                    final int prePrePreDownloadWait = preDownloadWaittimeMillis - captchaTimeoutMillis;
                    logger.info("Waittime is higher than interactive captcha timeout --> Waiting a part of it before solving captcha to avoid timeouts");
                    logger.info("Pre-pre download waittime seconds: " + (prePrePreDownloadWait / 1000));
                    this.sleep(prePrePreDownloadWait, link);
                }
            }
        }
    }

    protected boolean handleHCaptcha(final DownloadLink link, Browser br, final Form captchaForm) throws Exception {
        final CaptchaHelperHostPluginHCaptcha hCaptcha = getCaptchaHelperHostPluginHCaptcha(this, br);
        /*
         * This contains a workaround for a widespread design-flaw when using hcaptcha and a long wait-time in browser: We need to split up
         * the total waittime in such a case otherwise our solution token will expire before we get the chance to send it!
         */
        logger.info("Detected captcha method \"hcaptcha\" type '" + hCaptcha.getType() + "' for this host");
        this.waitBeforeInteractiveCaptcha(link, hCaptcha.getSolutionTimeout());
        final String response = hCaptcha.getToken();
        setCaptchaResponse(br, hCaptcha, captchaForm, response);
        return true;
    }

    protected boolean handleRecaptchaV2(final DownloadLink link, Browser br, final Form captchaForm) throws Exception {
        /*
         * This contains a workaround for a widespread design-flaw when using reCaptchaV2 and a long wait-time in browser: We need to split
         * up the wait in such a case.
         */
        final CaptchaHelperHostPluginRecaptchaV2 rc2 = getCaptchaHelperHostPluginRecaptchaV2(this, br);
        logger.info("Detected captcha method \"RecaptchaV2\" normal-type '" + rc2.getType() + "' for this host");
        this.waitBeforeInteractiveCaptcha(link, rc2.getSolutionTimeout());
        final String recaptchaV2Response = rc2.getToken();
        setCaptchaResponse(br, rc2, captchaForm, recaptchaV2Response);
        return true;
    }

    protected CaptchaHelperHostPluginHCaptcha getCaptchaHelperHostPluginHCaptcha(PluginForHost plugin, Browser br) throws PluginException {
        return new CaptchaHelperHostPluginHCaptcha(this, br);
    }

    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2(PluginForHost plugin, Browser br) throws PluginException {
        return new CaptchaHelperHostPluginRecaptchaV2(this, br);
    }

    /** Handles all kinds of captchas, also login-captcha - fills the given captchaForm. */
    public void handleCaptcha(final DownloadLink link, Browser br, final Form captchaForm) throws Exception {
        /* Captcha START */
        if (new Regex(getCorrectBR(br), "(geetest_challenge|geetest_validate|geetest_seccode)").matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unsupported captcha type geetest", 3 * 60 * 60 * 1000l);
        } else if (new Regex(getCorrectBR(br), Pattern.compile("\\$\\.post\\(\\s*\"/ddl\"", Pattern.CASE_INSENSITIVE)).matches()) {
            /* 2019-06-06: Rare case */
            final String captchaResponse;
            final CaptchaHosterHelperInterface captchaHelper;
            if (containsHCaptcha(getCorrectBR(br))) {
                /* E.g. uploadbank.com */
                final CaptchaHelperHostPluginHCaptcha hCaptcha = getCaptchaHelperHostPluginHCaptcha(this, br);
                logger.info("Detected captcha method \"hCaptcha\" type '" + hCaptcha.getType() + "' for this host");
                captchaHelper = hCaptcha;
                this.waitBeforeInteractiveCaptcha(link, hCaptcha.getSolutionTimeout());
                captchaResponse = hCaptcha.getToken();
            } else {
                /* Assume reCaptchaV2 is required */
                final CaptchaHelperHostPluginRecaptchaV2 rc2 = getCaptchaHelperHostPluginRecaptchaV2(this, br);
                logger.info("Detected captcha method \"RecaptchaV2\" type '" + rc2.getType() + "' for this host");
                captchaHelper = rc2;
                this.waitBeforeInteractiveCaptcha(link, rc2.getSolutionTimeout());
                captchaResponse = captchaHelper.getToken();
            }
            /*
             * 2017-12-07: New - solve- and check reCaptchaV2 here via ajax call, then wait- and submit the main downloadform. This might as
             * well be a workaround by the XFS developers to avoid expiring reCaptchaV2 challenges. Example: filefox.cc
             */
            /* 2017-12-07: New - this case can only happen during download and cannot be part of the login process! */
            /* Do not put the result in the given Form as the check itself is handled via Ajax right here! */
            captchaForm.put("g-recaptcha-response", "");
            final Form ajaxCaptchaForm = new Form();
            ajaxCaptchaForm.setMethod(MethodType.POST);
            ajaxCaptchaForm.setAction("/ddl");
            final InputField inputField_Rand = captchaForm.getInputFieldByName("rand");
            final String file_id = PluginJSonUtils.getJson(br, "file_id");
            if (inputField_Rand != null) {
                /* This is usually given */
                ajaxCaptchaForm.put("rand", inputField_Rand.getValue());
            }
            if (!StringUtils.isEmpty(file_id)) {
                /* This is usually given */
                ajaxCaptchaForm.put("file_id", file_id);
            }
            ajaxCaptchaForm.put("op", "captcha1");
            setCaptchaResponse(br, captchaHelper, ajaxCaptchaForm, captchaResponse);
            /* User existing Browser object as we get a cookie which is required later. */
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            this.submitForm(br, ajaxCaptchaForm);
            if (!br.toString().equalsIgnoreCase("OK")) {
                if (br.toString().equalsIgnoreCase("ERROR: Wrong captcha")) {
                    /* 2019-12-14: Happens but should never happen ... */
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    this.logger.warning("Fatal " + captchaHelper + " ajax handling failure");
                    checkErrorsLastResort(br, null);
                }
            }
            br.getHeaders().remove("X-Requested-With");
            link.setProperty(PROPERTY_captcha_required, Boolean.TRUE);
        } else if (containsHCaptcha(getCorrectBR(br))) {
            if (handleHCaptcha(link, br, captchaForm)) {
                link.setProperty(PROPERTY_captcha_required, Boolean.TRUE);
            }
        } else if (containsRecaptchaV2Class(getCorrectBR(br))) {
            if (handleRecaptchaV2(link, br, captchaForm)) {
                link.setProperty(PROPERTY_captcha_required, Boolean.TRUE);
            }
        } else {
            if (containsPlainTextCaptcha(getCorrectBR(br))) {
                logger.info("Detected captcha method \"plaintext captchas\" for this host");
                /* Captcha method by ManiacMansion */
                String[][] letters = new Regex(br, "<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(&#\\d+;)</span>").getMatches();
                if (letters == null || letters.length == 0) {
                    /* Try again, this time look in non-cleaned-up html as correctBR() could have removed this part! */
                    letters = new Regex(br.toString(), "<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(&#\\d+;)</span>").getMatches();
                    if (letters == null || letters.length == 0) {
                        logger.warning("plaintext captchahandling broken!");
                        checkErrorsLastResort(br, null);
                    }
                }
                final SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
                for (String[] letter : letters) {
                    capMap.put(Integer.parseInt(letter[0]), Encoding.htmlDecode(letter[1]));
                }
                final StringBuilder code = new StringBuilder();
                for (String value : capMap.values()) {
                    code.append(value);
                }
                captchaForm.put("code", code.toString());
                logger.info("Put captchacode " + code.toString() + " obtained by captcha metod \"plaintext captchas\" in captchaForm");
                link.setProperty(PROPERTY_captcha_required, Boolean.TRUE);
            } else if (StringUtils.containsIgnoreCase(getCorrectBR(br), "/captchas/")) {
                logger.info("Detected captcha method \"Standard captcha\" for this host");
                final String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), "");
                String captchaurl = null;
                if (sitelinks == null || sitelinks.length == 0) {
                    logger.warning("Standard captcha captchahandling broken!");
                    checkErrorsLastResort(br, null);
                }
                for (final String linkTmp : sitelinks) {
                    if (linkTmp.contains("/captchas/")) {
                        captchaurl = linkTmp;
                        break;
                    }
                }
                if (StringUtils.isEmpty(captchaurl)) {
                    /* Fallback e.g. for relative URLs (e.g. subyshare.com [bad example, needs special handling anways!]) */
                    captchaurl = new Regex(getCorrectBR(br), "(/captchas/[a-z0-9]+\\.jpg)").getMatch(0);
                }
                if (captchaurl == null) {
                    logger.warning("Standard captcha captchahandling broken2!");
                    checkErrorsLastResort(br, null);
                }
                String code = getCaptchaCode("xfilesharingprobasic", captchaurl, link);
                captchaForm.put("code", code);
                logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
                link.setProperty(PROPERTY_captcha_required, Boolean.TRUE);
            } else if (new Regex(getCorrectBR(br), "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)").matches()) {
                logger.info("Detected captcha method \"reCaptchaV1\" for this host");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Website uses reCaptchaV1 which has been shut down by Google. Contact website owner!");
            } else if (this.containsSolvemediaCaptcha(getCorrectBR(br))) {
                logger.info("Detected captcha method \"solvemedia\" for this host");
                final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                File cf = null;
                try {
                    cf = sm.downloadCaptcha(getLocalCaptchaFile());
                } catch (final InterruptedException e) {
                    throw e;
                } catch (final Exception e) {
                    if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support", -1, e);
                    } else {
                        throw e;
                    }
                }
                final String code = getCaptchaCode("solvemedia", cf, link);
                final String chid = sm.getChallenge(code);
                captchaForm.put("adcopy_challenge", chid);
                captchaForm.put("adcopy_response", "manual_challenge");
                link.setProperty(PROPERTY_captcha_required, Boolean.TRUE);
            } else if (br.containsHTML("id=\"capcode\" name= \"capcode\"")) {
                logger.info("Detected captcha method \"keycaptcha\"");
                String result = handleCaptchaChallenge(getDownloadLink(), new KeyCaptcha(this, br, getDownloadLink()).createChallenge(this));
                if (result == null) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if ("CANCEL".equals(result)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL);
                }
                captchaForm.put("capcode", result);
                link.setProperty(PROPERTY_captcha_required, Boolean.TRUE);
            } else {
                link.setProperty(PROPERTY_captcha_required, Boolean.FALSE);
            }
            /* Captcha END */
        }
    }

    protected boolean containsPlainTextCaptcha(final String correctBR) {
        return correctBR != null && StringUtils.containsIgnoreCase(correctBR, ";background:#ccc;text-align");
    }

    /** Tries to find 1st download Form for free(and Free-Account) download. */
    public Form findFormDownload1Free(final Browser br) throws Exception {
        final Form ret = br == null ? null : br.getFormByInputFieldKeyValue("op", "download1");
        if (ret != null) {
            ret.remove("method_premium");
            /* Fix/Add "method_free" value if necessary. */
            if (!ret.hasInputFieldByName("method_free") || ret.getInputFieldByName("method_free").getValue() == null) {
                String method_free_value = ret.getRegex("\"method_free\" value=\"([^<>\"]+)\"").getMatch(0);
                if (method_free_value == null || method_free_value.equals("")) {
                    method_free_value = "Free Download";
                }
                ret.put("method_free", Encoding.urlEncode(method_free_value));
            }
        }
        return ret;
    }

    /** Tries to find 2nd download Form for free(and Free-Account) download. */
    protected Form findFormDownload2Free(final Browser br) {
        Form ret = null;
        /* First try to find Form for video hosts with multiple qualities. */
        final Form[] forms = br.getForms();
        for (final Form form : forms) {
            final InputField op_field = form.getInputFieldByName("op");
            /* E.g. name="op" value="download_orig" */
            if (form.containsHTML("method_") && op_field != null && op_field.getValue().contains("download")) {
                ret = form;
                break;
            }
        }
        /* Nothing found? Fallback to simpler handling - this is more likely to pickup a wrong Form! */
        if (ret == null) {
            ret = br.getFormbyProperty("name", "F1");
            if (ret == null) {
                ret = br.getFormByInputFieldKeyValue("op", "download2");
            }
        }
        final InputField adblock_detected = ret != null ? ret.getInputField("adblock_detected") : null;
        if (adblock_detected != null && StringUtils.isEmpty(adblock_detected.getValue())) {
            adblock_detected.setValue("0");
        }
        return ret;
    }

    /**
     * Tries to find download Form for premium download.
     *
     * @throws Exception
     */
    public Form findFormDownload2Premium(final DownloadLink downloadLink, final Account account, final Browser br) throws Exception {
        return br == null ? null : br.getFormbyProperty("name", "F1");
    }

    /**
     * Check if a stored directlink exists under property 'property' and if so, check if it is still valid (leads to a downloadable content
     * [NOT html]).
     *
     * @throws Exception
     */
    protected final String checkDirectLink(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty = getDownloadModeDirectlinkProperty(account);
        final String dllink = link.getStringProperty(getDownloadModeDirectlinkProperty(account));
        if (dllink != null) {
            final String validDirecturl = checkDirectLinkAndSetFilesize(link, dllink, false);
            if (validDirecturl != null) {
                return validDirecturl;
            } else {
                link.removeProperty(directurlproperty);
                return null;
            }
        }
        return null;
    }

    /**
     * Checks if a directurl leads to downloadable content and if so, returns true. <br />
     * This will also return true if the serverside connection limit has been reached. <br />
     *
     * @param link
     *            : The DownloadLink
     * @param directurl
     *            : Directurl which should lead to downloadable content
     * @param setFilesize
     *            : true = setVerifiedFileSize filesize if directurl is really downloadable
     * @throws Exception
     */
    protected final String checkDirectLinkAndSetFilesize(final DownloadLink link, final String directurl, final boolean setFilesize) throws Exception {
        if (StringUtils.isEmpty(directurl) || !directurl.startsWith("http")) {
            return null;
        }
        URLConnectionAdapter con = null;
        boolean throwException = false;
        try {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            if (supportsHEADRequestForDirecturlCheck()) {
                con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(directurl));
            } else {
                con = openAntiDDoSRequestConnection(br2, br2.createGetRequest(directurl));
            }
            if (con.getResponseCode() == 503) {
                /* 503 too many connections: URL is valid but we can't use it at this moment. */
                throwException = true;
                exception503ConnectionLimitReached();
                return directurl;
            } else if (looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() >= 0 && con.getCompleteContentLength() < 100) {
                    /* Rare case */
                    throw new Exception("very likely no file but an error message!length=" + con.getCompleteContentLength());
                } else {
                    if (setFilesize && con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return directurl;
                }
            } else if (StringUtils.equalsIgnoreCase(con.getContentType(), "application/vnd.apple.mpegurl")) {
                /* HLS download --> HLS URL is valid */
                return directurl;
            } else {
                /* Failure */
                br2.followConnection(true);
                throw new Exception("no downloadable content?" + con.getResponseCode() + "|" + con.getContentType() + "|" + con.isContentDisposition());
            }
        } catch (final Exception e) {
            /* Failure */
            if (throwException) {
                throw e;
            } else {
                logger.log(e);
                return null;
            }
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
    }

    private final boolean attemptStoredDownloadurlDownload(final DownloadLink link, final Account account) throws Exception {
        final String directurlproperty = getDownloadModeDirectlinkProperty(account);
        final String url = link.getStringProperty(directurlproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        boolean throwException = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, account), this.getMaxChunks(account));
            if (dl.getConnection().getResponseCode() == 503) {
                brc.followConnection(true);
                throwException = true;
                valid = true;
                exception503ConnectionLimitReached();
                return true;
            } else if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else if (StringUtils.equalsIgnoreCase(dl.getConnection().getContentType(), "application/vnd.apple.mpegurl")) {
                /* HLS download */
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable ignore) {
                }
                valid = true;
                dl = new HLSDownloader(link, brc, url);
                return true;
            } else {
                /* Remove property so we don't retry the same invalid directurl again next time. */
                link.removeProperty(directurlproperty);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final InterruptedException e) {
            throw e;
        } catch (final Exception e) {
            if (throwException) {
                throw e;
            } else {
                logger.log(e);
                return false;
            }
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    @Override
    public boolean hasAutoCaptcha() {
        /* Assume we never got auto captcha as most services will use e.g. reCaptchaV2 nowdays. */
        return false;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null || acc.getType() == AccountType.FREE) {
            /* Anonymous downloads & Free account downloads may have captchas */
            return true;
        } else {
            /* Premium accounts don't have captchas */
            return false;
        }
    }

    /** Cleans correctedBrowserRequestMap */
    @Override
    public void clean() {
        try {
            super.clean();
        } finally {
            synchronized (correctedBrowserRequestMap) {
                correctedBrowserRequestMap.clear();
            }
        }
    }

    /** Traits used to cleanup html of our basic browser object and put it into correctedBR. */
    public ArrayList<String> getCleanupHTMLRegexes() {
        final ArrayList<String> regexStuff = new ArrayList<String>();
        // remove custom rules first!!! As html can change because of generic cleanup rules.
        /* generic cleanup */
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: ?none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        return regexStuff;
    }

    protected String replaceCorrectBR(Browser br, String pattern, String target) {
        /* Do not e.g. remove captcha forms from html! */
        if (StringUtils.containsIgnoreCase(pattern, "none") && (containsHCaptcha(target) || containsRecaptchaV2Class(target) || containsPlainTextCaptcha(target))) {
            return null;
        } else {
            return "";
        }
    }

    /** Removes HTML code which could break the plugin and puts it into correctedBR. */
    protected String correctBR(final Browser br) {
        synchronized (correctedBrowserRequestMap) {
            final Request request = br.getRequest();
            String correctedBR = correctedBrowserRequestMap.get(request);
            if (correctedBR == null) {
                correctedBR = br.toString();
                final ArrayList<String> regexStuff = getCleanupHTMLRegexes();
                // remove custom rules first!!! As html can change because of generic cleanup rules.
                /* generic cleanup */
                boolean modified = false;
                for (final String aRegex : regexStuff) {
                    final String results[] = new Regex(correctedBR, aRegex).getColumn(0);
                    if (results != null) {
                        for (final String result : results) {
                            final String replace = replaceCorrectBR(br, aRegex, result);
                            if (replace != null) {
                                correctedBR = correctedBR.replace(result, replace);
                                modified = true;
                            }
                        }
                    }
                }
                if (modified && request != null && request.isRequested()) {
                    correctedBrowserRequestMap.put(request, correctedBR);
                } else {
                    correctedBrowserRequestMap.remove(request);
                }
            }
            this.correctedBR = correctedBR;
            return correctedBR;
        }
    }

    protected String getCorrectBR(Browser br) {
        synchronized (correctedBrowserRequestMap) {
            final String ret = correctedBrowserRequestMap.get(br.getRequest());
            if (ret != null) {
                return ret;
            } else {
                return br.toString();
            }
        }
    }

    /**
     * Function to find the final downloadlink. </br>
     * This will also find video directurls of embedded videos if the player is 'currently visible'.
     */
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        String dllink = br.getRedirectLocation();
        if (dllink == null || new Regex(dllink, this.getSupportedLinks()).matches()) {
            if (StringUtils.isEmpty(dllink)) {
                for (final Pattern pattern : getDownloadurlRegexes()) {
                    dllink = new Regex(src, pattern).getMatch(0);
                    if (dllink != null) {
                        break;
                    }
                }
            }
            // if (dllink == null) {
            // /* Try short version */
            // dllink = new Regex(src, "(\"|')(" + String.format(dllinkRegexFile_2, getHostsPatternPart()) + ")\\1").getMatch(1);
            // }
            // if (dllink == null) {
            // /* Try short version without hardcoded domains and wide */
            // dllink = new Regex(src, "(" + String.format(dllinkRegexFile_2, getHostsPatternPart()) + ")").getMatch(0);
            // }
            if (StringUtils.isEmpty(dllink)) {
                final String cryptedScripts[] = new Regex(src, "p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
                if (cryptedScripts != null && cryptedScripts.length != 0) {
                    for (String crypted : cryptedScripts) {
                        dllink = decodeDownloadLink(link, account, br, crypted);
                        if (dllink != null) {
                            break;
                        }
                    }
                }
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            dllink = getDllinkVideohost(link, account, br, src);
        }
        if (dllink == null && this.isImagehoster()) {
            /* Used for imagehosts */
            dllink = getDllinkImagehost(link, account, br, src);
        }
        if (dllink != null && Encoding.isHtmlEntityCoded(dllink)) {
            /* 2020-02-10: E.g. files.im */
            dllink = Encoding.htmlOnlyDecode(dllink);
        }
        return dllink;
    }

    protected String getDllinkImagehost(DownloadLink link, Account account, Browser br, final String src) {
        /*
         * 2019-07-24: This is basically a small workaround because if a file has a "bad filename" the filename inside our URL may just look
         * like it is a thumbnail although it is not. If we find several URLs and all are the same we may still just take one of them
         * although it could be a thumbnail.
         */
        final Map<String, Integer> possibleDllinks = new HashMap<String, Integer>();
        for (final Pattern regex : getImageDownloadurlRegexes()) {
            final String[] dllinksTmp = new Regex(src, regex).getColumn(0);
            for (final String url : dllinksTmp) {
                Integer count = possibleDllinks.get(url);
                if (count == null) {
                    count = 1;
                } else {
                    count++;
                }
                possibleDllinks.put(url, count);
            }
        }
        Entry<String, Integer> best = null;
        for (Entry<String, Integer> entry : possibleDllinks.entrySet()) {
            /* Avoid downloading thumbnails */
            /* 2019-07-24: Improve recognization of thumbnails e.g. https://img67.imagetwist.com/th/123456/[a-z0-9]{12}.jpg */
            if (entry.getKey().matches(".+_t\\.[A-Za-z]{3,4}$") || entry.getKey().matches(".+/th/\\d+.*$")) {
                continue;
            } else if (best == null) {
                best = entry;
            } else if (entry.getValue() > best.getValue()) {
                best = entry;
            }
        }
        if (best == null && possibleDllinks.size() > 0) {
            best = possibleDllinks.entrySet().iterator().next();
        }
        if (best != null) {
            final String dllink = best.getKey();
            return dllink;
        } else {
            return null;
        }
    }

    /**
     * Tries to find stream-URL for videohosts.
     *
     * @param link
     *            TODO
     * @param account
     *            TODO
     * @param br
     *            TODO
     */
    protected String getDllinkVideohost(final DownloadLink link, final Account account, final Browser br, final String src) {
        String dllink = null;
        /* RegExes for videohosts */
        String jssource = new Regex(src, "\"?sources\"?\\s*:\\s*(\\[[^\\]]+\\])").getMatch(0);
        if (StringUtils.isEmpty(jssource)) {
            /* 2019-07-04: Wider attempt - find sources via pattern of their video-URLs. */
            jssource = new Regex(src, "[A-Za-z0-9]+\\s*:\\s*(\\[[^\\]]+[a-z0-9]{60}/v\\.mp4[^\\]]+\\])").getMatch(0);
        }
        if (!StringUtils.isEmpty(jssource)) {
            logger.info("Found video json source");
            /*
             * Different services store the values we want under different names. E.g. vidoza.net uses 'res', most providers use 'label'.
             */
            final String[] possibleQualityObjectNames = new String[] { "label", "res" };
            /*
             * Different services store the values we want under different names. E.g. vidoza.net uses 'src', most providers use 'file'.
             */
            final String[] possibleStreamURLObjectNames = new String[] { "file", "src" };
            try {
                /*
                 * Important: Default is -1 so that even if only one quality is available without quality-identifier, it will be used!
                 */
                long quality_picked = -1;
                String dllink_temp = null;
                final List<Object> ressourcelist = (List) JavaScriptEngineFactory.jsonToJavaObject(jssource);
                final boolean onlyOneQualityAvailable = ressourcelist.size() == 1;
                final int userSelectedQuality = getPreferredStreamQuality();
                if (userSelectedQuality == -1) {
                    logger.info("Looking for BEST video stream");
                } else {
                    logger.info("Looking for user selected video stream quality: " + userSelectedQuality);
                }
                boolean foundUserSelectedQuality = false;
                for (final Object videoo : ressourcelist) {
                    /* Check for single URL without any quality information e.g. uqload.com */
                    if (videoo instanceof String && onlyOneQualityAvailable) {
                        logger.info("Only one quality available --> Returning that");
                        dllink_temp = (String) videoo;
                        if (dllink_temp.startsWith("http")) {
                            dllink = dllink_temp;
                            break;
                        }
                    }
                    final Map<String, Object> entries;
                    if (videoo instanceof Map) {
                        entries = (Map<String, Object>) videoo;
                        for (final String possibleStreamURLObjectName : possibleStreamURLObjectNames) {
                            if (entries.containsKey(possibleStreamURLObjectName)) {
                                dllink_temp = (String) entries.get(possibleStreamURLObjectName);
                                break;
                            }
                        }
                    } else {
                        entries = null;
                    }
                    if (StringUtils.isEmpty(dllink_temp)) {
                        /* No downloadurl found --> Continue */
                        continue;
                    } else if (dllink_temp.contains(".mpd")) {
                        /* 2020-05-20: This plugin cannot yet handle DASH stream downloads */
                        logger.info("Skipping DASH stream: " + dllink_temp);
                        continue;
                    }
                    /* Find quality + downloadurl */
                    long quality_temp = 0;
                    for (final String possibleQualityObjectName : possibleQualityObjectNames) {
                        try {
                            final Object quality_temp_o = entries.get(possibleQualityObjectName);
                            if (quality_temp_o != null && quality_temp_o instanceof Number) {
                                quality_temp = (int) JavaScriptEngineFactory.toLong(quality_temp_o, 0);
                            } else if (quality_temp_o != null && quality_temp_o instanceof String) {
                                /* E.g. '360p' */
                                final String res = new Regex((String) quality_temp_o, "(\\d+)p?$").getMatch(0);
                                if (res != null) {
                                    quality_temp = (int) Long.parseLong(res);
                                }
                            }
                            if (quality_temp > 0) {
                                break;
                            }
                        } catch (final Throwable e) {
                            /* This should never happen */
                            logger.log(e);
                            logger.info("Failed to find quality via key '" + possibleQualityObjectName + "' for current downloadurl candidate: " + dllink_temp);
                            if (!onlyOneQualityAvailable) {
                                continue;
                            }
                        }
                    }
                    if (StringUtils.isEmpty(dllink_temp)) {
                        continue;
                    } else if (quality_temp == userSelectedQuality) {
                        /* Found user selected quality */
                        logger.info("Found user selected quality: " + userSelectedQuality);
                        foundUserSelectedQuality = true;
                        quality_picked = quality_temp;
                        dllink = dllink_temp;
                        break;
                    } else {
                        /* Look for best quality */
                        if (quality_temp > quality_picked) {
                            quality_picked = quality_temp;
                            dllink = dllink_temp;
                        }
                    }
                }
                if (!StringUtils.isEmpty(dllink)) {
                    logger.info("Quality handling for multiple video stream sources succeeded - picked quality is: " + quality_picked);
                    if (foundUserSelectedQuality) {
                        logger.info("Successfully found user selected quality: " + userSelectedQuality);
                    } else {
                        logger.info("Successfully found BEST quality: " + quality_picked);
                    }
                } else {
                    logger.info("Failed to find any stream downloadurl");
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("BEST handling for multiple video source failed");
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            /* 2019-07-04: Examplehost: vidoza.net */
            /* TODO: Check if we can remove 'regexVideoStreamDownloadURL' or integrate it in this function. */
            dllink = regexVideoStreamDownloadURL(src);
        }
        if (StringUtils.isEmpty(dllink)) {
            final String check = new Regex(src, "file\\s*:\\s*\"(https?[^<>\"]*?\\.(?:mp4|flv))\"").getMatch(0);
            if (StringUtils.isNotEmpty(check) && !StringUtils.containsIgnoreCase(check, "/images/")) {
                // jwplayer("flvplayer").onError(function()...
                dllink = check;
            }
        }
        return dllink;
    }

    /** Generic RegEx to find common XFS stream download URLs */
    private final String regexVideoStreamDownloadURL(final String src) {
        String dllink = new Regex(src, Pattern.compile("(https?://[^/]+[^\"]+[a-z0-9]{60}/v\\.mp4)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (StringUtils.isEmpty(dllink)) {
            /* Wider attempt */
            dllink = new Regex(src, Pattern.compile("\"(https?://[^/]+/[a-z0-9]{60}/[^\"]+)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        }
        return dllink;
    }

    protected Class<? extends XFSConfigVideo> getVideoConfigInterface() {
        final Class<? extends XFSConfig> configInterface = this.getConfigInterface();
        if (configInterface != null && XFSConfigVideo.class.isAssignableFrom(configInterface)) {
            return (Class<? extends XFSConfigVideo>) configInterface;
        } else {
            return null;
        }
    }

    /** Returns user selected stream quality. -1 = BEST/no selection */
    protected final int getPreferredStreamQuality() {
        final Class<? extends XFSConfigVideo> cfgO = getVideoConfigInterface();
        if (cfgO != null) {
            final XFSConfigVideo cfg = PluginJsonConfig.get(cfgO);
            final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
            switch (quality) {
            case Q2160P:
                return 2160;
            case Q1080P:
                return 1080;
            case Q720P:
                return 720;
            case Q480P:
                return 480;
            case Q360P:
                return 360;
            case BEST:
            default:
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     * Returns URL to the video thumbnail.
     */
    @Deprecated
    private String getVideoThumbnailURL(final String src) {
        String url_thumbnail = new Regex(src, "image\\s*:\\s*\"(https?://[^<>\"]+)\"").getMatch(0);
        if (StringUtils.isEmpty(url_thumbnail)) {
            /* 2019-05-16: e.g. uqload.com */
            url_thumbnail = new Regex(src, "poster\\s*:\\s*\"(https?://[^<>\"]+)\"").getMatch(0);
        }
        return url_thumbnail;
    }

    public String decodeDownloadLink(final DownloadLink link, final Account account, final Browser br, final String s) {
        String decoded = null;
        try {
            final Regex params = new Regex(s, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");
            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");
            while (c != 0) {
                c--;
                if (k[c].length() != 0) {
                    p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                }
            }
            decoded = p;
        } catch (Exception e) {
            logger.log(e);
        }
        String dllink = null;
        if (decoded != null) {
            dllink = getDllinkVideohost(link, account, br, decoded);
            if (StringUtils.isEmpty(dllink)) {
                /* Open regex is possible because in the unpacked JS there are usually only 1-2 URLs. */
                dllink = new Regex(decoded, "(?:\"|')(https?://[^<>\"']*?\\.(avi|flv|mkv|mp4|m3u8))(?:\"|')").getMatch(0);
            }
            if (StringUtils.isEmpty(dllink)) {
                /* Maybe rtmp */
                dllink = new Regex(decoded, "(?:\"|')(rtmp://[^<>\"']*?mp4:[^<>\"']+)(?:\"|')").getMatch(0);
            }
        }
        return dllink;
    }

    protected boolean isDllinkFile(final String url) {
        if (!StringUtils.isEmpty(url)) {
            for (final Pattern pattern : this.getDownloadurlRegexes()) {
                final String urlMatch = new Regex(url, pattern).getMatch(0);
                if (urlMatch != null) {
                    return true;
                }
            }
        }
        return false;
    }

    protected final String getDllinkHostPattern() {
        return "[A-Za-z0-9\\-\\.]*";
    }

    /** Returns pre-download-waittime (seconds) from inside HTML. */
    @Deprecated
    protected String regexWaittime() {
        return regexWaittime(correctedBR);
    }

    /** Returns pre-download-waittime (seconds) from inside HTML. */
    protected String regexWaittime(final String html) {
        String waitStr = new Regex(html, "id=(?:\"|\\')countdown_str(?:\"|\\')[^>]*>[^<>]*<span id=[^>]*>\\s*(\\d+)\\s*</span>").getMatch(0);
        if (waitStr == null) {
            waitStr = new Regex(html, "class=\"seconds\"[^>]*>\\s*(\\d+)\\s*</span>").getMatch(0);
        }
        if (waitStr == null) {
            /* More open RegEx */
            waitStr = new Regex(html, "class=\"seconds\"[^>]*>\\s*(\\d+)\\s*<").getMatch(0);
        }
        return waitStr;
    }

    /** Returns list of possible final downloadurl patterns. Match 0 will be used to find downloadurls in html source! */
    protected List<Pattern> getDownloadurlRegexes() {
        final List<Pattern> patterns = new ArrayList<Pattern>();
        /* 2020-04-01: TODO: Maybe add this part to the end: (\\s+|\\s*>|\\s*\\)|\\s*;) (?) */
        /* Allow ' in URL */
        patterns.add(Pattern.compile("\"" + String.format("(https?://(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|%s)(?::\\d+)?/(?:files|d|cgi\\-bin/dl\\.cgi|dl)/(?:\\d+/)?[a-z0-9]+/[^<>\"/]*)", this.getDllinkHostPattern()) + "\""));
        /* Allow ' in URL but must end on '); */
        patterns.add(Pattern.compile(String.format("(https?://(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|%s)(?::\\d+)?/(?:files|d|cgi\\-bin/dl\\.cgi|dl)/(?:\\d+/)?[a-z0-9]+/[^<>\"/]*)", this.getDllinkHostPattern()) + "'\\s*\\)\\s*;"));
        /* Don't allow ' in URL */
        patterns.add(Pattern.compile(String.format("(https?://(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|%s)(?::\\d+)?/(?:files|d|cgi\\-bin/dl\\.cgi|dl)/(?:\\d+/)?[a-z0-9]+/[^<>\"\\'/]*)", this.getDllinkHostPattern())));
        return patterns;
    }

    /** Returns list of possible final image-host-downloadurl patterns. Match 0 will be used to find downloadurls in html source! */
    protected List<Pattern> getImageDownloadurlRegexes() {
        final List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(Pattern.compile(String.format("(https?://(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|%s)(?::\\d+)?(?:/img/\\d+/[^<>\"'\\[\\]]+|/img/[a-z0-9]+/[^<>\"'\\[\\]]+|/img/[^<>\"'\\[\\]]+|/i/\\d+/[^<>\"'\\[\\]]+|/i/\\d+/[^<>\"'\\[\\]]+(?!_t\\.[A-Za-z]{3,4})))", this.getDllinkHostPattern())));
        return patterns;
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param num
     *            : (+1|-1)
     */
    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null) {
            final AtomicInteger freeRunning = getFreeRunning();
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
    }

    @Override
    protected void getPage(String page) throws Exception {
        getPage(br, page, true);
    }

    protected void getPage(final Browser br, String page, final boolean correctBr) throws Exception {
        getPage(br, page);
        if (correctBr) {
            correctBR(br);
        }
    }

    @Override
    protected void postPage(String page, final String postdata) throws Exception {
        postPage(br, page, postdata, true);
    }

    protected void postPage(final Browser br, String page, final String postdata, final boolean correctBr) throws Exception {
        postPage(br, page, postdata);
        if (correctBr) {
            correctBR(br);
        }
    }

    @Override
    protected void submitForm(final Form form) throws Exception {
        submitForm(br, form, true);
    }

    protected void submitForm(final Browser br, final Form form, final boolean correctBr) throws Exception {
        submitForm(br, form);
        if (correctBr) {
            correctBR(br);
        }
    }

    /**
     * Handles pre download (pre-captcha) waittime.
     */
    protected void waitTime(final DownloadLink link, final long timeBefore) throws PluginException {
        /* Ticket Time */
        final String waitStr = regexWaittime();
        if (this.preDownloadWaittimeSkippable()) {
            /* Very rare case! */
            logger.info("Skipping pre-download waittime: " + waitStr);
        } else {
            if (waitStr != null && waitStr.matches("\\d+")) {
                logger.info("Found waittime, parsing waittime: " + waitStr);
                int wait = Integer.parseInt(waitStr);
                if (wait == 0) {
                    logger.info("Strange: Found waittime of zero seconds in HTML");
                    return;
                }
                /*
                 * Check how much time has passed during eventual captcha event before this function has been called and see how much time
                 * is left to wait.
                 */
                final int extraWaitSeconds = 1;
                int passedTime = (int) ((Time.systemIndependentCurrentJVMTimeMillis() - timeBefore) / 1000) - extraWaitSeconds;
                wait -= passedTime;
                if (passedTime > 0) {
                    /* This usually means that the user had to solve a captcha which cuts down the remaining time we have to wait. */
                    logger.info("Total passed time during captcha: " + passedTime);
                }
                if (wait > 0) {
                    logger.info("Waiting final waittime: " + wait);
                    sleep(wait * 1000l, link);
                } else if (wait < wait - extraWaitSeconds) {
                    /* User needed more time to solve the captcha so there is no waittime left :) */
                    logger.info("Congratulations: Time to solve captcha was higher than waittime --> No waittime left");
                } else {
                    /* No waittime at all */
                    logger.info("Found no waittime");
                }
            } else {
                /* No waittime at all */
                logger.info("Found no waittime");
            }
        }
    }

    /**
     * Fix filenames for HLS video downloads. </br>
     * Ignores HLS audio for now.
     */
    protected void fixFilenameHLSDownload(final DownloadLink link) {
        /* Either final filename from previous download attempt or filename found in HTML. */
        final String orgNameWithExt = link.getName();
        if (orgNameWithExt != null) {
            link.setFinalFileName(this.correctOrApplyFileNameExtension(orgNameWithExt, ".mp4"));
        }
    }

    protected String getFileNameFromConnection(final URLConnectionAdapter connection, final DownloadLink link) {
        String fileName = getFileNameFromDispositionHeader(connection);
        if (StringUtils.isEmpty(fileName)) {
            fileName = Plugin.getFileNameFromURL(connection.getURL());
            fileName = URLEncode.decodeURIComponent(fileName);
            /* Ignore typical meaningless names. */
            if (fileName != null && fileName.matches("(?i)video\\.(mp4|mkv)$")) {
                fileName = null;
            }
        }
        return fileName;
    }

    /**
     * This fixes filenames from all xfs modules: file hoster, audio/video streaming (including transcoded video), or blocked link checking
     * which is based on fuid.
     *
     * @version 0.4
     * @author raztoki
     */
    protected void fixFilename(final URLConnectionAdapter connection, final DownloadLink link) {
        /* TODO: Maybe make use of already given methods to e.g. extract filename without extension from String. */
        /* Previous (e.h. html) filename without extension */
        String orgName = null;
        /* Server filename with extension */
        String servName = null;
        /* Server filename without extension */
        String servExt = null;
        /* Either final filename from previous download attempt or filename found in HTML. */
        String orgNameExt = link.getFinalFileName();
        if (StringUtils.isEmpty(orgNameExt)) {
            orgNameExt = link.getName();
        }
        /* Extension of orgNameExt */
        String orgExt = null;
        if (!StringUtils.isEmpty(orgNameExt) && StringUtils.contains(orgNameExt, ".")) {
            orgExt = orgNameExt.substring(orgNameExt.lastIndexOf("."));
        }
        if (!StringUtils.isEmpty(orgExt)) {
            orgName = new Regex(orgNameExt, "^(.+)" + Pattern.quote(orgExt) + "$").getMatch(0);
        } else {
            /* No extension given */
            orgName = orgNameExt;
        }
        String servNameExt = connection != null ? getFileNameFromConnection(connection, link) : null;
        if (!StringUtils.isEmpty(servNameExt) && !StringUtils.contains(servNameExt, ".")) {
            /* Extension according to Content-Type header */
            final String mimeExt = connection != null ? getExtensionFromMimeType(connection.getContentType()) : null;
            if (mimeExt != null) {
                servNameExt = servNameExt + "." + mimeExt;
            }
        }
        if (!StringUtils.isEmpty(servNameExt) && StringUtils.contains(servNameExt, ".")) {
            servExt = servNameExt.substring(servNameExt.lastIndexOf("."));
            servName = new Regex(servNameExt, "(.+)" + Pattern.quote(servExt)).getMatch(0);
        } else {
            /* No extension available */
            servName = servNameExt;
        }
        final String FFN;
        if (StringUtils.equalsIgnoreCase(orgName, this.getFUIDFromURL(link))) {
            /* Current filename only consists of fuid --> Prefer full server filename */
            FFN = servNameExt;
            logger.info("fixFileName case 1: prefer servNameExt: orgName == fuid --> Use servNameExt");
        } else if (StringUtils.isEmpty(orgExt) && !StringUtils.isEmpty(servExt) && (StringUtils.containsIgnoreCase(servName, orgName) && !StringUtils.equalsIgnoreCase(servName, orgName))) {
            /*
             * When partial match of filename exists. eg cut off by quotation mark miss match, or orgNameExt has been abbreviated by hoster
             * --> Prefer server filename
             */
            FFN = servNameExt;
            logger.info("fixFileName case 2: prefer servNameExt: previous filename had no extension given && servName contains orgName while servName != orgName --> Use servNameExt");
        } else if (!StringUtils.isEmpty(orgExt) && !StringUtils.isEmpty(servExt) && !StringUtils.equalsIgnoreCase(orgExt, servExt)) {
            /*
             * Current filename has extension given but server filename has other extension --> Swap extensions, trust the name we have but
             * use extension from server
             */
            FFN = orgName + servExt;
            logger.info(String.format("fixFileName case 3: prefer orgName + servExt: Previous filename had no extension given && servName contains orgName while servName != orgName --> Use orgName + servExt | Old ext: %s | New ext: %s", orgExt, servExt));
        } else {
            FFN = orgNameExt;
            logger.info("fixFileName case 4: prefer orgNameExt");
        }
        logger.info("fixFileName: before=" + orgNameExt + "|after=" + FFN);
        link.setFinalFileName(FFN);
    }

    /** Returns unique id from inside URL - usually with this pattern: [a-z0-9]{12} */
    public String getFUIDFromURL(final DownloadLink link) {
        final URL_TYPE type = getURLType(link);
        return getFUID(link, type);
    }

    /**
     * In some cases, URL may contain filename which can be used as fallback e.g. 'https://host.tld/<fuid>/<filename>(\\.html)?'. </br>
     * Examples without '.html' ending: vipfile.cc, prefiles.com
     */
    public String getFilenameFromURL(final DownloadLink link) {
        try {
            String result = null;
            final String url_name_RegEx = "/[a-z0-9]{12}/(.*?)(?:\\.html)?$";
            /**
             * It's important that we check the contentURL too as we do alter pluginPatternMatcher in { @link
             * #correctDownloadLink(DownloadLink) }
             */
            final String contentURL = getPluginContentURL(link);
            if (contentURL != null) {
                result = new Regex(new URL(contentURL).getPath(), url_name_RegEx).getMatch(0);
            }
            if (result == null) {
                result = new Regex(new URL(link.getPluginPatternMatcher()).getPath(), url_name_RegEx).getMatch(0);
            }
            return result;
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    protected String getFallbackFilename(final DownloadLink link, final Browser br) {
        String filenameURL = this.getFilenameFromURL(link);
        if (filenameURL != null) {
            return URLEncode.decodeURIComponent(filenameURL);
        } else {
            if (this.isVideohoster_enforce_video_filename() || this.isVideohosterEmbedHTML(br)) {
                return this.getFUIDFromURL(link) + ".mp4";
            } else if (this.isImagehoster()) {
                return this.getFUIDFromURL(link) + ".jpg";
            } else {
                return this.getFUIDFromURL(link);
            }
        }
    }

    protected void handlePassword(final Form pwform, final DownloadLink link) throws PluginException {
        if (isPasswordProtectedHTML(this.br, pwform)) {
            link.setPasswordProtected(true);
            if (pwform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("URL is password protected");
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
                if (StringUtils.isEmpty(passCode)) {
                    logger.info("User has entered blank password, exiting handlePassword");
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Pre-Download Password not provided");
                }
            }
            logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
            pwform.put("password", Encoding.urlEncode(passCode));
            link.setDownloadPassword(passCode);
        } else {
            link.setPasswordProtected(false);
        }
    }

    /**
     * Checks for (-& handles) all kinds of errors e.g. wrong captcha, wrong downloadpassword, waittimes and server error-responsecodes such
     * as 403, 404 and 503. <br />
     * checkAll: If enabled, ,this will also check for wrong password, wrong captcha and 'Skipped countdown' errors. <br/>
     */
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (new Regex(html, "(?i)>\\s*Wrong password").matches()) {
                if (link.isPasswordProtected()) {
                    final String userEnteredPassword = link.getDownloadPassword();
                    /* handle password has failed in the past, additional try catching / resetting values */
                    logger.warning("Wrong password, the entered password \"" + userEnteredPassword + "\" is wrong, retrying...");
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    /*
                     * 2020-03-26: Extremely rare case: Either plugin failure or serverside failure e.g. URL is password protected but
                     * website does never ask for the password e.g. 2020-03-26: ddl.to. We cannot use link.getDownloadPassword() to check
                     * this because users can enter download passwords at any time no matter whether they're required/used or not.
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Got error 'wrong password' but website never prompted for one");
                }
            } else if (new Regex(html, "(?i)>\\s*Wrong captcha").matches()) {
                logger.warning("Wrong captcha (or wrong password as well)!");
                /*
                 * TODO: Find a way to avoid using a property for this or add the property in very plugin which overrides handleCaptcha e.g.
                 * subyshare.com. If a dev forgets to set this, it will cause invalid errormessages on wrong captcha!
                 */
                final boolean websiteDidAskForCaptcha = link.getBooleanProperty(PROPERTY_captcha_required, false);
                if (websiteDidAskForCaptcha) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    /* This should never happen. Either developer mistake or broken filehost website. */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server says 'wrong captcha' but never prompted for one");
                }
            } else if (new Regex(html, ">\\s*Skipped countdown\\s*<").matches()) {
                /* 2019-08-28: e.g. "<br><b class="err">Skipped countdown</b><br>" */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Fatal countdown error (countdown skipped)");
            }
        }
        /** Wait time reconnect handling */
        final String limitBasedOnNumberofFilesAndTime = new Regex(html, "(?i)>\\s*(You have reached the maximum limit \\d+ files in \\d+ hours)").getMatch(0);
        final String preciseWaittime = new Regex(html, "((You have reached the download(\\-| )limit|You have to wait)[^<>]+)").getMatch(0);
        if (preciseWaittime != null) {
            /* Reconnect waittime with given (exact) waittime usually either up to the minute or up to the second. */
            final String tmphrs = new Regex(preciseWaittime, "(?i)\\s*(\\d+)\\s*hours?").getMatch(0);
            final String tmpmin = new Regex(preciseWaittime, "(?i)\\s*(\\d+)\\s*minutes?").getMatch(0);
            final String tmpsec = new Regex(preciseWaittime, "(?i)\\s*(\\d+)\\s*seconds?").getMatch(0);
            final String tmpdays = new Regex(preciseWaittime, "(?i)\\s*(\\d+)\\s*days?").getMatch(0);
            int waittime;
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                /* This should not happen! This is an indicator of developer-failure! */
                logger.info("Waittime RegExes seem to be broken - using default waittime");
                waittime = 60 * 60 * 1000;
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) {
                    hours = Integer.parseInt(tmphrs);
                }
                if (tmpmin != null) {
                    minutes = Integer.parseInt(tmpmin);
                }
                if (tmpsec != null) {
                    seconds = Integer.parseInt(tmpsec);
                }
                if (tmpdays != null) {
                    days = Integer.parseInt(tmpdays);
                }
                waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            }
            logger.info("Detected reconnect waittime (milliseconds): " + waittime);
            /* Not enough wait time to reconnect -> Wait short and retry */
            if (waittime < 180000) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait until new downloads can be started", waittime);
            } else if (account != null) {
                /*
                 * 2020-04-17: Some hosts will have trafficlimit and e.g. only allow one file every X minutes so his errormessage might be
                 * confusing to some users. Now it should cover both cases at the same time.
                 */
                throw new AccountUnavailableException("Download limit reached or wait until next download can be started", waittime);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        } else if (limitBasedOnNumberofFilesAndTime != null) {
            /*
             * 2019-05-09: New: Seems like XFS owners can even limit by number of files inside specified timeframe. Example: hotlink.cc; 150
             * files per 24 hours
             */
            /* Typically '>You have reached the maximum limit 150 files in 24 hours' */
            ipBlockedOrAccountLimit(link, account, limitBasedOnNumberofFilesAndTime, 15 * 60 * 1000l);
        } else if (StringUtils.containsIgnoreCase(html, "You're using all download slots for IP")) {
            ipBlockedOrAccountLimit(link, account, "You're using all download slots for IP...", 5 * 60 * 1000l);
        } else if (StringUtils.containsIgnoreCase(html, "Error happened when generating Download Link")) {
            /* Rare issue */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Error happened when generating Download Link'", 10 * 60 * 1000l);
        }
        /** Error handling for premiumonly links */
        if (isPremiumOnly(br)) {
            String filesizelimit = new Regex(html, "You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                throw new AccountRequiredException("As free user you can download files up to " + filesizelimit + " only");
            } else {
                logger.info("Only downloadable via premium");
                throw new AccountRequiredException();
            }
        } else if (new Regex(html, "(?i)>\\s*Expired download session").matches()) {
            /* Rare error */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Expired download session'", 10 * 60 * 1000l);
        } else if (isServerUnderMaintenance(br)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server is under maintenance", 30 * 60 * 1000l);
        }
        /* Host-type specific errors */
        /* Videohoster */
        if (new Regex(html, "(?i)>\\s*Video is processing now").matches()) {
            /* E.g. '<div id="over_player_msg">Video is processing now. <br>Conversion stage: <span id='enc_pp'>...</span></div>' */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not (yet) downloadable: Video is still being encoded or broken", 10 * 60 * 1000l);
        }
        if (br.containsHTML("(?i)>\\s*Downloads disabled for this file")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Uploader has disabled downloads for this file");
        }
        /*
         * Errorhandling for accounts that are valid but cannot be used yet because the user has to add his mail to the account via website.
         * E.g. accounts which have been generated via balance/points of uploaders' accounts. This should be a rare case. In this case,
         * every request you do on the website will redirect to /?op=my_account along with an errormessage (sometimes).
         */
        if (account != null && (StringUtils.containsIgnoreCase(br.getURL(), "op=my_account") || StringUtils.containsIgnoreCase(br.getRedirectLocation(), "op=my_account"))) {
            /* Attempt to make this work language-independant: Rely only on URL and NOT html! */
            final String accountErrorMsg;
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                accountErrorMsg = String.format("Ergänze deine E-Mail Adresse unter %s/?op=my_account um diesen Account verwenden zu können!", this.getHost());
            } else {
                accountErrorMsg = String.format("Go to %s/?op=my_account and enter your e-mail in order to be able to use this account!", this.getHost());
            }
            throw new AccountUnavailableException(accountErrorMsg, 10 * 60 * 1000l);
        }
        checkResponseCodeErrors(br.getHttpConnection());
    }

    protected void checkErrorsLoginWebsite(final Browser br, final Account account) throws Exception {
    }

    /** Throws appropriate Exception depending on whether or not an account is given. */
    private void ipBlockedOrAccountLimit(final DownloadLink link, final Account account, final String errorMsg, final long waitMillis) throws PluginException {
        if (account != null) {
            throw new AccountUnavailableException(errorMsg, waitMillis);
        } else {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMsg, waitMillis);
        }
    }

    /** Use this during download handling instead of just throwing PluginException with LinkStatus ERROR_PLUGIN_DEFECT! */
    protected void checkErrorsLastResort(final Browser br, final Account account) throws PluginException {
        logger.info("Last resort errorhandling");
        if (account != null && br.getHttpConnection().getResponseCode() == 200 && !this.isLoggedin(br)) {
            /* TODO: Maybe add a better check e.g. access mainpage and check loggedin state */
            throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
        }
        String website_error = br.getRegex("class=\"err\"[^>]*?>([^<>]+)<").getMatch(0);
        if (website_error != null) {
            if (Encoding.isHtmlEntityCoded(website_error)) {
                website_error = Encoding.htmlDecode(website_error);
            }
            logger.info("Found website error: " + website_error);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, website_error, 5 * 60 * 1000l);
        }
        /* 2020-06-05 E.g. <div id="over_player_msg">File is awaiting for moderation</div> */
        String website_error_videoplayer = br.getRegex("id=\"over_player_msg\"[^>]*?>([^<>\"]+)<").getMatch(0);
        if (website_error_videoplayer != null) {
            if (Encoding.isHtmlEntityCoded(website_error_videoplayer)) {
                website_error = Encoding.htmlDecode(website_error_videoplayer);
            }
            logger.info("Found website videoplayer error: " + website_error_videoplayer);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, website_error_videoplayer, 5 * 60 * 1000l);
        }
        logger.warning("Unknown error happened");
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /** Handles all kinds of error-responsecodes! Same for API and website! */
    public void checkResponseCodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con != null) {
            final long responsecode = con.getResponseCode();
            if (responsecode == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (responsecode == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
            } else if (responsecode == 416) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
            } else if (responsecode == 500) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500", 5 * 60 * 1000l);
            } else if (responsecode == 503) {
                exception503ConnectionLimitReached();
            }
        }
    }

    private void exception503ConnectionLimitReached() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 connection limit reached", 15 * 60 * 1000l);
    }

    /**
     * Handles all kinds of errors which can happen if we get the final downloadlink but we get html code instead of the file we want to
     * download.
     */
    public void checkServerErrors(final Browser br, final DownloadLink link, final Account account) throws NumberFormatException, PluginException {
        final String html = getCorrectBR(br);
        if (new Regex(html, "^(No file|error_nofile)$").matches()) {
            /* Possibly dead file but it is supposed to be online so let's wait and retry! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'No file'", 30 * 60 * 1000l);
        } else if (new Regex(html, "^Wrong IP$").matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'Wrong IP'", 2 * 60 * 60 * 1000l);
        } else if (new Regex(html, "^Expired$").matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'Expired'", 2 * 60 * 60 * 1000l);
        } else if (new Regex(html, "(^File Not Found$|<h1>404 Not Found</h1>)").matches()) {
            /* most likely result of generated link that has expired -raztoki */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        }
    }

    protected boolean supports_lifetime_account() {
        return false;
    }

    @Deprecated
    /** TODO: Find out where this is used. Lifetime accounts are not part of usual XFS hosts! */
    protected boolean is_lifetime_account(final Browser br) {
        return br.getRegex("(?i)>\\s*Premium account expire\\s*</TD><TD><b>Lifetime</b>").matches();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (this.enableAccountApiOnlyMode()) {
            return this.fetchAccountInfoAPI(this.br, account);
        } else {
            return this.fetchAccountInfoWebsite(account);
        }
    }

    protected AccountInfo fetchAccountInfoWebsiteStorage(final Browser br, final Account account, final AccountInfo ai) throws Exception {
        final String space[] = new Regex(getCorrectBR(br), ">Used space:</td>.*?<td.*?b>([0-9\\.]+) ?(KB|MB|GB|TB)?</b>").getRow(0);
        if ((space != null && space.length != 0) && (space[0] != null && space[1] != null)) {
            /* free users it's provided by default */
            ai.setUsedSpace(space[0] + " " + space[1]);
        } else if ((space != null && space.length != 0) && space[0] != null) {
            /* premium users the Mb value isn't provided for some reason... */
            ai.setUsedSpace(space[0] + "Mb");
        }
        return ai;
    }

    protected AccountInfo fetchAccountInfoWebsiteTraffic(final Browser br, final Account account, final AccountInfo ai) throws Exception {
        /*
         * trafficleft is usually not given via API so we'll have to check for it via website. Also we do not trsut 'unlimited traffic' via
         * API yet.
         */
        String trafficLeftStr = regExTrafficLeft(br);
        /* Example non english: brupload.net */
        final boolean userHasUnlimitedTraffic = trafficLeftStr != null && trafficLeftStr.matches(".*?(nlimited|Ilimitado).*?");
        if (trafficLeftStr != null && !userHasUnlimitedTraffic && !trafficLeftStr.equalsIgnoreCase("Mb")) {
            trafficLeftStr = Encoding.htmlDecode(trafficLeftStr).trim();
            /* Need to set 0 traffic left, as getSize returns positive result, even when negative value supplied. */
            long trafficLeft = 0;
            if (trafficLeftStr.startsWith("-")) {
                /* Negative traffic value = User downloaded more than he is allowed to (rare case) --> No traffic left */
                trafficLeft = 0;
            } else {
                trafficLeft = SizeFormatter.getSize(trafficLeftStr);
            }
            /* 2019-02-19: Users can buy additional traffic packages: Example(s): subyshare.com */
            final String usableBandwidth = br.getRegex("Usable Bandwidth\\s*<span[^>]*>\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*/\\s*[0-9\\.]+\\s*[TGMKB]+\\s*<").getMatch(0);
            if (usableBandwidth != null) {
                trafficLeft = Math.max(trafficLeft, SizeFormatter.getSize(usableBandwidth));
            }
            ai.setTrafficLeft(trafficLeft);
        } else {
            ai.setUnlimitedTraffic();
        }
        return ai;
    }

    protected boolean trustAccountInfoAPI(final Browser br, Account account, AccountInfo ai) throws Exception {
        return true;
    }

    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        AccountInfo ai = null;
        loginWebsite(null, account, true);
        boolean apiSuccess = false;
        /*
         * Only access URL if we haven't accessed it before already. Some sites will redirect to their Account-Info page right after
         * logging-in or our login-function when it is verifying cookies and not performing a full login.
         */
        if (br.getURL() == null || !br.getURL().contains(getRelativeAccountInfoURL())) {
            getPage(this.getMainPage() + getRelativeAccountInfoURL());
        }
        {
            /*
             * 2019-07-11: apikey handling - prefer account info via API instead of website if allowed.
             */
            String apikey = null;
            try {
                /*
                 * 2019-08-13: Do not hand over corrected_br as source as correctBR() might remove important parts of the html and because
                 * XFS owners will usually not add html traps into the html of accounts (especially ) we can use the original unmodified
                 * html here.
                 */
                apikey = this.findAPIKey(this.br.cloneBrowser());
            } catch (InterruptedException e) {
                throw e;
            } catch (final Throwable e) {
                /*
                 * 2019-08-16: All kinds of errors may happen when trying to access the API. It is preferable if it works but we cannot rely
                 * on it working so we need that website fallback!
                 */
                logger.info("Failed to find apikey (with Exception) --> Continuing via website");
                logger.log(e);
            }
            if (apikey != null) {
                /*
                 * 2019-07-11: Use API even if 'supports_api()' is disabled because if it works it is a much quicker and more reliable way
                 * to get account information.
                 */
                logger.info("Found apikey --> Trying to get AccountInfo via API");
                /* Save apikey for later usage */
                synchronized (account) {
                    account.setProperty(PROPERTY_ACCOUNT_apikey, apikey);
                    try {
                        ai = this.fetchAccountInfoAPI(this.br.cloneBrowser(), account);
                        apiSuccess = trustAccountInfoAPI(br, account, ai);
                    } catch (final InterruptedException e) {
                        throw e;
                    } catch (final Throwable e) {
                        e.printStackTrace();
                        logger.warning("Failed to find accountinfo via API even though apikey is given; probably serverside API failure --> Fallback to website handling");
                    }
                }
            }
        }
        if (apiSuccess && (!ai.isUnlimitedTraffic() || ai.hasProperty(PROPERTY_ACCOUNT_INFO_TRUST_UNLIMITED_TRAFFIC))) {
            /* Trust API info */
            logger.info("Successfully found complete AccountInfo via API");
            /* API with trafficleft value is uncommon -> Make sure devs easily take note of this! */
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* Devs only */
                String accStatus;
                if (ai.getStatus() != null) {
                    accStatus = ai.getStatus();
                } else {
                    accStatus = account.getType().toString();
                }
                ai.setStatus("[API] " + accStatus);
            }
            return ai;
        } else if (ai == null) {
            logger.info("AccountInfo via API not possible -> Obtaining all AccountInfo from website");
            /*
             * apikey can also be used e.g. for mass-linkchecking. Make sure that we keep only a valid apikey otherwise other stuff may
             * break!
             */
            account.removeProperty(PROPERTY_ACCOUNT_apikey);
            /*
             * Do not remove the saved API domain because if a user e.g. adds an apikey without adding an account later on, it might still
             * be useful!
             */
            // this.getPluginConfig().removeProperty(PROPERTY_PLUGIN_api_domain_with_protocol);
            /* Use new AccountInfo object to use with account data from website. */
            ai = new AccountInfo();
        } else {
            logger.info("Found AccountInfo via API but trying to obtain trafficleft value from website (usually not given via API)");
        }
        ai = fetchAccountInfoWebsiteTraffic(br, account, ai);
        if (apiSuccess) {
            logger.info("Successfully found AccountInfo without trafficleft via API (fetched trafficleft via website)");
            return ai;
        }
        ai = fetchAccountInfoWebsiteStorage(br, account, ai);
        if (supports_lifetime_account() && is_lifetime_account(br)) {
            ai.setValidUntil(-1);
            setAccountLimitsByType(account, AccountType.LIFETIME);
        } else {
            final Long expire_milliseconds = fetchAccountInfoWebsiteExpireDate(br, account, ai);
            if (expire_milliseconds == null) {
                logger.info("Account is a FREE account as no expiredate has been found");
                ai.setValidUntil(-1);
                setAccountLimitsByType(account, AccountType.FREE);
            } else if (expire_milliseconds < 0) {
                logger.info("Premium expired --> Free account");
                ai.setValidUntil(-1);
                setAccountLimitsByType(account, AccountType.FREE);
            } else if (expire_milliseconds == Long.MAX_VALUE) {
                logger.info("Lifetime Premium account");
                ai.setValidUntil(-1);
                setAccountLimitsByType(account, AccountType.LIFETIME);
            } else {
                logger.info("Premium account");
                ai.setValidUntil(expire_milliseconds);
                setAccountLimitsByType(account, AccountType.PREMIUM);
            }
        }
        return ai;
    }

    protected Long fetchAccountInfoWebsiteExpireDate(Browser br, Account account, AccountInfo ai) throws Exception {
        /* 2019-07-11: It is not uncommon for XFS websites to display expire-dates even though the account is not premium anymore! */
        final AtomicBoolean isPreciseTimestampFlag = new AtomicBoolean(false);
        final Long expire_milliseconds_from_expiredate = findExpireTimestamp(account, br, isPreciseTimestampFlag);
        long expire_milliseconds_precise_to_the_second = -1;
        final String[] supports_precise_expire_date = (isPreciseTimestampFlag.get() && expire_milliseconds_from_expiredate != null) ? null : this.supportsPreciseExpireDate();
        if (supports_precise_expire_date != null && supports_precise_expire_date.length > 0) {
            /*
             * A more accurate expire time, down to the second. Usually shown on 'extend premium account' page. Case[0] e.g. 'flashbit.cc',
             * Case [1] e.g. takefile.link, example website which has no precise expiredate at all: anzfile.net
             */
            final List<String> paymentURLs;
            final String last_working_payment_url = this.getPluginConfig().getStringProperty("property_last_working_payment_url", null);
            if (StringUtils.isNotEmpty(last_working_payment_url)) {
                paymentURLs = new ArrayList<String>();
                logger.info("Found stored last_working_payment_url --> Trying this first in an attempt to save http requests: " + last_working_payment_url);
                paymentURLs.add(last_working_payment_url);
                /* Add all remaining URLs, start with the last working one */
                for (final String paymentURL : supports_precise_expire_date) {
                    if (!paymentURLs.contains(paymentURL)) {
                        paymentURLs.add(paymentURL);
                    }
                }
            } else {
                /* Add all possible payment URLs. */
                logger.info("last_working_payment_url is not available --> Going through all possible paymentURLs");
                paymentURLs = Arrays.asList(supports_precise_expire_date);
            }
            /* Go through possible paymentURLs in an attempt to find an exact expiredate if the account is premium. */
            for (final String paymentURL : paymentURLs) {
                if (StringUtils.isEmpty(paymentURL)) {
                    continue;
                } else {
                    try {
                        getPage(paymentURL);
                    } catch (final Throwable e) {
                        logger.log(e);
                        /* Skip failures due to timeout or bad http error-responses */
                        continue;
                    }
                }
                /* Find html snippet which should contain our expiredate. */
                final String expireSecond = findExpireDate(br);
                if (!StringUtils.isEmpty(expireSecond)) {
                    final String tmpYears = new Regex(expireSecond, "(\\d+)\\s+years?").getMatch(0);
                    final String tmpdays = new Regex(expireSecond, "(\\d+)\\s+days?").getMatch(0);
                    final String tmphrs = new Regex(expireSecond, "(\\d+)\\s+hours?").getMatch(0);
                    final String tmpmin = new Regex(expireSecond, "(\\d+)\\s+minutes?").getMatch(0);
                    final String tmpsec = new Regex(expireSecond, "(\\d+)\\s+seconds?").getMatch(0);
                    long years = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
                    if (!StringUtils.isEmpty(tmpYears)) {
                        years = Integer.parseInt(tmpYears);
                    }
                    if (!StringUtils.isEmpty(tmpdays)) {
                        days = Integer.parseInt(tmpdays);
                    }
                    if (!StringUtils.isEmpty(tmphrs)) {
                        hours = Integer.parseInt(tmphrs);
                    }
                    if (!StringUtils.isEmpty(tmpmin)) {
                        minutes = Integer.parseInt(tmpmin);
                    }
                    if (!StringUtils.isEmpty(tmpsec)) {
                        seconds = Integer.parseInt(tmpsec);
                    }
                    expire_milliseconds_precise_to_the_second = ((years * 86400000 * 365) + (days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000));
                }
                if (expire_milliseconds_precise_to_the_second > 0) {
                    /* Later we will decide whether we are going to use this value or not. */
                    logger.info("Successfully found precise expire-date via paymentURL: \"" + paymentURL + "\" : " + expireSecond);
                    this.getPluginConfig().setProperty("property_last_working_payment_url", paymentURL);
                    break;
                } else {
                    logger.info("Failed to find precise expire-date via paymentURL: \"" + paymentURL + "\"");
                }
            }
        }
        final long currentTime = br.getCurrentServerTime(System.currentTimeMillis());
        if (expire_milliseconds_precise_to_the_second > 0) {
            /* Add current time to parsed value */
            expire_milliseconds_precise_to_the_second += currentTime;
        }
        final long expire_milliseconds;
        if (isPreciseTimestampFlag.get() && expire_milliseconds_from_expiredate != null) {
            logger.info("Using precise expire-date");
            expire_milliseconds = expire_milliseconds_from_expiredate.longValue();
        } else if (expire_milliseconds_precise_to_the_second > 0) {
            logger.info("Using precise expire-date");
            expire_milliseconds = expire_milliseconds_precise_to_the_second;
        } else if (expire_milliseconds_from_expiredate != null) {
            logger.info("Using expire-date which is up to 24 hours precise");
            expire_milliseconds = expire_milliseconds_from_expiredate.longValue();
        } else {
            logger.info("Failed to find any useful expire-date at all");
            expire_milliseconds = -1;
        }
        if (expire_milliseconds < 0 || (expire_milliseconds - currentTime) <= 0) {
            /* If the premium account is expired or we cannot find an expire-date we'll simply accept it as a free account. */
            if (expire_milliseconds > 0) {
                return -expire_milliseconds;
            } else {
                return null;
            }
        } else {
            /* Expire date is in the future --> It is a premium account */
            return expire_milliseconds;
        }
    }

    protected Long findExpireTimestamp(final Account account, final Browser br, AtomicBoolean isPreciseTimestampFlag) throws Exception {
        String expireStr = new Regex(getCorrectBR(br), "(\\d{1,2} (January|February|March|April|May|June|July|August|September|October|November|December) \\d{4})").getMatch(0);
        if (expireStr != null) {
            /*
             * 2019-12-17: XFS premium accounts usually don't expire just before the next day. They will end to the same time of the day
             * when they were bought but website only displays it to the day which is why we set it to just before the next day to prevent
             * them from expiring too early in JD. XFS websites with API may provide more precise information on the expiredate (down to the
             * second).
             */
            expireStr += " 23:59:59";
            return TimeFormatter.getMilliSeconds(expireStr, "dd MMMM yyyy HH:mm:ss", Locale.ENGLISH);
        }
        expireStr = new Regex(getCorrectBR(br), ">\\s*Premium\\s*(?:account expire|until):\\s*</span>\\s*[^>]*>([\\d]+-[\\w{2}]+-[\\d]+\\s[\\d:]+)</").getMatch(0);
        if (expireStr != null) {
            /**
             * e.g. kenfiles.com
             *
             * <span class="profile-ud-label">Premium account expire:</span> <span class="profile-ud-value">2023-02-07 19:58:15</span>
             */
            final long ret = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            if (ret > 0) {
                isPreciseTimestampFlag.set(true);
                return ret;
            }
        }
        return -1l;
    }

    protected String findExpireDate(final Browser br) throws Exception {
        boolean allHTML = false;
        String preciseExpireHTML = new Regex(getCorrectBR(br), "<div[^>]*class=\"[^\"]*accexpire[^\"]*\"[^>]*>.*?</div>").getMatch(-1);
        if (preciseExpireHTML == null) {
            allHTML = true;
            preciseExpireHTML = getCorrectBR(br);
        }
        // pattern good enough for all html
        String expireSecond = new Regex(preciseExpireHTML, "(?:Premium(-| )Account expires?(?: in)?|Twoje premium wygaśnie za)\\s*:\\s*(?:</span>)?\\s*(?:</span>)?\\s*(?:<span>)?\\s*([a-zA-Z0-9, ]+)\\s*</").getMatch(-1);
        if (StringUtils.isEmpty(expireSecond)) {
            /* e.g. kenfiles.com */
            expireSecond = new Regex(preciseExpireHTML, Pattern.compile(">\\s*Your premium expires?\\s*:\\s*(\\d+ years?, )?(\\d+ days?, )?(\\d+ hours?, )?(\\d+ minutes?, )?\\d+ seconds\\s*<", Pattern.CASE_INSENSITIVE)).getMatch(-1);
        }
        if (StringUtils.isEmpty(expireSecond)) {
            /**
             * e.g. filejoker.com <div class="col-12">Premium Account expires in 209 days, 11 hours</div>
             */
            expireSecond = new Regex(preciseExpireHTML, Pattern.compile(">\\s*Premium Account expires in\\s*(\\d+ years?,?\\s*)?(\\d+ days?,?\\s*)?(\\d+ hours?,?\\s*)?(\\d+ minutes?,?\\s*)?(\\d+ seconds)?\\s*<", Pattern.CASE_INSENSITIVE)).getMatch(-1);
        }
        if (StringUtils.isEmpty(expireSecond) && !allHTML) {
            /*
             * Last attempt - wider RegEx but we expect the 'second(s)' value to always be present!! Example: file-up.org:
             * "<p style="direction: ltr; display: inline-block;">1 year, 352 days, 22 hours, 36 minutes, 45 seconds</p>"
             */
            expireSecond = new Regex(preciseExpireHTML, Pattern.compile(">\\s*(\\d+ years?, )?(\\d+ days?, )?(\\d+ hours?, )?(\\d+ minutes?, )?\\d+ seconds\\s*<", Pattern.CASE_INSENSITIVE)).getMatch(-1);
        }
        if (StringUtils.isEmpty(expireSecond) && !StringUtils.isEmpty(preciseExpireHTML)) {
            /*
             * 2019-09-07: This html-class may also be given for non-premium accounts e.g. fileup.cc
             */
            logger.info("html contains 'accexpire' class but we failed to find a precise expiredate --> Either we have a free account or failed to find precise expiredate although it is given");
        }
        return expireSecond;
    }

    /**
     * Tries to find apikey on website which, if given, usually camn be found on /?op=my_account Example host which has 'API mod'
     * installed:</br>
     * This will also try to get- and save the API host with protocol in case it differs from the plugins' main host (examples:
     * ddownload.co, vup.to). clicknupload.org </br>
     * apikey will usually be located here: "/?op=my_account"
     */
    protected String findAPIKey(final Browser brc) throws Exception {
        /*
         * 2019-07-11: apikey handling - prefer that instead of website. Even if an XFS website has the "API mod" enabled, we will only find
         * a key here if the user at least once pressed the "Generate API Key" button or if the XFS 'api mod' used by the website admin is
         * configured to display apikeys by default for all users.
         */
        String apikey = regexAPIKey(brc);
        String generateApikeyUrl = this.regexGenerateAPIKeyURL(brc);
        /*
         * 2019-07-28: If no apikey has ever been generated by the user but generate_apikey_url != null we can generate the first apikey
         * automatically.
         */
        if (StringUtils.isEmpty(apikey) && generateApikeyUrl != null && allowToGenerateAPIKeyInWebsiteMode()) {
            if (Encoding.isHtmlEntityCoded(generateApikeyUrl)) {
                /*
                 * 2019-07-28: Some hosts have "&&amp;" inside URL (= buggy) - also some XFS hosts will only allow apikey generation once
                 * and when pressing "change key" afterwards, it will always be the same. This may also be a serverside XFS bug.
                 */
                generateApikeyUrl = Encoding.htmlDecode(generateApikeyUrl);
            }
            logger.info("Failed to find apikey but host has api-mod enabled --> Trying to generate first apikey for this account via: " + generateApikeyUrl);
            try {
                brc.setFollowRedirects(true);
                getPage(brc, generateApikeyUrl);
                apikey = regexAPIKey(brc);
                if (apikey == null) {
                    /*
                     * 2019-10-01: Some hosts will not display an APIKey immediately e.g. vup.to 'New API key generated. Please wait 1-2
                     * minutes while the key is being generated and refresh the page afterwards.'. This should not be an issue for us as the
                     * APIKey will be detected upon next account-check.
                     */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find generated apikey - possible plugin failure");
                } else {
                    logger.info("Successfully found newly generated apikey: " + apikey);
                }
            } catch (final Throwable e) {
                logger.exception("Exception occured during accessing generateApikeyUrl", e);
            }
        }
        if (apikey != null) {
            findAPIHost(brc, apikey);
        }
        return apikey;
    }

    /** Finds API host. Call this before attempting to use a previously found apikey in website mode! */
    protected void findAPIHost(final Browser brc, final String apikey) {
        if (apikey == null) {
            return;
        }
        logger.info("Found apikey! Trying to find api domain with protocol");
        String url_with_apikey = brc.getRegex("(https?://[^/]+/api/account/info[^<>\"\\']*key=" + apikey + "[^<>\"\\']*)").getMatch(0);
        boolean api_uses_special_domain = false;
        if (url_with_apikey == null) {
            logger.info("Unable to find API domain - assuming it is the same es the plugins'");
        } else {
            try {
                if (Encoding.isHtmlEntityCoded(url_with_apikey)) {
                    /*
                     * 2019-07-28: Some hosts have "&&amp;" inside URL (= buggy) - also some XFS hosts will only allow apikey generation
                     * once and when pressing "change key" afterwards, it will always be the same. This may also be a serverside XFS bug.
                     */
                    url_with_apikey = Encoding.htmlDecode(url_with_apikey);
                }
                final URL apiurl = new URL(url_with_apikey);
                final String apihost = Browser.getHost(apiurl, true);
                if (!apihost.equalsIgnoreCase(this.getHost())) {
                    logger.info(String.format("API domain is %s while main domain of plugin is %s", apihost, this.getHost()));
                    api_uses_special_domain = true;
                    final String test = apiurl.getProtocol() + "://" + apiurl.getHost() + "/api";
                    this.getPluginConfig().setProperty(PROPERTY_PLUGIN_api_domain_with_protocol, test);
                } else {
                    logger.info("API domain and main domain are the same: " + this.getHost());
                }
            } catch (final Throwable e) {
                logger.exception("Error while trying to find API domain", e);
            }
        }
        if (!api_uses_special_domain) {
            /* Important: Dump old data - maybe apihost was different and is now the same! */
            this.getPluginConfig().removeProperty(PROPERTY_PLUGIN_api_domain_with_protocol);
        }
    }

    protected String regexAPIKey(final Browser br) {
        return br.getRegex("/api/account/info\\?key=([a-z0-9]+)").getMatch(0);
    }

    protected String regexGenerateAPIKeyURL(final Browser br) {
        return br.getRegex("\"([^\"]*?op=my_account[^\"]*?generate_api_key=1[^\"]*?token=[a-f0-9]{32}[^\"]*?)\"").getMatch(0);
    }

    protected void setAccountLimitsByType(final Account account, final AccountType type) {
        account.setType(type);
        switch (type) {
        case LIFETIME:
        case PREMIUM:
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(this.getMaxSimultanPremiumDownloadNum());
            break;
        case FREE:
            account.setConcurrentUsePossible(false);
            account.setMaxSimultanDownloads(getMaxSimultaneousFreeAccountDownloads());
            break;
        case UNKNOWN:
        default:
            account.setConcurrentUsePossible(false);
            account.setMaxSimultanDownloads(1);
            break;
        }
    }

    public Form findLoginform(final Browser br) {
        Form loginform = br.getFormbyProperty("name", "FL");
        if (loginform == null) {
            /* More complicated way to find loginform ... */
            final Form[] allForms = br.getForms();
            for (final Form aForm : allForms) {
                final InputField inputFieldOP = aForm.getInputFieldByName("op");
                if (inputFieldOP != null && "login".equalsIgnoreCase(inputFieldOP.getValue())) {
                    loginform = aForm;
                    break;
                }
            }
        }
        if (loginform != null) {
            final InputField redirect = loginform.getInputFieldByName("redirect");
            if (redirect != null && StringUtils.isNotEmpty(redirect.getValue())) {
                try {
                    final String value = URLDecoder.decode(redirect.getValue(), "UTF-8");
                    if (value != null && canHandle(value)) {
                        /*
                         * Prevent redirect value redirecting to file-download straight away which would lead to an exception/download
                         * failure.
                         */
                        logger.info("clear login redirect to download:" + value);
                        redirect.setValue("");
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
            }
        }
        return loginform;
    }

    /** Returns Form required to click on 'continue to image' for image-hosts. */
    public Form findImageForm(final Browser br) {
        final Form imghost_next_form = br.getFormbyKey("next");
        if (imghost_next_form != null && imghost_next_form.hasInputFieldByName("method_premium")) {
            imghost_next_form.remove("method_premium");
        }
        return imghost_next_form;
    }

    /** Tries to find available traffic-left value inside html code. */
    protected String regExTrafficLeft(final Browser br) {
        /* 2020-30-09: progressbar with tooltip */
        final String src = this.getCorrectBR(br);
        String availabletraffic = new Regex(src, "Traffic available(?:\\s*today)?\\s*[^<>]*:?(?:<[^>]*>)?</TD>\\s*<TD[^>]*>\\s*<div[^>]*title\\s*=\\s*\"\\s*([^<>\"']+)\\s*available").getMatch(0);
        if (StringUtils.isEmpty(availabletraffic)) {
            /* Traffic can also be negative! */
            availabletraffic = new Regex(src, "Traffic available(?:\\s*today)?\\s*[^<>]*:?(?:<[^>]*>)?</TD>\\s*<TD[^>]*>\\s*(?:<b[^>]*>)?\\s*([^<>\"']+)").getMatch(0);
            if (StringUtils.isEmpty(availabletraffic)) {
                /* 2019-02-11: For newer XFS versions */
                availabletraffic = new Regex(src, ">\\s*Traffic available(?:\\s*today)?\\s*</div>\\s*<div class=\"txt\\d+\">\\s*([^<>\"]+)\\s*<").getMatch(0);
            }
            if (StringUtils.isEmpty(availabletraffic)) {
                // wrzucajpliki.pl
                // <span>Traffic available</span><div class="price"><sup>MB</sup>102400</div>
                final String trafficLeft = new Regex(src, ">\\s*Traffic available(?:\\s*today)?\\s*</[^>]*>\\s*<div class=\"(?:txt\\d+|price)\">\\s*(.*?)\\s*</div").getMatch(0);
                final String unit = new Regex(trafficLeft, "<sup>\\s*([TGMKB]+)\\s*</sup>").getMatch(0);
                final String left = new Regex(trafficLeft, "</sup>\\s*([\\-\\s*]*[0-9\\.]+)").getMatch(0);
                if (unit != null && left != null) {
                    availabletraffic = left + " " + unit;
                }
            }
        }
        if (StringUtils.isEmpty(availabletraffic)) {
            /* filejoker.net */
            final String formGroup = new Regex(src, ">\\s*Traffic available(?:\\s*today)?\\s*:?\\s*</[^>]*>(.*?)<div\\s*class\\s*=\\s*\"form-group").getMatch(0);
            String trafficDetails[] = new Regex(formGroup, "title\\s*=\\s*\"\\s*([0-9\\.]+\\s*[TGMB]+\\s*)/\\s*([0-9\\.]+\\s*[TGMB]+\\s*)\"").getRow(0);
            if (trafficDetails != null) {
                /**
                 * kenfiles.com
                 *
                 * >Traffic available
                 * today</span><span><a href="https://kenfiles.com/contact" title="671Mb/50000Mb" data-toggle="tooltip">49329 Mb</a></span>
                 */
                final long used = SizeFormatter.getSize(trafficDetails[0]);
                final long max = SizeFormatter.getSize(trafficDetails[1]);
                if (used > 0 && max > 0) {
                    return (max - used) + "b";
                }
            }
            /**
             * filejoker.net
             *
             * >Traffic Available:</label> <div class="col-12 col-md-8 col-lg"> <div class="progress">
             * <div class="progress-bar progress-bar-striped bg-success" role="progressbar" style="width:47.95%" aria-valuenow="47.95"
             * aria-valuemin="0" aria-valuemax="100" title="47951 MB available">47.95%</div>
             */
            availabletraffic = new Regex(formGroup, "title\\s*=\\s*\"\\s*([\\-\\s*]*[0-9\\.]+\\s*[TGMB]+\\s*)(?:available)?\"").getMatch(0);
        }
        if (StringUtils.isNotEmpty(availabletraffic)) {
            return availabletraffic;
        } else {
            return null;
        }
    }

    public boolean isLoggedin(final Browser brc) {
        /**
         * Please use valid combinations only! login or email alone without xfss is NOT valid!
         */
        final String mainpage = getMainPage(brc);
        logger.info("Doing login-cookiecheck for: " + mainpage);
        final String cookieXFSS = brc.getCookie(mainpage, "xfss", Cookies.NOTDELETEDPATTERN);
        final String cookieXFSTS = brc.getCookie(mainpage, "xfsts", Cookies.NOTDELETEDPATTERN);
        final boolean login_xfss_CookieOkay = StringUtils.isAllNotEmpty(brc.getCookie(mainpage, "login", Cookies.NOTDELETEDPATTERN), cookieXFSS);
        /* xfsts cookie is mostly used in xvideosharing sites (videohosters) example: vidoza.net */
        final boolean login_xfsts_CookieOkay = StringUtils.isAllNotEmpty(brc.getCookie(mainpage, "login", Cookies.NOTDELETEDPATTERN), cookieXFSTS);
        /* 2019-06-21: Example website which uses rare email cookie: filefox.cc (so far the only known!) */
        final boolean email_xfss_CookieOkay = StringUtils.isAllNotEmpty(brc.getCookie(mainpage, "email", Cookies.NOTDELETEDPATTERN), cookieXFSS);
        final boolean email_xfsts_CookieOkay = StringUtils.isAllNotEmpty(brc.getCookie(mainpage, "email", Cookies.NOTDELETEDPATTERN), cookieXFSTS);
        /* buttons or sites that are only available for logged in users */
        // remove script tags
        // remove comments, eg ddl.to just comment some buttons/links for expired cookies/non logged in
        final String htmlWithoutScriptTagsAndComments = brc.toString().replaceAll("(?s)(<script.*?</script>)", "").replaceAll("(?s)(<!--.*?-->)", "");
        final String ahrefPattern = "<a[^<]*href\\s*=\\s*\"[^\"]*";
        /**
         * Test cases </br>
         * op=logout: ddownload.com </br>
         * /(user_)?logout\": ?? </br>
         * logout\\.html: fastclick.to <br>
         * /logout/: crockdown.com:
         *
         *
         */
        final boolean logout = new Regex(htmlWithoutScriptTagsAndComments, ahrefPattern + "(&|\\?)op=logout").matches() || new Regex(htmlWithoutScriptTagsAndComments, ahrefPattern + "/(user_)?logout/?\"").matches() || new Regex(htmlWithoutScriptTagsAndComments, ahrefPattern + "/logout\\.html\"").matches();
        final boolean login = new Regex(htmlWithoutScriptTagsAndComments, ahrefPattern + "(&|\\?)op=login").matches() || new Regex(htmlWithoutScriptTagsAndComments, ahrefPattern + "/(user_)?login/?\"").matches() || new Regex(htmlWithoutScriptTagsAndComments, ahrefPattern + "/login\\.html\"").matches();
        // unsafe, not every site does redirect
        final boolean loginURLFailed = brc.getURL().contains("op=") && brc.getURL().contains("op=login");
        /*
         * 2019-11-11: Set myAccountOkay to true if there is currently a redirect which means in this situation we rely on our cookie ONLY.
         * This may be the case if a user has direct downloads enabled. We access downloadurl --> Redirect happens --> We check for login
         */
        final boolean isRedirect = brc.getRedirectLocation() != null;
        final boolean myAccountOkay = new Regex(htmlWithoutScriptTagsAndComments, ahrefPattern + "(&|\\?)op=my_account").matches() || new Regex(htmlWithoutScriptTagsAndComments, ahrefPattern + "/my(-|_)account\"").matches() || new Regex(htmlWithoutScriptTagsAndComments, ahrefPattern + "/account/?\"").matches();
        logger.info("login_xfss_CookieOkay:" + login_xfss_CookieOkay);
        logger.info("login_xfsts_CookieOkay:" + login_xfsts_CookieOkay);
        logger.info("email_xfss_CookieOkay:" + email_xfss_CookieOkay);
        logger.info("email_xfsts_CookieOkay:" + email_xfsts_CookieOkay);
        logger.info("logout_exists:" + logout);
        logger.info("login_exists:" + login);
        logger.info("myaccount_exists:" + myAccountOkay);
        logger.info("redirect:" + isRedirect);
        logger.info("loginURLFailed:" + loginURLFailed);
        final boolean ret = (login_xfss_CookieOkay || email_xfss_CookieOkay || login_xfsts_CookieOkay || email_xfsts_CookieOkay) && ((logout || (myAccountOkay && !login) || isRedirect) && !loginURLFailed);
        logger.info("loggedin:" + ret);
        return ret;
    }

    /** Returns the full URL to the page which should contain the loginForm. */
    public String getLoginURL() {
        return getMainPage() + "/login.html";
    }

    /**
     * Returns the relative URL to the page which should contain all account information (account type, expiredate, apikey, remaining
     * traffic).
     */
    protected String getRelativeAccountInfoURL() {
        return "/?op=my_account";
    }

    protected boolean containsInvalidLoginsMessage(final Browser br) {
        return br != null && br.containsHTML("(?i)>\\s*Incorrect Login or Password\\s*<");
    }

    protected boolean containsBlockedIPLoginMessage(final Browser br) {
        return br != null && (br.containsHTML("(?i)>\\s*You can't login from this IP") || br.containsHTML("(?i)>\\s*Your IP is banned\\s*<"));
    }

    protected void fillWebsiteLoginForm(Browser br, Form loginform, Account account) {
        {
            final String user = Encoding.urlEncode(account.getUser());
            InputField userField = null;
            final String userFieldNames[] = new String[] { "login", "email" };
            for (String userFieldName : userFieldNames) {
                userField = loginform.getInputFieldByName(userFieldName);
                if (userField != null) {
                    break;
                }
            }
            if (userField != null) {
                userField.setValue(user);
            } else {
                loginform.put(userFieldNames[0], user);
            }
        }
        {
            final String password = Encoding.urlEncode(account.getPass());
            InputField passwordField = null;
            final String passwordFieldNames[] = new String[] { "password", "pass" };
            for (String passwordFieldName : passwordFieldNames) {
                passwordField = loginform.getInputFieldByName(passwordFieldName);
                if (passwordField != null) {
                    break;
                }
            }
            if (passwordField != null) {
                passwordField.setValue(password);
            } else {
                loginform.put(passwordFieldNames[0], password);
            }
        }
    }

    /**
     * @param validateCookies
     *            true = Check whether stored cookies are still valid, if not, perform full login <br/>
     *            false = Set stored cookies and trust them if they're not older than 300000l
     *
     */
    public boolean loginWebsite(final DownloadLink link, final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            final boolean followRedirects = br.isFollowingRedirects();
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies == null && this.requiresCookieLogin()) {
                    /**
                     * Cookie login required but user did not put cookies into the password field: </br>
                     * Ask user to login via exported browser cookies e.g. xubster.com.
                     */
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                if (userCookies != null) {
                    br.setCookies(getMainPage(), userCookies);
                    if (!validateCookies) {
                        /* Trust cookies without check */
                        return false;
                    }
                    if (this.verifyCookies(account, userCookies)) {
                        /**
                         * If user enters cookies to login he can enter whatever he wants into the "username" field but we want unique
                         * usernames --> Try to find real username of added account and set it.
                         */
                        String cookiesUsername = br.getCookie(br.getHost(), "login", Cookies.NOTDELETEDPATTERN);
                        if (StringUtils.isEmpty(cookiesUsername)) {
                            cookiesUsername = br.getCookie(br.getHost(), "email", Cookies.NOTDELETEDPATTERN);
                        }
                        if (!StringUtils.isEmpty(cookiesUsername)) {
                            cookiesUsername = Encoding.htmlDecode(cookiesUsername).trim();
                        }
                        /**
                         * During cookie login, user can enter whatever he wants into username field.</br>
                         * Most users will enter their real username but to be sure to have unique usernames we don't trust them and try to
                         * get the real username out of our cookies.
                         */
                        if (StringUtils.isEmpty(cookiesUsername)) {
                            /* Not a major problem but worth logging. */
                            logger.warning("Failed to find username via cookie");
                        } else {
                            logger.info("Found username by cookie: " + cookiesUsername);
                            if (!account.getUser().equals(cookiesUsername)) {
                                logger.info("Setting new username by cookie | New: " + cookiesUsername + " | Old: " + account.getUser());
                                account.setUser(cookiesUsername);
                            }
                        }
                        return true;
                    } else {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                } else if (cookies != null) {
                    br.setCookies(getMainPage(), cookies);
                    if (!validateCookies) {
                        /* Trust cookies without check */
                        return false;
                    }
                    if (this.verifyCookies(account, cookies)) {
                        account.saveCookies(br.getCookies(getMainPage()), "");
                        return true;
                    }
                }
                logger.info("Full login required");
                /*
                 * 2019-08-20: Some hosts (rare case) will fail on the first attempt even with correct logindata and then demand a captcha.
                 * Example: filejoker.net
                 */
                int login_counter = 1;
                final int maxLoginAttempts = 3;
                br.clearCookies(getMainPage());
                // getPage(getMainPage()); //loginForm most likely never included on mainPage and thus we open getLoginURL first
                boolean userSolvedAtLeastOneLoginCaptcha = false;
                do {
                    logger.info("Performing full website login attempt: " + login_counter + "/" + maxLoginAttempts + " | Multiple attempts will only happen if a captcha is required");
                    Form loginForm = findLoginform(this.br);
                    if (loginForm == null) {
                        // some sites (eg filejoker) show login captcha AFTER first login attempt, so only reload getLoginURL(without
                        // captcha) if required
                        getPage(getLoginURL());
                        if (br.getHttpConnection().getResponseCode() == 404) {
                            /* Required for some XFS setups - use as common fallback. */
                            getPage(getMainPage() + "/login");
                        }
                        loginForm = findLoginform(this.br);
                        if (loginForm == null) {
                            logger.warning("Failed to find loginform");
                            /* E.g. 503 error during login */
                            this.checkErrorsLoginWebsite(br, account);
                            checkResponseCodeErrors(br.getHttpConnection());
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                    fillWebsiteLoginForm(br, loginForm, account);
                    /* Handle login-captcha if required */
                    final int captchasBefore = getChallenges().size();
                    handleCaptcha(new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true), br, loginForm);
                    final int captchasAfter = getChallenges().size();
                    final boolean captchaRequiredInThisRun = captchasAfter > captchasBefore;
                    if (captchaRequiredInThisRun) {
                        userSolvedAtLeastOneLoginCaptcha = true;
                    } else if (login_counter > 1) {
                        /*
                         * After first login attempt(login_counter >=1), there must either be a login captcha (= initial captcha or first
                         * captcha attempt was wrong) or the logins are wrong.
                         */
                        logger.info("Logins seem to be invalid because no login captcha required on login attempt: " + login_counter);
                        break;
                    }
                    submitForm(loginForm);
                    final boolean captchaRequiredInNextRun = this.containsCaptcha(this.findLoginform(br));
                    if (!captchaRequiredInNextRun) {
                        logger.info("Ending login loop because: No captcha required in next run --> No more attempts needed");
                        break;
                    } else if (userSolvedAtLeastOneLoginCaptcha && (containsInvalidLoginsMessage(br) || containsBlockedIPLoginMessage(br))) {
                        logger.info("Logins seem to be invalid because: There has been a login captcha but server response indicates invalid logins on login attempt: " + login_counter);
                        break;
                    }
                    login_counter++;
                } while (!this.isLoggedin(this.br) && login_counter <= maxLoginAttempts);
                if (!this.isLoggedin(this.br)) {
                    logger.info("Login failed after attempts: " + login_counter);
                    if (getCorrectBR(br).contains("op=resend_activation")) {
                        /* User entered correct logindata but hasn't activated his account yet. */
                        throw new AccountUnavailableException("\r\nYour account has not yet been activated!\r\nActivate it via the URL you received via E-Mail and try again!", 5 * 60 * 1000l);
                    } else if (containsInvalidLoginsMessage(br)) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new AccountInvalidException("\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.");
                        } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new AccountInvalidException("\r\nNieprawidłowa nazwa użytkownika / hasło!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.");
                        } else {
                            throw new AccountInvalidException("\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.");
                        }
                    } else if (containsBlockedIPLoginMessage(br)) {
                        throw new AccountInvalidException("\r\nYou can't login from this IP!\r\n");
                    } else if (containsCaptcha(this.findLoginform(br))) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new AccountInvalidException("\r\nUngültiges Login captcha!\r\nVersuche es erneut.");
                        } else {
                            throw new AccountInvalidException("\r\nInvalid login captcha answer!\r\nTry again.");
                        }
                    } else {
                        // unknown reason, containsInvalidLoginsMessage did not match
                        throw new AccountInvalidException();
                    }
                }
                account.saveCookies(br.getCookies(getMainPage()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            } finally {
                br.setFollowRedirects(followRedirects);
            }
        }
    }

    /** Sets given cookies and checks if we can login with them. */
    protected boolean verifyCookies(final Account account, final Cookies cookies) throws Exception {
        br.setCookies(getMainPage(), cookies);
        getPage(getMainPage() + getRelativeAccountInfoURL());
        if (isLoggedin(this.br)) {
            logger.info("Successfully logged in via cookies");
            return true;
        } else {
            logger.info("Cookie login failed");
            br.clearCookies(br.getHost());
            return false;
        }
    }

    protected boolean containsCaptcha(final Form form) {
        return form != null && containsCaptcha(form.getHtmlCode());
    }

    protected boolean containsCaptcha(final Browser br) {
        return br != null && containsCaptcha(br.getRequest().getHtmlCode());
    }

    protected boolean containsCaptcha(final String str) {
        if (str == null) {
            return false;
        } else if (this.containsSolvemediaCaptcha(str) || containsHCaptcha(str) || containsRecaptchaV2Class(str) || containsPlainTextCaptcha(str)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 2019-05-29: This is only EXPERIMENTAL! App-login: https://play.google.com/store/apps/details?id=net.sibsoft.xfsuploader <br/>
     * Around 2016 this has been implemented for some XFS websites but was never really used.It will return an XML response. Fragments of it
     * may still work for some XFS websites e.g. official DEMO website 'xfilesharing.com' and also 'europeup.com'. The login-cookie we get
     * is valid for the normal website as well! Biggest downside: Whenever a login-captcha is required (e.g. on too many wrong logins), this
     * method will NOT work!! <br/>
     * It seems like all or most of all XFS websites support this way of logging-in - even websites which were never officially supported
     * via XFS app (e.g. fileup.cc).
     */
    @Deprecated
    protected final boolean loginAPP(final Account account, boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                br.setHeader("User-Agent", "XFS-Mobile");
                br.setHeader("Content-Type", "application/x-www-form-urlencoded");
                /* Load cookies */
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                boolean validatedLoginCookies = false;
                /* 2019-08-29: Cookies will become invalid very soon so let's always verify them! */
                validateCookies = true;
                if (cookies != null) {
                    br.setCookies(getMainPage(), cookies);
                    if (!validateCookies) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        return false;
                    }
                    logger.info("Verifying login-cookies");
                    getPage(getMainPage() + "/");
                    /* Missing login cookies? --> Login failed */
                    validatedLoginCookies = StringUtils.isEmpty(br.getCookie(getMainPage(), "xfss", Cookies.NOTDELETEDPATTERN));
                }
                if (validatedLoginCookies) {
                    /* No additional check required --> We know cookies are valid and we're logged in --> Done! */
                    logger.info("Successfully logged in via cookies");
                } else {
                    logger.info("Performing full login");
                    br.clearCookies(getMainPage());
                    final Form loginform = new Form();
                    loginform.setMethod(MethodType.POST);
                    loginform.setAction(getMainPage());
                    loginform.put("op", "api_get_limits");
                    loginform.put("login", Encoding.urlEncode(account.getUser()));
                    loginform.put("password", Encoding.urlEncode(account.getPass()));
                    submitForm(loginform);
                    /*
                     * Returns XML: ExtAllowed, ExtNotAllowed, MaxUploadFilesize, ServerURL[for uploads], SessionID[our login cookie],
                     * Error, SiteName, LoginLogic
                     */
                    /* Missing login cookies? --> Login failed */
                    if (StringUtils.isEmpty(br.getCookie(getMainPage(), "xfss", Cookies.NOTDELETEDPATTERN))) {
                        if (getCorrectBR(br).contains("op=resend_activation")) {
                            /* User entered correct logindata but has not activated his account ... */
                            throw new AccountInvalidException("\r\nYour account has not yet been activated!\r\nActivate it via the URL you should have received via E-Mail and try again!");
                        } else {
                            throw new AccountInvalidException();
                        }
                    }
                }
                // /* Returns ballance, space, days(?premium days remaining?) - this call is not supported by all XFS sites - in this case
                // it'll return 404. */
                // final Form statsform = new Form();
                // statsform.setMethod(MethodType.POST);
                // statsform.setAction(getMainPage() + "/cgi-bin/uapi.cgi");
                // statsform.put("op", "api_get_stat");
                // submitForm(statsform);
                // final String spaceUsed = br.getRegex("<space>(\\d+\\.\\d+GB)</space>").getMatch(0);
                // final String balance = br.getRegex("<ballance>\\$(\\d+)</ballance>").getMatch(0);
                // // final String days = br.getRegex("<days>(\\d+)</days>").getMatch(0);
                // if (spaceUsed != null) {
                // account.getAccountInfo().setUsedSpace(SizeFormatter.getSize(spaceUsed));
                // }
                // if (balance != null) {
                // account.getAccountInfo().setAccountBalance(balance);
                // }
                account.saveCookies(br.getCookies(getMainPage()), "");
                validatedLoginCookies = true;
                return validatedLoginCookies;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    protected boolean isAccountLoginVerificationEnabled(final Account account, final boolean verifiedLogin) {
        return !verifiedLogin;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.resolveShortURL(this.br.cloneBrowser(), link, account);
        if (this.attemptStoredDownloadurlDownload(link, account)) {
            try {
                if (dl.getConnection() != null) {
                    fixFilename(dl.getConnection(), link);
                } else {
                    fixFilenameHLSDownload(link);
                }
            } catch (final Exception ignore) {
                logger.log(ignore);
            }
            logger.info("Using stored directurl");
            try {
                /* add a download slot */
                controlMaxFreeDownloads(account, link, +1);
                /* start the dl */
                dl.startDownload();
            } finally {
                /* remove download slot */
                controlMaxFreeDownloads(account, link, -1);
            }
            return;
        }
        if (this.enableAccountApiOnlyMode()) {
            /* API mode */
            handleDownload(link, account, null, this.getDllinkAPI(link, account), null);
        } else {
            /* Website mode (this will still prefer API whenever possible) */
            final String contentURL = this.getContentURL(link);
            if (AccountType.FREE.equals(account.getType())) {
                /*
                 * Perform linkcheck without logging in. TODO: Remove this and check for offline later as this would save one http request.
                 */
                requestFileInformationWebsite(link, account, true);
                br.setFollowRedirects(false);
                final boolean verifiedLogin = loginWebsite(link, account, false);
                /* Access main Content-URL */
                this.getPage(contentURL);
                if (isAccountLoginVerificationEnabled(account, verifiedLogin) && !isLoggedin(this.br)) {
                    loginWebsite(link, account, true);
                    getPage(contentURL);
                }
                if (!AccountType.FREE.equals(account.getType())) {
                    // account is no longer free, retry handlePremium handling
                    setBrowser(createNewBrowserInstance());
                    handlePremium(link, account);
                } else {
                    doFree(link, account);
                }
                return;
            } else {
                /* First API --> This will also do linkcheck but only require one http request */
                String dllink = null;
                try {
                    dllink = this.getDllinkAPI(link, account);
                } catch (final Throwable e) {
                    /* Do not allow exception to happen --> Fallback to website instead */
                    logger.log(e);
                    logger.warning("Error in API download handling");
                }
                /* API failed/not supported? Try website! */
                String officialVideoDownloadURL = null;
                final DownloadMode mode = this.getPreferredDownloadModeFromConfig();
                if (StringUtils.isEmpty(dllink)) {
                    /* TODO: Maybe skip this, check for offline later */
                    requestFileInformationWebsite(link, account, true);
                    br.setFollowRedirects(false);
                    final boolean verifiedLogin = loginWebsite(link, account, false);
                    getPage(contentURL);
                    if (isAccountLoginVerificationEnabled(account, verifiedLogin) && !isLoggedin(this.br)) {
                        loginWebsite(link, account, true);
                        getPage(contentURL);
                    }
                    /*
                     * Check for final downloadurl here because if user/host has direct downloads enabled, PluginPatternMatcher will
                     * redirect to our final downloadurl thus isLoggedin might return false although we are loggedin!
                     */
                    /*
                     * Official video download is sometimes only available via account (example: xvideosharing.com)!
                     */
                    officialVideoDownloadURL = getDllinkViaOfficialVideoDownload(this.br.cloneBrowser(), link, account, false);
                    dllink = getDllink(link, account, br, getCorrectBR(br));
                    if (StringUtils.isEmpty(dllink) && (mode == DownloadMode.STREAM || StringUtils.isEmpty(officialVideoDownloadURL))) {
                        final Form dlForm = findFormDownload2Premium(link, account, this.br);
                        if (dlForm == null) {
                            checkErrors(br, getCorrectBR(br), link, account, true);
                            logger.warning("Failed to find Form download2");
                            checkErrorsLastResort(br, account);
                        }
                        handlePassword(dlForm, link);
                        final URLConnectionAdapter formCon = openAntiDDoSRequestConnection(br, br.createFormRequest(dlForm));
                        if (looksLikeDownloadableContent(formCon)) {
                            /* Very rare case - e.g. tiny-files.com */
                            handleDownload(link, account, null, dllink, formCon.getRequest());
                            return;
                        } else {
                            br.followConnection(true);
                            runPostRequestTask(br);
                            this.correctBR(br);
                        }
                        checkErrors(br, getCorrectBR(br), link, account, true);
                        officialVideoDownloadURL = getDllinkViaOfficialVideoDownload(this.br.cloneBrowser(), link, account, false);
                        dllink = getDllink(link, account, br, getCorrectBR(br));
                    }
                }
                handleDownload(link, account, officialVideoDownloadURL, dllink, null);
            }
        }
    }

    /**
     * rewrite/fix wrong protocol from http to https if required
     *
     * @param link
     * @param account
     * @param br
     * @param dllink
     * @return
     * @throws Exception
     */
    protected String fixProtocol(final DownloadLink link, final Account account, final Browser br, final String dllink) throws Exception {
        if (dllink != null && !StringUtils.startsWithCaseInsensitive(dllink, "rtmp")) {
            final URL url = br.getURL(dllink);
            if (url.getPort() != -1 && StringUtils.equalsIgnoreCase(url.getProtocol(), "http")) {
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setAllowedResponseCodes(400);
                    brc.getPage(url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/");
                    if (brc.getHttpConnection().getResponseCode() == 400 && brc.containsHTML("The plain HTTP request was sent to HTTPS port")) {
                        final String ret = url.toString().replaceFirst("(?i)^(http://)", "https://");
                        logger.info("fixProtocol downloadlink = " + dllink + "->" + ret);
                        return ret;
                    }
                } catch (final IOException e) {
                    logger.log(e);
                }
            }
        }
        return dllink;
    }

    protected void handleDownload(final DownloadLink link, final Account account, final String directurl, final Request req) throws Exception {
        handleDownload(link, account, null, directurl, req);
    }

    protected void handleDownload(final DownloadLink link, final Account account, final String officialVideoDownloadURL, final String directurl, final Request req) throws Exception {
        final DownloadMode mode = this.getPreferredDownloadModeFromConfig();
        String finalDownloadlink;
        if (!StringUtils.isEmpty(officialVideoDownloadURL) && (mode == null || mode == DownloadMode.ORIGINAL || StringUtils.isEmpty(directurl))) {
            /* Official video download */
            finalDownloadlink = officialVideoDownloadURL;
        } else {
            /* Fallback / File download / Stream download */
            finalDownloadlink = directurl;
        }
        if (Encoding.isHtmlEntityCoded(finalDownloadlink)) {
            finalDownloadlink = Encoding.htmlOnlyDecode(finalDownloadlink);
        }
        if (req != null && req.getHttpConnection() != null) {
            req.getHttpConnection().disconnect();
        }
        final boolean resume = this.isResumeable(link, account);
        int maxChunks = getMaxChunks(account);
        if (maxChunks > 1) {
            logger.info("@Developer: fixme! maxChunks may not be fixed positive:" + maxChunks);
            maxChunks = -maxChunks;
        }
        if (!resume) {
            if (maxChunks != 1) {
                logger.info("@Developer: fixme! no resume allowed but maxChunks is not 1:" + maxChunks);
                maxChunks = 1;
            }
        }
        if (req != null) {
            logger.info("Final downloadlink = Form download");
            /*
             * Save directurl before download-attempt as it should be valid even if it e.g. fails because of server issue 503 (= too many
             * connections) --> Should work fine after the next try.
             */
            final String location = req.getLocation();
            // TODO: add fixProtocol support
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, req, resume, maxChunks);
            if (location != null) {
                /* E.g. redirect to downloadurl --> We can save that URL */
                storeDirecturl(link, account, location);
            }
            handleDownloadErrors(dl.getConnection(), link, account);
            try {
                fixFilename(dl.getConnection(), link);
            } catch (final Exception ignore) {
                logger.log(ignore);
            }
            try {
                /* add a download slot */
                controlMaxFreeDownloads(account, link, +1);
                /* start the dl */
                dl.startDownload();
            } finally {
                /* remove download slot */
                controlMaxFreeDownloads(account, link, -1);
            }
        } else {
            if (StringUtils.isEmpty(finalDownloadlink) || (!finalDownloadlink.startsWith("http") && !finalDownloadlink.startsWith("rtmp") && !finalDownloadlink.startsWith("/"))) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                checkErrors(br, getCorrectBR(br), link, account, true);
                checkErrorsLastResort(br, account);
            }
            finalDownloadlink = fixProtocol(link, account, br, finalDownloadlink);
            logger.info("Final downloadlink = " + finalDownloadlink + " starting the download...");
            if (finalDownloadlink.startsWith("rtmp")) {
                /* 2022-01-27: rtmp is not supported anymore */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming protocol rtmp");
            } else if (finalDownloadlink.contains(".m3u8")) {
                /* 2019-08-29: HLS download - more and more streaming-hosts use this */
                finalDownloadlink = handleQualitySelectionHLS(this.br.cloneBrowser(), finalDownloadlink);
                checkFFmpeg(link, "Download a HLS Stream");
                dl = new HLSDownloader(link, br, finalDownloadlink);
                try {
                    fixFilenameHLSDownload(link);
                } catch (final Exception e) {
                    logger.log(e);
                }
                try {
                    /* add a download slot */
                    controlMaxFreeDownloads(account, link, +1);
                    /* start the dl */
                    dl.startDownload();
                } finally {
                    /* remove download slot */
                    controlMaxFreeDownloads(account, link, -1);
                }
            } else {
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finalDownloadlink, resume, maxChunks);
                /*
                 * Save directurl before download-attempt as it should be valid even if it e.g. fails because of server issue 503 (= too
                 * many connections) --> Should work fine after the next try.
                 */
                storeDirecturl(link, account, dl.getConnection().getURL().toString());
                handleDownloadErrors(dl.getConnection(), link, account);
                try {
                    fixFilename(dl.getConnection(), link);
                } catch (final Exception e) {
                    logger.log(e);
                }
                try {
                    /* add a download slot */
                    controlMaxFreeDownloads(account, link, +1);
                    /* start the dl */
                    dl.startDownload();
                } finally {
                    /* remove download slot */
                    controlMaxFreeDownloads(account, link, -1);
                }
            }
        }
    }

    /** Returns user selected streaming quality. Returns BEST by default / no selection. */
    protected String handleQualitySelectionHLS(final Browser br, final String hlsMaster) throws Exception {
        if (StringUtils.isEmpty(hlsMaster)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.getPage(br, hlsMaster);
        final List<HlsContainer> hlsQualities = HlsContainer.getHlsQualities(br);
        if (hlsQualities == null || hlsQualities.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "HLS stream broken?");
        }
        HlsContainer hlsSelected = null;
        final int userSelectedQuality = getPreferredStreamQuality();
        if (userSelectedQuality == -1) {
            logger.info("Looking for BEST video stream");
            hlsSelected = HlsContainer.findBestVideoByBandwidth(hlsQualities);
        } else {
            logger.info("Looking for user selected video stream quality: " + userSelectedQuality);
            for (final HlsContainer hlsQualityTmp : hlsQualities) {
                final int height = hlsQualityTmp.getHeight();
                if (height == userSelectedQuality) {
                    logger.info("Successfully found selected quality: " + userSelectedQuality);
                    hlsSelected = hlsQualityTmp;
                    break;
                }
            }
            if (hlsSelected == null) {
                logger.info("Failed to find user selected quality --> Returning BEST instead");
                hlsSelected = HlsContainer.findBestVideoByBandwidth(hlsQualities);
            }
        }
        logger.info(String.format("Picked stream quality = %sp", hlsSelected.getHeight()));
        return hlsSelected.getDownloadurl();
    }

    /** Stores final downloadurl on current DownloadLink object */
    protected void storeDirecturl(final DownloadLink link, final Account account, final String directurl) {
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        link.setProperty(directlinkproperty, directurl);
    }

    /** Handles errors right before starting the download. */
    protected void handleDownloadErrors(final URLConnectionAdapter con, final DownloadLink link, final Account account) throws Exception {
        if (!looksLikeDownloadableContent(con)) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            correctBR(br);
            checkResponseCodeErrors(con);
            checkServerErrors(br, link, account);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadlink did not lead to downloadable content");
        } else {
            try {
                checkResponseCodeErrors(con);
            } catch (final PluginException e) {
                try {
                    br.followConnection(true);
                } catch (IOException ioe) {
                    throw Exceptions.addSuppressed(e, ioe);
                }
                throw e;
            }
        }
    }

    /* *************************** PUT API RELATED METHODS HERE *************************** */
    protected String getAPIBase() {
        final String custom_apidomain = this.getPluginConfig().getStringProperty(PROPERTY_PLUGIN_api_domain_with_protocol);
        if (custom_apidomain != null) {
            return custom_apidomain;
        } else {
            return getMainPage() + "/api";
        }
    }

    /** If enabled this plugin will attempt to generate an API key if no API key was found and it looks like the website supports that. */
    protected boolean allowToGenerateAPIKeyInWebsiteMode() {
        return true;
    }

    /** Generates final downloadurl via API if API usage is allowed and apikey is available. */
    /*
     * TODO: check/add support for URL_TYPE.FILE
     */
    protected String getDllinkAPI(final DownloadLink link, final Account account) throws Exception {
        /**
         * Only execute this if you know that the currently used host supports this! </br>
         * Only execute this if an apikey is given! </br>
         * Only execude this if you know that a particular host has enabled this API call! </br>
         * Important: For some hosts, this API call will only be available for premium accounts, no for free accounts!
         */
        if (this.enableAccountApiOnlyMode() || this.allowAPIDownloadIfApikeyIsAvailable(link, account)) {
            /* 2019-11-04: Linkcheck is not required here - download API will return offline status. */
            // requestFileInformationAPI(link, account);
            logger.info("Trying to get dllink via API");
            final String apikey = getAPIKeyFromAccount(account);
            if (StringUtils.isEmpty(apikey)) {
                /* This should never happen */
                logger.warning("Cannot do this without apikey");
                return null;
            }
            final String fileid_to_download;
            if (requiresAPIGetdllinkCloneWorkaround(account)) {
                logger.info("Trying to download file via clone workaround");
                getPage(this.getAPIBase() + "/file/clone?key=" + apikey + "&file_code=" + this.getFUIDFromURL(link));
                this.checkErrorsAPI(this.br, link, account);
                fileid_to_download = PluginJSonUtils.getJson(br, "filecode");
                if (StringUtils.isEmpty(fileid_to_download)) {
                    logger.warning("Failed to find new fileid in clone handling");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                logger.info("Trying to download file via api without workaround");
                fileid_to_download = this.getFUIDFromURL(link);
            }
            /*
             * Users can also chose a preferred quality via '&q=h' but we prefer to receive all and then chose to easily have a fallback in
             * case the quality selected by our user is not available.
             */
            /* Documentation videohost: https://xfilesharingpro.docs.apiary.io/#reference/file/file-clone/get-direct-link */
            /*
             * Documentation filehost:
             * https://xvideosharing.docs.apiary.io/#reference/file/file-direct-link/get-links-to-all-available-qualities
             */
            getPage(this.getAPIBase() + "/file/direct_link?key=" + apikey + "&file_code=" + fileid_to_download);
            this.checkErrorsAPI(this.br, link, account);
            final Map<String, Object> entries = restoreFromString(this.br.toString(), TypeRef.MAP);
            final Map<String, Object> result = (Map<String, Object>) entries.get("result");
            /**
             * TODO: Add quality selection. 2020-05-20: Did not add selection yet because so far this API call has NEVER worked for ANY
             * filehost&videohost!
             */
            /* For videohosts: Pick the best quality */
            String dllink = null;
            final String[] qualities = new String[] { "o", "h", "n", "l" };
            for (final String quality : qualities) {
                final Map<String, Object> quality_tmp = (Map<String, Object>) result.get(quality);
                if (quality_tmp != null) {
                    dllink = (String) quality_tmp.get("url");
                    if (!StringUtils.isEmpty(dllink)) {
                        break;
                    }
                }
            }
            if (StringUtils.isEmpty(dllink)) {
                /* For filehosts (= no different qualities available) */
                logger.info("Failed to find any quality - downloading original file");
                dllink = (String) result.get("url");
                // final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            }
            if (!StringUtils.isEmpty(dllink)) {
                logger.info("Successfully found dllink via API");
                return dllink;
            } else {
                logger.warning("Failed to find dllink via API");
                this.checkErrorsAPI(br, link, account);
                /**
                 * TODO: Check if defect message makes sense here. Once we got better errorhandling we can eventually replace this with a
                 * waittime.
                 */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return null;
    }

    /**
     * Advantages over website: <br/>
     * - Always precise expire-date <br/>
     * - All info we need via one single http request <br/>
     * - Consistent
     */
    protected AccountInfo fetchAccountInfoAPI(final Browser br, final Account account) throws Exception {
        /*
         * 2020-03-20: TODO: Check if more XFS sites include 'traffic_left' and 'premium_traffic_left' here and implement it. See Plugins
         * ShareOnlineTo and DdlTo
         */
        final AccountInfo ai = new AccountInfo();
        loginAPI(br, account);
        Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        /** 2019-07-31: Better compare expire-date against their serverside time if possible! */
        final String server_timeStr = (String) entries.get("server_time");
        entries = (Map<String, Object>) entries.get("result");
        long expire_milliseconds_precise_to_the_second = 0;
        final String email = (String) entries.get("email");
        final long currentTime;
        if (server_timeStr != null && server_timeStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            currentTime = TimeFormatter.getMilliSeconds(server_timeStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        } else {
            /* Fallback */
            currentTime = System.currentTimeMillis();
        }
        String expireStr = (String) entries.get("premium_expire");
        if (StringUtils.isEmpty(expireStr)) {
            /*
             * 2019-05-30: Seems to be a typo by the guy who develops the XFS script in the early versions of thei "API mod" :D 2019-07-28:
             * Typo is fixed in newer XFSv3 versions - still we'll keep both versions in just to make sure it will always work ...
             */
            expireStr = (String) entries.get("premim_expire");
        }
        /*
         * 2019-08-22: For newly created free accounts, an expire-date will always be given, even if the account has never been a premium
         * account. This expire-date will usually be the creation date of the account then --> Handling will correctly recognize it as a
         * free account!
         */
        if (expireStr != null && expireStr.matches("\\d{4}\\-\\d{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            expire_milliseconds_precise_to_the_second = TimeFormatter.getMilliSeconds(expireStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        /*
         * 2019-08-22: Sadly there is no "traffic_left" value given. Upper handling will try to find it via website. Because we access
         * account-info page anyways during account-check we at least don't have to waste another http-request for that.
         */
        ai.setUnlimitedTraffic();
        final long premiumDurationMilliseconds = expire_milliseconds_precise_to_the_second - currentTime;
        if (premiumDurationMilliseconds <= 0) {
            /* Expired premium or no expire date given --> It is usually a Free Account */
            setAccountLimitsByType(account, AccountType.FREE);
        } else {
            /* Expire date is in the future --> It is a premium account */
            ai.setValidUntil(System.currentTimeMillis() + premiumDurationMilliseconds);
            setAccountLimitsByType(account, AccountType.PREMIUM);
        }
        {
            /* Now set less relevant account information */
            final long balance = JavaScriptEngineFactory.toLong(entries.get("balance"), -1);
            /* 2019-07-26: values can also be "inf" for "Unlimited": "storage_left":"inf" */
            // final long storage_left = JavaScriptEngineFactory.toLong(entries.get("storage_left"), 0);
            final long storage_used_bytes = JavaScriptEngineFactory.toLong(entries.get("storage_used"), -1);
            if (storage_used_bytes > -1) {
                ai.setUsedSpace(storage_used_bytes);
            }
            if (balance > -1) {
                ai.setAccountBalance(balance);
            }
        }
        if (this.enableAccountApiOnlyMode() && !StringUtils.isEmpty(email)) {
            /*
             * Each account is unique. Do not care what the user entered - trust what API returns! </br> This is not really important - more
             * visually so that something that makes sense is displayed to the user in his account managers' "Username" column!
             */
            account.setUser(email);
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* Devs only */
            String accStatus;
            if (ai.getStatus() != null) {
                accStatus = ai.getStatus();
            } else {
                accStatus = account.getType().toString();
            }
            ai.setStatus("[API] " + accStatus);
        }
        return ai;
    }

    /**
     * More info see supports_api()
     */
    protected final void loginAPI(final Browser br, final Account account) throws Exception {
        synchronized (account) {
            final boolean followRedirects = br.isFollowingRedirects();
            try {
                br.setCookiesExclusive(true);
                final String apikey = this.getAPIKeyFromAccount(account);
                if (!this.isAPIKey(apikey)) {
                    throw new AccountInvalidException("Invalid apikey format!");
                }
                getPage(br, this.getAPIBase() + "/account/info?key=" + apikey);
                final String msg = PluginJSonUtils.getJson(br, "msg");
                final String status = PluginJSonUtils.getJson(br, "status");
                /* 2019-05-30: There are no cookies at all (only "__cfduid" [Cloudflare cookie] sometimes.) */
                final boolean jsonOK = msg != null && msg.equalsIgnoreCase("ok") && status != null && status.equals("200");
                if (!jsonOK) {
                    /* E.g. {"msg":"Wrong auth","server_time":"2019-05-29 19:29:03","status":403} */
                    throw new AccountInvalidException();
                }
            } finally {
                br.setFollowRedirects(followRedirects);
            }
        }
    }

    protected final AvailableStatus requestFileInformationAPI(final DownloadLink link, final String apikey) throws Exception {
        massLinkcheckerAPI(new DownloadLink[] { link }, apikey);
        if (link.getAvailableStatus() == AvailableStatus.FALSE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return link.getAvailableStatus();
    }

    /**
     * Checks multiple URLs via API. Only works when an apikey is given!
     */
    public boolean massLinkcheckerAPI(final DownloadLink[] urls, final String apikey) {
        if (urls == null || urls.length == 0 || !this.isAPIKey(apikey)) {
            return false;
        }
        boolean linkcheckerHasFailed = false;
        try {
            final Browser br = new Browser();
            this.prepBrowser(br, getMainPage());
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /*
                     * We test max 50 links at once. 2020-05-29: XFS default API linkcheck limit is exactly 50 items. If you check more than
                     * 50 items, it will only return results for the first 50 items.
                     */
                    if (index == urls.length || links.size() == 50) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                final ArrayList<DownloadLink> apiLinkcheckLinks = new ArrayList<DownloadLink>();
                sb.delete(0, sb.capacity());
                for (final DownloadLink link : links) {
                    try {
                        this.resolveShortURL(br.cloneBrowser(), link, null);
                    } catch (final PluginException e) {
                        logger.log(e);
                        if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                            link.setAvailableStatus(AvailableStatus.FALSE);
                        } else if (e.getLinkStatus() == LinkStatus.ERROR_IP_BLOCKED) {
                            link.setAvailableStatus(AvailableStatus.TRUE);
                        } else {
                            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
                        }
                        if (!link.isNameSet()) {
                            setWeakFilename(link, null);
                        }
                        /*
                         * We cannot check shortLinks via API so if we're unable to convert them to TYPE_NORMAL we basically already checked
                         * them here. Also we have to avoid sending wrong fileIDs to the API otherwise linkcheck WILL fail!
                         */
                        continue;
                    }
                    sb.append(this.getFUIDFromURL(link));
                    sb.append("%2C");
                    apiLinkcheckLinks.add(link);
                }
                if (apiLinkcheckLinks.isEmpty()) {
                    /* Rare edge-case */
                    logger.info("Seems like we got only shortURLs -> Nothing left to be checked via API");
                } else {
                    getPage(br, getAPIBase() + "/file/info?key=" + apikey + "&file_code=" + sb.toString());
                    try {
                        this.checkErrorsAPI(br, links.get(0), null);
                    } catch (final Throwable e) {
                        logger.log(e);
                        /* E.g. invalid apikey, broken serverside API, developer mistake (e.g. sent fileIDs in invalid format) */
                        logger.info("Fatal failure");
                        return false;
                    }
                    Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                    final List<Object> ressourcelist = (List<Object>) entries.get("result");
                    for (final DownloadLink link : apiLinkcheckLinks) {
                        Map<String, Object> fileInfo = null;
                        final String thisFUID = this.getFUIDFromURL(link);
                        for (final Object fileO : ressourcelist) {
                            final Map<String, Object> fileInfoTmp = (Map<String, Object>) fileO;
                            String fuid_temp = (String) fileInfoTmp.get("filecode");
                            if (StringUtils.isEmpty(fuid_temp)) {
                                /* 2022-08-09 */
                                fuid_temp = (String) fileInfoTmp.get("file_code");
                            }
                            if (StringUtils.equals(fuid_temp, thisFUID)) {
                                fileInfo = fileInfoTmp;
                                break;
                            }
                        }
                        if (fileInfo == null) {
                            /**
                             * This should never happen. Possible reasons: </br>
                             * - Wrong APIKey </br>
                             * - We tried to check too many items at once </br>
                             * - API only allows users to check self-uploaded content --> Disable API linkchecking in plugin! </br>
                             * - API does not not allow linkchecking at all --> Disable API linkchecking in plugin! </br>
                             */
                            logger.warning("WTF failed to find information for fuid: " + this.getFUIDFromURL(link));
                            linkcheckerHasFailed = true;
                            continue;
                        }
                        /* E.g. check for "result":[{"status":404,"filecode":"xxxxxxyyyyyy"}] */
                        final long status = JavaScriptEngineFactory.toLong(fileInfo.get("status"), 404);
                        if (!link.isNameSet()) {
                            setWeakFilename(link, null);
                        }
                        String filename = null;
                        boolean isVideohost = false;
                        if (status != 200) {
                            link.setAvailable(false);
                        } else {
                            link.setAvailable(true);
                            filename = (String) fileInfo.get("name");
                            if (StringUtils.isEmpty(filename)) {
                                filename = (String) fileInfo.get("file_title");
                            }
                            final long filesize = JavaScriptEngineFactory.toLong(fileInfo.get("size"), 0);
                            final Object canplay = fileInfo.get("canplay");
                            final Object views_started = fileInfo.get("views_started");
                            final Object views = fileInfo.get("views");
                            final Object length = fileInfo.get("length");
                            isVideohost = canplay != null || views_started != null || views != null || length != null;
                            /* Filesize is not always given especially not for videohosts. */
                            if (filesize > 0) {
                                link.setDownloadSize(filesize);
                            }
                        }
                        if (!isVideohost) {
                            isVideohost = this.internal_isVideohoster_enforce_video_filename(link);
                        }
                        if (!StringUtils.isEmpty(filename)) {
                            /*
                             * At least for videohosts, filenames from json would often not contain a file extension!
                             */
                            if (Encoding.isHtmlEntityCoded(filename)) {
                                filename = Encoding.htmlDecode(filename);
                            }
                            if (isVideohost) {
                                filename = this.correctOrApplyFileNameExtension(filename, ".mp4");
                            }
                            /* Trust API filenames -> Set as final filename. */
                            link.setFinalFileName(filename);
                        } else {
                            final String name = link.getName();
                            if (name != null && isVideohost) {
                                link.setName(this.correctOrApplyFileNameExtension(filename, ".mp4"));
                            }
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            return false;
        } finally {
            if (linkcheckerHasFailed) {
                logger.info("Seems like massLinkcheckerAPI availablecheck is not supported by this host");
                this.getPluginConfig().setProperty("MASS_LINKCHECKER_API_LAST_FAILURE_TIMESTAMP", System.currentTimeMillis());
            }
        }
        if (linkcheckerHasFailed) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Can be executed after API calls to check for- and handle errors. </br>
     * Example good API response: {"msg":"OK","server_time":"2020-05-25 13:09:37","status":200,"result":[{"...
     */
    protected void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws NumberFormatException, PluginException {
        /**
         * 2019-10-31: TODO: Add support for more errorcodes e.g. downloadlimit reached, premiumonly, password protected, wrong password,
         * wrong captcha. [PW protected + captcha protected download handling is not yet implemented serverside]
         */
        String errorCodeStr = null;
        String errorMsg = null;
        int statuscode = -1;
        try {
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            final Object statusO = entries.get("status");
            if (statusO instanceof String) {
                errorCodeStr = (String) statusO;
            } else {
                statuscode = ((Number) statusO).intValue();
            }
            errorMsg = (String) entries.get("msg");
        } catch (final Throwable e) {
            logger.log(e);
            logger.info("API json parsing error");
        }
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = "Unknown error";
        }
        /**
         * TODO: Maybe first check for errormessage based on text, then handle statuscode. </br>
         * One statuscode can be returned with different errormessages!
         */
        switch (statuscode) {
        case -1:
            /* No error */
            break;
        case 200:
            /* No error */
            break;
        case 400:
            /* {"msg":"Invalid key","server_time":"2019-10-31 17:20:02","status":400} */
            /* 2021-04-01: This can also happen: {"msg":"Invalid file codes","server_time":"2021-04-01 13:39:48","status":400} */
            /*
             * This should never happen!
             */
            throw new AccountInvalidException("Invalid apikey!\r\nEntered apikey does not match expected format.");
        case 403:
            if (errorMsg.equalsIgnoreCase("This function not allowed in API")) {
                /* {"msg":"This function not allowed in API","server_time":"2019-10-31 17:02:31","status":403} */
                /* This should never happen! Plugin needs to be */
                if (link == null) {
                    /*
                     * Login via API either not supported at all (wtf why is there an apikey available) or only for special/unlocked users!
                     */
                    throw new AccountInvalidException("API login impossible!");
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Unsupported API function - plugin might need update", 2 * 60 * 60 * 1000l);
                }
            } else {
                /* {"msg":"Wrong auth","server_time":"2019-10-31 16:54:05","status":403} */
                throw new AccountInvalidException("Invalid or expired apikey!\r\nWhen changing your apikey via website, make sure to update it in JD too!");
            }
        case 404:
            /* {"msg":"No file","server_time":"2019-10-31 17:23:17","status":404} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        default:
            /* Do not throw Exception here - usually website will be used as fallback and website-errors will be handled correctly */
            logger.info("Unknown API error: " + errorCodeStr);
            break;
        }
    }

    protected final String getAPIKeyFromAccount(final Account account) {
        synchronized (account) {
            final String apikey;
            if (this.enableAccountApiOnlyMode()) {
                /* In API only mode, apikey is stored in password field. */
                apikey = account.getPass();
            } else {
                /* In website mode we store apikey as a property on our current account object. */
                apikey = account.getStringProperty(PROPERTY_ACCOUNT_apikey);
            }
            if (isAPIKey(apikey)) {
                return apikey;
            } else {
                return null;
            }
        }
    }

    /** @return apikey but only if it is considered valid! */
    protected final String getAPIKeyFromConfig() {
        final Class<? extends XFSConfigVideo> cfgO = getVideoConfigInterface();
        if (cfgO == null) {
            return null;
        } else {
            final String apikey = PluginJsonConfig.get(cfgO).getApikey();
            if (this.isAPIKey(apikey)) {
                return apikey;
            } else {
                return null;
            }
        }
    }

    protected final DownloadMode getPreferredDownloadModeFromConfig() {
        final Class<? extends XFSConfigVideo> cfgO = getVideoConfigInterface();
        if (cfgO == null) {
            return DownloadMode.ORIGINAL;
        } else {
            return PluginJsonConfig.get(cfgO).getPreferredDownloadMode();
        }
    }

    /**
     * This will try to return an apikey, preferably from a valid account. </br>
     * Uses API key from config as fallback.
     */
    protected final String getAPIKey() {
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        if (acc != null && this.getAPIKeyFromAccount(acc) != null) {
            return this.getAPIKeyFromAccount(acc);
        } else {
            return this.getAPIKeyFromConfig();
        }
    }

    protected boolean isAPIKey(final String apiKey) {
        if (apiKey != null && apiKey.matches("^[a-z0-9]{16,}$")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        if (this.enableAccountApiOnlyMode()) {
            return new XFSApiAccountFactory(callback);
        } else {
            return new DefaultEditAccountPanel(callback, !getAccountwithoutUsername());
        }
    }

    public static class XFSApiAccountFactory extends MigPanel implements AccountBuilderInterface {
        private static final long serialVersionUID = 1L;
        private final String      PINHELP          = "Enter your API Key";

        private String getPassword() {
            if (this.pass == null) {
                return null;
            }
            if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
                return null;
            }
            return new String(this.pass.getPassword());
        }

        public boolean updateAccount(Account input, Account output) {
            boolean changed = false;
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                changed = true;
            }
            if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                changed = true;
            }
            return changed;
        }

        private final ExtPasswordField pass;
        private static String          EMPTYPW = " ";
        private final JLabel           idLabel;

        public XFSApiAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to find your API Key:"));
            add(new JLink("https://examplehost.com/?op=my_account"));
            this.add(this.idLabel = new JLabel("Enter your API Key:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(PINHELP);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                // name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            final String password = getPassword();
            if (password == null || !password.trim().matches("^[a-z0-9]{16,}$")) {
                idLabel.setForeground(Color.RED);
                return false;
            }
            idLabel.setForeground(Color.BLACK);
            return getPassword() != null;
        }

        @Override
        public Account getAccount() {
            return new Account(null, getPassword());
        }
    }

    /**
     * pseudo redirect control!
     */
    @Override
    protected void runPostRequestTask(final Browser ibr) throws Exception {
        final String redirect;
        if (!ibr.isFollowingRedirects() && (redirect = ibr.getRedirectLocation()) != null) {
            if (!this.isImagehoster()) {
                if (!isDllinkFile(redirect)) {
                    super.getPage(ibr, redirect);
                    return;
                }
            } else {
                super.getPage(ibr, redirect);
                return;
            }
        }
    }

    /**
     * Use this to set filename based on filename inside URL or fuid as filename either before a linkcheck happens so that there is a
     * readable filename displayed in the linkgrabber or also for mass-linkchecking as in this case these is no filename given inside HTML.
     */
    protected void setWeakFilename(final DownloadLink link, final Browser br) {
        final String weakFilename = this.getFallbackFilename(link, br);
        if (weakFilename != null) {
            link.setName(weakFilename);
        }
    }

    /** Returns empty StringArray for filename, filesize, filehash, [more information in the future?] */
    public final String[] internal_getFileInfoArray() {
        return new String[3];
    }

    /**
     * This can 'automatically' detect whether a host supports embedding videos. <br />
     * Example: uqload.com</br>
     * Do not override unless really needed!
     */
    protected final boolean internal_isVideohosterEmbed(final Browser br) {
        return isVideohosterEmbed() || isVideohosterEmbedHTML(br);
    }

    /**
     * Decides whether to enforce a filename with a '.mp4' ending or not. </br>
     * Names are either enforced if the configuration of the script implies this or if it detects that embedding videos is possible. </br>
     * Do not override - at least try to avoid having to!!
     */
    private final boolean internal_isVideohoster_enforce_video_filename(final DownloadLink link) {
        return internal_isVideohosterEmbed(this.br) || isVideohoster_enforce_video_filename() || isEmbedURL(link);
    }

    @Override
    public boolean internal_supportsMassLinkcheck() {
        return this.supportsAPIMassLinkcheck() || this.supportsMassLinkcheckOverWebsite() || this.enableAccountApiOnlyMode();
    }

    /**
     * Override this and let it return true whenever an user provided API key is available to allow the plugin to do single linkchecks via
     * API. </br>
     *
     * @default false
     */
    protected boolean supportsAPISingleLinkcheck() {
        // return isAPIKey(this.getAPIKey());
        /* On Override, you would typically use the above line of code as return value. */
        return false;
    }

    /** @default false */
    protected boolean supportsAPIMassLinkcheck() {
        // return isAPIKey(this.getAPIKey());
        /* On Override, you would typically use the above line of code as return value. */
        return false;
    }

    /**
     * This can 'automatically' detect whether a host supports availablecheck via 'abuse' URL. <br />
     * Example: uploadboy.com</br>
     * Do not override - at least try to avoid having to!!
     */
    protected boolean internal_supports_availablecheck_filename_abuse() {
        final boolean supportedByIndicatingHtmlCode = new Regex(getCorrectBR(br), "op=report_file&(?:amp;)?id=" + this.getFUIDFromURL(this.getDownloadLink())).matches();
        boolean allowedByAutoHandling = true;
        final SubConfiguration config = this.getPluginConfig();
        final long timestampLastFailure = config.getLongProperty(PROPERTY_PLUGIN_REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_TIMESTAMP, 0);
        final String last_version = config.getStringProperty(PROPERTY_PLUGIN_REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_VERSION, null);
        if (timestampLastFailure > 0 && StringUtils.equalsIgnoreCase(getPluginVersionHash(), last_version)) {
            final long timestampCooldown = timestampLastFailure + internal_waittime_on_alternative_availablecheck_failures();
            if (timestampCooldown > System.currentTimeMillis()) {
                logger.info("internal_supports_availablecheck_filename_abuse is still deactivated as it did not work on the last attempt");
                logger.info("Time until retry: " + TimeFormatter.formatMilliSeconds(timestampCooldown - System.currentTimeMillis(), 0));
                allowedByAutoHandling = false;
            }
        }
        return (this.supports_availablecheck_filename_abuse() || supportedByIndicatingHtmlCode) && allowedByAutoHandling;
    }

    protected boolean internal_supports_availablecheck_alt() {
        boolean allowedByAutoHandling = true;
        final SubConfiguration config = this.getPluginConfig();
        final long timestampLastFailure = config.getLongProperty(PROPERTY_PLUGIN_ALT_AVAILABLECHECK_LAST_FAILURE_TIMESTAMP, 0);
        final String last_version = config.getStringProperty(PROPERTY_PLUGIN_ALT_AVAILABLECHECK_LAST_FAILURE_VERSION, null);
        if (timestampLastFailure > 0 && StringUtils.equalsIgnoreCase(getPluginVersionHash(), last_version)) {
            final long timestampCooldown = timestampLastFailure + internal_waittime_on_alternative_availablecheck_failures();
            if (timestampCooldown > System.currentTimeMillis()) {
                logger.info("internal_supports_availablecheck_alt is still deactivated as it did not work on the last attempt");
                logger.info("Time until retry: " + TimeFormatter.formatMilliSeconds(timestampCooldown - System.currentTimeMillis(), 0));
                allowedByAutoHandling = false;
            }
        }
        return supports_availablecheck_alt() && allowedByAutoHandling;
    }

    /**
     * Defines the time to wait until a failed linkcheck method will be tried again. This should be set to > 24 hours as its purpose is to
     * minimize unnecessary http requests.
     */
    protected long internal_waittime_on_alternative_availablecheck_failures() {
        return 7 * 24 * 60 * 60 * 1000;
    }

    /**
     * Function to check whether or not a filehost is running XFS API mod or not. Only works for APIs running on their main domain and not
     * any other/special domain! </br>
     * Example test working & API available: https://fastfile.cc/api/account/info </br>
     * Example not working but API available: https://api-v2.ddownload.com/api/account/info </br>
     * Example API not available (= XFS API Mod not installed): </br>
     */
    private boolean test_looks_like_supports_api() throws IOException {
        br.getPage(this.getAPIBase() + "/account/info");
        /* 2020-05-29: Answer we'd expect if API is available: {"msg":"Invalid key","server_time":"2020-05-29 17:16:36","status":400} */
        final String msg = PluginJSonUtils.getJson(br, "msg");
        final String server_time = PluginJSonUtils.getJson(br, "server_time");
        if (!StringUtils.isEmpty(msg) && !StringUtils.isEmpty(server_time)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Class<? extends XFSConfig> getConfigInterface() {
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public Boolean verifyDownloadableContent(Set<LazyHostPlugin> plugins, final URLConnectionAdapter urlConnection) {
        if (plugins != null) {
            plugins.add(getLazyP());
        }
        if (urlConnection.getCompleteContentLength() == 7 && urlConnection.getContentType().matches("(?i)^.*application/octet-stream.*")) {
            // filejoker
            // HTTP/1.1 200 OK
            // Content-Type: application/octet-stream
            // Content-Length: 7
            // ETag: "48ae7c8c-7"
            // Response: Expired = 7 length
            return Boolean.FALSE;
        } else if (urlConnection.getCompleteContentLength() == 7 && urlConnection.getContentType().matches("(?i)^.*text/html.*")) {
            // normal
            // HTTP/1.1 200 OK
            // Content-Type: text/html
            // Content-Length: 7
            // Response: Expired = 7 length
            return Boolean.FALSE;
        } else if (urlConnection.isContentDisposition()) {
            // HTTP/1.1 200 OK
            // Content-Type: text/html; charset=UTF-8
            // Content-Disposition: inline; filename=error.html
            final String contentDispositionHeader = urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_DISPOSITION);
            final String contentDispositionFileName = HTTPConnectionUtils.getFileNameFromDispositionHeader(contentDispositionHeader);
            final boolean inlineFlag = contentDispositionHeader.matches("(?i)^\\s*inline\\s*;?.*");
            if (inlineFlag && (contentDispositionFileName != null && contentDispositionFileName.matches("(?i)^.*\\.html?$"))) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }
}