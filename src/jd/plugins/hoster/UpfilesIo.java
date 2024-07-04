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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class UpfilesIo extends PluginForHost {
    public UpfilesIo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String PROPERTY_DIRECTURL = "directurl";

    @Override
    public String getAGBLink() {
        return "https://upfiles.com/page/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "upfiles.com", "upfiles.app", "upfiles.io", "upfiles.download", "upfilesurls.com", "nexnoo.com" });
        return ret;
    }

    private final List<String> getDeadDomains() {
        return Arrays.asList(new String[] { "upfiles.io", "upfiles.app" });
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]{2,})");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2022-07-18: Main domain changed from upfiles.io to upfiles.com (upfiles.app) */
        return this.rewriteHost(getPluginDomains(), host);
    }

    private String getContentURL(final DownloadLink link) {
        final String domain;
        final String domainFromURL = Browser.getHost(link.getPluginPatternMatcher());
        if (getDeadDomains().contains(domainFromURL)) {
            domain = this.getHost();
        } else {
            domain = domainFromURL;
        }
        return "https://" + domain + "/" + this.getFID(link);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fileID = getFID(link);
        if (fileID != null) {
            return this.getHost() + "://" + fileID;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            /* Set fallback-name */
            link.setName(fid);
        }
        if (fid.toLowerCase().equals(fid)) {
            /* Invalid fileID e.g.: https://upfiles.com/contact */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        if (storedDirecturl != null) {
            final URLConnectionAdapter con = this.checkDownloadableRequest(link, br, new GetRequest(storedDirecturl), -1, true);
            if (con != null) {
                logger.info("Successfully checked stored directurl");
                link.setFinalFileName(Plugin.getFileNameFromConnection(con));
                return AvailableStatus.TRUE;
            }
        }
        br.getPage(this.getContentURL(link));
        checkErrors(br);
        final String filename = br.getRegex("class=\"file-title[^\"]+\"[^>]*>([^<]+)</div>").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        final String filesize = br.getRegex("(?i)<h3>\\s*Download\\s*: [^<]* \\(([0-9\\.]+ [A-Za-z]{1,5})\\)</h3>").getMatch(0);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        } else {
            /* Harder way to find filesize */
            final String directurl = getDownloadFileUrl(link, MethodName.requestFileInformation);
            if (!StringUtils.isEmpty(directurl)) {
                basicLinkCheck(br, br.createHeadRequest(directurl), link, null, null, FILENAME_SOURCE.FINAL);
                link.setProperty(PROPERTY_DIRECTURL, br.getHttpConnection().getURL().toExternalForm());
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        // requestFileInformation(link);
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        final boolean resume = true;
        final int maxchunks = 0;
        if (storedDirecturl != null) {
            logger.info("Attempting to re-use stored directurl");
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, storedDirecturl, resume, maxchunks);
                if (this.looksLikeDownloadableContent(dl.getConnection())) {
                    logger.info("Successfully re-used stored directurl");
                    dl.startDownload();
                    return;
                } else {
                    logger.info("Failed to re-use stored directurl");
                    br.followConnection(true);
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("Failed to re-use stored directurl");
            }
        }
        br.setFollowRedirects(true);
        br.getPage(getContentURL(link));
        checkErrors(br);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String downloadUrl = getDownloadFileUrl(link, MethodName.handleFree);
        if (StringUtils.isEmpty(downloadUrl)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadUrl, resume, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(PROPERTY_DIRECTURL, dl.getConnection().getURL().toExternalForm());
        dl.startDownload();
    }

    enum MethodName {
        requestFileInformation,
        handleFree
    }

    private String getDownloadFileUrl(final DownloadLink link, final MethodName methodname) throws Exception {
        final String csrfToken = br.getRegex("csrf-token\" content=\"([^\"]+)\"").getMatch(0);
        final UrlQuery query = new UrlQuery();
        query.add("_token", csrfToken);
        query.add("ccp", "1");
        query.add("action", "continue");
        br.postPage(br.getURL(), query);
        // final Regex fileInfo = br.getRegex("(?i)<h3>\\s*Download\\s*:\\s*([^<]+) \\(([^\\)]+)\\)\\s*</h3>");
        // if (fileInfo.matches()) {
        // link.setName(Encoding.htmlDecode(fileInfo.getMatch(0)).trim());
        // link.setDownloadSize(SizeFormatter.getSize(fileInfo.getMatch(1)));
        // }
        /* The following part is basically a form of SiteTemplate.MightyScript_AdLinkFly */
        final UrlQuery query2 = new UrlQuery();
        final String captchaType = PluginJSonUtils.getJson(br, "captcha_type");
        if (captchaType != null) {
            final boolean allowCaptchaDuringLinkcheck = false;
            if (methodname == MethodName.requestFileInformation && !allowCaptchaDuringLinkcheck) {
                /* Do not ask user for captcha during availablecheck */
                return null;
            }
            if (captchaType.equalsIgnoreCase("recaptcha_v2_checkbox")) {
                final String reCaptchaSiteKey = PluginJSonUtils.getJson(br, "recaptcha_v2_checkbox_site_key");
                final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaSiteKey).getToken();
                query2.add("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            } else {
                final String hcaptchaSiteKey = PluginJSonUtils.getJson(br, "hcaptcha_checkbox_site_key");
                final String hcaptchaToken = new CaptchaHelperHostPluginHCaptcha(this, br, hcaptchaSiteKey).getToken();
                query2.add("g-recaptcha-response", Encoding.urlEncode(hcaptchaToken));
                query2.add("h-captcha-response", Encoding.urlEncode(hcaptchaToken));
            }
        }
        query2.add("_token", csrfToken);
        // query2.add("view_form_data", view_form_data);
        query2.add("action", "captcha");
        br.postPage(br.getURL(), query2);
        final String view_form_data = br.getRegex("view_form_data\" value=\"([^\"]+)\"").getMatch(0);
        if (view_form_data == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String waitSecondsStr = br.getRegex("class=\"timer\">\\s*(\\d+)\\s*</span>").getMatch(0);
        final int waitSeconds = Integer.parseInt(waitSecondsStr);
        if (methodname == MethodName.handleFree) {
            sleep(waitSeconds * 1001l, link);
        } else {
            Thread.sleep(waitSeconds * 1001l);
        }
        final UrlQuery query3 = new UrlQuery();
        query3.add("_token", csrfToken);
        query3.add("view_form_data", view_form_data);
        br.postPage("/file/go", query3);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        return entries.get("url").toString();
    }

    private void checkErrors(final Browser br) throws PluginException {
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}