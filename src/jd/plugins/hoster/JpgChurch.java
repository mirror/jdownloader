//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLSearch;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.decrypter.JpgChurchCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class JpgChurch extends PluginForHost {
    public JpgChurch(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST };
    }

    /* Connection stuff */
    private static final boolean       free_resume        = true;
    private static final int           free_maxchunks     = 1;
    private String                     dllink             = null;
    private final String               PROPERTY_USER      = "user";
    public static final String         PROPERTY_PHPSESSID = "phpsessid";
    /* Don't touch the following! */
    private static final AtomicInteger freeRunning        = new AtomicInteger(0);

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "jpg.fishing", "jpg.fish", "jpg.church" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/img/([^/\\?#]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2023-02-21: Main domain changed from "jpg.church" to "jpg.fish". */
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/page/tos";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        String fid = null;
        if (link != null && StringUtils.equals(getHost(), link.getHost()) && (fid = getFID(link)) != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getContentURL(final DownloadLink link) {
        return "https://" + this.getHost() + "/img/" + this.getFID(link);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.correctOrApplyFileNameExtension(this.getFID(link).replaceAll("-+", " "), ".jpg"));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String title = null;
        String filesizeStr = null;
        /*
         * Re-use old sessionID for password protected items. This can avoid the need of additional steps such as having to enter the
         * password and/or having to enter a captcha.
         */
        final String passwordSessionPhpsessid = link.getStringProperty(PROPERTY_PHPSESSID);
        if (passwordSessionPhpsessid != null) {
            br.setCookie(this.getHost(), "PHPSESSID", passwordSessionPhpsessid);
        }
        final String contentURL = getContentURL(link);
        boolean useWebsite = false;
        if (link.isPasswordProtected()) {
            useWebsite = true;
        } else {
            final UrlQuery query = new UrlQuery();
            query.add("url", URLEncode.encodeURIComponent(contentURL));
            query.add("format", "json");
            br.getPage("https://" + this.getHost() + "/oembed/?" + query.toString());
            if (br.getHttpConnection().getResponseCode() == 403) {
                /* Link is broken/invalid */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                /* Content is offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Check if item is password protected */
            if (br.getHttpConnection().getResponseCode() == 401) {
                logger.info("This item is password protected");
                link.setPasswordProtected(true);
                /* Website needed to handle password stuff. */
                useWebsite = true;
                if (!isDownload) {
                    /* Do not ask for password during availablecheck. */
                    return AvailableStatus.TRUE;
                }
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (entries == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            title = (String) entries.get("title");
            final String thumbnailURL = (String) entries.get("url");
            if (thumbnailURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Remove part of this URL to get the full image. */
            this.dllink = thumbnailURL.replaceFirst("(?i)\\.md\\.(jpe?g|webp|gif)$", ".$1");
            final String author = (String) entries.get("author");
            if (author != null) {
                link.setProperty(PROPERTY_USER, author);
            }
            /* Set correct Referer header. */
            br.setCurrentURL(contentURL);
        }
        if (useWebsite) {
            br.getPage(contentURL);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Form pwform = JpgChurchCrawler.getPasswordForm(br);
            if (pwform != null) {
                logger.info("This item is password protected");
                link.setPasswordProtected(true);
                if (!isDownload) {
                    /* Do not ask for password during availablecheck. */
                    return AvailableStatus.TRUE;
                }
                String passCode = link.getDownloadPassword();
                int counter = 0;
                boolean success = false;
                do {
                    if (passCode == null || counter > 0) {
                        passCode = getUserInput("Password?", link);
                    }
                    pwform.put("content-password", Encoding.urlEncode(passCode));
                    br.submitForm(pwform);
                    // if (!this.canHandle(br.getURL())) {
                    // br.getPage(contentURLCleaned);
                    // }
                    pwform = JpgChurchCrawler.getPasswordForm(br);
                    if (pwform == null) {
                        logger.info("User entered valid password: " + passCode);
                        success = true;
                        break;
                    } else {
                        logger.info("User entered invalid password: " + passCode);
                        counter++;
                    }
                } while (counter <= 2);
                if (!success) {
                    /* Invalidate potentially previously saved password */
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                link.setDownloadPassword(passCode);
                /* Save session cookie to speed-up next attempt. */
                link.setProperty(PROPERTY_PHPSESSID, br.getCookie(br.getHost(), "PHPSESSID"));
            }
            title = HTMLSearch.searchMetaTag("og:title", br.getRequest().getHtmlCode());
            /* Filesize in html code is available when file has an official download button. */
            filesizeStr = br.getRegex("btn-download default\"[^>]*rel=\"tooltip\"[^>]*title=\"\\d+ x \\d+ - [A-Za-z0-9]+ (\\d+[^\"]+)\"").getMatch(0);
            /* Prefer official download */
            dllink = br.getRegex("href\\s*=\\s*\"(https?://[^\"]+)\"[^>]*download=\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("property\\s*=\\s*\"og:image\" content\\s*=\\s*\"(https?://[^\"]+)\"").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("<link rel\\s*=\\s*\"image_src\" href\\s*=\\s*\"(https?://[^\"]+)\">").getMatch(0);
            }
            final String author = br.getRegex("username\\s*:\\s*\"([^\"]+)\"").getMatch(0);
            if (author != null) {
                link.setProperty(PROPERTY_USER, author);
            }
        }
        if (!StringUtils.isEmpty(title)) {
            final String ext = dllink != null ? getFileNameExtensionFromURL(dllink) : null;
            title = Encoding.htmlDecode(title).trim();
            if (ext == null) {
                link.setName(title);
            } else {
                link.setFinalFileName(this.correctOrApplyFileNameExtension(title, ext));
            }
        }
        if (!StringUtils.isEmpty(filesizeStr)) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
        }
        if (!StringUtils.isEmpty(dllink) && (StringUtils.isEmpty(filesizeStr))) {
            final Browser brc = br.cloneBrowser();
            final URLConnectionAdapter con;
            if ((con = checkDownloadableRequest(link, brc, brc.createHeadRequest(dllink), 0, true)) == null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Media broken?");
            } else {
                /* Only now can we be sure to have the correct file-extension. */
                final String extByMimetype = getExtensionFromMimeType(con.getContentType());
                if (extByMimetype != null && !StringUtils.isEmpty(title)) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, "." + extByMimetype));
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } /* Add a download slot */
        controlMaxFreeDownloads(null, link, +1);
        try {
            /* Start download */
            dl.startDownload();
        } finally {
            /* Remove download slot */
            controlMaxFreeDownloads(null, link, -1);
        }
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        if (account == null) {
            synchronized (freeRunning) {
                final int before = freeRunning.get();
                final int after = before + num;
                freeRunning.set(after);
                logger.info("freeRunning(" + link.getName() + ")|max:" + getMaxSimultanFreeDownloadNum() + "|before:" + before + "|after:" + after + "|num:" + num);
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // final int max = 100;
        final int running = freeRunning.get();
        // final int ret = Math.min(running + 1, max);
        // return ret;
        return running + 1;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.CheveretoImageHosting;
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
}