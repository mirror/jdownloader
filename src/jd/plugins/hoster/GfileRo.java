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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gfile.ro" }, urls = { "https?://(?:www\\.)?gfile\\.ro/\\?do=fD\\&fI=([a-f0-9]{32})" })
public class GfileRo extends PluginForHost {
    public GfileRo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.gfile.ro/?do=termeni";
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

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Set fallback-filename */
            link.setName(this.getFID(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Check for other errors like rate-limits/IP-limits */
        checkErrors(br);
        if (br.containsHTML("class=\"errorbox\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML(this.getFID(link))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("\\'\\d+\\'\\);\">([^<>\"]+)</a>").getMatch(0);
        String filenameWhenPasswordProtected = br.getRegex("<td align=\"center\"[^>]+>([^<]+)</td>").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filenameWhenPasswordProtected != null) {
            /* This can consist of only line-breaks and spaces!! */
            filenameWhenPasswordProtected = Encoding.htmlDecode(filenameWhenPasswordProtected).trim();
            if (!StringUtils.isEmpty(filenameWhenPasswordProtected)) {
                link.setName(filenameWhenPasswordProtected);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = checkDirectLink(link, "directlink");
        if (dllink == null) {
            final Form pwProtected = getPasswordProtectedForm(br);
            if (pwProtected != null) {
                link.setPasswordProtected(true);
                String passCode = link.getDownloadPassword();
                if (passCode == null) {
                    passCode = getUserInput("Password?", link);
                }
                pwProtected.put("parola", Encoding.urlEncode(passCode));
                br.submitForm(pwProtected);
                checkErrors(br);
                if (getPasswordProtectedForm(br) != null) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    logger.info("Correct password entered: " + passCode);
                }
            } else {
                link.setPasswordProtected(false);
            }
            dllink = br.getRegex("\"(https?://[^/]+/file\\.php[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private void checkErrors(final Browser br) throws PluginException {
        final String waitMinutesRateLimitStr = br.getRegex("(?i)Te rug&#259;m s&#259; re&#238;ncerci peste <span[^>]*>\\s*(\\d+)\\s*</span>\\s*minute").getMatch(0);
        if (waitMinutesRateLimitStr != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waitMinutesRateLimitStr) * 60 * 1000l);
        }
    }

    private Form getPasswordProtectedForm(final Browser br) {
        return br.getFormbyActionRegex(".*file_download.*");
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
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
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

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}