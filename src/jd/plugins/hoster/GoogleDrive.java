//  jDownloader - Downloadmanager
//  Copyright (C) 2013  JD-Team support@jdownloader.org
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.google.GoogleHelper;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "docs.google.com" }, urls = { "https?://(?:www\\.)?(?:docs|drive)\\.google\\.com/(?:(?:leaf|open|uc)\\?([^<>\"/]+)?id=[A-Za-z0-9\\-_]+|file/d/[A-Za-z0-9\\-_]+)|https?://video\\.google\\.com/get_player\\?docid=[A-Za-z0-9\\-_]+" })
public class GoogleDrive extends PluginForHost {

    public GoogleDrive(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://accounts.google.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://support.google.com/drive/answer/2450387?hl=en-GB";
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) throws PluginException {
        final String id = getID(link);
        if (id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            link.setLinkID(id);
            link.setUrlDownload("https://docs.google.com/file/d/" + id);
        }
    }

    private static final String  NOCHUNKS                       = "NOCHUNKS";
    private boolean              pluginloaded                   = false;
    private boolean              privatefile                    = false;
    private boolean              download_might_not_be_possible = false;
    /* Connection stuff */
    private static final boolean FREE_RESUME                    = true;
    private static final int     FREE_MAXCHUNKS                 = 0;
    private static final int     FREE_MAXDOWNLOADS              = 20;

    @SuppressWarnings("deprecation")
    private String getID(DownloadLink downloadLink) {
        // known url formats
        // https://docs.google.com/file/d/0B4AYQ5odYn-pVnJ0Z2V4d1E5UWc/preview?pli=1
        // can't dl these particular links, same with document/doc, presentation/present and view
        // https://docs.google.com/uc?id=0B4AYQ5odYn-pVnJ0Z2V4d1E5UWc&export=download
        // https://docs.google.com/leaf?id=0B_QJaGmmPrqeZjJkZDFmYzEtMTYzMS00N2Y2LWI2NDUtMjQ1ZjhlZDhmYmY3
        // https://docs.google.com/open?id=0B9Z2XD2XD2iQNmxzWjd1UTdDdnc
        // https://video.google.com/get_player?docid=0B2vAVBc_577958658756vEo2eUk
        if (downloadLink == null) {
            return null;
        }
        String id = new Regex(downloadLink.getDownloadURL(), "/file/d/([a-zA-Z0-9\\-_]+)").getMatch(0);
        if (id == null) {
            id = new Regex(downloadLink.getDownloadURL(), "video\\.google\\.com/get_player\\?docid=([A-Za-z0-9\\-_]+)").getMatch(0);
        }
        if (id == null) {
            id = new Regex(downloadLink.getDownloadURL(), "(?!rev)id=([a-zA-Z0-9\\-_]+)").getMatch(0);
        }
        return id;
    }

    public String agent = null;

    public Browser prepBrowser(Browser pbr) {
        // used within the decrypter also, leave public
        // language determined by the accept-language
        // user-agent required to use new ones otherwise blocks with javascript notice.
        if (pbr == null) {
            pbr = new Browser();
        }
        if (agent == null) {
            agent = UserAgents.stringUserAgent();
        }
        pbr.getHeaders().put("User-Agent", agent);
        pbr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        pbr.setCustomCharset("utf-8");
        pbr.setFollowRedirects(true);
        return pbr;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        privatefile = false;
        download_might_not_be_possible = false;
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                login(this.br, aa);
            } catch (final Throwable e) {
            }
        }
        prepBrowser(br);
        br.getPage("https://docs.google.com/leaf?id=" + getID(link));
        if (br.containsHTML("<p class=\"error\\-caption\">Sorry, we are unable to retrieve this document\\.</p>") || this.br.getHttpConnection().getResponseCode() == 403 || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getURL().contains("accounts.google.com/")) {
            link.getLinkStatus().setStatusText("You are missing the rights to download this file");
            privatefile = true;
            return AvailableStatus.TRUE;
        }
        String jsredirect = this.br.getRegex("var url = \\'(http[^<>\"]*?)\\'").getMatch(0);
        if (jsredirect != null) {
            final String url_gdrive = "https://drive.google.com/file/d/" + getID(link) + "/view?ddrp=1";
            this.br.getPage(url_gdrive);
        }
        String filename = br.getRegex("'title': '([^<>\"]*?)'").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"filename\":\"([^\"]+)\",").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>([^\"]+) - Google Drive</title>").getMatch(0);
        }
        if (filename == null) {
            /*
             * Chances are high that we have a non-officially-downloadable-document (pdf). PDF is displayed in browser via images (1 image
             * per page) - we would need a decrypter for this.
             */
            download_might_not_be_possible = true;
            final String type = this.br.getRegex("<meta property=\"og:type\" content=\"([^<>\"]+)\">").getMatch(0);
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\">").getMatch(0);
            if (filename != null && type != null) {
                if (type.equals("article") && !filename.endsWith(".pdf")) {
                    filename += ".pdf";
                }
            }
        }
        String size = br.getRegex("\"sizeInBytes\":(\\d+),").getMatch(0);
        if (filename == null) {
            if (this.br.containsHTML("initFolderLandingPageApplication")) {
                logger.info("This looks like an empty folder ...");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename.trim());
        if (size != null) {
            link.setVerifiedFileSize(Long.parseLong(size));
            link.setDownloadSize(SizeFormatter.getSize(size));
        } else {
            final Browser br2 = br.cloneBrowser();
            final URLConnectionAdapter con = br2.openGetConnection("https://docs.google.com/uc?id=" + getID(link) + "&export=download");
            try {
                if (con.isOK()) {
                    if (con.isContentDisposition()) {
                        final String fileName = getFileNameFromHeader(con);
                        if (fileName != null) {
                            link.setFinalFileName(fileName);
                        }
                        if (con.getCompleteContentLength() != -1) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        }
                    } else {
                        br2.followConnection();
                        size = br2.getRegex("\\((\\d+(?:[,\\.]\\d)?\\s*[KMGT])\\)</span>").getMatch(0);
                        if (size != null) {
                            link.setDownloadSize(SizeFormatter.getSize(size + "B"));
                        }
                    }
                } else if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } finally {
                con.disconnect();
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        if (privatefile) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        br.setFollowRedirects(false);
        String dllink = null;
        String streamLink = null;
        /* Download not possible ? Download stream! */
        String stream_map = br.getRegex("\"fmt_stream_map\":\"(.*?)\"").getMatch(0);
        if (stream_map != null) {
            final String[] links = stream_map.split("\\|");
            streamLink = links[links.length - 1];
            streamLink = Encoding.unicodeDecode(streamLink);
        } else {
            stream_map = br.getRegex("\"fmt_stream_map\",\"(.*?)\"").getMatch(0);
            if (stream_map != null) {
                final String[] links = stream_map.split("\\|");
                streamLink = links[links.length - 1];
                streamLink = Encoding.unicodeDecode(streamLink);
            }
        }
        stream_map = br.getRegex("\"url_encoded_fmt_stream_map\",\"(.*?)\"").getMatch(0);
        if (stream_map != null) {
            final String[] links = stream_map.split("\\,");
            for (int i = 0; i < links.length; i++) {
                links[i] = Encoding.unicodeDecode(links[i]);
            }
            final UrlQuery query = Request.parseQuery(links[0]);
            streamLink = Encoding.urlDecode(query.get("url"), false);
        }
        br.getPage("https://docs.google.com/uc?id=" + getID(downloadLink) + "&export=download");
        if (br.containsHTML("error\\-subcaption\">Too many users have viewed or downloaded this file recently\\. Please try accessing the file again later\\.|<title>Google Drive – (Quota|Cuota|Kuota|La quota|Quote)")) {
            // so its not possible to download at this time.
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download not possible at this point in time - wait or try with your google account!", 60 * 60 * 1000);
        } else if (this.br.containsHTML("class=\"uc\\-error\\-caption\"")) {
            /* 2017-02-06: This could also be another error but we catch it by the classname to make this more language independant! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download not possible at this point in time - wait or try with your google account!", 60 * 60 * 1000);
        }
        if ((br.containsHTML("<TITLE>Not Found</TITLE>") || this.br.getHttpConnection().getResponseCode() == 404) && streamLink == null) {
            if (download_might_not_be_possible) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This content cannot be downloaded (officially) and/or you're miasing the rights for that");
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = br.getRegex("href=\"([^\"]+)\">Download anyway</a>").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("href=\"(/uc\\?export=download[^\"]+)\">").getMatch(0);
                if (dllink != null) {
                    dllink = HTMLEntities.unhtmlentities(dllink);
                }
            } else {
                br.getPage(HTMLEntities.unhtmlentities(dllink));
                dllink = br.getRedirectLocation();
            }
        }
        if (dllink == null && streamLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink == null) {
            dllink = streamLink;
        }
        boolean resume = true;
        int maxChunks = 0;
        if (downloadLink.getBooleanProperty(GoogleDrive.NOCHUNKS, false) || !resume) {
            maxChunks = 1;
        }
        br.setFollowRedirects(true);
        if (downloadLink.getVerifiedFileSize() == -1) {
            final Browser brc = br.cloneBrowser();
            final GetRequest request = new GetRequest(brc.getURL(dllink));
            request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity");
            request.getHeaders().put(HTTPConstants.HEADER_REQUEST_RANGE, "bytes=0-");
            URLConnectionAdapter con = null;
            try {
                con = brc.openRequestConnection(request);
                if (con.isOK()) {
                    if (con.getResponseCode() == 206 && con.getCompleteContentLength() > 0) {
                        downloadLink.setVerifiedFileSize(con.getCompleteContentLength());
                        downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);
                    } else if (con.isContentDisposition() && con.getCompleteContentLength() > 0) {
                        downloadLink.setVerifiedFileSize(con.getCompleteContentLength());
                        downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);
                    }
                }
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        while (true) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resume, maxChunks);
            if (dl.getConnection().getContentType().equals("text/html")) {
                if (dl.getConnection().getResponseCode() == 416) {
                    dl.getConnection().disconnect();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
                }
                br.followConnection();
                if (br.containsHTML("<p class=\"uc-warning-caption\">Google Drive can't scan this file for viruses\\.</p>")) {
                    // dllink = br.getRegex("href=\"(/uc\\?export=download.*?)\">Download anyway</a>").getMatch(0);
                    dllink = br.getRegex("href=\"((/a/.*?/)?/uc\\?export=download.*?)\">Download anyway</a>").getMatch(0); // w/ account
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dllink = HTMLEntities.unhtmlentities(dllink);
                    continue;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            break;
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(GoogleDrive.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(GoogleDrive.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(GoogleDrive.NOCHUNKS, false) == false) {
                downloadLink.setProperty(GoogleDrive.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    public static boolean login(final Browser br, final Account account) throws Exception {
        final GoogleHelper helper = new GoogleHelper(br);
        return helper.login(account);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            if (!login(this.br, account)) {
                throw new Exception("Login Failed");
            }
        } catch (final Exception e) {
            ai.setStatus(e.getMessage());
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* free accounts cannot have captchas */
        account.setConcurrentUsePossible(true);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (link != null) {
            link.setProperty("ServerComaptibleForByteRangeRequest", true);
            link.removeProperty(GoogleDrive.NOCHUNKS);
        }
    }
}