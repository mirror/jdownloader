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

import java.io.IOException;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "badoinkvr.com" }, urls = { "https?://(?:www\\.)?badoinkvr\\.com/vrpornvideo/([a-z0-9\\-_]+)\\-(\\d+)/?" })
public class BadoinkvrCom extends PluginForHost {
    public BadoinkvrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }
    /* DEV NOTES */
    // Tags: Porn plugin
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://badoinkvr.com/";
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
        dllink = null;
        final String fid = this.getFID(link);
        final String extDefault = ".mp4";
        final String titleFromURL = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0).replace("_", " ").trim();
        if (!link.isNameSet()) {
            link.setName(fid + "_" + titleFromURL + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("property=\"og:title\" content=\"([^<>\"]+) \\- BaDoink VR\"").getMatch(0);
        String ext;
        if (!StringUtils.isEmpty(dllink)) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
            if (ext != null && !ext.matches("\\.(?:flv|mp4)")) {
                ext = default_extension;
            }
        } else {
            ext = default_extension;
        }
        if (!StringUtils.isEmpty(title)) {
            title = fid + "_" + title;
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setName(this.correctOrApplyFileNameExtension(title, ext));
        }
        boolean userHasCompatibleMOCHAccount = false;
        final List<Account> moch_accounts = AccountController.getInstance().getMultiHostAccounts(this.getHost());
        if (moch_accounts != null) {
            for (final Account moch_account_temp : moch_accounts) {
                if (moch_account_temp.isValid() && moch_account_temp.isEnabled()) {
                    userHasCompatibleMOCHAccount = true;
                    break;
                }
            }
        }
        long lowest_filesize = 0;
        if (userHasCompatibleMOCHAccount) {
            /* Assume that MOCH will return lowest quality possible, see if we can find the lowest filesize. */
            final String[] filesizes = br.getRegex("class=\"video-dl-options-file-info\">\\d+fps \\(([^\"]+)\\)</span>").getColumn(0);
            int i = 0;
            for (final String filesize_str : filesizes) {
                final long filesize_tmp = SizeFormatter.getSize(filesize_str);
                if (i == 0) {
                    lowest_filesize = filesize_tmp;
                    continue;
                }
                if (filesize_tmp < lowest_filesize) {
                    lowest_filesize = filesize_tmp;
                }
                i++;
            }
        }
        if (lowest_filesize > 0) {
            /* Successfully found 'MOCH-filesize' --> Display assumed filesize for MOCH download. */
            link.setDownloadSize(lowest_filesize);
        } else if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String extReal = Plugin.getExtensionFromMimeTypeStatic(con.getContentType());
                if (extReal != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, "." + extReal));
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            /* No download or only trailer download possible. */
            throw new AccountRequiredException();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
