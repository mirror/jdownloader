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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "files.d-lan.dp.ua", "zfile.in.ua" }, urls = { "https?://(?:www\\.)?files\\.d\\-lan\\.dp\\.ua/download\\?file=([a-f0-9]{32})(?:\\&name=[^/\\&#]+)?", "https?://(?:www\\.)?zfile\\.in\\.ua/download\\?file=([a-f0-9]{32})(?:\\&name=[^/\\&#]+)?" })
public class FilesDlanDpUa extends PluginForHost {
    public FilesDlanDpUa(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://files.d-lan.dp.ua/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);

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
        this.setBrowserExclusive();
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        final String url_name = new Regex(link.getPluginPatternMatcher(), "name=(.+)").getMatch(0);
        link.setLinkID(linkid);
        link.setMD5Hash(linkid);
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("\\&g=1|/error\\.png\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("type=\"password\" name=\"pass\"")) {
            /* 2018-08-07: Password protected - not yet supported. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("/images/404\\.png")) {
            /* 2020-12-03: They do not answer with a 404 http response anymore thus we have to check HTML. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("Название файла\\s*?:\\s*?(?:<b>)?([^<>\"]+)<").getMatch(0);
        String filesize = br.getRegex("Размер: ([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = url_name;
        }
        if (StringUtils.isEmpty(filename)) {
            /* Final fallback */
            filename = linkid;
        }
        filename = Encoding.htmlDecode(filename).trim();
        link.setName(filename);
        if (filesize != null) {
            filesize = filesize.replace("Гбайта", "GB").replace("Мбайт", "MB").replace("Кбайта", "kb");
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            requestFileInformation(link);
            dllink = br.getRegex("\"(https?://[^/]+/download\\d+\\.php[^<>\"]+)\"").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                try {
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
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