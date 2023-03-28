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
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "upload.ee" }, urls = { "https?://(?:www\\.)?upload\\.ee/files/(\\d+)/([^/]+)\\.html" })
public class UploadEe extends PluginForHost {
    // DEV NOTES:
    // other: urls can work without *.html, but it has to be
    // domain/files/\d+/validfilename
    // free: unlimited connections
    // protocol: no https
    // captchatype: null
    public UploadEe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceAll("s?://upload", "://www.upload"));
    }

    @Override
    public String getAGBLink() {
        return "https://www.upload.ee/rules.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* Set filename from inside URL as temporary filename. */
        if (!link.isNameSet()) {
            link.setName(new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "lng", "eng");
        br.getPage(link.getPluginPatternMatcher());
        final boolean isOffline;
        if (br.containsHTML("(?i)(>\\s*There is no such file\\.[\r\n\t]+<|<title>UPLOAD\\.EE \\- File does not exist</title>|File was deleted by user|File was deleted automatically because of long time after last downloads|does not exist on disk)") || this.br.getHttpConnection().getResponseCode() == 404) {
            isOffline = true;
        } else {
            isOffline = false;
        }
        String filename = br.getRegex("(?i)(File|Файл): <b>(.*?)</b>").getMatch(1);
        if (filename == null) {
            filename = br.getRegex("(?i)<title>UPLOAD\\.EE\\s+\\-\\s+(?:Download|Закачать)?\\s*(.*?)\\s*-\\s*(Download|Закачать)\\s*</title>").getMatch(0);
        }
        String filesize = br.getRegex("(?i)(Size|Размер): (.*?)<br />").getMatch(1);
        if (filename != null) {
            link.setName(filename.trim());
        }
        if (filesize != null) {
            /* E.g. 1,024.00 KB --> 1024.00 KB */
            filesize = filesize.replace(",", "");
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (isOffline) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        final boolean resume = true;
        final int maxchunks = 0;
        if (!this.attemptStoredDownloadurlDownload(link, "freelink", resume, maxchunks)) {
            requestFileInformation(link);
            final String dllink = br.getRegex("(?i)\"(https?://[\\w\\-\\.]+upload\\.ee/download/" + this.getFID(link) + "/\\w+/[^\"> ]+)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("freelink", dllink);
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}