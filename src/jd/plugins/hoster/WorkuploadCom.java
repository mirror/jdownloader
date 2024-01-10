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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "workupload.com" }, urls = { "https?://(?:www\\.|en\\.)?workupload\\.com/(?:file|start|report)/([A-Za-z0-9]+)" })
public class WorkuploadCom extends PluginForHost {
    public WorkuploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        prepBR(br);
        return br;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 410 });
        /* 2023-01-10: This looks to be enough to get around their anti bot stuff */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        return br;
    }

    @Override
    public String getAGBLink() {
        return "http://workupload.com/tos";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = -1;

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

    private String generateFileURL(final DownloadLink link) {
        return "https://" + this.getHost() + "/file/" + getFID(link);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final String fileID = this.getFID(link);
        getPage(br, new GetRequest(generateFileURL(link)));
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410 || this.br.containsHTML("img/404\\.jpg\"|>Whoops\\! 404|> Datei gesperrt")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String sha256 = br.getRegex("(?i)([A-Fa-f0-9]{64})\\s*\\(SHA256\\)").getMatch(0);
        if (sha256 != null) {
            link.setSha256Hash(sha256);
        }
        if (isPasswordProtected(br)) {
            link.setPasswordProtected(true);
            /* Small trick to obtain filename for password protected files */
            final Browser brc = br.cloneBrowser();
            getPage(brc, new GetRequest(br.getURL("/report/" + fileID)));
            final String filename = brc.getRegex("<b>\\s*Datei\\s*: ([^<]+)</b>").getMatch(0);
            if (filename != null) {
                link.setName(Encoding.htmlDecode(filename).trim());
            } else {
                logger.warning("Failed to find filename");
            }
        } else {
            link.setPasswordProtected(false);
            String filename = br.getRegex("<td>\\s*Dateiname\\s*:\\s*</td><td>([^<>\"]*?)<").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"intro\">[\n\t\r ]*?<b>([^<>\"]+)</b>").getMatch(0);
            }
            String filesize = br.getRegex("<td>\\s*Dateigröße:\\s*</td><td>([^<>\"]*?)<").getMatch(0);
            if (filename == null || filesize == null) {
                Regex filenameSize = br.getRegex("<p class=\"intro\">[\n\t\r ]*?<b>(.*?)</b>[^\n\t\r <>\"]*?(\\d+(?:\\.\\d+)? ?(KB|MB|GB))[^\n\t\r <>\"]*?");
                if (filename == null) {
                    filename = filenameSize.getMatch(0);
                }
                if (filesize == null) {
                    filesize = filenameSize.getMatch(1);
                }
            }
            if (filesize == null) {
                filesize = br.getRegex("(\\d+(?:\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
            }
            if (filesize == null) {
                filesize = br.getRegex("(\\d+(?:\\.\\d+)? ?(?:B(?:ytes?)?))").getMatch(0);
            }
            if (filename == null) {
                filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
            if (filename != null) {
                link.setName(Encoding.htmlDecode(filename).trim());
            } else {
                logger.warning("Failed to find filename");
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            } else {
                logger.warning("Failed to find filesize");
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean isPasswordProtected(final Browser br) {
        return br.containsHTML("id=\"passwordprotected_file_password\"");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link);
    }

    private void handleDownload(final DownloadLink link) throws Exception, PluginException {
        final String directlinkproperty = "free_directlink";
        final String storedDirectlink = link.getStringProperty(directlinkproperty);
        String dllink = null;
        String referer = null;
        if (storedDirectlink != null) {
            logger.info("Re-using stored directurl: " + storedDirectlink);
            dllink = storedDirectlink;
            referer = generateFileURL(link);
        } else {
            requestFileInformation(link);
            referer = br.getURL();
            final String fileID = this.getFID(link);
            if (isPasswordProtected(br)) {
                String passCode = link.getDownloadPassword();
                if (passCode == null) {
                    passCode = getUserInput("Password?", link);
                }
                final PostRequest req = new PostRequest(br._getURL());
                req.addVariable("passwordprotected_file%5Bpassword%5D", Encoding.urlEncode(passCode));
                req.addVariable("passwordprotected_file%5Bsubmit%5D", "");
                req.addVariable("passwordprotected_file%5Bkey%5D", fileID);
                getPage(br, req);
                if (isPasswordProtected(br)) {
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                } else {
                    /* User entered valid password */
                    link.setDownloadPassword(passCode);
                }
            }
            getPage(br, new GetRequest(br.getURL("/start/" + fileID).toExternalForm()));
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            brc.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            getPage(brc, new GetRequest(brc.getURL("/api/file/getDownloadServer/" + fileID).toExternalForm()));
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> data = (Map<String, Object>) entries.get("data");
            dllink = data.get("url").toString();
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        }
        br.getHeaders().put("Referer", referer);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), 1);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (br.getURL().contains("/file/")) {
                    logger.info("Final downloadurl redirected to main url");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } catch (final Exception e) {
            if (storedDirectlink != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        dl.setFilenameFix(true);
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        dl.startDownload();
    }

    public void getPage(final Browser br, final Request req) throws PluginException, IOException {
        br.getPage(req);
        handleAntiBot(br, req);
    }

    private boolean isAntiBotCaptchaBlocked(final Browser br) {
        return br.containsHTML("class=\"fa fa-shield-check\"");
    }

    public void handleAntiBot(final Browser br, final Request req) throws PluginException {
        if (isAntiBotCaptchaBlocked(br)) {
            /* 2023-03-20: Added detection for this but captcha handling is still missing. */
            if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Anti bot block");
            }
            logger.info("Entered anti bot handling");
            // final String urlbefore = br.getURL();
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            boolean success = false;
            try {
                brc.getPage("/puzzle");
                final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                final Map<String, Object> data = (Map<String, Object>) entries.get("data");
                final String puzzle = data.get("puzzle").toString();
                String captcha = "";
                final int range = ((Number) data.get("range")).intValue();
                final List<String> find = (List<String>) data.get("find");
                int found = 0;
                for (int i = 0; i < range; i++) {
                    final int res = sha256(puzzle + i, find, i);
                    if (res == -1) {
                        continue;
                    }
                    captcha += res + " ";
                    if (found == find.size()) {
                        break;
                    }
                }
                logger.info("Captcha result = " + captcha);
                if (captcha.length() == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "WTF! Captcha result is empty!");
                }
                brc.postPage("/captcha", new UrlQuery().add("captcha", Encoding.urlEncode(captcha)));
                final String captchaCookie = brc.getCookie(brc.getHost(), "captcha");
                if (captchaCookie != null) {
                    logger.info("Captcha success: captchaCookie = " + captchaCookie);
                    br.getPage(req);
                    if (isAntiBotCaptchaBlocked(br)) {
                        logger.warning("WTF we are still/again bot blocked");
                        success = false;
                    } else {
                        success = true;
                    }
                } else {
                    logger.warning("WTF captcha failed");
                }
            } catch (final Exception e) {
                logger.log(e);
            }
            if (!success) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Anti bot block");
            }
        }
    }

    public static int sha256(final String puzzlevalue, final List<String> find, int i) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        final byte[] hash = md.digest(puzzlevalue.getBytes(StandardCharsets.UTF_8));
        final StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        final String hashHex = sb.toString();
        if (find.contains(hashHex)) {
            return i;
        } else {
            return -1;
        }
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