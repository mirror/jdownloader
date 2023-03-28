//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imx.to" }, urls = { "https?://(?:\\w+\\.)?imx\\.to/((?:u/)?(?:i|t)/\\d+/\\d+/\\d+/([a-z0-9]+)\\.[a-z]+|(?:i/|img\\-)[a-z0-9]+)" })
public class ImxTo extends PluginForHost {
    private static final String PROPERTY_DIRECTURL = "directurl";
    private static final String TYPE_THUMBNAIL     = "(?i)https?://[^/]+/(?:u/)?t/\\d+/\\d+/\\d+/([a-z0-9]+)\\.[a-z]+";
    private static final String TYPE_FULLSIZE      = "(?i)https?://[^/]+/(?:u/)?i/\\d+/\\d+/\\d+/([a-z0-9]+)\\.[a-z]+";

    public ImxTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://imx.to/page/terms";
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
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_THUMBNAIL)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_THUMBNAIL).getMatch(0);
        } else if (link.getPluginPatternMatcher().matches(TYPE_FULLSIZE)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_FULLSIZE).getMatch(0);
        } else {
            /* Assume we have TYPE_FULLSIZE */
            return new Regex(link.getPluginPatternMatcher(), "/(?:img-)?([a-z0-9]+)$").getMatch(0);
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            /* 2021-05-25: Don't do this because this way we won't get the original filenames! */
            // if (link.getPluginPatternMatcher().matches(TYPE_FULLSIZE)) {
            // link.setProperty(PROPERTY_DIRECTURL, link.getPluginPatternMatcher());
            // } else if (link.getPluginPatternMatcher().matches(TYPE_THUMBNAIL)) {
            // link.setProperty(PROPERTY_DIRECTURL, link.getPluginPatternMatcher().replace("/u/t/", "/u/i/"));
            // }
            // remember original direct full/thumbnai link
            final String url = link.getPluginPatternMatcher();
            if (url.matches(TYPE_THUMBNAIL)) {
                link.setProperty("imageLink", url);
            } else if (url.matches(TYPE_FULLSIZE)) {
                link.setProperty("imageLink", url);
            }
            final String newurl = "https://" + this.getHost() + "/i/" + fid;
            link.setPluginPatternMatcher(newurl);
            /*
             * Important as we pickup the 'img-' URLs without '.html' ending and we do not want the user to have broken content-URLs in JD!
             */
            if (url.matches(TYPE_THUMBNAIL) || url.matches(TYPE_FULLSIZE)) {
                link.setContentUrl(url);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".jpg");
        }
        this.setBrowserExclusive();
        if (this.checkDirectLink(link, PROPERTY_DIRECTURL) != null) {
            logger.info("Availablecheck via directurl complete");
            return AvailableStatus.TRUE;
        }
        br.setFollowRedirects(true);
        br.getPage("https://" + this.getHost() + "/i/" + this.getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(this.getFID(link))) {
            String imageLink = link.getStringProperty("imageLink");
            if (imageLink != null) {
                imageLink = imageLink.replaceFirst("/t/", "/i/");
                imageLink = imageLink.replaceFirst("https?://x", "https://i");
                link.setProperty(PROPERTY_DIRECTURL, imageLink);
                logger.info("Verify directurl:" + imageLink);
                if (this.checkDirectLink(link, PROPERTY_DIRECTURL) != null) {
                    logger.info("Availablecheck via directurl complete");
                    return AvailableStatus.TRUE;
                } else {
                    link.removeProperty(PROPERTY_DIRECTURL);
                }
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        getAndSetFileInfo(link);
        /* Find- and set directurl so we can save time and requests on downloadstart. */
        final String dllink = findDownloadurl(this.br);
        if (dllink != null) {
            link.setProperty(PROPERTY_DIRECTURL, dllink);
        }
        return AvailableStatus.TRUE;
    }

    private void getAndSetFileInfo(final DownloadLink link) {
        String filename = br.getRegex("<title>\\s*IMX\\.to\\s*/\\s*([^<>\"]+)\\s*</title>").getMatch(0);
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            filename = this.correctOrApplyFileNameExtension(filename, ".jpg");
            link.setFinalFileName(filename);
        }
        final String filesize = br.getRegex("(?i)FILESIZE\\s*<span[^>]*>([^<]+)</span>").getMatch(0);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        final String md5hash = br.getRegex("(?i)HASH\\s*<span[^>]*>([^<]+)</span>").getMatch(0);
        if (md5hash != null) {
            link.setMD5Hash(md5hash);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        if (!this.attemptStoredDownloadurlDownload(link)) {
            requestFileInformation(link);
            /* Form is not always present */
            final Form continueForm = br.getFormbyKey("imgContinue");
            if (continueForm != null) {
                logger.info("Sending imgContinue Form...");
                br.submitForm(continueForm);
                getAndSetFileInfo(link);
            }
            final String dllink = findDownloadurl(this.br);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 1 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 1 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 503) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 too many connections", 1 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            link.setProperty(PROPERTY_DIRECTURL, dl.getConnection().getURL().toString());
            dl.setAllowFilenameFromURL(true);// old core
        }
        dl.startDownload();
    }

    private String findDownloadurl(final Browser br) {
        return br.getRegex("\"(https?://[^/]+/u/i/[^\"]+)\" ").getMatch(0);
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    if (link.getFinalFileName() == null) {
                        String name = link.getName();
                        final String existingExt = getFileNameExtensionFromString(name);
                        if (existingExt == null || ".html".equals(existingExt)) {
                            name += ".jpg";
                        }
                        link.setFinalFileName(name);
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, false, 1);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                if (dl.getConnection().getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(dl.getConnection().getCompleteContentLength());
                }
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
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* 2022-07-11: More connections will lead to http error response 503 */
        return 1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.removeProperty(PROPERTY_DIRECTURL);
    }
}