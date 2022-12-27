package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.DispositionHeader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "copycase.com" }, urls = { "https?://(?:\\w+.)?copycase\\.com/(?:file|download)/[a-zA-Z0-9]{16}(/[^/]+)?" })
public class CopyCaseCom extends PluginForHost {
    public CopyCaseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://copycase.com/page/en-US/terms";
    }

    @Override
    public String getLinkID(DownloadLink link) {
        final String fileID = getFileID(link);
        if (fileID != null) {
            return "copycase.com://" + fileID;
        } else {
            return super.getLinkID(link);
        }
    }

    private final String FILE_PATTERN = "https?://(?:\\w+.)?copycase\\.com/file/([a-zA-Z0-9]{16})/?([^/]+)?";
    private String       downloadURL  = null;

    private String getFileID(DownloadLink link) {
        final String ret = new Regex(link.getPluginPatternMatcher(), "/(?:file|download)/([a-zA-Z0-9]{16})").getMatch(0);
        return ret;
    }

    @Override
    public void clean() {
        downloadURL = null;
        super.clean();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        downloadURL = null;
        final String fileID = getFileID(link);
        if (fileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!link.isNameSet()) {
            final String urlFileName = new Regex(link.getPluginPatternMatcher(), FILE_PATTERN).getMatch(1);
            if (urlFileName != null) {
                link.setName(URLEncode.decodeURIComponent(urlFileName));
            }
        }
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(false);
        brc.getPage("https://copycase.com/file/" + fileID);
        final String downloadRedirect = brc.getRedirectLocation();
        if (downloadRedirect == null) {
            if (brc.containsHTML("\"errors.not_found\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final URLConnectionAdapter con = brc.openHeadConnection(downloadRedirect);
        try {
            if (looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final DispositionHeader fileName = parseDispositionHeader(con);
                if (fileName != null && StringUtils.isNotEmpty(fileName.getFilename())) {
                    link.setFinalFileName(fileName.getFilename());
                }
                brc.followConnection();
                downloadURL = downloadRedirect;
                return AvailableStatus.TRUE;
            } else {
                brc.followConnection(true);
                handleError(brc, link, con);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } finally {
            con.disconnect();
        }
    }

    private void handleError(final Browser br, DownloadLink link, final URLConnectionAdapter con) throws Exception {
        switch (con.getResponseCode()) {
        case 404:
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 429:
            // HTTP/1.1 429 Too Many Requests
            final DispositionHeader fileName = parseDispositionHeader(con);
            if (fileName != null && StringUtils.isNotEmpty(fileName.getFilename())) {
                link.setFinalFileName(fileName.getFilename());
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        default:
            break;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        download(null, link, downloadURL);
    }

    private int getMaxChunks(final Account account) {
        if (account == null || AccountType.FREE.equals(account.getType())) {
            return -4;
        } else {
            return -4;
        }
    }

    private void download(final Account account, final DownloadLink link, final String downloadURL) throws Exception {
        if (downloadURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, true, getMaxChunks(account));
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            handleError(br, link, dl.getConnection());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
