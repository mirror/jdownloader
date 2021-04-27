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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.KeyValueStringEntry;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FruitpanCom extends PluginForHost {
    public FruitpanCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://www.test.com/help/privacy";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fruitpan.com" });
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
            ret.add("https?://down\\." + buildHostsPatternPart(domains) + "/fs/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME               = true;
    private static final int     FREE_MAXCHUNKS            = 0;
    private static final int     FREE_MAXDOWNLOADS         = 20;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private static final String  PROPERTY_DOWNLOAD_REFERER = "download_referer";

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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "language", "en_US");
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filesize = br.getRegex("let fileSize = \"(\\d+)\";").getMatch(0);
        if (filesize != null) {
            link.setVerifiedFileSize(SizeFormatter.getSize(filesize));
        }
        final String filenameJS = br.getRegex("let filename = (codeAndEncode\\([^\\)]+\\);)").getMatch(0);
        if (filenameJS != null) {
            String filename = null;
            try {
                final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
                final ScriptEngine engine = manager.getEngineByName("javascript");
                engine.eval("function codeAndEncode(_key, _str) {var keyUnicodeSum = 0;var codedStr = \"\";for (var j = 0; j <_key.length; j++) {keyUnicodeSum += _key.charCodeAt(j);}; for (var i = 0; i < _str.length; i++) {var _strXOR =  _str.charCodeAt(i) ^ keyUnicodeSum;codedStr += String.fromCharCode(_strXOR); };return  codedStr;};");
                engine.eval("var result = " + filenameJS);
                filename = engine.get("result").toString();
            } catch (final Exception e) {
                logger.log(e);
            }
            if (!StringUtils.isEmpty(filename)) {
                link.setFinalFileName(filename);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty, resumable, maxchunks)) {
            String continueURL = br.getRegex("(/file/down/[^\"/]+/[^\"/]+)\\.html\"").getMatch(0);
            final String captchatext = "/verifyimg/getPcv\"+\"/666";
            final String waitSecondsStr = br.getRegex("let countDown = \"(\\d+)\";").getMatch(0);
            if (continueURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (waitSecondsStr == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final int waitSeconds = Integer.parseInt(waitSecondsStr);
            /* Default waittime = 30 seconds and gets higher the more you download. Prefer reconnect at a certain point! */
            if (waitSeconds >= 300) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitSeconds * 1001l);
            } else if (!br.containsHTML(org.appwork.utils.Regex.escape(captchatext))) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final long timestampBeforeCaptcha = Time.systemIndependentCurrentJVMTimeMillis();
            final String code = this.getCaptchaCode("/verifyimg/getPcv/666.html", link);
            if (code == null || !code.matches("\\d{4}")) {
                logger.info("Invalid captcha answer format");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            continueURL += "/" + code + ".html";
            final long waittimeMillis = waitSeconds * 1001l;
            final long passedTime = Time.systemIndependentCurrentJVMTimeMillis() - timestampBeforeCaptcha;
            if (passedTime < waittimeMillis) {
                this.sleep(waittimeMillis - passedTime, link);
            } else {
                logger.info("Congratulations - captcha solving took longer than waittime!");
            }
            br.getPage(continueURL);
            if (br.containsHTML(org.appwork.utils.Regex.escape(captchatext))) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            final String downloadHost = br.getRegex("saveCdnUrl=\"(https?://[^/]+/)\";").getMatch(0);
            String downloadUrlpart = br.getRegex("let linkstr = saveCdnUrl\\+\"(.*?);").getMatch(0);
            if (downloadHost == null || downloadUrlpart == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadUrlpart = downloadUrlpart.replaceAll("(\"|\\+|\r|\n| )", "");
            final String dllink = downloadHost + downloadUrlpart;
            // final String[] cookieKeys = new String[] {"vid", "vid1", "vid3"};
            /* 2021-04-27: Important cookies: "vid", "vid1", "vid3" --> These may also be given inside our final downloadurl! */
            final String[][] cookieData = br.getRegex("setCookie\\(\"([^\"]+)\", \"([^\"]+)\"").getMatches();
            if (cookieData.length == 0) {
                logger.warning("Failed to find any pre-download cookies!");
            } else {
                for (final String cookiePair[] : cookieData) {
                    logger.info("Setting pre-download-cookie with key: " + cookiePair[0]);
                    br.setCookie(this.br.getHost(), cookiePair[0], cookiePair[1]);
                }
            }
            final String downloadReferer = this.br.getURL();
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Store directurl */
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
            /* Store cookies as we need them to re-use that directurl */
            final UrlQuery downloadCookies = new UrlQuery();
            final Cookies cookies = br.getCookies(br.getHost());
            final List<Cookie> cookiesList = cookies.getCookies();
            for (final Cookie cookie : cookiesList) {
                downloadCookies.add(cookie.getKey(), cookie.getValue());
            }
            link.setProperty(directlinkproperty + "_downloadcookies", downloadCookies.toString());
            link.setProperty(PROPERTY_DOWNLOAD_REFERER, downloadReferer);
        }
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return true;
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            final String downloadcookiesAsQueryString = link.getStringProperty(directlinkproperty + "_downloadcookies");
            if (downloadcookiesAsQueryString != null) {
                final UrlQuery query = UrlQuery.parse(downloadcookiesAsQueryString);
                for (final KeyValueStringEntry keyValuePair : query.list()) {
                    brc.setCookie(this.getHost(), keyValuePair.getKey(), keyValuePair.getValue());
                }
            }
            if (link.hasProperty(PROPERTY_DOWNLOAD_REFERER)) {
                brc.getHeaders().put("Referer", link.getStringProperty(PROPERTY_DOWNLOAD_REFERER));
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
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