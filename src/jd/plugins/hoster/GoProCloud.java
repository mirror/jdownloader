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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.gopro.Download;
import jd.plugins.components.gopro.FlexiJSonNodeResponse;
import jd.plugins.components.gopro.GoProConfig;
import jd.plugins.components.gopro.GoProVariant;
import jd.plugins.components.gopro.Media;
import jd.plugins.components.gopro.Variation;
import jd.plugins.decrypter.GoProCloudDecrypter;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.TypeRef;
import org.appwork.storage.flexijson.FlexiJSONParser;
import org.appwork.storage.flexijson.FlexiJSonNode;
import org.appwork.storage.flexijson.FlexiParserException;
import org.appwork.storage.flexijson.mapper.FlexiJSonMapper;
import org.appwork.storage.flexijson.mapper.FlexiMapperException;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.host.PluginFinder;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gopro.com" }, urls = { GoProCloud.HTTPS_GOPRO_COM_DOWNLOAD_PREMIUM_FREE })
public class GoProCloud extends PluginForHost/* implements MenuExtenderHandler */{
    public static final String MEDIA                                 = "media";
    public static final String MEDIA_DOWNLOAD                        = "media/download";
    public static final String HTTPS_GOPRO_COM_DOWNLOAD_PREMIUM_FREE = "https?://gopro\\.com/download(?:premium|free)/([^/]+)/([^/]+)";
    public static final String ACCESS_TOKEN                          = "accessToken";

    public GoProCloud(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://gopro.com/login");
        // if (!org.appwork.utils.Application.isHeadless()) {
        // MenuManagerMainmenu.getInstance().registerExtender(this);
        // MenuManagerMainToolbar.getInstance().registerExtender(this);
        // }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(br, account);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(20);
        return ai;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return GoProConfig.class;
    }

    @Override
    public String getLinkID(DownloadLink link) {
        final String ret = link != null ? createLinkID(link, getActiveVariantByLink(link)) : null;
        if (ret != null) {
            return ret;
        } else {
            return super.getLinkID(link);
        }
    }

    public static String createLinkID(DownloadLink link, LinkVariant linkVariant) {
        final Regex reg = new Regex(link.getPluginPatternMatcher(), HTTPS_GOPRO_COM_DOWNLOAD_PREMIUM_FREE);
        if (reg.getMatch(0) != null) {
            if (link.hasVariantSupport() && !link.hasGenericVariantSupport()) {
                return "gopro.com" + "://" + reg.getMatch(0) + "/" + reg.getMatch(1) + "/" + linkVariant._getUniqueId();
            } else {
                return "gopro.com" + "://" + reg.getMatch(0) + "/" + reg.getMatch(1);
            }
        } else {
            return null;
        }
    }

    public String login(Browser br, Account account) throws IOException, PluginException {
        if (account == null) {
            throw new AccountRequiredException();
        } else if (account.getUser() == null || !account.getUser().matches(".+@.+")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your E-Mail in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        synchronized (account) {
            try {
                String accessToken = account.getStringProperty(ACCESS_TOKEN, null);
                if (StringUtils.isEmpty(accessToken)) {
                    br.clearAll();
                    br.getPage("https://gopro.com/login");
                    final String token = br.getRegex("<meta name=\"csrf-token\" content=([^>]+)").getMatch(0);
                    if (StringUtils.isEmpty(token)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Map<String, Object> loginData = new HashMap<String, Object>();
                    loginData.put("email", account.getUser());
                    loginData.put("password", account.getPass());
                    loginData.put("referrer", "");
                    loginData.put("two_factor", "");
                    loginData.put("brand", "");
                    loginData.put("scope", "username, email, me");
                    loginData.put("clientUserAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36");
                    Map<String, Object> response = null;
                    loginLoop: while (!isAbort()) {
                        final PostRequest loginRequest = br.createJSonPostRequest("https://gopro.com/login", loginData);
                        loginRequest.getHeaders().put("x-csrf-token", token);
                        br.getPage(loginRequest);
                        response = restoreFromString(br.toString(), TypeRef.MAP);
                        final List<Map<String, Object>> errors = (List<Map<String, Object>>) response.get("_errors");
                        if (errors != null && errors.size() > 0) {
                            for (final Map<String, Object> error : errors) {
                                final Number code = (Number) error.get("code");
                                switch (code.intValue()) {
                                case 401:
                                    // {"_errors":[{"code":401,"description":"Invalid username/password combination."}],"statusCode":401}
                                    throw new AccountInvalidException("Invalid username/password combination");
                                case 4014:
                                    // {"_errors":[{"code":4014,"id":"","description":"Invalid Two-Factor Authentication Code. Please try again or request a new code"}],"statusCode":401}
                                    logger.info("2FA code required");
                                    final Browser brc = br.cloneBrowser();
                                    brc.getPage("https://gopro.com/2fa-code?email=" + URLEncode.encodeURIComponent(account.getUser()));
                                    final Map<String, Object> twoFACodeRequestResponse = restoreFromString(brc.toString(), TypeRef.MAP);
                                    final Number statusCode = (Number) twoFACodeRequestResponse.get("statusCode");
                                    if (statusCode == null || statusCode.intValue() != 200) {
                                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                                    }
                                    final DownloadLink dl_dummy;
                                    if (this.getDownloadLink() != null) {
                                        dl_dummy = this.getDownloadLink();
                                    } else {
                                        dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                                    }
                                    String twoFACode = getUserInput("Enter GoPro 2-Factor SMS authentication code", dl_dummy);
                                    if (twoFACode != null) {
                                        twoFACode = twoFACode.trim();
                                    }
                                    if (twoFACode == null || !twoFACode.matches("\\d{6}")) {
                                        throw new AccountInvalidException("Invalid 2-factor-authentication code format (must be 6 digits) !");
                                    }
                                    loginData.put("two_factor", twoFACode);
                                    continue loginLoop;
                                default:
                                    break;
                                }
                            }
                            throw new AccountInvalidException();
                        }
                        break;
                    }
                    accessToken = response != null ? (String) response.get("access_token") : null;
                    if (StringUtils.isEmpty(accessToken)) {
                        throw new AccountInvalidException();
                    }
                }
                account.setProperty(ACCESS_TOKEN, accessToken);
                br.getHeaders().put(HTTPConstants.HEADER_REQUEST_AUTHORIZATION, "Bearer " + accessToken);
                return accessToken;
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty(ACCESS_TOKEN);
                }
                throw e;
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "https://gopro.com/login/";
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final boolean hasCache = hasDownloadCache(link);
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        final Variation variation = loadDownloadURL(link, account);
        if (variation == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        final URLConnectionAdapter connection = br.openHeadConnection(variation.getHead());
        try {
            if (!looksLikeDownloadableContent(connection)) {
                if (hasCache) {
                    clearDownloadCache(link);
                    return requestFileInformation(link);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            if (connection.getCompleteContentLength() > 0) {
                link.setDownloadSize(connection.getCompleteContentLength());
            }
        } finally {
            connection.disconnect();
        }
        return AvailableStatus.TRUE;
    }

    protected Variation loadDownloadURL(final DownloadLink link, final Account account) throws Exception {
        final String url = link.getPluginPatternMatcher();
        final Regex reg = new Regex(url, HTTPS_GOPRO_COM_DOWNLOAD_PREMIUM_FREE);
        final String id = reg.getMatch(0);
        final String variant = reg.getMatch(1);
        try {
            final boolean isPremium = isPremium(link);
            if (isPremium) {
                login(br, account);
            }
            final FlexiJSonMapper mapper = new FlexiJSonMapper();
            final Media media = mapper.jsonToObject(getMediaResponse(this, isPremium ? account : null, br, id, link).jsonNode, Media.TYPEREF);
            final Download resp = mapper.jsonToObject(getDownloadResponse(this, isPremium ? account : null, br, id, link).jsonNode, Download.TYPEREF);
            Variation source = null;
            if (link.hasVariantSupport() && !link.hasGenericVariantSupport()) {
                final GoProVariant activeVariant = (GoProVariant) getActiveVariantByLink(link);
                for (Variation v : resp.getEmbedded().getVariations()) {
                    if (activeVariant._getUniqueId().equals(v.getLabel())) {
                        // source
                        source = v;
                        break;
                    }
                    if (activeVariant.getId().equals(v.getHeight() + "p")) {
                        source = v;
                        break;
                    }
                    if (activeVariant.getId().equals(v.getLabel() + "_" + v.getHeight())) {
                        source = v;
                        break;
                    }
                }
                if (source == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            if (source == null) {
                try {
                    final int index = Integer.parseInt(variant);
                    for (Variation v : resp.getEmbedded().getFiles()) {
                        if (index == v.getItem_number()) {
                            source = v;
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                }
            }
            if (source == null) {
                for (Variation v : resp.getEmbedded().getVariations()) {
                    if (variant.equals(v.getLabel())) {
                        source = v;
                        break;
                    }
                }
            }
            if (source == null) {
                for (Variation v : resp.getEmbedded().getSidecar_files()) {
                    if (variant.equals(v.getLabel())) {
                        source = v;
                        break;
                    }
                }
            }
            if (source == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                setFinalFileName(this, PluginJsonConfig.get(GoProConfig.class), media, link, source);
                return source;
            }
        } catch (FlexiParserException e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
        } catch (FlexiMapperException e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
        }
    }

    public static void setFinalFileName(Plugin plugin, GoProConfig config, Media media, DownloadLink link, Variation source) throws MalformedURLException {
        String name = media.getFilename();
        if (source != null) {
            try {
                final UrlQuery url = UrlQuery.parse(source.getUrl());
                name = HTTPConnectionUtils.parseDispositionHeader(url.getDecoded("response-content-disposition")).getFilename();
            } catch (Exception e) {
            }
        }
        if (StringUtils.isEmpty(name)) {
            // may happen for shared links;
            name = source != null ? new Regex(source.getUrl(), ".*/(.+\\....)\\?").getMatch(0) : null;
            name = media.getId() + "-" + StringUtils.valueOrEmpty(name);
        }
        String fileExtension = media.getFile_extension();
        if (source != null) {
            if ("gpr".equals(source.getType())) {
                fileExtension = "gpr";
            } else if ("zip".equals(source.getType())) {
                fileExtension = "zip";
            }
        }
        if (StringUtils.isNotEmpty(fileExtension)) {
            name = plugin.correctOrApplyFileNameExtension(name, "." + fileExtension);
        }
        if (config.isUseOriginalGoProFileNames()) {
            link.setFinalFileName(name);
        } else {
            String variant = "";
            if (source != null && "source".equals(source.getLabel()) && !"Photo".equals(media.getType()) && !"Burst".equals(media.getType()) && !"TimeLapse".equals(media.getType())) {
                variant = "_source";
            } else if ((source != null && source.getLabel() == null) || "Burst".equals(media.getType()) || "TimeLapse".equals(media.getType())) {
                if (!name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                    // burst or timelapse image
                    int digits = (int) (Math.log10(media.getItem_count()) + 1);
                    variant = "_" + Files.getFileNameWithoutExtension(media.getFilename()) + "." + StringUtils.fillPre(source.getItem_number() + "", "0", digits);
                }
            }
            if (config.isAddMediaTypeToFileName()) {
                variant = "_" + media.getType() + variant;
            }
            if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                link.setFinalFileName(Files.getFileNameWithoutExtension(name) + variant + "." + Files.getExtension(name));
            } else {
                long height = source == null ? media.getHeight() : source.getHeight();
                link.setFinalFileName(Files.getFileNameWithoutExtension(name) + "_" + height + "p" + variant + "." + Files.getExtension(name));
            }
        }
    }

    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
        if (downloadLink.hasVariantSupport() && !downloadLink.hasGenericVariantSupport()) {
            return downloadLink.getVariant(GoProVariant.class);
        } else {
            return super.getActiveVariantByLink(downloadLink);
        }
    }

    public static WeakHashMap<DownloadLink, String> LINKCACHE = new WeakHashMap<DownloadLink, String>();

    @Override
    public PluginForHost assignPlugin(PluginFinder finder, DownloadLink link) {
        final PluginForHost ret = super.assignPlugin(finder, link);
        if (ret != null) {
            addToCache(link);
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        super.correctDownloadLink(link);
        addToCache(link);
    }

    public static void addToCache(DownloadLink link) {
        if (link == null) {
            return;
        }
        synchronized (LINKCACHE) {
            final Regex reg = new Regex(link.getPluginPatternMatcher(), HTTPS_GOPRO_COM_DOWNLOAD_PREMIUM_FREE);
            final String id = reg.getMatch(0);
            LINKCACHE.put(link, id);
        }
    }

    public List<? extends LinkVariant> getVariantsByLink(DownloadLink downloadLink) {
        if (downloadLink.hasVariantSupport() && !downloadLink.hasGenericVariantSupport()) {
            return downloadLink.getVariants(GoProVariant.class);
        } else {
            return super.getVariantsByLink(downloadLink);
        }
    }

    private boolean isPremium(DownloadLink link) {
        return link.getPluginPatternMatcher().matches("(?i).*/downloadpremium/.*");
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        download(link, account);
    }

    protected void download(DownloadLink link, final Account account) throws IOException, PluginException, Exception {
        boolean hasCache = hasDownloadCache(link);
        for (int i = 0; i < 2; i++) {
            final Variation variation = loadDownloadURL(link, account);
            if (variation == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url = variation.getUrl();
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, -5);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                if (hasCache && i == 0) {
                    clearDownloadCache(link);
                    hasCache = false;
                    continue;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                dl.startDownload();
                return;
            }
        }
    }

    public static boolean hasDownloadCache(DownloadLink link) {
        return StringUtils.isNotEmpty(link.getStringProperty(MEDIA_DOWNLOAD));
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink.getPluginPatternMatcher().matches("(?i)(https?://)?[^/]*gopro\\.com/v/.+")) {
            // is a shared gopro link that is downloadable without an account
            return true;
        } else {
            return account != null && super.canHandle(downloadLink, account);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        download(link, null);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public static Request doAPIRequest(Plugin plugin, Account account, Browser br, final String url) throws Exception {
        final String usedAccessToken = account != null ? account.getStringProperty(ACCESS_TOKEN, null) : null;
        Request request = br.createGetRequest(url);
        request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT, "application/vnd.gopro.jk.media+json; version=2.0.0");
        br.getPage(request);
        if (br.getHttpConnection().getResponseCode() == 401 && account != null) {
            synchronized (account) {
                if (StringUtils.equals(usedAccessToken, account.getStringProperty(ACCESS_TOKEN, null))) {
                    account.removeProperty(ACCESS_TOKEN);
                }
                if (plugin instanceof GoProCloud) {
                    ((GoProCloud) plugin).login(br, account);
                    request = request.cloneRequest();
                    br.getPage(request);
                } else if (plugin instanceof GoProCloudDecrypter) {
                    ((GoProCloudDecrypter) plugin).login(br, account);
                    request = request.cloneRequest();
                    br.getPage(request);
                }
            }
        }
        return request;
    }

    public static FlexiJSonNodeResponse getDownloadResponse(Plugin plugin, final Account account, Browser br, String id, DownloadLink cacheSource) throws Exception {
        String jsonString = cacheSource != null ? cacheSource.getStringProperty(MEDIA_DOWNLOAD) : null;
        if (StringUtils.isEmpty(jsonString)) {
            Request request = doAPIRequest(plugin, account, br, "https://api.gopro.com/media/" + id + "/download");
            jsonString = request.getHtmlCode();
        }
        try {
            final FlexiJSonNode ret = new FlexiJSONParser(jsonString).parse();
            if (ret != null && cacheSource != null) {
                cacheSource.setProperty(MEDIA_DOWNLOAD, jsonString);
            }
            return new FlexiJSonNodeResponse(ret, jsonString);
        } catch (FlexiParserException e) {
            clearDownloadCache(cacheSource);
            throw e;
        }
    }

    public static FlexiJSonNodeResponse getMediaResponse(Plugin plugin, final Account account, Browser br, String id, DownloadLink cacheSource) throws Exception {
        String jsonString = cacheSource != null ? cacheSource.getStringProperty(MEDIA) : null;
        if (StringUtils.isEmpty(jsonString)) {
            Request request = doAPIRequest(plugin, account, br, "https://api.gopro.com/media/" + id);
            jsonString = request.getHtmlCode();
        }
        try {
            final FlexiJSonNode ret = new FlexiJSONParser(jsonString).parse();
            if (ret != null && cacheSource != null) {
                cacheSource.setProperty(MEDIA, jsonString);
            }
            return new FlexiJSonNodeResponse(ret, jsonString);
        } catch (FlexiParserException e) {
            clearDownloadCache(cacheSource);
            throw e;
        }
    }

    public static void clearDownloadCache(DownloadLink cacheSource) {
        if (cacheSource != null) {
            cacheSource.removeProperty(MEDIA_DOWNLOAD);
            // cacheSource.removeProperty(MEDIA);
        }
    }

    public static void setCache(DownloadLink link, String responseMedia, String responseMediaDownload) {
        if (responseMediaDownload != null) {
            link.setProperty(MEDIA_DOWNLOAD, responseMediaDownload);
        }
        if (responseMedia != null) {
            link.setProperty(MEDIA, responseMedia);
        }
    }
    // @Override
    // public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
    // if (manager instanceof MenuManagerMainmenu) {
    // return null;
    // } else if (manager instanceof MenuManagerMainToolbar) {
    // for (MenuItemData m : mr.getItems()) {
    // ActionData ad = m.getActionData();
    // if (ad != null) {
    // String cl = ad.getClazzName();
    // if (StringUtils.equals(cl, SyncGoProLibraryToolbarAction.class.getName())) {
    // return null;
    // }
    // }
    // }
    // mr.add(SyncGoProLibraryToolbarAction.class);
    // }
    // return null;
    // }
}
