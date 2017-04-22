//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.io.File;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesupload.org" }, urls = { "https?://(www\\.)?filesupload\\.org/([a-f0-9]{32}/.+|[A-Za-z0-9]+)" })
public class FilesUploadOrg extends antiDDoSForHost {

    public FilesUploadOrg(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(MAINPAGE + "/upgrade." + TYPE);
    }

    // For sites which use this script: http://www.yetishare.com/
    // YetiShareBasic Version 0.3.2-psp
    // mods: doFree[added new getddllink handling]
    // limit-info:
    // protocol: no https
    // captchatype: null

    @Override
    public String getAGBLink() {
        return MAINPAGE + "/terms." + TYPE;
    }

    /* Other constants */
    private String               fuid                                         = null;
    private final String         MAINPAGE                                     = "http://filesupload.org";
    private final String         domains                                      = "(filesupload\\.org)";
    private final String         TYPE                                         = "php";
    private static final String  SIMULTANDLSLIMIT                             = "?e=You+have+reached+the+maximum+concurrent+downloads";
    private static final String  SIMULTANDLSLIMITUSERTEXT                     = "Max. simultan downloads limit reached, wait to start more downloads from this host";
    private static final String  SERVERERROR                                  = "e=Error%3A+Could+not+open+file+for+reading.";
    private static final String  SERVERERRORUSERTEXT                          = "Server error";
    private static final String  WAIT_BETWEEN_DOWNLOADS_LIMIT                 = "e=You+must+wait+";
    private static final String  WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT        = "You must wait between downloads!";
    private static final int     WAIT_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT = 10;
    private static final int     ADDITIONAL_WAIT_SECONDS                      = 3;
    /* In case there is no information when accessing the main link */
    private static final boolean AVAILABLE_CHECK_OVER_INFO_PAGE               = true;

    /* Connection stuff */
    private static final boolean FREE_RESUME                                  = true;
    private static final int     FREE_MAXCHUNKS                               = 1;
    private static final int     FREE_MAXDOWNLOADS                            = 1;

    private boolean isNewLinkType(final DownloadLink downloadLink) {
        return downloadLink != null ? downloadLink.getDownloadURL().matches(".+filesupload\\.org/[a-f0-9]{32}/.+") : false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = null, filesize = null, md5 = null;
        if (isNewLinkType(link)) {
            setFuid(link);
            getPage(link.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("filename\\s*:\\s*</b>\\s*<i>\\s*(.*?)</i>").getMatch(0);
            filesize = br.getRegex("size\\s*:\\s*</b>\\s*<i>\\s*(.*?)</i>").getMatch(0);
            md5 = br.getRegex("md5\\s*:\\s*</b>\\s*<i>\\s*([a-f0-9]{32})</i>").getMatch(0);
        } else if (AVAILABLE_CHECK_OVER_INFO_PAGE) {
            getPage(link.getDownloadURL() + "~i");
            if (!br.getURL().contains("~i") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("Filename:[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]*?)<").getMatch(0);
            if (filename == null || inValidate(Encoding.htmlDecode(filename).trim()) || Encoding.htmlDecode(filename).trim().equals("  ")) {
                /* Filename might not be available here either */
                filename = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            }
            filesize = br.getRegex("Filesize:[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        } else {
            getPage(link.getDownloadURL());
            handleErrors();
            if (br.getURL().contains(WAIT_BETWEEN_DOWNLOADS_LIMIT)) {
                link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
                link.getLinkStatus().setStatusText(WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT);
                return AvailableStatus.TRUE;
            } else if (br.getURL().contains(SERVERERROR)) {
                link.setName(new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
                link.getLinkStatus().setStatusText(SERVERERRORUSERTEXT);
                return AvailableStatus.TRUE;
            }
            if (br.getURL().contains("/error." + TYPE) || br.getURL().contains("/index." + TYPE) || (!br.containsHTML("class=\"downloadPageTable(V2)?\"") && !br.containsHTML("class=\"download\\-timer\"")) || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex fInfo = br.getRegex("<strong>([^<>\"]*?) \\((\\d+(,\\d+)?(\\.\\d+)? (KB|MB|GB))\\)<");
            filename = fInfo.getMatch(0);
            if (filename == null) {
                filename = br.getRegex("File: ([^<>\"]*?)<br/>").getMatch(0);
            }
            filesize = fInfo.getMatch(1);
            if (filename == null || filesize == null) {
                /* Get piece of the page which usually contains filename- and size */
                final String page_piece = br.getRegex("(<div class=\"contentPageWrapper\">.*?class=\"link btn\\-free\")").getMatch(0);
                if (page_piece != null) {
                    final String endings = jd.plugins.hoster.DirectHTTP.ENDINGS;
                    if (filename == null) {
                        filename = new Regex(page_piece, "([^<>/\r\n\t:\\?\"]+" + endings + "[^<>/\r\n\t:\\?\"]*)").getMatch(0);
                    }
                    if (filesize == null) {
                        filesize = new Regex(page_piece, "(\\d+(,\\d+)?(\\.\\d+)? (KB|MB|GB))").getMatch(0);
                    }
                }
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename).trim());
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize.replace(",", "")).trim()));
        }
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    private void setFuid(final DownloadLink link) {
        fuid = new Regex(link.getDownloadURL(), ".+filesupload\\.org/([a-f0-9]{32})/.+").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (AVAILABLE_CHECK_OVER_INFO_PAGE && !isNewLinkType(downloadLink)) {
            getPage(downloadLink.getDownloadURL());
        }
        doFree(downloadLink, "free_directlink", FREE_RESUME, FREE_MAXCHUNKS);
    }

    public void doFree(final DownloadLink downloadLink, final String directlinkproperty, final boolean resume, final int maxchunks) throws Exception, PluginException {
        boolean captcha = false;
        String continue_link = checkDirectLink(downloadLink, directlinkproperty);
        if (continue_link != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, continue_link, resume, maxchunks);
        } else {
            if (br.getURL().contains(SIMULTANDLSLIMIT)) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, SIMULTANDLSLIMITUSERTEXT, 1 * 60 * 1000l);
            } else if (br.getURL().contains(WAIT_BETWEEN_DOWNLOADS_LIMIT)) {
                final String wait_minutes = new Regex(br.getURL(), "wait\\+(\\d+)\\+minutes?").getMatch(0);
                if (wait_minutes != null) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT, Integer.parseInt(wait_minutes) * 60 * 1001l);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, WAIT_BETWEEN_DOWNLOADS_LIMIT_USERTEXT, WAIT_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT * 60 * 1001l);
            } else if (br.getURL().contains(SERVERERROR)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, SERVERERRORUSERTEXT, 5 * 60 * 1000l);
            }

            /* Handle up to 3 pre-download pages before the (eventually existing) captcha */
            for (int i = 1; i <= 3; i++) {
                if (isNewLinkType(downloadLink)) {
                    continue_link = br.getRegex("<a href=\"(/download-or-watch/" + fuid + "/[^\"]+)").getMatch(0);
                    getPage(continue_link);
                    continue_link = br.getRegex("<source src=\"(https?://[a-zA-Z0-9_-.]*filesupload\\.org(?::\\d+)?/[^\"]+md5=[a-f0-9]{32}[^\"]+)").getMatch(0);
                    continue_link = HTMLEntities.unhtmlentities(continue_link);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, continue_link, resume, maxchunks);
                    if (dl.getConnection().isContentDisposition()) {
                        break;
                    }
                    br.followConnection();
                    if (br.getURL().contains(SERVERERROR)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, SERVERERRORUSERTEXT, 5 * 60 * 1000l);
                    }
                } else {
                    logger.info("Handling pre-download page #" + i);
                    continue_link = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
                    if (continue_link == null) {
                        continue_link = getDllink();
                    }
                    if (continue_link == null) {
                        logger.info("No continue_link available, stepping out of pre-download loop");
                        break;
                    } else {
                        logger.info("Found continue_link, continuing...");
                    }
                    final String waittime = br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
                    if (waittime != null) {
                        logger.info("Found waittime, waiting (seconds): " + waittime + " + " + ADDITIONAL_WAIT_SECONDS + " additional seconds");
                        sleep((Integer.parseInt(waittime) + ADDITIONAL_WAIT_SECONDS) * 1001l, downloadLink);
                    } else {
                        logger.info("Current pre-download page has no waittime");
                    }
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, continue_link, resume, maxchunks);
                    if (dl.getConnection().isContentDisposition()) {
                        break;
                    }
                    br.followConnection();
                    if (br.getURL().contains(SERVERERROR)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, SERVERERRORUSERTEXT, 5 * 60 * 1000l);
                    }
                }
            }
        }
        if (continue_link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dl.getConnection().isContentDisposition()) {
            /* Do not follow connection, already done above */
            handleErrors();
            final String captchaAction = br.getRegex("<div class=\"captchaPageTable\">[\t\n\r ]+<form method=\"POST\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
            final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
            if (captchaAction == null || rcID == null) {
                logger.warning("Failed to find captcha information");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            captcha = true;
            final Recaptcha rc = new Recaptcha(br, this);
            rc.setId(rcID);
            rc.load();
            for (int icaptcha = 1; icaptcha <= 5; icaptcha++) {
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode("recaptcha", cf, downloadLink);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaAction, "submit=continue&submitted=1&d=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, resume, maxchunks);
                if (!dl.getConnection().isContentDisposition()) {
                    br.followConnection();
                    if (br.getURL().contains("error.php?e=Error%3A+Could+not+open+file+for+reading")) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
                    }
                    rc.reload();
                    continue;
                }
                break;
            }
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink() {
        return br.getRegex("\"(https?://([A-Za-z0-9\\-\\.]+)?" + domains + "/[^<>\"\\?]*?\\?download_token=[A-Za-z0-9]+)\"").getMatch(0);
    }

    private void handleErrors() throws PluginException {
        if (br.containsHTML("Error: Too many concurrent download requests")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
        } else if (br.getURL().contains(SIMULTANDLSLIMIT)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, SIMULTANDLSLIMITUSERTEXT, 1 * 60 * 1000l);
        } else if (br.containsHTML(">Failed connecting to the database with the supplied connection details")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server database error", 3 * 60 * 1000l);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_YetiShare;
    }

}