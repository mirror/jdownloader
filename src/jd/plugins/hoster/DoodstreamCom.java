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
package jd.plugins.hoster;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DoodstreamCom extends XFileSharingProBasic {
    public DoodstreamCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-08-31: null<br />
     * other:<br />
     */
    private static final String TYPE_STREAM   = "https?://[^/]+/e/.+";
    private static final String TYPE_DOWNLOAD = "https?://[^/]+/d/.+";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "dood.so", "doodstream.com", "dood.to", "doodapi.com", "dood.watch", "dood.cx", "doodstream.co", "dood.la" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2021-01-15: Main domain has changed from doodstream.com to dood.so */
        return this.rewriteHost(getPluginDomains(), host);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return DoodstreamCom.buildAnnotationUrls(getPluginDomains());
    }

    public static final String getDefaultAnnotationPatternPartDoodstream() {
        return "/(?:e|d)/[a-z0-9]+";
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + DoodstreamCom.getDefaultAnnotationPatternPartDoodstream());
        }
        return ret.toArray(new String[0]);
    }

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
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return -2;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String linkpart = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/(.+)").getMatch(0);
        if (linkpart != null) {
            link.setPluginPatternMatcher(getMainPage() + "/" + linkpart);
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 10;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 10;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2020-08-31: Special */
        return true;
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        try {
            final String result = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "([a-z0-9]+)$").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public String getFilenameFromURL(final DownloadLink dl) {
        return null;
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filesize_alt_fast() {
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        return false;
    }

    @Override
    protected boolean isShortURL(DownloadLink link) {
        return false;
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br, final String html) {
        /* 2021-08-20: Hoster is playing cat & mouse games by adding fake "file not found" texts */
        if (new Regex(html, "<iframe src=\"/e/\"").matches()) {
            /* 2021-26-04 */
            // all videos are now
            // <h1>Not Found</h1>
            // <p>video you are looking for is not found.</p>
            if (new Regex(html, "minimalUserResponseInMiliseconds\\s*=").matches()) {
                return false;
            } else if (new Regex(html, "'(/cptr/.*?)'").getMatch(0) != null) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    protected String getFallbackFilename(DownloadLink dl) {
        String fallBack = super.getFallbackFilename(dl);
        if (!StringUtils.endsWithCaseInsensitive(fallBack, ".mp4")) {
            fallBack += ".mp4";
        }
        return fallBack;
    }

    protected String doodExe(final String crp, final String crs) {
        try {
            if ("N_crp".equals(crp)) {
                return crs;
            }
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            engine.eval("var _0x4ee0=[\"\\x30\\x20\\x42\\x79\\x74\\x65\",\"\\x6C\\x6F\\x67\",\"\\x66\\x6C\\x6F\\x6F\\x72\",\"\\x70\\x6F\\x77\",\"\\x72\\x6F\\x75\\x6E\\x64\",\"\\x20\",\"\\x42\\x79\\x74\\x65\\x73\",\"\\x4B\\x42\",\"\\x4D\\x42\",\"\\x47\\x42\",\"\\x54\\x42\",\"\\x30\",\"\\x30\\x30\",\"\\x3A\",\"\\x20\\x48\\x72\\x73\",\"\\x20\\x4D\\x69\\x6E\\x73\",\"\\x20\\x53\\x65\\x63\",\"\",\"\\x6A\\x6F\\x69\\x6E\",\"\\x73\\x6F\\x72\\x74\",\"\\x73\\x70\\x6C\\x69\\x74\",\"\\x6C\\x65\\x6E\\x67\\x74\\x68\",\"\\x63\\x68\\x61\\x72\\x41\\x74\",\"\\x69\\x6E\\x64\\x65\\x78\\x4F\\x66\",\"\\x2B\",\"\\x72\\x65\\x70\\x6C\\x61\\x63\\x65\\x41\\x6C\\x6C\",\"\\x2B\\x2D\\x2D\\x2B\",\"\\x5D\",\"\\x2B\\x2D\\x2B\",\"\\x5B\",\"\\x2B\\x2E\\x2E\\x2B\",\"\\x29\",\"\\x2B\\x2E\\x2B\",\"\\x28\"];");
            engine.eval("_0x4ee0[25]=\"replace\"");
            engine.eval("function doodExe(_0xc93ex9,_0xc93ex6){for(var _0xc93ex5=_0xc93ex9[_0x4ee0[20]](_0x4ee0[17])[_0x4ee0[19]]()[_0x4ee0[18]](_0x4ee0[17]),_0xc93ex7=_0x4ee0[17],_0xc93exa=0;_0xc93exa< _0xc93ex6[_0x4ee0[21]];_0xc93exa+= 1){_0xc93ex7+= _0xc93ex5[_0x4ee0[22]](_0xc93ex9[_0x4ee0[23]](_0xc93ex6[_0x4ee0[22]](_0xc93exa)))}; return _0xc93ex7= (_0xc93ex7= (_0xc93ex7= (_0xc93ex7= (_0xc93ex7= _0xc93ex7[_0x4ee0[25]](_0x4ee0[32],_0x4ee0[33]))[_0x4ee0[25]](_0x4ee0[30],_0x4ee0[31]))[_0x4ee0[25]](_0x4ee0[28],_0x4ee0[29]))[_0x4ee0[25]](_0x4ee0[26],_0x4ee0[27]))[_0x4ee0[25]](_0x4ee0[24],_0x4ee0[5])}");
            engine.eval("var result=doodExe(\"" + crp + "\",\"" + crs + "\");");
            return engine.get("result").toString().replace("+", " ");// replace vs replaceAll
        } catch (final Throwable e) {
            logger.log(e);
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean downloadsStarted) throws Exception {
        correctDownloadLink(link);
        /* First, set fallback-filename */
        if (!link.isNameSet()) {
            setWeakFilename(link);
        }
        this.br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        /* Allow redirects to other content-IDs but files should be offline if there is e.g. a redirect to an unsupported URL format. */
        if (isOffline(link, this.br, getCorrectBR(br)) || !this.canHandle(this.br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String cptr = br.getRegex("'(/cptr/.*?)'").getMatch(0);
        if (cptr != null && link.getFinalFileName() == null) {
            final Browser brc = br.cloneBrowser();
            brc.getPage(cptr);
            try {
                final Map<String, Object> response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                String filename = doodExe((String) JavaScriptEngineFactory.walkJson(response, "ttl/crp"), (String) JavaScriptEngineFactory.walkJson(response, "ttl/crs"));
                if (!StringUtils.isEmpty(filename)) {
                    if (!StringUtils.endsWithCaseInsensitive(filename, ".mp4")) {
                        filename += ".mp4";
                    }
                    link.setFinalFileName(filename);
                }
                final String filesize = doodExe((String) JavaScriptEngineFactory.walkJson(response, "siz/crp"), (String) JavaScriptEngineFactory.walkJson(response, "siz/crs"));
                if (!StringUtils.isEmpty(filesize)) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize));
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        if (link.getFinalFileName() == null) {
            if (link.getPluginPatternMatcher().matches(TYPE_STREAM)) {
                /* First try to get filename from Chromecast json */
                String filename = new Regex(getCorrectBR(br), "<title>\\s*([^<>\"]*?)\\s*-\\s*DoodStream\\.com\\s*</title>").getMatch(0);
                if (filename == null) {
                    filename = new Regex(getCorrectBR(br), "<meta name\\s*=\\s*\"og:title\"[^>]*content\\s*=\\s*\"([^<>\"]+)\"\\s*>").getMatch(0);
                }
                if (StringUtils.isEmpty(filename)) {
                    link.setName(this.getFallbackFilename(link));
                } else {
                    if (!StringUtils.endsWithCaseInsensitive(filename, ".mp4")) {
                        filename += ".mp4";
                    }
                    link.setFinalFileName(filename);
                }
            } else {
                String filename = br.getRegex("<meta name\\s*=\\s*\"og:title\"[^>]*content\\s*=\\s*\"([^<>\"]+)\"\\s*>").getMatch(0);
                if (StringUtils.isEmpty(filename)) {
                    link.setName(this.getFallbackFilename(link));
                } else {
                    if (!StringUtils.endsWithCaseInsensitive(filename, ".mp4")) {
                        filename += ".mp4";
                    }
                    link.setFinalFileName(filename);
                }
                final String filesize = br.getRegex("class\\s*=\\s*\"size\">.*?</i>\\s*([^<>\"]+)\\s*<").getMatch(0);
                if (!StringUtils.isEmpty(filesize)) {
                    link.setDownloadSize(SizeFormatter.getSize(filesize));
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    protected void checkSSLInspection(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        if (br.containsHTML(">\\s*SSL Inspection\\s*<") || br.containsHTML(">\\s*Would you like to proceed with this session\\?\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        /*
         * 2019-05-21: TODO: Maybe try download right away instead of checking this here --> This could speed-up the
         * download-start-procedure!
         */
        String dllink = checkDirectLink(link, directlinkproperty);
        if (StringUtils.isEmpty(dllink)) {
            if (link.getPluginPatternMatcher().matches(TYPE_DOWNLOAD)) {
                /* Basically the same as the other type but hides that via iFrame. */
                final String embedURL = br.getRegex("<iframe[^>]*src=\"(/e/[a-z0-9]+)\"").getMatch(0);
                if (embedURL == null) {
                    checkSSLInspection(br, link, account);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    this.getPage(embedURL);
                }
            }
            String captchaContainer = br.getRegex("\\$\\.get\\(\"(/[^\"]+op=validate\\&gc_response=)").getMatch(0);
            if (captchaContainer != null) {
                final Browser brc = br.cloneBrowser();
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, brc, "6LceDBYbAAAAANJ5Kb2RKbuywtAw4YETbEbDAT8l").getToken();
                this.getPage(brc, captchaContainer + Encoding.urlEncode(recaptchaV2Response), true);
                sleep(1000, link);
                getPage(br.getURL());// location.reload();
                captchaContainer = br.getRegex("\\$\\.get\\(\"(/[^\"]+op=validate\\&gc_response=)").getMatch(0);
                if (captchaContainer != null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String continue_url = br.getRegex("'(/pass_md5/[^<>\"\\']+)'").getMatch(0);
            if (continue_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String token = br.getRegex("\\&token=([a-z0-9]+)").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.getPage(continue_url);
            /* Make sure we got a valid URL befopre continuing! */
            final URL dlurl = new URL(br.toString());
            // final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            dllink = dlurl.toString();
            dllink += "?token=" + token + "&expiry=" + System.currentTimeMillis();
        }
        handleDownload(link, account, dllink, null);
    }

    /* *************************** PUT API RELATED METHODS HERE *************************** */
    @Override
    protected String getAPIBase() {
        /* 2020-08-31: See here: https://doodstream.com/api-docs */
        // final String custom_apidomain = this.getPluginConfig().getStringProperty(PROPERTY_PLUGIN_api_domain_with_protocol);
        // if (custom_apidomain != null) {
        // return custom_apidomain;
        // } else {
        // return "https://doodapi.com/api";
        // }
        return "https://doodapi.com/api";
    }
}