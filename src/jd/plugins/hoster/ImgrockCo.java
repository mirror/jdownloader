//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imgrock.co" }, urls = { "https?://(www\\.)?imgrock\\.(?:net|co)/(embed\\-)?[a-z0-9]{12}" })
public class ImgrockCo extends antiDDoSForHost {
    /* Some HTML code to identify different (error) states */
    private static final String            HTML_PASSWORDPROTECTED          = "<br><b>Passwor(d|t):</b> <input";
    private static final String            HTML_MAINTENANCE_MODE           = ">This server is in maintenance mode";
    /* Here comes our XFS-configuration */
    /* primary website url, take note of redirects */
    private static final String            COOKIE_HOST                     = "http://imgrock.co";
    private static final String            NICE_HOST                       = COOKIE_HOST.replaceAll("(https://|http://)", "");
    private static final String            NICE_HOSTproperty               = COOKIE_HOST.replaceAll("(https://|http://|\\.|\\-)", "");
    /* domain names used within download links */
    private static final String            DOMAINS                         = "(imgrock\\.net|imgrock\\.co)";
    /*
     * If activated, filename can be null - fuid will be used instead then. Also the code will check for imagehosts-continue-POST-forms and
     * check for imagehost final downloadlinks.
     */
    private static final boolean           AUDIOHOSTER                     = false;
    /* If activated, checks if the video is directly available via "vidembed" --> Skips ALL waittimes- and captchas */
    private static final boolean           VIDEOHOSTER                     = false;
    /* If activated, checks if the video is directly available via "embed" --> Skips all waittimes & captcha in most cases */
    private static final boolean           VIDEOHOSTER_2                   = false;
    /* Enable this for imagehosts */
    private static final boolean           IMAGEHOSTER                     = true;
    private static final boolean           SUPPORTS_HTTPS                  = false;
    private static final boolean           SUPPORTS_HTTPS_FORCED           = false;
    private static final boolean           SUPPORTS_AVAILABLECHECK_ALT     = true;
    private static final boolean           SUPPORTS_AVAILABLECHECK_ABUSE   = false;
    private static final boolean           ENABLE_HTML_FILESIZE_CHECK      = false;
    /* Waittime stuff */
    private static final boolean           WAITFORCED                      = false;
    private static final int               WAITSECONDSMIN                  = 3;
    private static final int               WAITSECONDSMAX                  = 100;
    private static final int               WAITSECONDSFORCED               = 5;
    /* Connection stuff */
    private static final boolean           FREE_RESUME                     = true;
    private static final int               FREE_MAXCHUNKS                  = 1;
    private static final int               FREE_MAXDOWNLOADS               = 20;
    private static final boolean           ACCOUNT_FREE_RESUME             = true;
    private static final int               ACCOUNT_FREE_MAXCHUNKS          = 1;
    private static final int               ACCOUNT_FREE_MAXDOWNLOADS       = 20;
    private static final boolean           ACCOUNT_PREMIUM_RESUME          = true;
    private static final int               ACCOUNT_PREMIUM_MAXCHUNKS       = 0;
    private static final int               ACCOUNT_PREMIUM_MAXDOWNLOADS    = 20;
    /* Linktypes */
    private static final String            TYPE_EMBED                      = "https?://[A-Za-z0-9\\-\\.]+/embed\\-[a-z0-9]{12}";
    private static final String            TYPE_NORMAL                     = "https?://[A-Za-z0-9\\-\\.]+/[a-z0-9]{12}";
    private static final String            USERTEXT_ALLWAIT_SHORT          = "Waiting till new downloads can be started";
    private static final String            USERTEXT_MAINTENANCE            = "This server is under maintenance";
    private static final String            USERTEXT_PREMIUMONLY_LINKCHECK  = "Only downloadable via premium or registered";
    /* Properties */
    private static final String            PROPERTY_DLLINK_FREE            = "freelink";
    private static final String            PROPERTY_DLLINK_ACCOUNT_FREE    = "freelink2";
    private static final String            PROPERTY_DLLINK_ACCOUNT_PREMIUM = "premlink";
    private static final String            PROPERTY_PASS                   = "pass";
    /* Used variables */
    private String                         correctedBR                     = "";
    private String                         fuid                            = null;
    private String                         passCode                        = null;
    private static AtomicReference<String> agent                           = new AtomicReference<String>(null);
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger           totalMaxSimultanFreeDownload    = new AtomicInteger(FREE_MAXDOWNLOADS);
    /* don't touch the following! */
    private static AtomicInteger           maxFree                         = new AtomicInteger(1);
    private static AtomicInteger           maxPrem                         = new AtomicInteger(1);
    private static Object                  LOCK                            = new Object();

    /* DEV NOTES */
    // XfileSharingProBasic Version 2.7.0.3
    // Tags: Script, template
    // mods:
    // limit-info:
    // protocol: no https
    // captchatype: null
    // other: related to imgview.net ?
    // TODO: Add case maintenance + alternative filesize check
    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fuid = getFUIDFromURL(link);
        final String protocol;
        /* link cleanup, prefer https if possible */
        if (SUPPORTS_HTTPS || SUPPORTS_HTTPS_FORCED) {
            protocol = "https://";
        } else {
            protocol = "http://";
        }
        final String corrected_downloadurl = protocol + NICE_HOST + "/" + fuid;
        if (link.getDownloadURL().matches(TYPE_EMBED)) {
            final String url_embed = protocol + NICE_HOST + "/embed-" + fuid + ".html";
            /* Make sure user gets the kind of content urls that he added to JD. */
            link.setContentUrl(url_embed);
        }
        link.setUrlDownload(corrected_downloadurl);
    }

    @Override
    public String rewriteHost(String host) {
        if ("imgrock.net".equals(getHost())) {
            if (host == null || "imgrock.net".equals(host)) {
                return "imgrock.co";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/tos.html";
    }

    @SuppressWarnings("deprecation")
    public ImgrockCo(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    @SuppressWarnings({ "deprecation", "unused" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String[] fileInfo = new String[3];
        Browser altbr = null;
        br.setFollowRedirects(true);
        correctDownloadLink(link);
        altbr = br.cloneBrowser();
        setFUID(link);
        getPage(link.getDownloadURL());
        if (br.containsHTML(">Sorry for inconvenience\\.<") && br.containsHTML("<h2>Cannot Load Page\\?\\?\\?</h2>")) {
            final String location = br.getRegex("window\\.location\\s*=\\s*('|\")(.*?)\\1").getMatch(1);
            if (location != null) {
                getPage(location);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (new Regex(correctedBR, "(No such file|>File Not Found<|>The file was removed by|Reason for deletion:\n|File Not Found|>The file expired)").matches()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (new Regex(correctedBR, HTML_MAINTENANCE_MODE).matches()) {
            if (SUPPORTS_AVAILABLECHECK_ABUSE) {
                fileInfo[0] = this.getFnameViaAbuseLink(altbr, link);
                if (fileInfo[0] != null) {
                    link.setName(Encoding.htmlDecode(fileInfo[0]).trim());
                    return AvailableStatus.TRUE;
                }
            }
            link.getLinkStatus().setStatusText(USERTEXT_MAINTENANCE);
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.getURL().contains("/?op=login&redirect=")) {
            logger.info("PREMIUMONLY handling: Trying alternative linkcheck");
            link.getLinkStatus().setStatusText(USERTEXT_PREMIUMONLY_LINKCHECK);
            try {
                if (SUPPORTS_AVAILABLECHECK_ABUSE) {
                    fileInfo[0] = this.getFnameViaAbuseLink(altbr, link);
                    if (altbr.containsHTML(">No such file<")) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
                if (SUPPORTS_AVAILABLECHECK_ALT) {
                    postPage(altbr, COOKIE_HOST + "/?op=checkfiles", "op=checkfiles&process=Check+URLs&list=" + Encoding.urlEncode(link.getDownloadURL()));
                    fileInfo[1] = altbr.getRegex(">" + link.getDownloadURL() + "</td><td style=\"color:green;\">Found</td><td>([^<>\"]*?)</td>").getMatch(0);
                }
                /* 2nd offline check */
                if ((SUPPORTS_AVAILABLECHECK_ALT && altbr.containsHTML("(>" + link.getDownloadURL() + "</td><td style=\"color:red;\">Not found\\!</td>|" + this.fuid + " not found\\!</font>)")) && fileInfo[0] == null) {
                    /* SUPPORTS_AVAILABLECHECK_ABUSE == false and-or could not find any filename. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (fileInfo[0] != null || fileInfo[1] != null) {
                    /* We know the link is online, set all information we got */
                    link.setAvailable(true);
                    if (fileInfo[0] != null) {
                        link.setName(Encoding.htmlDecode(fileInfo[0].trim()));
                    } else {
                        link.setName(fuid);
                    }
                    if (fileInfo[1] != null) {
                        link.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
                    }
                    return AvailableStatus.TRUE;
                }
            } catch (final Throwable e) {
                logger.info("Unknown error occured in alternative linkcheck:");
                e.printStackTrace();
            }
            logger.warning("Alternative linkcheck failed!");
            return AvailableStatus.UNCHECKABLE;
        }
        scanInfo(fileInfo);
        /* Imagehosts often do not show any filenames, at least not on the first page plus they often have their abuse-url disabled. */
        if (IMAGEHOSTER && fileInfo[0] == null) {
            fileInfo[0] = this.fuid;
        }
        if (fileInfo[0] == null || fileInfo[0].equals("")) {
            if (correctedBR.contains("You have reached the download(\\-| )limit")) {
                logger.warning("Waittime detected, please reconnect to make the linkchecker work!");
                return AvailableStatus.UNCHECKABLE;
            }
            logger.warning("filename equals null, throwing \"plugin defect\"");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (fileInfo[2] != null && !fileInfo[2].equals("")) {
            link.setMD5Hash(fileInfo[2].trim());
        }
        fileInfo[0] = fileInfo[0].replaceAll("(</b>|<b>|\\.html)", "");
        link.setName(fileInfo[0].trim());
        if (fileInfo[1] == null && SUPPORTS_AVAILABLECHECK_ALT) {
            /* Do alt availablecheck here but don't check availibility because we already know that the file must be online! */
            logger.info("Filesize not available, trying altAvailablecheck");
            try {
                postPage(altbr, COOKIE_HOST + "/?op=checkfiles", "op=checkfiles&process=Check+URLs&list=" + Encoding.urlEncode(link.getDownloadURL()));
                fileInfo[1] = altbr.getRegex(">" + link.getDownloadURL() + "</td><td style=\"color:green;\">Found</td><td>([^<>\"]*?)</td>").getMatch(0);
            } catch (final Throwable e) {
            }
        }
        if (fileInfo[1] != null && !fileInfo[1].equals("")) {
            link.setDownloadSize(SizeFormatter.getSize(fileInfo[1]));
        }
        return AvailableStatus.TRUE;
    }

    private String[] scanInfo(final String[] fileInfo) {
        final String sharebox0 = "copy\\(this\\);.+>(.+) - ([\\d\\.]+ (?:B|KB|MB|GB))</a></textarea>[\r\n\t ]+</div>";
        final String sharebox1 = "copy\\(this\\);.+\\](.+) - ([\\d\\.]+ (?:B|KB|MB|GB))\\[/URL\\]";
        /* standard traits from base page */
        if (fileInfo[0] == null) {
            fileInfo[0] = new Regex(correctedBR, "You have requested.*?https?://(www\\.)?" + DOMAINS + "/" + fuid + "/(.*?)</font>").getMatch(2);
            if (fileInfo[0] == null) {
                fileInfo[0] = new Regex(correctedBR, "fname\"( type=\"hidden\")? value=\"(.*?)\"").getMatch(1);
                if (fileInfo[0] == null) {
                    fileInfo[0] = new Regex(correctedBR, "<h2>Download File(.*?)</h2>").getMatch(0);
                    /* traits from download1 page below */
                    if (fileInfo[0] == null) {
                        fileInfo[0] = new Regex(correctedBR, "Filename:? ?(<[^>]+> ?)+?([^<>\"\\']+)").getMatch(1);
                        // next two are details from sharing box
                        if (fileInfo[0] == null) {
                            fileInfo[0] = new Regex(correctedBR, sharebox0).getMatch(0);
                            if (fileInfo[0] == null) {
                                fileInfo[0] = new Regex(correctedBR, sharebox1).getMatch(0);
                                if (fileInfo[0] == null) {
                                    /* Link of the box without filesize */
                                    fileInfo[0] = new Regex(correctedBR, "onFocus=\"copy\\(this\\);\">http://(www\\.)?" + DOMAINS + "/" + fuid + "/([^<>\"]*?)</textarea").getMatch(2);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (ENABLE_HTML_FILESIZE_CHECK) {
            if (fileInfo[1] == null) {
                fileInfo[1] = new Regex(correctedBR, "\\(([0-9]+ bytes)\\)").getMatch(0);
                if (fileInfo[1] == null) {
                    fileInfo[1] = new Regex(correctedBR, "</font>[ ]+\\(([^<>\"\\'/]+)\\)(.*?)</font>").getMatch(0);
                    // next two are details from sharing box
                    if (fileInfo[1] == null) {
                        fileInfo[1] = new Regex(correctedBR, sharebox0).getMatch(1);
                        if (fileInfo[1] == null) {
                            fileInfo[1] = new Regex(correctedBR, sharebox1).getMatch(1);
                            // generic failover#1
                            if (fileInfo[1] == null) {
                                fileInfo[1] = new Regex(correctedBR, "(\\d+(?:\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
                            }
                            // generic failover#2
                            if (fileInfo[1] == null) {
                                fileInfo[1] = new Regex(correctedBR, "(\\d+(?:\\.\\d+)? ?(?:B(?:ytes?)?))").getMatch(0);
                            }
                        }
                    }
                }
            }
        }
        if (fileInfo[2] == null) {
            fileInfo[2] = new Regex(correctedBR, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
        }
        return fileInfo;
    }

    private String getFnameViaAbuseLink(final Browser br, final DownloadLink dl) throws Exception {
        getPage(br, "http://" + NICE_HOST + "/?op=report_file&id=" + fuid);
        return br.getRegex("<b>Filename\\s*:?\\s*</b></td><td>([^<>\"]*?)</td>").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, PROPERTY_DLLINK_FREE);
    }

    @SuppressWarnings({ "unused", "deprecation", "static-access" })
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        br.setFollowRedirects(false);
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
                getPage(brv, "/mp3embed-" + fuid);
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
                getPage(brv, "/vidembed-" + fuid);
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
                final String embed_access = "http://" + COOKIE_HOST.replace("http://", "") + "/embed-" + fuid + ".html";
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
                getPage(downloadLink.getDownloadURL());
            }
        }
        /* 6, do we have an imagehost? */
        if (dllink == null && IMAGEHOSTER) {
            checkErrors(downloadLink, false);
            Form imghost_next_form = null;
            do {
                imghost_next_form = this.br.getFormbyKey("next");
                if (imghost_next_form != null) {
                    imghost_next_form.remove("method_premium");
                    /* end of backward compatibility */
                    imghost_next_form = jd.plugins.hoster.ImgmazeCom.fixImghost_next_form(this.br, imghost_next_form);
                    submitForm(imghost_next_form);
                    checkErrors(downloadLink, false);
                    dllink = getDllink();
                    /* For imagehosts, filenames are often not given until we can actually see/download the image! */
                    final String image_filename = new Regex(correctedBR, "class=\"pic\" alt=\"([^<>\"]*?)\"").getMatch(0);
                    if (image_filename != null) {
                        downloadLink.setName(Encoding.htmlDecode(image_filename));
                    }
                }
            } while (imghost_next_form != null);
        }
        /* 7, continue like normal */
        if (dllink == null) {
            final Form download1 = getFormByKey("op", "download1");
            if (download1 != null) {
                download1.remove("method_premium");
                /*
                 * stable is lame, issue finding input data fields correctly. eg. closes at ' quotation mark - remove when jd2 goes stable!
                 */
                if (downloadLink.getName().contains("'")) {
                    String fname = new Regex(br, "<input type=\"hidden\" name=\"fname\" value=\"([^\"]+)\">").getMatch(0);
                    if (fname != null) {
                        download1.put("fname", Encoding.urlEncode(fname));
                    } else {
                        logger.warning("Could not find 'fname'");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
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
                handlePluginBroken(downloadLink, "dlform_f1_null", 3);
            }
            /* how many forms deep do you want to try? */
            int repeat = 2;
            for (int i = 0; i <= repeat; i++) {
                dlForm.remove(null);
                final long timeBefore = System.currentTimeMillis();
                boolean password = false;
                boolean skipWaittime = false;
                if (new Regex(correctedBR, HTML_PASSWORDPROTECTED).matches()) {
                    password = true;
                    logger.info("The downloadlink seems to be password protected.");
                }
                /* md5 can be on the subsequent pages - it is to be found very rare in current XFS versions */
                if (downloadLink.getMD5Hash() == null) {
                    String md5hash = new Regex(correctedBR, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
                    if (md5hash != null) {
                        downloadLink.setMD5Hash(md5hash.trim());
                    }
                }
                /* Captcha START */
                if (correctedBR.contains(";background:#ccc;text-align")) {
                    logger.info("Detected captcha method \"plaintext captchas\" for this host");
                    /* Captcha method by ManiacMansion */
                    final String[][] letters = new Regex(br, "<span style=\\'position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;\\'>(&#\\d+;)</span>").getMatches();
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
                    logger.info("Detected captcha method \"Re Captcha\" for this host");
                    final Recaptcha rc = new Recaptcha(br, this);
                    rc.findID();
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, downloadLink);
                    dlForm.put("recaptcha_challenge_field", rc.getChallenge());
                    dlForm.put("recaptcha_response_field", Encoding.urlEncode(c));
                    logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
                    /* wait time is usually skippable for reCaptcha handling */
                    skipWaittime = true;
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
                if (password) {
                    passCode = handlePassword(dlForm, downloadLink);
                }
                if (!skipWaittime) {
                    waitTime(timeBefore, downloadLink);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5 * 60 * 1000l);
            }
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

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            /* define custom browser headers and language settings */
            prepBr.setCookie(COOKIE_HOST, "lang", "english");
        }
        return prepBr;
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
        correctedBR = jd.plugins.hoster.ImgmazeCom.correctBR(this.br);
    }

    private String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(correctedBR, "(\"|\\')(https?://(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|([\\w\\-\\.]+\\.)?" + DOMAINS + ")(:\\d{1,4})?/(files|d|cgi\\-bin/dl\\.cgi)/(\\d+/)?[a-z0-9]+/[^<>\"/]*?)(\"|\\')").getMatch(1);
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
            /* Sometimes used for streaming */
            dllink = new Regex(correctedBR, "file:[\t\n\r ]*?\"(http[^<>\"]*?\\.(?:mp4|flv))\"").getMatch(0);
        }
        if (dllink == null && IMAGEHOSTER) {
            /* Used for image-hosts */
            dllink = new Regex(correctedBR, "(https?://(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|([\\w\\-\\.]+\\.)?" + DOMAINS + ")/img/[^<>\"\\']+)").getMatch(0);
            if (dllink == null) {
                dllink = new Regex(correctedBR, "(https?://[^/]+/img/\\d+/[^<>\"\\']+)").getMatch(0);
            }
            if (dllink == null) {
                dllink = new Regex(correctedBR, "(https?://[^/]+/img/[a-z0-9]+/[^<>\"\\']+)").getMatch(0);
            }
            if (dllink == null) {
                dllink = new Regex(correctedBR, "(https?://(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|([\\w\\-\\.]+\\.)?" + DOMAINS + ")/i/\\d+/[^<>\"\\']+(?!_t\\.[A-Za-z]{3,4}))").getMatch(0);
            }
            if (dllink == null) {
                dllink = new Regex(correctedBR, "(https?://[^/]+/i/\\d+/[^<>\"\\']+(?!_t\\.[A-Za-z]{3,4}))").getMatch(0);
            }
            if (dllink != null && dllink.matches(".+_t\\.[A-Za-z]{3,4}$")) {
                /* Do NOT download thumbnails! */
                dllink = null;
            }
        }
        return dllink;
    }

    private String decodeDownloadLink(final String s) {
        String decoded = null;
        try {
            Regex params = new Regex(s, "\\'(.*?[^\\\\])\\',(\\d+),(\\d+),\\'(.*?)\\'");
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
            finallink = new Regex(decoded, "(\"|\\')(https?://[^<>\"\\']*?\\.(avi|flv|mkv|mp4))(\"|\\')").getMatch(1);
        }
        return finallink;
    }

    @Override
    protected void getPage(final String page) throws Exception {
        super.getPage(page);
        correctBR();
    }

    @Override
    protected void postPage(final String page, final String postdata) throws Exception {
        super.postPage(page, postdata);
        correctBR();
    }

    @Override
    protected void submitForm(final Form form) throws Exception {
        super.submitForm(form);
        correctBR();
    }

    /**
     * Handles pre download (pre-captcha) waittime. If WAITFORCED it ensures to always wait long enough even if the waittime RegEx fails.
     */
    @SuppressWarnings("unused")
    private void waitTime(long timeBefore, final DownloadLink downloadLink) throws PluginException {
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
        if (ttt != null) {
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
            sleep(wait * 1000l, downloadLink);
        }
    }

    // TODO: remove this when v2 becomes stable. use br.getFormbyKey(String key, String value)
    /**
     * Returns the first form that has a 'key' that equals 'value'.
     *
     * @param key
     * @param value
     * @return
     */
    private Form getFormByKey(final String key, final String value) {
        Form[] workaround = br.getForms();
        if (workaround != null) {
            for (Form f : workaround) {
                for (InputField field : f.getInputFields()) {
                    if (key != null && key.equals(field.getKey())) {
                        if (value == null && field.getValue() == null) {
                            return f;
                        }
                        if (value != null && value.equals(field.getValue())) {
                            return f;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * This fixes filenames from all xfs modules: file hoster, audio/video streaming (including transcoded video), or blocked link checking
     * which is based on fuid.
     *
     * @version 0.2
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
            orgName = new Regex(orgNameExt, "(.+)" + orgExt).getMatch(0);
        } else {
            orgName = orgNameExt;
        }
        // if (orgName.endsWith("...")) orgName = orgName.replaceFirst("\\.\\.\\.$", "");
        String servNameExt = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        if (!inValidate(servNameExt) && servNameExt.contains(".")) {
            servExt = servNameExt.substring(servNameExt.lastIndexOf("."));
            servName = new Regex(servNameExt, "(.+)" + servExt).getMatch(0);
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

    private void setFUID(final DownloadLink dl) {
        fuid = getFUIDFromURL(dl);
    }

    @SuppressWarnings("deprecation")
    private String getFUIDFromURL(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([a-z0-9]{12})$").getMatch(0);
    }

    private String handlePassword(final Form pwform, final DownloadLink thelink) throws PluginException {
        if (passCode == null) {
            passCode = Plugin.getUserInput("Password?", thelink);
        }
        if (passCode == null || passCode.equals("")) {
            logger.info("User has entered blank password, exiting handlePassword");
            passCode = null;
            thelink.setProperty(PROPERTY_PASS, Property.NULL);
            return null;
        }
        if (pwform == null) {
            /* so we know handlePassword triggered without any form */
            logger.info("Password Form == null");
        } else {
            logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
            pwform.put("password", Encoding.urlEncode(passCode));
        }
        thelink.setProperty(PROPERTY_PASS, passCode);
        return passCode;
    }

    private void checkErrors(final DownloadLink theLink, final boolean checkAll) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (new Regex(correctedBR, HTML_PASSWORDPROTECTED).matches() && correctedBR.contains("Wrong password")) {
                /* handle password has failed in the past, additional try catching / resetting values */
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                passCode = null;
                theLink.setProperty(PROPERTY_PASS, Property.NULL);
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
            String WAIT = new Regex(correctedBR, "((You have reached the download(\\-| )limit|You have to wait)[^<>]+)").getMatch(0);
            String tmphrs = new Regex(WAIT, "\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs == null) {
                tmphrs = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            }
            String tmpmin = new Regex(WAIT, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin == null) {
                tmpmin = new Regex(correctedBR, "You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            }
            String tmpsec = new Regex(WAIT, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(WAIT, "\\s+(\\d+)\\s+days?").getMatch(0);
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
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        }
        if (correctedBR.contains("Error happened when generating Download Link")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 10 * 60 * 1000l);
        }
        /** Error handling for only-premium links */
        if (new Regex(correctedBR, "( can download files up to |Upgrade your account to download bigger files|>Upgrade your account to download (?:larger|bigger) files|>The file you requested reached max downloads limit for Free Users|Please Buy Premium To download this file<|This file reached max downloads limit|>This file is available for Premium Users only)").matches()) {
            String filesizelimit = new Regex(correctedBR, "You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.info("As free user you can download files up to " + filesizelimit + " only");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else {
                logger.info("Only downloadable via premium");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
        } else if (br.getURL().contains("/?op=login&redirect=")) {
            logger.info("Only downloadable via premium");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (new Regex(correctedBR, HTML_MAINTENANCE_MODE).matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, USERTEXT_MAINTENANCE, 2 * 60 * 60 * 1000l);
        }
    }

    private void checkServerErrors() throws NumberFormatException, PluginException {
        if (new Regex(correctedBR, Pattern.compile("No file", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'no file'", 2 * 60 * 60 * 1000l);
        }
        if (new Regex(correctedBR, Pattern.compile("Wrong IP", Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'Wrong IP'", 2 * 60 * 60 * 1000l);
        }
        if (new Regex(correctedBR, "(File Not Found|<h1>404 Not Found</h1>)").matches()) {
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
        dl.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error occured: " + error);
        } else {
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Plugin is broken");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    // @SuppressWarnings("deprecation")
    // @Override
    // public AccountInfo fetchAccountInfo(final Account account) throws Exception {
    // final AccountInfo ai = new AccountInfo();
    // /* reset maxPrem workaround on every fetchaccount info */
    // maxPrem.set(1);
    // try {
    // login(account, true);
    // } catch (final PluginException e) {
    // account.setValid(false);
    // throw e;
    // }
    // final String space[] = new Regex(correctedBR, ">Used space:</td>.*?<td.*?b>([0-9\\.]+) ?(KB|MB|GB|TB)?</b>").getRow(0);
    // if ((space != null && space.length != 0) && (space[0] != null && space[1] != null)) {
    // /* free users it's provided by default */
    // ai.setUsedSpace(space[0] + " " + space[1]);
    // } else if ((space != null && space.length != 0) && space[0] != null) {
    // /* premium users the Mb value isn't provided for some reason... */
    // ai.setUsedSpace(space[0] + "Mb");
    // }
    // account.setValid(true);
    // final String availabletraffic = new Regex(correctedBR, "Traffic available.*?:</TD><TD><b>([^<>\"\\']+)</b>").getMatch(0);
    // if (availabletraffic != null && !availabletraffic.contains("nlimited") && !availabletraffic.equalsIgnoreCase(" Mb")) {
    // availabletraffic.trim();
    // /* need to set 0 traffic left, as getSize returns positive result, even when negative value supplied. */
    // if (!availabletraffic.startsWith("-")) {
    // ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
    // } else {
    // ai.setTrafficLeft(0);
    // }
    // } else {
    // ai.setUnlimitedTraffic();
    // }
    // /* If the premium account is expired we'll simply accept it as a free account. */
    // final String expire = new Regex(correctedBR,
    // "(\\d{1,2} (January|February|March|April|May|June|July|August|September|October|November|December) \\d{4})").getMatch(0);
    // long expire_milliseconds = 0;
    // if (expire != null) {
    // expire_milliseconds = TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH);
    // }
    // if ((expire_milliseconds - System.currentTimeMillis()) <= 0) {
    // maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
    // account.setType(AccountType.FREE);
    // account.setMaxSimultanDownloads(maxPrem.get());
    // account.setConcurrentUsePossible(false);
    // ai.setStatus("Registered (free) account");
    // } else {
    // ai.setValidUntil(expire_milliseconds);
    // maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
    // account.setType(AccountType.PREMIUM);
    // account.setMaxSimultanDownloads(maxPrem.get());
    // account.setConcurrentUsePossible(true);
    // ai.setStatus("Premium account");
    // }
    // return ai;
    // }
    //
    // private void login(final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // try {
    // /* Load cookies */
    // br.setCookiesExclusive(true);
    // prepBrowser(br);
    // final Cookies cookies = account.loadCookies("");
    // if (cookies != null && !force) {
    // this.br.setCookies(this.getHost(), cookies);
    // return;
    // }
    // br.setFollowRedirects(true);
    // getPage(COOKIE_HOST + "/login.html");
    // final Form loginform = br.getFormbyProperty("name", "FL");
    // if (loginform == null) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // }
    // loginform.put("login", Encoding.urlEncode(account.getUser()));
    // loginform.put("password", Encoding.urlEncode(account.getPass()));
    // submitForm(loginform);
    // if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nUngültiger Benutzername, Passwort oder login Captcha!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort
    // stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es
    // erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nBłędny użytkownik/hasło lub kod Captcha wymagany do zalogowania!\r\nUpewnij się, że prawidłowo wprowadziłes hasło i nazwę
    // użytkownika. Dodatkowo:\r\n1. Jeśli twoje hasło zawiera znaki specjalne, zmień je (usuń) i spróbuj ponownie!\r\n2. Wprowadź hasło i
    // nazwę użytkownika ręcznie bez użycia opcji Kopiuj i Wklej.",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nInvalid username/password or login captcha!\r\nYou're sure that the username and password you entered are correct? Some
    // hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your
    // username/password by hand without copy & paste.",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // }
    // if (!br.getURL().contains("/?op=my_account")) {
    // getPage("/?op=my_account");
    // }
    // if (!new Regex(correctedBR, "(Premium(\\-| )Account expire|>Renew premium<)").matches()) {
    // account.setType(AccountType.FREE);
    // } else {
    // account.setType(AccountType.PREMIUM);
    // }
    // account.saveCookies(this.br.getCookies(this.getHost()), "");
    // } catch (final PluginException e) {
    // account.clearCookies("");
    // throw e;
    // }
    // }
    // }
    //
    // @SuppressWarnings("deprecation")
    // @Override
    // public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
    // passCode = downloadLink.getStringProperty(PROPERTY_PASS);
    // requestFileInformation(downloadLink);
    // login(account, false);
    // if (account.getType() == AccountType.FREE) {
    // requestFileInformation(downloadLink);
    // doFree(downloadLink, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, PROPERTY_DLLINK_ACCOUNT_FREE);
    // } else {
    // String dllink = checkDirectLink(downloadLink, PROPERTY_DLLINK_ACCOUNT_PREMIUM);
    // if (dllink == null) {
    // br.setFollowRedirects(false);
    // getPage(downloadLink.getDownloadURL());
    // dllink = getDllink();
    // if (dllink == null) {
    // Form dlform = br.getFormbyProperty("name", "F1");
    // if (dlform != null && new Regex(correctedBR, HTML_PASSWORDPROTECTED).matches()) {
    // passCode = handlePassword(dlform, downloadLink);
    // }
    // checkErrors(downloadLink, true);
    // if (dlform == null) {
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // submitForm(dlform);
    // checkErrors(downloadLink, true);
    // dllink = getDllink();
    // }
    // }
    // if (dllink == null) {
    // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // logger.info("Final downloadlink = " + dllink + " starting the download...");
    // dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
    // if (dl.getConnection().getContentType().contains("html")) {
    // if (dl.getConnection().getResponseCode() == 503) {
    // throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5
    // * 60 * 1000l);
    // }
    // logger.warning("The final dllink seems not to be a file!");
    // br.followConnection();
    // correctBR();
    // checkServerErrors();
    // handlePluginBroken(downloadLink, "dllinknofile", 3);
    // }
    // fixFilename(downloadLink);
    // downloadLink.setProperty(PROPERTY_DLLINK_ACCOUNT_PREMIUM, dllink);
    // dl.startDownload();
    // }
    // }
    //
    // @Override
    // public int getMaxSimultanPremiumDownloadNum() {
    // /* workaround for free/premium issue on stable 09581 */
    // return maxPrem.get();
    // }
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