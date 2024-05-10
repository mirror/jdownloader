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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.KemonoPartyCrawler;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.text.TextDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { KemonoPartyCrawler.class })
public class KemonoParty extends PluginForHost {
    public KemonoParty(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String                      PROPERTY_TITLE              = "title";
    public static String                      PROPERTY_BETTER_FILENAME    = "better_filename";
    public static String                      PROPERTY_TEXT               = "text";
    public static String                      PROPERTY_PORTAL             = "portal";
    public static String                      PROPERTY_USERID             = "userid";
    public static String                      PROPERTY_POSTID             = "postid";
    public static String                      PROPERTY_DATE               = "date";
    public static String                      PROPERTY_POST_CONTENT_INDEX = "postContentIndex";
    private static Map<String, AtomicInteger> freeRunning                 = new HashMap<String, AtomicInteger>();
    public static final String                UNIQUE_ID_PREFIX            = "kemonocoomer://";

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
    public String getAGBLink() {
        return "https://" + getHost() + "/contact";
    }

    private static List<String[]> getPluginDomains() {
        return KemonoPartyCrawler.getPluginDomains();
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2023-10-27: Domains have changed in the past. */
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([^/]+)/user/([^/]+)/post/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        try {
            final String portal = link.getStringProperty(PROPERTY_PORTAL);
            final String userid = link.getStringProperty(PROPERTY_USERID);
            final String postid = link.getStringProperty(PROPERTY_POSTID);
            if (this.isTextFile(link)) {
                return UNIQUE_ID_PREFIX + "textfile/portal/" + portal + "/user/" + userid + "/post/" + postid;
            } else {
                final String path = new URL(link.getPluginPatternMatcher()).getPath();
                final String sha256Hash = getSha256HashFromPath(path);
                if (sha256Hash != null) {
                    return UNIQUE_ID_PREFIX + "filehash_sha256/" + sha256Hash;
                } else {
                    return UNIQUE_ID_PREFIX + "path/" + path;
                }
            }
        } catch (final Exception ignore) {
            return super.getLinkID(link);
        }
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        return this.getLinkID(link);
    }

    private String getFID(final DownloadLink link) {
        final String portal = link.getStringProperty(PROPERTY_PORTAL);
        final String userid = link.getStringProperty(PROPERTY_USERID);
        final String postid = link.getStringProperty(PROPERTY_POSTID);
        final int index = link.getIntegerProperty(PROPERTY_POST_CONTENT_INDEX, -1);
        if (portal != null && userid != null && postid != null && index != -1) {
            /* Media/Files */
            return portal + "_" + userid + "_" + postid + "_index_" + index;
        } else if (portal != null && userid != null && postid != null) {
            /* Raw text content */
            return portal + "_" + userid + "_" + postid;
        } else {
            return null;
        }
    }

    private boolean isTextFile(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_TEXT)) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns sha256 hash if it is present in the given url. */
    public static String getSha256HashFromURL(final String url) {
        try {
            return getSha256HashFromPath(new URL(url).getPath());
        } catch (final MalformedURLException ignore) {
            ignore.printStackTrace();
            return null;
        }
    }

    public static String getSha256HashFromPath(final String path) {
        return new Regex(path, "/([a-fA-F0-9]{64})").getMatch(0);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        if (isTextFile(link)) {
            if (!link.isNameSet()) {
                /* Fallback */
                link.setName(this.getFID(link) + ".txt");
            }
            final String textContent = link.getStringProperty(PROPERTY_TEXT);
            if (StringUtils.isEmpty(textContent)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            try {
                link.setDownloadSize(textContent.getBytes("UTF-8").length);
            } catch (final UnsupportedEncodingException ignore) {
                ignore.printStackTrace();
            }
        } else {
            final String sha256 = getSha256HashFromURL(link.getPluginPatternMatcher());
            if (sha256 != null) {
                link.setSha256Hash(sha256);
            }
            String betterFilename = link.getStringProperty(PROPERTY_BETTER_FILENAME);
            if (betterFilename == null) {
                betterFilename = KemonoPartyCrawler.getBetterFilenameFromURL(link.getPluginPatternMatcher());
            }
            if (betterFilename != null) {
                link.setFinalFileName(betterFilename);
            }
            if (!isDownload) {
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = brc.openHeadConnection(link.getPluginPatternMatcher());
                    handleConnectionErrors(brc, con);
                    if (con.getCompleteContentLength() > 0) {
                        if (con.isContentDecoded()) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        } else {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                    }
                    if (betterFilename == null) {
                        final String filenameFromHeader = Plugin.getFileNameFromHeader(con);
                        if (filenameFromHeader != null) {
                            link.setFinalFileName(filenameFromHeader);
                        }
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    protected boolean looksLikeDownloadableContent(URLConnectionAdapter urlConnection) {
        if ((urlConnection.getResponseCode() == 200 || urlConnection.getResponseCode() == 206) && getFileNameFromDispositionHeader(urlConnection) != null) {
            return true;
        } else {
            return super.looksLikeDownloadableContent(urlConnection);
        }
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 429) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "429 Too Many Requests", 1 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File broken?");
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        if (this.isTextFile(link)) {
            /* Write text to file */
            final String text = link.getStringProperty(PROPERTY_TEXT);
            dl = new TextDownloader(this, link, text);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 1);
            handleConnectionErrors(br, dl.getConnection());
            /* Add a download slot */
            controlMaxFreeDownloads(null, link, +1);
            try {
                dl.startDownload();
            } finally {
                /* remove download slot */
                controlMaxFreeDownloads(null, link, -1);
            }
        }
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
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        final int max = Integer.MAX_VALUE;
        final int running = getFreeRunning().get();
        final int ret = Math.min(running + 1, max);
        return ret;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}