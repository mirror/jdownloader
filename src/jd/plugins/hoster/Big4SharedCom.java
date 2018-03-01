//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "big4shared.com" }, urls = { "https?://(?:www\\.)?(?:big4shared\\.com|b4sh\\.me)/(?:embed\\-)?[a-z0-9]{12}" })
public class Big4SharedCom extends antiDDoSForHost {
    /* Some HTML code to identify different (error) states */
    private static final String  HTML_PASSWORDPROTECTED             = "<br><b>Passwor(d|t):</b> <input";
    private static final String  HTML_MAINTENANCE_MODE              = ">This server is in maintenance mode";
    /* Here comes our XFS-configuration */
    private final boolean        SUPPORTS_HTTPS                     = true;
    /* primary website url, take note of redirects */
    private final String         COOKIE_HOST                        = "http://big4shared.com".replaceFirst("https?://", SUPPORTS_HTTPS ? "https://" : "http://");
    private final String         NICE_HOSTproperty                  = COOKIE_HOST.replaceAll("(https://|http://|\\.|\\-)", "");
    /* domain names used within download links */
    private final static String  DOMAINS                            = "(?:big4shared\\.com|b4sh\\.me)";
    private final static String  dllinkRegexFile                    = "https?://(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|(?:[\\w\\-\\.]+\\.)?%s)(?::\\d{1,4})?/(?:files|d|cgi\\-bin/dl\\.cgi)/(?:\\d+/)?[a-z0-9]+/[^<>\"/]*?";
    private final static String  dllinkRegexImage                   = "https?://(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|(?:[\\w\\-\\.]+\\.)?%s)(?:/img/\\d+/[^<>\"'\\[\\]]+|/img/[a-z0-9]+/[^<>\"'\\[\\]]+|/img/[^<>\"'\\[\\]]+|/i/\\d+/[^<>\"'\\[\\]]+|/i/\\d+/[^<>\"'\\[\\]]+(?!_t\\.[A-Za-z]{3,4}))";
    /* Errormessages inside URLs */
    private static final String  URL_ERROR_PREMIUMONLY              = "/?op=login&redirect=";
    /* All kinds of XFS-plugin-configuration settings - be sure to configure this correctly when developing new XFS plugins! */
    /*
     * If activated, filename can be null - fuid will be used instead then. Also the code will check for imagehosts-continue-POST-forms and
     * check for imagehost final downloadlinks.
     */
    private final boolean        AUDIOHOSTER                        = false;
    /* If activated, checks if the video is directly available via "vidembed" --> Skips ALL waittimes- and captchas */
    private final boolean        VIDEOHOSTER                        = false;
    /* If activated, checks if the video is directly available via "embed" --> Skips all waittimes & captcha in most cases */
    private final boolean        VIDEOHOSTER_2                      = false;
    private final boolean        VIDEOHOSTER_ENFORCE_VIDEO_FILENAME = false;
    /*
     * Enable this for imagehosts --> fuid will be used as filename if none is available, doFree will check for correct filename and doFree
     * will check for videohoster "next" Download/Ad- Form.
     */
    private final boolean        IMAGEHOSTER                        = false;
    private final boolean        SUPPORTS_AVAILABLECHECK_ALT        = true;
    /*
     * true = check via postPage, false = we access the check_files site first and parse the Form to cover eventually required tokens inside
     * the Form.
     */
    private final boolean        SUPPORTS_AVAILABLECHECK_ALT_FAST   = true;
    private final boolean        SUPPORTS_AVAILABLECHECK_ABUSE      = true;
    /*
     * Scan in html code for filesize? Disable this if a website either does not contain any filesize information in its html or it only
     * contains misleading information such as fake texts.
     */
    private final boolean        SUPPORTS_HTML_FILESIZE_CHECK       = true;
    /* Pre-Download waittime stuff */
    private final boolean        WAITFORCED                         = false;
    private final int            WAITSECONDSMIN                     = 3;
    private final int            WAITSECONDSMAX                     = 100;
    private final int            WAITSECONDSFORCED                  = 5;
    /* Supported linktypes */
    private final String         TYPE_EMBED                         = "https?://[A-Za-z0-9\\-\\.]+/embed\\-[a-z0-9]{12}";
    private final String         TYPE_NORMAL                        = "https?://[A-Za-z0-9\\-\\.]+/[a-z0-9]{12}";
    /* Texts displayed to the user in some errorcases */
    private final String         USERTEXT_ALLWAIT_SHORT             = "Waiting till new downloads can be started";
    private final String         USERTEXT_MAINTENANCE               = "This server is under maintenance";
    private final String         USERTEXT_PREMIUMONLY_LINKCHECK     = "Only downloadable via premium or registered";
    /* Properties */
    private final String         PROPERTY_DLLINK_FREE               = "freelink";
    private final String         PROPERTY_DLLINK_ACCOUNT_FREE       = "freelink2";
    private final String         PROPERTY_DLLINK_ACCOUNT_PREMIUM    = "premlink";
    private final String         PROPERTY_PASS                      = "pass";
    /* Used variables */
    private String               correctedBR                        = "";
    private String               fuid                               = null;
    private String               passCode                           = null;
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger totalMaxSimultanFreeDownload       = new AtomicInteger(20);
    /* don't touch the following! */
    private static AtomicInteger maxFree                            = new AtomicInteger(1);
    private static Object        LOCK                               = new Object();

    /**
     * DEV NOTES XfileSharingProBasic Version 2.7.6.7<br />
     ****************************
     * NOTES from raztoki <br/>
     * - no need to set setfollowredirect true. <br />
     * - maintain the primary domain base url (protocol://subdomain.domain.tld.cctld), everything else will be based off that! do not fubar
     * with standard browser behaviours.
     ****************************
     * mods:<br />
     * limit-info:<br />
     * General maintenance mode information: If an XFS website is in FULL maintenance mode (e.g. not only one url is in maintenance mode but
     * ALL) it is usually impossible to get any filename/filesize/status information!<br />
     * captchatype: reCaptchaV2<br />
     * other:<br />
     */
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fuid = this.fuid != null ? this.fuid : getFUIDFromURL(link);
        if (fuid != null) {
            /* link cleanup, prefer https if possible */
            if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
                link.setContentUrl(COOKIE_HOST + "/embed-" + fuid + ".html");
            }
            link.setPluginPatternMatcher(COOKIE_HOST + "/" + fuid);
            link.setLinkID(getHost() + "://" + fuid);
        }
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.setCookie(COOKIE_HOST, "lang", "english");
        }
        return prepBr;
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/tos.html";
    }

    public Big4SharedCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    @SuppressWarnings({ "unused" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String[] fileInfo = new String[3];
        Browser altbr = null;
        fuid = null;
        correctDownloadLink(link);
        getPage(link.getPluginPatternMatcher());
        setFUID(link);
        if (new Regex(correctedBR, "(No such file|>File Not Found<|>The file was removed by|Reason for deletion:\n|File Not Found|>The file expired)").matches()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        altbr = br.cloneBrowser();
        if (new Regex(correctedBR, HTML_MAINTENANCE_MODE).matches()) {
            /* In maintenance mode this sometimes is a way to find filenames! */
            if (SUPPORTS_AVAILABLECHECK_ABUSE) {
                fileInfo[0] = this.getFnameViaAbuseLink(altbr, link);
                if (!inValidate(fileInfo[0])) {
                    link.setName(Encoding.htmlOnlyDecode(fileInfo[0]).trim());
                    return AvailableStatus.TRUE;
                }
            }
            link.getLinkStatus().setStatusText(USERTEXT_MAINTENANCE);
            return AvailableStatus.UNCHECKABLE;
        } else if (br.getURL().contains(URL_ERROR_PREMIUMONLY)) {
            /*
             * Hosts whose urls are all premiumonly usually don't display any information about the URL at all - only maybe online/ofline.
             * There are 2 alternative ways to get this information anyways!
             */
            logger.info("PREMIUMONLY handling: Trying alternative linkcheck");
            link.getLinkStatus().setStatusText(USERTEXT_PREMIUMONLY_LINKCHECK);
            if (SUPPORTS_AVAILABLECHECK_ABUSE) {
                fileInfo[0] = this.getFnameViaAbuseLink(altbr, link);
                if (altbr.containsHTML(">No such file<")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            if (SUPPORTS_AVAILABLECHECK_ALT) {
                fileInfo[1] = getFilesizeViaAvailablecheckAlt(altbr, link);
            }
            /* 2nd offline check */
            if ((SUPPORTS_AVAILABLECHECK_ALT && altbr.containsHTML("(>" + Pattern.quote(link.getPluginPatternMatcher()) + "</td><td style=\"color:red;\">Not found\\!</td>|" + this.fuid + " not found\\!</font>)")) && inValidate(fileInfo[0])) {
                /* SUPPORTS_AVAILABLECHECK_ABUSE == false and-or could not find any filename. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!inValidate(fileInfo[0]) || !inValidate(fileInfo[1])) {
                /* We know the link must be online, lets set all information we got */
                link.setAvailable(true);
                if (!inValidate(fileInfo[0])) {
                    link.setName(Encoding.htmlOnlyDecode(fileInfo[0].trim()));
                } else {
                    link.setName(fuid);
                }
                if (!inValidate(fileInfo[1])) {
                    link.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
                }
                return AvailableStatus.TRUE;
            }
            logger.warning("Alternative linkcheck failed!");
            return AvailableStatus.UNCHECKABLE;
        }
        scanInfo(fileInfo);
        /* Filename abbreviated over x chars long --> Use getFnameViaAbuseLink as a workaround to find the full-length filename! */
        if (!inValidate(fileInfo[0]) && fileInfo[0].trim().endsWith("&#133;") && SUPPORTS_AVAILABLECHECK_ABUSE) {
            logger.warning("filename length is larrrge");
            fileInfo[0] = this.getFnameViaAbuseLink(altbr, link);
        } else if (inValidate(fileInfo[0]) && SUPPORTS_AVAILABLECHECK_ABUSE) {
            /* We failed to find the filename via html --> Try getFnameViaAbuseLink */
            logger.info("Failed to find filename, trying getFnameViaAbuseLink");
            fileInfo[0] = this.getFnameViaAbuseLink(altbr, link);
        }
        if (inValidate(fileInfo[0]) && IMAGEHOSTER) {
            /*
             * Imagehosts often do not show any filenames, at least not on the first page plus they often have their abuse-url disabled. Add
             * ".jpg" extension so that linkgrabber filtering is possible although we do not y<et have our final filename.
             */
            fileInfo[0] = this.fuid + ".jpg";
            link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
        }
        if (inValidate(fileInfo[0])) {
            /*
             * We failed to find the filename --> Do a last check, maybe we've reached a downloadlimit. This is a rare case - usually plugin
             * code needs to be updated in this case!
             */
            if (correctedBR.contains("You have reached the download(\\-| )limit")) {
                logger.warning("Waittime detected, please reconnect to make the linkchecker work!");
                return AvailableStatus.UNCHECKABLE;
            }
            logger.warning("filename equals null, throwing \"plugin defect\"");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!inValidate(fileInfo[2])) {
            link.setMD5Hash(fileInfo[2].trim());
        }
        /* Remove some html tags - usually not necessary! */
        fileInfo[0] = fileInfo[0].replaceAll("(</b>|<b>|\\.html)", "").trim();
        if (VIDEOHOSTER || VIDEOHOSTER_2 || VIDEOHOSTER_ENFORCE_VIDEO_FILENAME) {
            /* For videohosts we often get ugly filenames such as 'some_videotitle.avi.mkv.mp4' --> Correct that! */
            fileInfo[0] = this.removeDoubleExtensions(fileInfo[0], "mp4");
        }
        /* Finally set the name but do not yet set the finalFilename! */
        link.setName(fileInfo[0]);
        if (inValidate(fileInfo[1]) && SUPPORTS_AVAILABLECHECK_ALT) {
            /*
             * We failed to find Do alt availablecheck here but don't check availibility based on alt availablecheck html because we already
             * know that the file must be online!
             */
            logger.info("Failed to find filesize --> Trying getFilesizeViaAvailablecheckAlt");
            fileInfo[1] = getFilesizeViaAvailablecheckAlt(altbr, link);
        }
        if (!inValidate(fileInfo[1])) {
            link.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
        }
        return AvailableStatus.TRUE;
    }

    private String[] scanInfo(final String[] fileInfo) {
        final String sharebox0 = "copy\\(this\\);.+>(.+) - ([\\d\\.]+ (?:B|KB|MB|GB))</a></textarea>[\r\n\t ]+</div>";
        final String sharebox1 = "copy\\(this\\);.+\\](.+) - ([\\d\\.]+ (?:B|KB|MB|GB))\\[/URL\\]";
        /* standard traits from base page */
        if (inValidate(fileInfo[0])) {
            fileInfo[0] = new Regex(correctedBR, "You have requested.*?https?://(www\\.)?" + DOMAINS + "/" + fuid + "/(.*?)</font>").getMatch(1);
            if (inValidate(fileInfo[0])) {
                fileInfo[0] = new Regex(correctedBR, "fname\"( type=\"hidden\")? value=\"(.*?)\"").getMatch(1);
                if (inValidate(fileInfo[0])) {
                    fileInfo[0] = new Regex(correctedBR, "<h2>Download File(.*?)</h2>").getMatch(0);
                    /* traits from download1 page below */
                    if (inValidate(fileInfo[0])) {
                        fileInfo[0] = new Regex(correctedBR, "Filename:? ?(<[^>]+> ?)+?([^<>\"']+)").getMatch(1);
                        // next two are details from sharing box
                        if (inValidate(fileInfo[0])) {
                            fileInfo[0] = new Regex(correctedBR, sharebox0).getMatch(0);
                            if (inValidate(fileInfo[0])) {
                                fileInfo[0] = new Regex(correctedBR, sharebox1).getMatch(0);
                                if (inValidate(fileInfo[0])) {
                                    /* Link of the box without filesize */
                                    fileInfo[0] = new Regex(correctedBR, "onFocus=\"copy\\(this\\);\">http://(www\\.)?" + DOMAINS + "/" + fuid + "/([^<>\"]*?)</textarea").getMatch(1);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (inValidate(fileInfo[0])) {
            fileInfo[0] = new Regex(correctedBR, "class=\"dfilename\">([^<>\"]*?)<").getMatch(0);
        }
        if (inValidate(fileInfo[0])) {
            /* 2017-04-11: Typically for XVideoSharing sites */
            fileInfo[0] = new Regex(correctedBR, Pattern.compile("<title>Watch ([^<>\"]+)</title>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        }
        if (SUPPORTS_HTML_FILESIZE_CHECK) {
            if (inValidate(fileInfo[1])) {
                fileInfo[1] = new Regex(correctedBR, "\\(([0-9]+ bytes)\\)").getMatch(0);
                if (inValidate(fileInfo[1])) {
                    fileInfo[1] = new Regex(correctedBR, "</font>[ ]+\\(([^<>\"'/]+)\\)(.*?)</font>").getMatch(0);
                    // next two are details from sharing box
                    if (inValidate(fileInfo[1])) {
                        fileInfo[1] = new Regex(correctedBR, sharebox0).getMatch(1);
                        if (inValidate(fileInfo[1])) {
                            fileInfo[1] = new Regex(correctedBR, sharebox1).getMatch(1);
                            // generic failover#1
                            if (inValidate(fileInfo[1])) {
                                fileInfo[1] = new Regex(correctedBR, "(\\d+(?:\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
                            }
                            // generic failover#2
                            if (inValidate(fileInfo[1])) {
                                fileInfo[1] = new Regex(correctedBR, "(\\d+(?:\\.\\d+)? ?(?:B(?:ytes?)?))").getMatch(0);
                            }
                        }
                    }
                }
            }
        }
        /* MD5 is only available in very very rare cases! */
        if (inValidate(fileInfo[2])) {
            fileInfo[2] = new Regex(correctedBR, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
        }
        return fileInfo;
    }

    /**
     * Get filename via abuse-URL.<br />
     * E.g. needed if officially only logged in users can see filenameor filename is missing for whatever reason.<br />
     * Especially often needed for <b><u>IMAGEHOSTER</u> ' s</b>.<br />
     * Important: Only call this if <b><u>SUPPORTS_AVAILABLECHECK_ABUSE</u></b> is <b>true</b>!<br />
     *
     * @throws Exception
     */
    private String getFnameViaAbuseLink(final Browser br, final DownloadLink dl) throws Exception {
        getPage(br, COOKIE_HOST + "/?op=report_file&id=" + fuid, false);
        return br.getRegex("<b>Filename\\s*:?\\s*</b></td><td>([^<>\"]*?)</td>").getMatch(0);
    }

    /**
     * Get filename via mass-linkchecker/alternative availablecheck.<br />
     * E.g. needed if officially only logged in users can see filesize or filesize is missing for whatever reason.<br />
     * Especially often needed for <b><u>IMAGEHOSTER</u> ' s</b>.<br />
     * Important: Only call this if <b><u>SUPPORTS_AVAILABLECHECK_ALT</u></b> is <b>true</b>!<br />
     */
    private String getFilesizeViaAvailablecheckAlt(final Browser br, final DownloadLink dl) {
        String filesize = null;
        try {
            /**
             * TODO: 2018-02-09: Current XFS versions may all use 'check_files' inside URL AND querry parameter instead of 'checkfiles'!
             * Maybe add a logic to check this via html code so we do not waste attempts / implement both versions.
             */
            if (SUPPORTS_AVAILABLECHECK_ALT_FAST) {
                postPage(br, COOKIE_HOST + "/?op=checkfiles", "op=checkfiles&process=Check+URLs&list=" + Encoding.urlEncode(dl.getPluginPatternMatcher()), false);
            } else {
                /* Try to get the Form IF NEEDED as it can contain tokens which are missing otherwise. */
                br.getPage("/?op=check_files");
                final Form checkfiles_form = br.getFormByInputFieldKeyValue("op", "check_files");
                if (checkfiles_form == null) {
                    logger.info("Failed to find check_files Form --> AltAvailablecheck failed");
                    return null;
                }
                checkfiles_form.put("list", Encoding.urlEncode(dl.getPluginPatternMatcher()));
                submitForm(br, checkfiles_form);
            }
            filesize = br.getRegex(this.fuid + "</td>\\s*?<td style=\"color:green;\">Found</td>\\s*?<td>([^<>\"]*?)</td>").getMatch(0);
        } catch (final Throwable e) {
        }
        return filesize;
    }

    /**
     * Removes double extensions (of video hosts) to correct ugly filenames such as 'some_videoname.mkv.flv.mp4'.<br />
     *
     * @param filename
     *            input filename whose extensions will be replaced by parameter defaultExtension.
     * @param defaultExtension
     *            Extension which is supposed to replace the (multiple) wrong extension(s).
     */
    private String removeDoubleExtensions(String filename, final String defaultExtension) {
        if (filename == null || defaultExtension == null) {
            return filename;
        }
        String ext_temp = null;
        int index = 0;
        while (filename.contains(".")) {
            /* First let's remove all common video extensions */
            index = filename.lastIndexOf(".");
            ext_temp = filename.substring(index);
            if (ext_temp != null && new Regex(ext_temp, Pattern.compile("\\.(avi|divx|flv|mkv|mov|mp4)", Pattern.CASE_INSENSITIVE)).matches()) {
                filename = filename.substring(0, index);
                continue;
            }
            break;
        }
        /* Add desired video extension */
        if (!filename.endsWith("." + defaultExtension)) {
            filename += "." + defaultExtension;
        }
        return filename;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, true, -5, PROPERTY_DLLINK_FREE);
    }

    @SuppressWarnings({ "unused" })
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        passCode = downloadLink.getStringProperty(PROPERTY_PASS);
        /* 1, bring up saved final links */
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        /* 2, check for streaming/direct links on the first page */
        if (dllink == null) {
            dllink = getDllink();
        }
        /* 3, do they provide audio hosting? */
        if (dllink == null && AUDIOHOSTER && downloadLink.getName().endsWith(".mp3")) {
            try {
                logger.info("Trying to get link via mp3embed");
                final Browser brv = br.cloneBrowser();
                getPage(brv, "/mp3embed-" + fuid, false);
                dllink = brv.getRedirectLocation();
                if (dllink == null) {
                    dllink = brv.getRegex("flashvars=\"file=(https?://[^<>\"]*?\\.mp3)\"").getMatch(0);
                }
                if (dllink == null) {
                    logger.info("Failed to get link via mp3embed because: " + br.toString());
                } else {
                    logger.info("Successfully found link via mp3embed");
                }
            } catch (final Throwable e) {
                logger.info("Failed to get link via mp3embed");
            }
        }
        /* 4, do they provide video hosting? */
        if (dllink == null && VIDEOHOSTER) {
            try {
                logger.info("Trying to get link via vidembed");
                final Browser brv = br.cloneBrowser();
                getPage(brv, "/vidembed-" + fuid, false);
                dllink = brv.getRedirectLocation();
                if (dllink == null) {
                    logger.info("Failed to get link via vidembed because: " + br.toString());
                } else {
                    logger.info("Successfully found link via vidembed");
                }
            } catch (final Throwable e) {
                logger.info("Failed to get link via vidembed");
            }
        }
        /* 5, do they provide video hosting #2? */
        if (dllink == null && VIDEOHOSTER_2) {
            try {
                logger.info("Trying to get link via embed");
                final String embed_access = COOKIE_HOST + "/embed-" + fuid + ".html";
                getPage(embed_access);
                dllink = getDllink();
                if (dllink == null) {
                    logger.info("Failed to get link via embed because: " + br.toString());
                } else {
                    logger.info("Successfully found link via embed");
                }
            } catch (final Throwable e) {
                logger.info("Failed to get link via embed");
            }
            if (dllink == null) {
                /* If failed, go back to the beginning */
                getPage(downloadLink.getPluginPatternMatcher());
            }
        }
        /* 6, do we have an imagehost? */
        if (dllink == null && IMAGEHOSTER) {
            checkErrors(downloadLink, false);
            Form imghost_next_form = null;
            do {
                imghost_next_form = br.getFormbyKey("next");
                if (imghost_next_form != null) {
                    imghost_next_form.remove("method_premium");
                    /* end of backward compatibility */
                    submitForm(imghost_next_form);
                    checkErrors(downloadLink, false);
                    dllink = getDllink();
                    /* For imagehosts, filenames are often not given until we can actually see/download the image! */
                    final String image_filename = new Regex(correctedBR, "class=\"pic\" alt=\"([^<>\"]*?)\"").getMatch(0);
                    if (image_filename != null) {
                        downloadLink.setName(Encoding.htmlOnlyDecode(image_filename));
                    }
                }
            } while (imghost_next_form != null);
        }
        /* 7, continue like normal */
        if (dllink == null) {
            final Form download1 = br.getFormByInputFieldKeyValue("op", "download1");
            if (download1 != null) {
                download1.remove("method_premium");
                /* Fix/Add "method_free" value if necessary. */
                if (!download1.hasInputFieldByName("method_free") || download1.getInputFieldByName("method_free").getValue() == null) {
                    String method_free_value = download1.getRegex("\"method_free\" value=\"([^<>\"]+)\"").getMatch(0);
                    if (method_free_value == null || method_free_value.equals("")) {
                        method_free_value = "Free Download";
                    }
                    download1.put("method_free", Encoding.urlEncode(method_free_value));
                }
                /* end of backward compatibility */
                submitForm(download1);
                checkErrors(downloadLink, false);
                dllink = getDllink();
            }
        }
        if (dllink == null) {
            Form dlForm = br.getFormbyProperty("name", "F1");
            if (dlForm == null) {
                /* Last chance - maybe our errorhandling kicks in here. */
                checkErrors(downloadLink, false);
                /* Okay we finally have no idea what happened ... */
                handlePluginBroken(downloadLink, "dlform_f1_null", 3);
            }
            /* how many forms deep do you want to try? */
            int repeat = 2;
            for (int i = 0; i <= repeat; i++) {
                dlForm.remove(null);
                final long timeBefore = System.currentTimeMillis();
                boolean skipWaittime = false;
                if (new Regex(correctedBR, HTML_PASSWORDPROTECTED).matches()) {
                    logger.info("The downloadlink seems to be password protected.");
                    handlePassword(dlForm, downloadLink);
                }
                /* md5 can be on the subsequent pages - it is to be found very rare in current XFS versions */
                if (downloadLink.getMD5Hash() == null) {
                    String md5hash = new Regex(correctedBR, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
                    if (md5hash != null) {
                        downloadLink.setMD5Hash(md5hash.trim());
                    }
                }
                /* Captcha START */
                if (correctedBR.contains("class=\"g-recaptcha\"")) {
                    /*
                     * 2017-12-07: New - solve- and check reCaptchaV2 here via ajax call, then wait- and submit the main downloadform. This
                     * might as well be a workaround by the XFS developers to avoid expiring reCaptchaV2 challenges.
                     */
                    logger.info("Detected captcha method \"RecaptchaV2\" for this host");
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    if (new Regex(correctedBR, Pattern.compile("\\$\\.post\\(\\s*?\"/ddl\"", Pattern.CASE_INSENSITIVE)).matches()) {
                        /* 2017-12-07: New */
                        /* Do not put the result in this Form as the check is handled below already */
                        dlForm.put("g-recaptcha-response", "");
                        final Form specialCaptchaForm = new Form();
                        specialCaptchaForm.setMethod(MethodType.POST);
                        specialCaptchaForm.setAction("/ddl");
                        final InputField if_Rand = dlForm.getInputFieldByName("rand");
                        final String file_id = PluginJSonUtils.getJson(br, "file_id");
                        if (if_Rand != null) {
                            /* This is usually given */
                            specialCaptchaForm.put("rand", if_Rand.getValue());
                        }
                        if (!StringUtils.isEmpty(file_id)) {
                            /* This is usually given */
                            specialCaptchaForm.put("file_id", file_id);
                        }
                        specialCaptchaForm.put("op", "captcha1");
                        specialCaptchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                        /* User existing Browser object as we get a cookie which is required later. */
                        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                        this.submitForm(br, specialCaptchaForm);
                        if (!br.toString().equalsIgnoreCase("OK")) {
                            logger.warning("Fatal reCaptchaV2 special handling failure");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        br.getHeaders().remove("X-Requested-With");
                    } else {
                        /* Old */
                        dlForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    }
                } else {
                    if (correctedBR.contains(";background:#ccc;text-align")) {
                        logger.info("Detected captcha method \"plaintext captchas\" for this host");
                        /* Captcha method by ManiacMansion */
                        final String[][] letters = new Regex(br, "<span style='position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;'>(&#\\d+;)</span>").getMatches();
                        if (letters == null || letters.length == 0) {
                            logger.warning("plaintext captchahandling broken!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
                        for (String[] letter : letters) {
                            capMap.put(Integer.parseInt(letter[0]), Encoding.htmlDecode(letter[1]));
                        }
                        final StringBuilder code = new StringBuilder();
                        for (String value : capMap.values()) {
                            code.append(value);
                        }
                        dlForm.put("code", code.toString());
                        logger.info("Put captchacode " + code.toString() + " obtained by captcha metod \"plaintext captchas\" in the form.");
                    } else if (correctedBR.contains("/captchas/")) {
                        logger.info("Detected captcha method \"Standard captcha\" for this host");
                        final String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
                        String captchaurl = null;
                        if (sitelinks == null || sitelinks.length == 0) {
                            logger.warning("Standard captcha captchahandling broken!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        for (String link : sitelinks) {
                            if (link.contains("/captchas/")) {
                                captchaurl = link;
                                break;
                            }
                        }
                        if (captchaurl == null) {
                            logger.warning("Standard captcha captchahandling broken!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        String code = getCaptchaCode("xfilesharingprobasic", captchaurl, downloadLink);
                        dlForm.put("code", code);
                        logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
                    } else if (new Regex(correctedBR, "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)").matches()) {
                        logger.info("Detected captcha method \"reCaptchaV1\" for this host");
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.findID();
                        rc.load();
                        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                        dlForm.put("recaptcha_challenge_field", rc.getChallenge());
                        dlForm.put("recaptcha_response_field", Encoding.urlEncode(c));
                        logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
                        /*
                         * 2017-07-25: Waittime for reCaptchaV1 was skipple in older XFS versions over a long period of time but is usually
                         * NOT skippable anymore.
                         */
                        skipWaittime = false;
                    } else if (br.containsHTML("solvemedia\\.com/papi/")) {
                        logger.info("Detected captcha method \"solvemedia\" for this host");
                        final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                        File cf = null;
                        try {
                            cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        } catch (final Exception e) {
                            if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                            }
                            throw e;
                        }
                        final String code = getCaptchaCode("solvemedia", cf, downloadLink);
                        final String chid = sm.getChallenge(code);
                        dlForm.put("adcopy_challenge", chid);
                        dlForm.put("adcopy_response", "manual_challenge");
                    } else if (br.containsHTML("id=\"capcode\" name= \"capcode\"")) {
                        logger.info("Detected captcha method \"keycaptca\"");
                        String result = handleCaptchaChallenge(getDownloadLink(), new KeyCaptcha(this, br, getDownloadLink()).createChallenge(this));
                        if (result == null) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        if ("CANCEL".equals(result)) {
                            throw new PluginException(LinkStatus.ERROR_FATAL);
                        }
                        dlForm.put("capcode", result);
                        skipWaittime = false;
                    }
                    /* Captcha END */
                }
                if (!skipWaittime) {
                    waitTime(downloadLink, timeBefore);
                }
                submitForm(dlForm);
                logger.info("Submitted DLForm");
                checkErrors(downloadLink, true);
                dllink = getDllink();
                if (dllink == null && (!br.containsHTML("<Form name=\"F1\" method=\"POST\" action=\"\"") || i == repeat)) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (dllink == null && br.containsHTML("<Form name=\"F1\" method=\"POST\" action=\"\"")) {
                    dlForm = br.getFormbyProperty("name", "F1");
                    invalidateLastChallengeResponse();
                    continue;
                } else {
                    validateLastChallengeResponse();
                    break;
                }
            }
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        if (dllink.startsWith("rtmp")) {
            try {
                dl = new RTMPDownload(this, downloadLink, dllink);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            final String playpath = new Regex(dllink, "(mp4:.+)").getMatch(0);
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(downloadLink.getPluginPatternMatcher());
            rtmp.setUrl(dllink);
            if (playpath != null) {
                rtmp.setPlayPath(playpath);
            }
            rtmp.setFlashVer("WIN 25,0,0,148");
            rtmp.setSwfVfy("CHECK_ME");
            rtmp.setApp("vod/");
            rtmp.setResume(false);
            fixFilename(downloadLink);
            try {
                /* add a download slot */
                controlFree(+1);
                /* start the dl */
                ((RTMPDownload) dl).startDownload();
            } finally {
                /* remove download slot */
                controlFree(-1);
            }
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                checkResponseCodeErrors(dl.getConnection());
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                correctBR();
                checkServerErrors();
                handlePluginBroken(downloadLink, "dllinknofile", 3);
            }
            downloadLink.setProperty(directlinkproperty, dllink);
            fixFilename(downloadLink);
            try {
                /* add a download slot */
                controlFree(+1);
                /* start the dl */
                dl.startDownload();
            } finally {
                /* remove download slot */
                controlFree(-1);
            }
        }
    }

    /**
     * Check if a stored directlink exists under property 'property' and if so, check if it is still valid (leads to a downloadable content
     * [NOT html]).
     */
    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    /* do not add @Override here to keep 0.* compatibility */
    public boolean hasAutoCaptcha() {
        return true;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        }
        return false;
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    private synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    /* Removes HTML code which could break the plugin */
    private void correctBR() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();
        // remove custom rules first!!! As html can change because of generic cleanup rules.
        /* generic cleanup */
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: ?none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (String aRegex : regexStuff) {
            String results[] = new Regex(correctedBR, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    correctedBR = correctedBR.replace(result, "");
                }
            }
        }
    }

    /** Function to find the final downloadlink. */
    @SuppressWarnings({ "unused", "unchecked", "rawtypes" })
    private String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(correctedBR, "(\"|')(" + String.format(dllinkRegexFile, DOMAINS) + ")\\1").getMatch(1);
            /* Use wider and wider RegEx */
            if (dllink == null) {
                dllink = new Regex(correctedBR, "(" + String.format(dllinkRegexFile, DOMAINS) + ")(\"|')").getMatch(0);
            }
            if (dllink == null) {
                /* Finally try without hardcoded domains */
                dllink = new Regex(correctedBR, "(" + String.format(dllinkRegexFile, "[A-Za-z0-9\\-\\.]+") + ")(\"|')").getMatch(0);
            }
            if (dllink == null) {
                final String cryptedScripts[] = new Regex(correctedBR, "p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
                if (cryptedScripts != null && cryptedScripts.length != 0) {
                    for (String crypted : cryptedScripts) {
                        dllink = decodeDownloadLink(crypted);
                        if (dllink != null) {
                            break;
                        }
                    }
                }
            }
        }
        if (dllink == null) {
            /* RegExes sometimes used for streaming */
            final String jssource = new Regex(correctedBR, "sources[\t\n\r ]*?:[\t\n\r ]*?(\\[.*?\\])").getMatch(0);
            if (inValidate(dllink) && jssource != null) {
                try {
                    HashMap<String, Object> entries = null;
                    Object quality_temp_o = null;
                    long quality_temp = 0;
                    long quality_best = 0;
                    String dllink_temp = null;
                    final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(jssource);
                    for (final Object videoo : ressourcelist) {
                        entries = (HashMap<String, Object>) videoo;
                        dllink_temp = (String) entries.get("file");
                        quality_temp_o = entries.get("label");
                        if (quality_temp_o != null && quality_temp_o instanceof Long) {
                            quality_temp = JavaScriptEngineFactory.toLong(quality_temp_o, 0);
                        } else if (quality_temp_o != null && quality_temp_o instanceof String) {
                            /* E.g. '360p' */
                            quality_temp = Long.parseLong(new Regex((String) quality_temp_o, "(\\d+)p").getMatch(0));
                        }
                        if (inValidate(dllink_temp) || quality_temp == 0) {
                            continue;
                        } else if (dllink_temp.contains(".m3u8")) {
                            /* Skip hls */
                            continue;
                        }
                        if (quality_temp > quality_best) {
                            quality_best = quality_temp;
                            dllink = dllink_temp;
                        }
                    }
                    if (!inValidate(dllink)) {
                        logger.info("BEST handling for multiple video source succeeded");
                    }
                } catch (final Throwable e) {
                    logger.info("BEST handling for multiple video source failed");
                }
            }
            if (inValidate(dllink)) {
                dllink = new Regex(correctedBR, "file:[\t\n\r ]*?\"(http[^<>\"]*?\\.(?:mp4|flv))\"").getMatch(0);
            }
        }
        if (dllink == null && IMAGEHOSTER) {
            /* Used for image-hosts */
            String[] possibleDllinks = new Regex(this.correctedBR, String.format(dllinkRegexImage, DOMAINS)).getColumn(0);
            if (possibleDllinks == null || possibleDllinks.length == 0) {
                /* Try without predefined domains */
                possibleDllinks = new Regex(this.correctedBR, String.format(dllinkRegexImage, "[A-Za-z0-9\\-\\.]+")).getColumn(0);
            }
            for (final String possibleDllink : possibleDllinks) {
                /* Do NOT download thumbnails! */
                if (possibleDllink != null && !possibleDllink.matches(".+_t\\.[A-Za-z]{3,4}$")) {
                    dllink = possibleDllink;
                    break;
                }
            }
        }
        return dllink;
    }

    private String decodeDownloadLink(final String s) {
        String decoded = null;
        try {
            Regex params = new Regex(s, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");
            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");
            while (c != 0) {
                c--;
                if (k[c].length() != 0) {
                    p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                }
            }
            decoded = p;
        } catch (Exception e) {
        }
        String finallink = null;
        if (decoded != null) {
            /* Open regex is possible because in the unpacked JS there are usually only 1 links */
            finallink = new Regex(decoded, "(?:\"|')(https?://[^<>\"']*?\\.(avi|flv|mkv|mp4))(?:\"|')").getMatch(0);
            if (finallink == null) {
                /* Maybe rtmp */
                finallink = new Regex(decoded, "(?:\"|')(rtmp://[^<>\"']*?mp4:[^<>\"']+)(?:\"|')").getMatch(0);
            }
        }
        return finallink;
    }

    @Override
    protected void getPage(String page) throws Exception {
        getPage(br, page, true);
    }

    private void getPage(final Browser br, String page, final boolean correctBr) throws Exception {
        getPage(br, page);
        if (correctBr) {
            correctBR();
        }
    }

    @Override
    protected void postPage(String page, final String postdata) throws Exception {
        postPage(br, page, postdata, true);
    }

    private void postPage(final Browser br, String page, final String postdata, final boolean correctBr) throws Exception {
        postPage(br, page, postdata);
        if (correctBr) {
            correctBR();
        }
    }

    @Override
    protected void submitForm(final Form form) throws Exception {
        submitForm(br, form, true);
    }

    private void submitForm(final Browser br, final Form form, final boolean correctBr) throws Exception {
        submitForm(br, form);
        if (correctBr) {
            correctBR();
        }
    }

    /**
     * Handles pre download (pre-captcha) waittime. If WAITFORCED it ensures to always wait long enough even if the waittime RegEx fails.
     */
    @SuppressWarnings("unused")
    private void waitTime(final DownloadLink downloadLink, final long timeBefore) throws PluginException {
        int wait = 0;
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /* Ticket Time */
        String ttt = new Regex(correctedBR, "id=\"countdown_str\">[^<>\"]+<span id=\"[^<>\"]+\"( class=\"[^<>\"]+\")?>([\n ]+)?(\\d+)([\n ]+)?</span>").getMatch(0);
        if (ttt == null) {
            ttt = new Regex(correctedBR, "id=\"countdown_str\" style=\"[^<>\"]+\">Wait <span id=\"[A-Za-z0-9]+\">(\\d+)</span>").getMatch(0);
        }
        if (ttt == null) {
            ttt = new Regex(correctedBR, "id=\"countdown_str\">Wait <span id=\"[A-Za-z0-9]+\">(\\d+)</span>").getMatch(0);
        }
        if (ttt == null) {
            ttt = new Regex(correctedBR, "class=\"seconds\"[^>]*?>\\s*?(\\d+)\\s*?</span>").getMatch(0);
        }
        if (ttt == null) {
            /* More open RegEx */
            ttt = new Regex(correctedBR, "class=\"seconds\">\\s*?(\\d+)\\s*?<").getMatch(0);
        }
        if (ttt != null) {
            logger.info("Found waittime: " + ttt);
            wait = Integer.parseInt(ttt);
            if (WAITFORCED && (wait >= WAITSECONDSMAX || wait <= WAITSECONDSMIN)) {
                logger.warning("Wait exceeds max/min, using forced wait!");
                wait = WAITSECONDSFORCED;
            }
        } else if (WAITFORCED) {
            int i = 0;
            while (i <= WAITSECONDSMIN) {
                i += new Random().nextInt(WAITSECONDSMIN);
            }
            wait = i;
        }
        wait -= passedTime;
        if (wait > 0) {
            logger.info("Waiting waittime: " + wait);
            sleep(wait * 1000l, downloadLink);
        } else {
            logger.info("Found no waittime");
        }
    }

    /**
     * This fixes filenames from all xfs modules: file hoster, audio/video streaming (including transcoded video), or blocked link checking
     * which is based on fuid.
     *
     * @version 0.4
     * @author raztoki
     */
    private void fixFilename(final DownloadLink downloadLink) {
        String orgName = null;
        String orgExt = null;
        String servName = null;
        String servExt = null;
        String orgNameExt = downloadLink.getFinalFileName();
        if (orgNameExt == null) {
            orgNameExt = downloadLink.getName();
        }
        if (!inValidate(orgNameExt) && orgNameExt.contains(".")) {
            orgExt = orgNameExt.substring(orgNameExt.lastIndexOf("."));
        }
        if (!inValidate(orgExt)) {
            orgName = new Regex(orgNameExt, "(.+)" + Pattern.quote(orgExt)).getMatch(0);
        } else {
            orgName = orgNameExt;
        }
        // if (orgName.endsWith("...")) orgName = orgName.replaceFirst("\\.\\.\\.$", "");
        String servNameExt = dl.getConnection() != null && getFileNameFromHeader(dl.getConnection()) != null ? Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())) : null;
        if (!inValidate(servNameExt) && servNameExt.contains(".")) {
            servExt = servNameExt.substring(servNameExt.lastIndexOf("."));
            servName = new Regex(servNameExt, "(.+)" + Pattern.quote(servExt)).getMatch(0);
        } else {
            servName = servNameExt;
        }
        String FFN = null;
        if (orgName.equalsIgnoreCase(fuid.toLowerCase())) {
            FFN = servNameExt;
        } else if (inValidate(orgExt) && !inValidate(servExt) && (servName.toLowerCase().contains(orgName.toLowerCase()) && !servName.equalsIgnoreCase(orgName))) {
            /*
             * when partial match of filename exists. eg cut off by quotation mark miss match, or orgNameExt has been abbreviated by hoster
             */
            FFN = servNameExt;
        } else if (!inValidate(orgExt) && !inValidate(servExt) && !orgExt.equalsIgnoreCase(servExt)) {
            FFN = orgName + servExt;
        } else {
            FFN = orgNameExt;
        }
        downloadLink.setFinalFileName(FFN);
    }

    /**
     * Sets XFS file-ID which is usually present inside the downloadurl added by the user. Usually it is [a-z0-9]{12}. <br />
     * Best to execute AFTER having accessed the downloadurl!
     */
    private void setFUID(final DownloadLink dl) throws PluginException {
        fuid = getFUIDFromURL(dl);
        /*
         * Rare case: Hoster has special URLs (e.g. migrated from other script e.g. YetiShare to XFS) --> Correct (internal) fuid is only
         * available via html
         */
        if (fuid == null) {
            logger.info("fuid not given inside URL, trying to find it inside html");
            fuid = new Regex(correctedBR, "type=\"hidden\" name=\"id\" value=\"([a-z0-9]{12})\"").getMatch(0);
            if (fuid == null) {
                logger.warning("Failed to find fuid inside html");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Found fuid inside html: " + fuid);
            correctDownloadLink(dl);
        }
    }

    private String getFUIDFromURL(final DownloadLink dl) {
        try {
            final String result = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "/(?:embed\\-)?([a-z0-9]{12})$").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void handlePassword(final Form pwform, final DownloadLink thelink) throws PluginException {
        passCode = thelink.getDownloadPassword();
        if (passCode == null) {
            passCode = Plugin.getUserInput("Password?", thelink);
            if (passCode == null || passCode.equals("")) {
                logger.info("User has entered blank password, exiting handlePassword");
                passCode = null;
                thelink.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Pre-Download Password not provided");
            }
        }
        logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
        pwform.put("password", Encoding.urlEncode(passCode));
        thelink.setDownloadPassword(passCode);
        return;
    }

    /**
     * Checks for (-& handles) all kinds of errors e.g. wrong captcha, wrong downloadpassword, waittimes and server error-responsecodes such
     * as 403, 404 and 503.
     */
    private void checkErrors(final DownloadLink theLink, final boolean checkAll) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (new Regex(correctedBR, HTML_PASSWORDPROTECTED).matches() && correctedBR.contains("Wrong password")) {
                /* handle password has failed in the past, additional try catching / resetting values */
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                passCode = null;
                theLink.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            if (correctedBR.contains("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (correctedBR.contains("\">Skipped countdown<")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Fatal countdown error (countdown skipped)");
            }
        }
        /** Wait time reconnect handling */
        if (new Regex(correctedBR, "(You have reached the download(\\-| )limit|You have to wait)").matches()) {
            /* adjust this regex to catch the wait time string for COOKIE_HOST */
            String wait = new Regex(correctedBR, "((You have reached the download(\\-| )limit|You have to wait)[^<>]+)").getMatch(0);
            String tmphrs = new Regex(wait, "\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs == null) {
                tmphrs = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            }
            String tmpmin = new Regex(wait, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin == null) {
                tmpmin = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            }
            String tmpsec = new Regex(wait, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(wait, "\\s+(\\d+)\\s+days?").getMatch(0);
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                logger.info("Waittime regexes seem to be broken");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) {
                    hours = Integer.parseInt(tmphrs);
                }
                if (tmpmin != null) {
                    minutes = Integer.parseInt(tmpmin);
                }
                if (tmpsec != null) {
                    seconds = Integer.parseInt(tmpsec);
                }
                if (tmpdays != null) {
                    days = Integer.parseInt(tmpdays);
                }
                int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                /* Not enough wait time to reconnect -> Wait short and retry */
                if (waittime < 180000) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.xfilesharingprobasic.allwait", USERTEXT_ALLWAIT_SHORT), waittime);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
        if (correctedBR.contains("You're using all download slots for IP")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Server error 'You're using all download slots for IP'", 10 * 60 * 1001l);
        }
        if (correctedBR.contains("Error happened when generating Download Link")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Error happened when generating Download Link'", 10 * 60 * 1000l);
        }
        /** Error handling for only-premium links */
        if (new Regex(correctedBR, "( can download files up to |>Upgrade your account to download (?:larger|bigger) files|>The file you requested reached max downloads limit for Free Users|Please Buy Premium To download this file<|This file reached max downloads limit|>This file is available for Premium Users only)").matches()) {
            String filesizelimit = new Regex(correctedBR, "You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                throw new AccountRequiredException("As free user you can download files up to " + filesizelimit + " only");
            } else {
                logger.info("Only downloadable via premium");
                throw new AccountRequiredException();
            }
        } else if (br.getURL().contains(URL_ERROR_PREMIUMONLY)) {
            logger.info("Only downloadable via premium");
            throw new AccountRequiredException();
        } else if (correctedBR.contains(">Expired download session")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Expired download session'", 10 * 60 * 1000l);
        }
        if (new Regex(correctedBR, HTML_MAINTENANCE_MODE).matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, USERTEXT_MAINTENANCE, 2 * 60 * 60 * 1000l);
        }
        checkResponseCodeErrors(br.getHttpConnection());
    }

    /** Handles all kinds of error-responsecodes! */
    private void checkResponseCodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con == null) {
            return;
        }
        final long responsecode = con.getResponseCode();
        if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404#1", 5 * 60 * 1000l);
        } else if (responsecode == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 connection limit reached, please contact our support!", 5 * 60 * 1000l);
        }
    }

    /**
     * Handles all kinds of errors which can happen if we get the final downloadlink but we get html code instead of the file we want to
     * download.
     */
    private void checkServerErrors() throws NumberFormatException, PluginException {
        // dead file
        if (new Regex(correctedBR.trim(), "^No file$").matches()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (new Regex(correctedBR.trim(), "^Wrong IP$").matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'Wrong IP'", 2 * 60 * 60 * 1000l);
        }
        // most likely result of generated link that has expired -raztoki
        if (new Regex(correctedBR.trim(), "(^File Not Found$|<h1>404 Not Found</h1>)").matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error (404)", 30 * 60 * 1000l);
        }
    }

    /**
     * Is intended to handle out of date errors which might occur seldom by re-tring a couple of times before throwing the out of date
     * error.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handlePluginBroken(final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
        int timesFailed = dl.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        if (timesFailed <= maxRetries) {
            logger.info(error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error occured: " + error);
        } else {
            logger.info(error + " -> Plugin is broken");
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        final String space[] = new Regex(correctedBR, ">Used space:</td>.*?<td.*?b>([0-9\\.]+) ?(KB|MB|GB|TB)?</b>").getRow(0);
        if ((space != null && space.length != 0) && (space[0] != null && space[1] != null)) {
            /* free users it's provided by default */
            ai.setUsedSpace(space[0] + " " + space[1]);
        } else if ((space != null && space.length != 0) && space[0] != null) {
            /* premium users the Mb value isn't provided for some reason... */
            ai.setUsedSpace(space[0] + "Mb");
        }
        account.setValid(true);
        /* Traffic can also be negative! */
        final String availabletraffic = new Regex(correctedBR, "Traffic available.*?:</TD><TD><b>([^<>\"']+)</b>").getMatch(0);
        if (availabletraffic != null && !availabletraffic.contains("nlimited") && !availabletraffic.equalsIgnoreCase(" Mb")) {
            availabletraffic.trim();
            /* need to set 0 traffic left, as getSize returns positive result, even when negative value supplied. */
            if (!availabletraffic.startsWith("-")) {
                ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
            } else {
                ai.setTrafficLeft(0);
            }
        } else {
            ai.setUnlimitedTraffic();
        }
        /* If the premium account is expired we'll simply accept it as a free account. */
        final String expire = new Regex(correctedBR, "(\\d{1,2} (January|February|March|April|May|June|July|August|September|October|November|December) \\d{4})").getMatch(0);
        long expire_milliseconds = 0;
        if (expire != null) {
            expire_milliseconds = TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH);
        }
        if ((expire_milliseconds - System.currentTimeMillis()) <= 0) {
            /* Expired premium or no expire date given --> It is usually a Free Account */
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
        } else {
            /* Expire date is in the future --> It is a premium account */
            ai.setValidUntil(expire_milliseconds);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    private Form findLoginform(final Browser br) {
        Form loginform = br.getFormbyProperty("name", "FL");
        if (loginform == null) {
            /* More complicated way to find loginform ... */
            final Form[] allForms = this.br.getForms();
            for (final Form aForm : allForms) {
                final InputField inputFieldOP = aForm.getInputFieldByName("op");
                if (inputFieldOP != null && "login".equalsIgnoreCase(inputFieldOP.getValue())) {
                    loginform = aForm;
                    break;
                }
            }
        }
        return loginform;
    }

    private boolean isLoggedinHTML() {
        return br.containsHTML("op=logout");
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedInViaCookies = false;
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    // if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l) {
                    // /* We trust these cookies as they're not that old --> Do not check them */
                    // return;
                    // }
                    getPage(COOKIE_HOST + "/");
                    loggedInViaCookies = isLoggedinHTML();
                    if (loggedInViaCookies) {
                        /* Save new cookie-timestamp */
                        account.saveCookies(br.getCookies(this.getHost()), "");
                    }
                    if (loggedInViaCookies && !force) {
                        return;
                    }
                }
                if (!loggedInViaCookies) {
                    getPage(COOKIE_HOST + "/login.html");
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        /* Required for some XFS setups. */
                        getPage(COOKIE_HOST + "/login");
                    }
                    Form loginform = findLoginform(this.br);
                    if (loginform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("login", Encoding.urlEncode(account.getUser()));
                    loginform.put("password", Encoding.urlEncode(account.getPass()));
                    if (br.containsHTML("class=\"g\\-recaptcha\"")) {
                        final DownloadLink dlinkbefore = this.getDownloadLink();
                        if (dlinkbefore == null) {
                            this.setDownloadLink(new DownloadLink(this, "Account", this.getHost(), "http://" + account.getHoster(), true));
                        }
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                        if (dlinkbefore != null) {
                            this.setDownloadLink(dlinkbefore);
                        }
                        loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    }
                    submitForm(loginform);
                    /* Missing login cookies or we still have the loginform --> Login failed */
                    final boolean loginCookieOkay = br.getCookie(COOKIE_HOST, "login") != null || br.getCookie(COOKIE_HOST, "xfss") != null;
                    final boolean loginFormOkay = findLoginform(this.br) == null;
                    final boolean loginURLOkay = br.getURL().contains("op=") && !br.getURL().contains("op=login");
                    if (!loginCookieOkay && !loginFormOkay && !loginURLOkay) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (!br.getURL().contains("/?op=my_account")) {
                    getPage("/?op=my_account");
                }
                if (!new Regex(correctedBR, "(Premium(-| )Account expire|>Renew premium<)").matches()) {
                    account.setType(AccountType.FREE);
                } else {
                    account.setType(AccountType.PREMIUM);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        passCode = downloadLink.getStringProperty(PROPERTY_PASS);
        /* Perform linkcheck without logging in */
        requestFileInformation(downloadLink);
        login(account, false);
        if (account.getType() == AccountType.FREE) {
            /* Perform linkcheck after logging in */
            requestFileInformation(downloadLink);
            doFree(downloadLink, true, -5, PROPERTY_DLLINK_ACCOUNT_FREE);
        } else {
            String dllink = checkDirectLink(downloadLink, PROPERTY_DLLINK_ACCOUNT_PREMIUM);
            if (dllink == null) {
                getPage(downloadLink.getPluginPatternMatcher());
                dllink = getDllink();
                if (dllink == null) {
                    final Form dlform = br.getFormbyProperty("name", "F1");
                    if (dlform != null && new Regex(correctedBR, HTML_PASSWORDPROTECTED).matches()) {
                        handlePassword(dlform, downloadLink);
                    }
                    checkErrors(downloadLink, true);
                    if (dlform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    submitForm(dlform);
                    checkErrors(downloadLink, true);
                    dllink = getDllink();
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                checkResponseCodeErrors(dl.getConnection());
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                correctBR();
                checkServerErrors();
                handlePluginBroken(downloadLink, "dllinknofile", 3);
            }
            fixFilename(downloadLink);
            downloadLink.setProperty(PROPERTY_DLLINK_ACCOUNT_PREMIUM, dllink);
            dl.startDownload();
        }
    }

    /**
     * pseudo redirect control!
     */
    @Override
    protected void runPostRequestTask(Browser ibr) throws Exception {
        final String redirect;
        if (!ibr.isFollowingRedirects() && (redirect = ibr.getRedirectLocation()) != null) {
            if (!IMAGEHOSTER) {
                if (!isDllinkFile(redirect)) {
                    super.getPage(ibr, redirect);
                    return;
                }
            } else {
                if (!isDllinkImage(redirect)) {
                    super.getPage(ibr, redirect);
                    return;
                }
            }
        }
    }

    private boolean isDllinkFile(final String url) {
        if (url == null) {
            return false;
        }
        return new Regex(url, Pattern.compile(String.format(dllinkRegexFile, "[A-Za-z0-9\\-\\.]+"), Pattern.CASE_INSENSITIVE)).matches();
    }

    private boolean isDllinkImage(final String url) {
        if (url == null) {
            return false;
        }
        return new Regex(url, Pattern.compile(String.format(dllinkRegexFile, "[A-Za-z0-9\\-\\.]+"), Pattern.CASE_INSENSITIVE)).matches();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }
}