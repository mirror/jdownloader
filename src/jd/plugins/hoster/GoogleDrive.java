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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.GoogleConfig;
import org.jdownloader.plugins.components.google.GoogleHelper;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UserAgents;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "docs.google.com" }, urls = { "https?://(?:www\\.)?(?:docs|drive)\\.google\\.com/(?:(?:leaf|open|uc)\\?([^<>\"/]+)?id=[A-Za-z0-9\\-_]+|(?:a/[a-zA-z0-9\\.]+/)?(?:file|document)/d/[A-Za-z0-9\\-_]+)|https?://video\\.google\\.com/get_player\\?docid=[A-Za-z0-9\\-_]+" })
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
    public String[] siteSupportedNames() {
        return new String[] { "docs.google.com", "googledrive" };
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
        String id = new Regex(downloadLink.getDownloadURL(), "/(?:file|document)/d/([a-zA-Z0-9\\-_]+)").getMatch(0);
        if (id == null) {
            id = new Regex(downloadLink.getDownloadURL(), "video\\.google\\.com/get_player\\?docid=([A-Za-z0-9\\-_]+)").getMatch(0);
        }
        if (id == null) {
            id = new Regex(downloadLink.getDownloadURL(), "(?!rev)id=([a-zA-Z0-9\\-_]+)").getMatch(0);
        }
        return id;
    }

    public String   agent      = null;
    private boolean isDocument = false;
    private String  dllink     = null;

    public Browser prepBrowser(Browser pbr, final Account account) {
        // used within the decrypter also, leave public
        // language determined by the accept-language
        // user-agent required to use new ones otherwise blocks with javascript notice.
        if (pbr == null) {
            pbr = new Browser();
        }
        if (account == null) {
            /* Only modify User-Agent if user does not own an account --> Otherwise his cookies cannot be used! */
            if (agent == null) {
                agent = UserAgents.stringUserAgent();
            }
            pbr.getHeaders().put("User-Agent", agent);
        }
        pbr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        pbr.setCustomCharset("utf-8");
        pbr.setFollowRedirects(true);
        pbr.setAllowedResponseCodes(new int[] { 429 });
        return pbr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        br = new Browser();
        privatefile = false;
        download_might_not_be_possible = false;
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            try {
                login(br, account, false);
            } catch (final PluginException e) {
                logger.log(e);
                logger.info("Login failure");
            }
        }
        prepBrowser(br, account);
        if (PluginJsonConfig.get(GoogleConfig.class).isEnableExperimentalFeatures()) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(constructDownloadUrl(link));
                if (con.isOK()) {
                    if (con.isContentDisposition()) {
                        String fileName = getFileNameFromHeader(con);
                        if (fileName != null) {
                            fileName = fileName.replace("의 사본", "");
                            link.setFinalFileName(fileName);
                        }
                        if (con.getCompleteContentLength() != -1) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        }
                        dllink = con.getURL().toString();
                    } else {
                        br.followConnection();
                        final String filename = br.getRegex("class=\"uc-name-size\"><a href=\"[^\"]+\">([^<>\"]+)<").getMatch(0);
                        if (filename != null) {
                            link.setName(Encoding.htmlDecode(filename).trim());
                        }
                        final String size = br.getRegex("\\((\\d+(?:[,\\.]\\d)?\\s*[KMGT])\\)</span>").getMatch(0);
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
                if (con != null) {
                    con.disconnect();
                }
            }
        } else {
            br.getPage("https://docs.google.com/leaf?id=" + getID(link));
            if (br.containsHTML("<p class=\"error\\-caption\">Sorry, we are unable to retrieve this document\\.</p>") || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getURL().contains("accounts.google.com/")) {
                link.getLinkStatus().setStatusText("You are missing the rights to download this file");
                privatefile = true;
                return AvailableStatus.TRUE;
            } else if (br.getHttpConnection().getResponseCode() == 429) {
                logger.info("429 too many reqests detected");
                if (br.getURL().contains("/sorry/index")) {
                    /*
                     * 2020-09-09: Google is sometimes blocking users/whole ISP IP subnets so they need to go through this step in order to
                     * e.g. continue downloading.
                     */
                    logger.info("Google 'ISP block captcha' detected");
                    if (!isDownload) {
                        logger.info("Don't ask for captcha during availablecheck");
                        return AvailableStatus.UNCHECKABLE;
                    }
                    final Form captchaForm = br.getForm(0);
                    if (captchaForm == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    /* 2020-09-11: TODO: This request might be missing the header "x-client-data" */
                    /* This should now redirect back to our normal download process .... */
                    br.submitForm(captchaForm);
                    /* Double-check to make sure access was granted */
                    if (br.getHttpConnection().getResponseCode() == 429) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "429 too many requests: Captcha failed");
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "429 too many requests");
                }
            }
            String jsredirect = br.getRegex("var url = \\'(http[^<>\"]*?)\\'").getMatch(0);
            if (jsredirect != null) {
                final String url_gdrive = "https://drive.google.com/file/d/" + getID(link) + "/view?ddrp=1";
                br.getPage(url_gdrive);
            }
            String filename = br.getRegex("'title': '([^<>\"]*?)'").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("\"filename\":\"([^\"]+)\",").getMatch(0);
            } else {
                filename = filename.replace("의 사본", "");
            }
            if (filename == null) {
                filename = br.getRegex("<title>([^\"]+) - Google Drive</title>").getMatch(0);
            }
            if (filename == null) {
                /*
                 * Chances are high that we have a non-officially-downloadable-document (pdf). PDF is displayed in browser via images (1
                 * image per page) - we would need a decrypter for this.
                 */
                download_might_not_be_possible = true;
                final String type = getType(br);
                filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\">").getMatch(0);
                if (filename != null && type != null) {
                    if (type.equals("article") && !filename.endsWith(".pdf")) {
                        isDocument = true;
                        // we can name it many formats! but we are only downloading as pdf at this stage.
                        filename += ".pdf";
                    }
                }
            }
            String size = br.getRegex("\"sizeInBytes\":(\\d+),").getMatch(0);
            if (size == null) {
                // value is within html or a subquent ajax request to fetch json..
                // devnote: to fix, look for the json request to https://clients\d+\.google\.com/drive/v2internal/files/ + fuid and find the
                // filesize, then search for the number within the base page. It's normally there. just not referenced as such.
                size = br.getRegex("\\[null,\"" + (filename != null ? Pattern.quote(filename) : "[^\"]") + "\"[^\r\n]+\\[null,\\d+,\"(\\d+)\"\\]").getMatch(0);
            }
            if (filename == null) {
                /* 2019-05-22: New: Fallback */
                filename = this.getLinkID(link);
            }
            if (filename == null) {
                if (br.containsHTML("initFolderLandingPageApplication")) {
                    logger.info("This looks like an empty folder ...");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = Encoding.unicodeDecode(filename.trim());
            link.setName(filename);
            if (size != null) {
                link.setVerifiedFileSize(Long.parseLong(size));
                link.setDownloadSize(SizeFormatter.getSize(size));
            } else {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = null;
                try {
                    con = br2.openGetConnection(constructDownloadUrl(link));
                    if (con.isOK()) {
                        if (con.isContentDisposition()) {
                            String fileName = getFileNameFromHeader(con);
                            if (fileName != null) {
                                fileName = fileName.replace("의 사본", "");
                                link.setFinalFileName(fileName);
                            }
                            if (con.getCompleteContentLength() != -1) {
                                link.setDownloadSize(con.getCompleteContentLength());
                            }
                            dllink = con.getURL().toString();
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
                    if (con != null) {
                        con.disconnect();
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public String getType(Browser br) {
        final String type = br.getRegex("<meta property=\"og:type\" content=\"([^<>\"]+)\">").getMatch(0);
        return type;
    }

    private String constructDownloadUrl(DownloadLink link) {
        return !isDocument ? "https://docs.google.com/uc?id=" + getID(link) + "&export=download" : "https://docs.google.com/document/export?format=pdf&id=" + getID(link) + "&includes_info_params=true";
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        doFree(link, null);
    }

    private void doFree(final DownloadLink link, final Account account) throws Exception {
        if (privatefile) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (isDocument && dllink == null) {
            // linkchecking should have download url provided.
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (PluginJsonConfig.get(GoogleConfig.class).isEnableExperimentalFeatures()) {
            /* This will not work for documents and stream URLs */
            if (this.dllink == null) {
                if (br.containsHTML("error\\-subcaption\">Too many users have viewed or downloaded this file recently\\. Please try accessing the file again later\\.|<title>Google Drive – (Quota|Cuota|Kuota|La quota|Quote)")) {
                    /*
                     * 2019-01-18: Its not possible to download at this time - sometimes it is possible to download such files when logged
                     * in but not necessarily!
                     */
                    downloadTempUnavailableAndOrOnlyViaAccount(account);
                } else if (br.containsHTML("class=\"uc\\-error\\-caption\"")) {
                    /*
                     * 2017-02-06: This could also be another error but we catch it by the classname to make this more language independant!
                     */
                    /*
                     * 2019-01-18: Its not possible to download at this time - sometimes it is possible to download such files when logged
                     * in but not necessarily!
                     */
                    downloadTempUnavailableAndOrOnlyViaAccount(account);
                }
                if (br.containsHTML("<TITLE>Not Found</TITLE>") || br.getHttpConnection().getResponseCode() == 404) {
                    if (download_might_not_be_possible) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "This content cannot be downloaded (officially) and/or you're missing the rights for that");
                    }
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    /* E.g. "This file is too big for Google to virus-scan it - download anyway?" */
                    dllink = br.getRegex("\"([^\"]*?/uc\\?export=download[^<>\"]+)\"").getMatch(0);
                    if (dllink != null) {
                        dllink = HTMLEntities.unhtmlentities(dllink);
                    }
                }
            }
        } else {
            // for some reason we do extra stuff, to not break existing code only enter if not document.
            if (!isDocument) {
                br.setFollowRedirects(false);
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
                br.getPage("https://docs.google.com/uc?id=" + getID(link) + "&export=download");
                if (br.containsHTML("error\\-subcaption\">Too many users have viewed or downloaded this file recently\\. Please try accessing the file again later\\.|<title>Google Drive – (Quota|Cuota|Kuota|La quota|Quote)")) {
                    /*
                     * 2019-01-18: Its not possible to download at this time - sometimes it is possible to download such files when logged
                     * in but not necessarily!
                     */
                    downloadTempUnavailableAndOrOnlyViaAccount(account);
                } else if (br.containsHTML("class=\"uc\\-error\\-caption\"")) {
                    /*
                     * 2017-02-06: This could also be another error but we catch it by the classname to make this more language independant!
                     */
                    /*
                     * 2019-01-18: Its not possible to download at this time - sometimes it is possible to download such files when logged
                     * in but not necessarily!
                     */
                    downloadTempUnavailableAndOrOnlyViaAccount(account);
                }
                if ((br.containsHTML("<TITLE>Not Found</TITLE>") || br.getHttpConnection().getResponseCode() == 404) && streamLink == null) {
                    if (download_might_not_be_possible) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "This content cannot be downloaded (officially) and/or you're missing the rights for that");
                    }
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    /* E.g. "This file is too big for Google to virus-scan it - download anyway?" */
                    dllink = br.getRegex("\"([^\"]*?/uc\\?export=download[^<>\"]+)\"").getMatch(0);
                    if (dllink != null) {
                        dllink = HTMLEntities.unhtmlentities(dllink);
                    }
                }
                if (dllink == null && streamLink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (dllink == null) {
                    dllink = streamLink;
                }
                br.setFollowRedirects(true);
                if (link.getVerifiedFileSize() == -1) {
                    // why do this here??? shouldnt this be action of the download core? -raztoki20170727
                    final Browser brc = br.cloneBrowser();
                    final GetRequest request = new GetRequest(brc.getURL(dllink));
                    request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity");
                    request.getHeaders().put(HTTPConstants.HEADER_REQUEST_RANGE, "bytes=0-");
                    URLConnectionAdapter con = null;
                    try {
                        con = brc.openRequestConnection(request);
                        if (con.isOK()) {
                            if (con.getResponseCode() == 206 && con.getCompleteContentLength() > 0) {
                                link.setVerifiedFileSize(con.getCompleteContentLength());
                                link.setProperty("ServerComaptibleForByteRangeRequest", true);
                            } else if (con.isContentDisposition() && con.getCompleteContentLength() > 0) {
                                link.setVerifiedFileSize(con.getCompleteContentLength());
                                link.setProperty("ServerComaptibleForByteRangeRequest", true);
                            }
                        }
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                }
            }
        }
        boolean resume = true;
        int maxChunks = 0;
        if (link.getBooleanProperty(GoogleDrive.NOCHUNKS, false) || !resume) {
            maxChunks = 1;
        }
        final Set<String> loopCheck = new HashSet<String>();
        while (true) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, maxChunks);
            /* 2020-03-18: Streams do not have content-disposition but often 206 partial content. */
            // if ((!dl.getConnection().isContentDisposition() && dl.getConnection().getResponseCode() != 206) ||
            // (dl.getConnection().getResponseCode() != 200 && dl.getConnection().getResponseCode() != 206)) {
            if (!dl.getConnection().isContentDisposition() || (dl.getConnection().getResponseCode() != 200 && dl.getConnection().getResponseCode() != 206)) {
                if (dl.getConnection().getResponseCode() == 403) {
                    downloadTempUnavailableAndOrOnlyViaAccount(account);
                } else if (dl.getConnection().getResponseCode() == 416) {
                    dl.getConnection().disconnect();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
                }
                try {
                    dl.getConnection().setAllowedResponseCodes(new int[] { dl.getConnection().getResponseCode() });
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                if (br.containsHTML("error\\-subcaption\">Too many users have viewed or downloaded this file recently\\. Please try accessing the file again later\\.|<title>Google Drive – (Quota|Cuota|Kuota|La quota|Quote)")) {
                    // so its not possible to download at this time.
                    downloadTempUnavailableAndOrOnlyViaAccount(account);
                } else if (br.containsHTML("class=\"uc\\-error\\-caption\"")) {
                    /*
                     * 2017-02-06: This could also be another error but we catch it by the classname to make this more language independant!
                     */
                    downloadTempUnavailableAndOrOnlyViaAccount(account);
                } else if (br.containsHTML("<p class=\"uc-warning-caption\">Google Drive can't scan this file for viruses\\.</p>")) {
                    // dllink = br.getRegex("href=\"(/uc\\?export=download.*?)\">Download anyway</a>").getMatch(0);
                    dllink = br.getRegex("href\\s*=\\s*\"((/a/[^\"<>]*?)?/uc\\?export=download[^\"<>]*?)\"\\s*>\\s*Download anyway\\s*</a>").getMatch(0); // w/
                    // account
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else if (!loopCheck.add(dllink)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        dllink = HTMLEntities.unhtmlentities(dllink);
                        continue;
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            break;
        }
        try {
            if (link.getFinalFileName() == null) {
                String fileName = getFileNameFromHeader(dl.getConnection());
                if (fileName != null) {
                    fileName = fileName.replace("의 사본", "");
                    link.setFinalFileName(fileName);
                }
            }
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(GoogleDrive.NOCHUNKS, false) == false) {
                    link.setProperty(GoogleDrive.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(GoogleDrive.NOCHUNKS, false) == false) {
                link.setProperty(GoogleDrive.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    /**
     * Use this for response 403 or messages like 'file can not be downloaded at this moment'. Such files will usually be downloadable via
     * account.
     */
    private void downloadTempUnavailableAndOrOnlyViaAccount(final Account account) throws PluginException {
        if (account != null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download impossible - wait and retry later or import the file into your account and dl it from there", 2 * 60 * 60 * 1000);
        } else {
            /* 2020-03-10: No gurantees that a download will work via account but most times it will! */
            /*
             * 2020-08-10: Updated Exception - rather wait and try again later because such file may be downloadable without account again
             * after some time!
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download impossible - add Google account and retry or wait and retry later", 2 * 60 * 60 * 1000);
            // throw new AccountRequiredException();
        }
    }

    private boolean login(final Browser br, final Account account, final boolean forceLoginValidation) throws Exception {
        final GoogleHelper helper = new GoogleHelper(br);
        helper.setLogger(this.getLogger());
        return helper.login(account, forceLoginValidation);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!login(br, account, true)) {
            throw new AccountUnavailableException("Login failed", 2 * 60 * 60 * 1000l);
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* Free accounts cannot have captchas */
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(20);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, true);
        doFree(link, account);
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

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return GoogleConfig.class;
    }
}