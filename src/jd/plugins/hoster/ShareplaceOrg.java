//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.YetiShareCore;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class ShareplaceOrg extends YetiShareCore {
    public ShareplaceOrg(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "shareplace.org", "shareplace.com" });
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
            /* Pattern for new/current URLs */
            String regex = "https?://(?:www\\.)?" + YetiShareCore.buildHostsPatternPart(domains) + YetiShareCore.getDefaultAnnotationPatternPart();
            /* Pattern for old URLs */
            regex += "|https?://(?:www\\.)?" + YetiShareCore.buildHostsPatternPart(domains) + "/\\?(?:d=)?([\\w]+)(/.*?)?";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    private final String PATTERN_OLD = "(?i)https?://[^/]+/\\?(?:d=)?([\\w]+)(/.*?)?";

    @Override
    protected String getContentURL(final DownloadLink link) {
        if (isOldURL(link.getPluginPatternMatcher())) {
            return link.getPluginPatternMatcher().replaceFirst("Download", "");
        } else {
            return super.getContentURL(link);
        }
    }

    @Override
    protected ArrayList<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("shareplace.com");
        return deadDomains;
    }

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

    @Override
    public boolean requiresWWW() {
        return false;
    }

    private boolean isOldURL(final String url) {
        if (url != null && url.matches(PATTERN_OLD)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isOldURL(final DownloadLink link) {
        return isOldURL(link.getPluginPatternMatcher());
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (isOldURL(link)) {
            return requestFileInformationOLD(link);
        } else {
            return super.requestFileInformation(link);
        }
    }

    @Override
    public void checkErrors(Browser br, DownloadLink link, Account account) throws PluginException {
        try {
            super.checkErrors(br, link, account);
        } catch (PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE && StringUtils.containsIgnoreCase(e.getMessage(), "Could not open file for reading")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 'Could not open file for reading'", 5 * 60 * 1000l, e);
            }
            throw e;
        }
    }

    public AvailableStatus requestFileInformationOLD(final DownloadLink link) throws Exception {
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid);
        }
        setBrowserExclusive();
        prepBrowserWebsite(this.br);
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        getPage(this.getContentURL(link));
        final String correctedBR = correctHTML_OLD(this.br);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.getURL().contains(fid)) {
            /* E.g. redirect to mainpage or errorpage. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (new Regex(correctedBR, "(?i)Your requested file is not found").patternFind()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String iframe = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
        if (iframe != null) {
            getPage(iframe);
        }
        String filename = new Regex(correctedBR, "Filename:\\s*</font></b>(.*?)<b>\\s*<br>").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        String filesize = br.getRegex("Filesize:\\s*</font></b>([^<]+)<b>").getMatch(0);
        if (!StringUtils.isEmpty(filename)) {
            /* Let's check if we can trust the results ... */
            filename = Encoding.htmlDecode(filename).trim();
            link.setFinalFileName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        if (isOldURL(link)) {
            handleFreeOLD(link);
        } else {
            super.handleFree(link);
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (isOldURL(link)) {
            /* Old website version version didn't have any (premium) accounts. */
            handleFreeOLD(link);
        } else {
            super.handlePremium(link, account);
        }
    }

    @Deprecated
    private void handleFreeOLD(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        /* 2016-08-23: Added captcha implementation */
        final String html_captcha = "/captcha\\.php";
        if (this.br.containsHTML(html_captcha)) {
            final String code = this.getCaptchaCode("mhfstandard", "/captcha.php?rand=" + System.currentTimeMillis(), link);
            this.br.postPage(this.br.getURL(), "captchacode=" + Encoding.urlEncode(code));
            if (this.br.containsHTML(html_captcha)) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            /* 2020-12-17: Pre-download wiattime can be skipped */
            // this.sleep(15 * 1001l, link);
        }
        String dllink = null;
        final boolean checkDirecturlCandidates = true;
        for (final String[] s : br.getRegex("<script language=\"Javascript\">(.*?)</script>").getMatches()) {
            if (!new Regex(s[0], "(vvvvvvvvv|teletubbies|zzipitime)").matches()) {
                continue;
            }
            dllink = rhinoOLD(link, s[0], checkDirecturlCandidates);
            if (dllink != null) {
                break;
            }
        }
        if (dllink == null) {
            if (br.containsHTML("<span>You have got max allowed download sessions from the same IP\\!</span>")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Limit reached or IP already loading", 60 * 60 * 1001l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!checkDirecturlCandidates) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("dllink doesn't seem to be a file...");
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error", 15 * 60 * 1000l);
            }
        }
        /* Workaround für fehlerhaften Filename Header */
        final String name = Plugin.getFileNameFromConnection(dl.getConnection());
        if (name != null) {
            link.setFinalFileName(Encoding.deepHtmlDecode(name));
        }
        dl.startDownload();
    }

    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        return link.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private String rhinoOLD(final DownloadLink link, final String s, final boolean checkResult) throws Exception {
        final String cleanup = new Regex(s, "(var.*?)var zzipitime").getMatch(0);
        final String[] vars = new Regex(s, "<a href=\"[a-z0-9 \\+]*'\\s*\\+\\s*(.*?)\\s*\\+\\s*'\"").getColumn(0);
        Exception lastException = null;
        if (vars != null) {
            final ArrayList<String> vrrs = new ArrayList<String>(Arrays.asList(vars));
            // Collections.reverse(vrrs);
            for (final String var : vrrs) {
                String result = null;
                final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
                final ScriptEngine engine = manager.getEngineByName("javascript");
                try {
                    engine.eval(cleanup);
                    result = (String) engine.get(var);
                } catch (final Throwable e) {
                    continue;
                }
                // crap here
                final String crap = new Regex(s, "<a href=\"([a-z]+)'\\s*\\+\\s*" + var).getMatch(0);
                if (crap != null) {
                    result = crap + result;
                }
                try {
                    /* Validate url */
                    new URL(result);
                } catch (Exception e) {
                    logger.log(e);
                    continue;
                }
                if (result == null || (result.contains("jdownloader") && !result.startsWith("http"))) {
                    continue;
                }
                if (checkResult) {
                    try {
                        dl = jd.plugins.BrowserAdapter.openDownload(br, link, result);
                        if (this.looksLikeDownloadableContent(dl.getConnection())) {
                            return result;
                        } else {
                            logger.info("Skipping potential final downloadurl: " + result);
                            try {
                                dl.getConnection().disconnect();
                            } catch (final Exception e) {
                            }
                            continue;
                        }
                    } catch (final Exception e) {
                        logger.log(e);
                        lastException = e;
                    }
                } else {
                    return result;
                }
            }
        }
        if (lastException != null) {
            logger.info("Failed to find any working final downloadurl -> Throwing last Exception");
            throw lastException;
        }
        return null;
    }

    /* Removes HTML code which could break the plugin */
    private String correctHTML_OLD(final Browser br) throws NumberFormatException, PluginException {
        String correctedBR = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();
        // remove custom rules first!!! As html can change because of generic cleanup rules.
        /* generic cleanup */
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: ?none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (String aRegex : regexStuff) {
            String results[] = new Regex(correctedBR, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    correctedBR = correctedBR.replace(result, "");
                }
            }
        }
        return correctedBR;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return -2;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -10;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }
}