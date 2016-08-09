//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogSource;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vimeo.com" }, urls = { "https?://(www\\.)?vimeo\\.com/(\\d+|channels/[a-z0-9\\-_]+/\\d+|[A-Za-z0-9\\-_]+/videos|ondemand/[A-Za-z0-9\\-_]+)|https?://player\\.vimeo.com/(?:video|external)/\\d+.+" }, flags = { 0 })
public class VimeoComDecrypter extends PluginForDecrypt {

    private static final String type_player_private_external_direct = "https?://player\\.vimeo.com/external/\\d+\\.[A-Za-z]{1,5}\\.mp4.+";
    private static final String type_player_private_external        = "https?://player\\.vimeo.com/external/\\d+(\\&forced_referer=[A-Za-z0-9=]+)?";
    private static final String type_player_private_forced_referer  = "https?://player\\.vimeo.com/video/\\d+.*?(\\&|\\?)forced_referer=[A-Za-z0-9=]+";
    public static final String  type_player                         = "https?://player\\.vimeo.com/video/\\d+.+";
    private static final String Q_MOBILE                            = "Q_MOBILE";
    private static final String Q_ORIGINAL                          = "Q_ORIGINAL";
    private static final String Q_HD                                = "Q_HD";
    private static final String Q_SD                                = "Q_SD";
    private static final String Q_BEST                              = "Q_BEST";
    private String              password                            = null;

    public VimeoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String LINKTYPE_USER = "https?://(www\\.)?vimeo\\.com/[A-Za-z0-9\\-_]+/videos";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("http://", "https://");
        if (parameter.matches(type_player_private_external_direct)) {
            final DownloadLink link;
            decryptedLinks.add(link = this.createDownloadlink("directhttp://" + parameter.replaceFirst("%20.+$", "")));
            final String fileName = Plugin.getFileNameFromURL(new URL(parameter));
            link.setForcedFileName(fileName);
            link.setFinalFileName(fileName);
            return decryptedLinks;
        } else if (parameter.matches(type_player_private_external)) {
            parameter = parameter.replace("/external/", "/video/");
        } else if (!parameter.matches(type_player_private_forced_referer) && parameter.matches(type_player)) {
            parameter = "https://vimeo.com/" + parameter.substring(parameter.lastIndexOf("/") + 1);
        }
        final SubConfiguration cfg = SubConfiguration.getConfig("vimeo.com");
        // when testing and dropping to frame, components will fail without clean browser.
        br = new Browser();
        setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400, 410 });
        if (parameter.matches(LINKTYPE_USER)) {
            br.getPage(parameter);
            if (br.containsHTML(">\\s*We couldn(?:'|&rsquo;)t find that page") || this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(createOfflinelink(parameter, "Could not find that page"));
                return decryptedLinks;
            }

            final String user_id = new Regex(parameter, "vimeo\\.com/([A-Za-z0-9\\-_]+)/videos").getMatch(0);
            String userName = br.getRegex(">Here are all of the videos that <a href=\"/user\\d+\">([^<>\"]*?)</a> has uploaded to Vimeo").getMatch(0);
            if (userName == null) {
                userName = user_id;
            }
            final String totalVideoNum = br.getRegex(">(\\d+(,\\d+)?) Total</a>").getMatch(0);
            int totalPages = 1;
            final String[] pages = br.getRegex("/videos/page:(\\d+)/").getColumn(0);
            if (pages != null && pages.length != 0) {
                for (final String apage : pages) {
                    final int currentp = Integer.parseInt(apage);
                    if (currentp > totalPages) {
                        totalPages = currentp;
                    }
                }
            }
            if (totalVideoNum == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final int totalVids = Integer.parseInt(totalVideoNum.replace(",", ""));
            for (int i = 1; i <= totalPages; i++) {
                if (this.isAbort()) {
                    logger.info("Decrypt process aborted by user: " + parameter);
                    return decryptedLinks;
                }
                if (i > 1) {
                    br.getPage("/" + user_id + "/videos/page:" + i + "/sort:date/format:detail");
                }
                final String[] videoIDs = br.getRegex("id=\"clip_(\\d+)\"").getColumn(0);
                if (videoIDs == null || videoIDs.length == 0) {
                    logger.info("Found no videos on current page -> Stopping");
                    break;
                }
                for (final String videoID : videoIDs) {
                    final DownloadLink fina = createDownloadlink("http://vimeo.com/" + videoID);
                    decryptedLinks.add(fina);
                }
                logger.info("Decrypted page: " + i + " of " + totalPages);
                logger.info("Found " + videoIDs.length + " videolinks on current page");
                logger.info("Found " + decryptedLinks.size() + " of " + totalVids + " total videolinks");
                if (decryptedLinks.size() >= totalVids) {
                    logger.info("Decrypted all videos, stopping");
                    break;
                }
            }
            logger.info("Decrypt done! Total amount of decrypted videolinks: " + decryptedLinks.size() + " of " + totalVids);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName("Videos of vimeo.com user " + userName);
            fp.addLinks(decryptedLinks);
        } else {
            /* Check if we got a forced Referer - if so, extract it, clean url, use it and set it on our DownloadLinks for later usage. */
            String vimeo_forced_referer = null;
            final String vimeo_forced_referer_url_part = new Regex(parameter, "(\\&forced_referer=.+)").getMatch(0);
            if (vimeo_forced_referer_url_part != null) {
                parameter = parameter.replace(vimeo_forced_referer_url_part, "");
                vimeo_forced_referer = Encoding.Base64Decode(new Regex(vimeo_forced_referer_url_part, "forced_referer=(.+)").getMatch(0));
            }
            final boolean new_way_allowed = true;
            final String ID = new Regex(parameter, "/(\\d+)").getMatch(0);
            String date = null;
            String channelName = null;
            String title = null;
            final String cleanVimeoURL;
            if (ID != null && !StringUtils.containsIgnoreCase(parameter, "/ondemand/")) {
                cleanVimeoURL = "https://vimeo.com/" + ID;
            } else {
                cleanVimeoURL = parameter;
            }
            /*
             * We used to simply change the vimeo.com/player/XXX links to normal vimeo.com/XXX links but in some cases, videos can only be
             * accessed via their 'player'-link with a specified Referer - if the referer is not given in such a case the site will say that
             * our video would be a private video.
             */
            final String orgParam = param.toString();
            if ((orgParam.matches(type_player_private_forced_referer) || orgParam.matches(type_player)) && new_way_allowed) {
                if (vimeo_forced_referer != null) {
                    br.getHeaders().put("Referer", vimeo_forced_referer);
                }
                /* We HAVE TO access the url via player.vimeo.com (with the correct Referer) otherwise we will only receive 403/404! */
                br.getPage("https://player.vimeo.com/video/" + ID);
                if (vimeo_forced_referer == null && br.getHttpConnection().getResponseCode() == 403) {
                    CrawledLink check = getCurrentLink().getSourceLink();
                    while (true) {
                        vimeo_forced_referer = check.getURL();
                        if (check == check.getSourceLink() || !StringUtils.equalsIgnoreCase(Browser.getHost(vimeo_forced_referer), "vimeo.com")) {
                            break;
                        } else {
                            check = check.getSourceLink();
                        }
                    }
                    if (!StringUtils.equalsIgnoreCase(Browser.getHost(vimeo_forced_referer), "vimeo.com")) {
                        br.getHeaders().put("Referer", vimeo_forced_referer);
                        br.getPage("https://player.vimeo.com/video/" + ID);
                    }
                }
                if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || "This video does not exist\\.".equals(PluginJSonUtils.getJson(br, "message"))) {
                    decryptedLinks.add(createOfflinelink(orgParam, ID, null));
                    return decryptedLinks;
                }
                if (br.containsHTML(containsPass())) {
                    try {
                        handlePW(param, ID, this.br);
                    } catch (final DecrypterException edc) {
                        logger.info("User entered too many wrong passwords --> Cannot decrypt link: " + parameter);
                        decryptedLinks.add(createOfflinelink(parameter, ID, null));
                        return decryptedLinks;
                    }
                }
                parameter = orgParam;
                final String owner_json = br.getRegex("\"owner\":\\{(.*?)\\}").getMatch(0);
                if (owner_json != null) {
                    channelName = PluginJSonUtils.getJson(owner_json, "name");
                }
            } else {
                // maybe required
                // br.setCookie(this.getHost(), "player", "");
                parameter = cleanVimeoURL;
                if (StringUtils.containsIgnoreCase(parameter, "/ondemand/")) {
                    final ArrayList<Account> accs = AccountController.getInstance().getValidAccounts(getHost());
                    if (accs != null) {
                        // not optimized
                        final Account account = accs.get(0);
                        br.getPage("https://www.vimeo.com/log_in");
                        final String xsrft = getXsrft(br);
                        // static post are bad idea, always use form.
                        final Form login = br.getFormbyProperty("id", "login_form");
                        if (login == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        login.put("token", Encoding.urlEncode(xsrft));
                        login.put("email", Encoding.urlEncode(account.getUser()));
                        login.put("password", Encoding.urlEncode(account.getPass()));
                        br.submitForm(login);
                        if (br.getCookie("http://vimeo.com", "vimeo") == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    } else {
                        return decryptedLinks;
                    }
                }
                try {
                    br.getPage(parameter);
                } catch (final BrowserException e) {
                    // HTTP/1.1 451 Unavailable For Legal Reasons
                    if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 451) {
                        decryptedLinks.add(createOfflinelink(parameter, ID, null));
                        return decryptedLinks;
                    }
                    throw e;
                }

                /* Workaround for User from Iran */
                if (br.containsHTML("<body><iframe src=\"http://10\\.10\\.\\d+\\.\\d+\\?type=(Invalid Site)?\\&policy=MainPolicy")) {
                    br.getPage("//player.vimeo.com/config/" + ID);
                }

                if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("Page not found|This video does not exist|>We couldn't find that page|>Sorry, there is no video here\\.<|>Either it was deleted or it never existed in the first place")) {
                    decryptedLinks.add(createOfflinelink(parameter, ID, null));
                    return decryptedLinks;
                }

                if (br.containsHTML(containsPass())) {
                    try {
                        handlePW(param, ID, br);
                        /*
                         * After successful password input we'll get json but we want the "normal" html which suits the code below -->
                         * Access main video url again!
                         */
                        br.getPage(parameter);
                    } catch (final DecrypterException edc) {
                        logger.info("User entered too many wrong passwords --> Cannot decrypt link: " + parameter);
                        decryptedLinks.add(createOfflinelink(parameter, ID, null));
                        return decryptedLinks;
                    }
                }

                if (br.containsHTML(">There was a problem loading this video")) {
                    decryptedLinks.add(createOfflinelink(parameter, ID, null));
                    return decryptedLinks;
                }
                // document.cookie = 'vuid=' + encodeURIComponent('35533916.335958829')
                final String vuid = br.getRegex("document\\.cookie\\s*=\\s*'vuid='\\s*\\+\\s*encodeURIComponent\\('(\\d+\\.\\d+)'\\)").getMatch(0);
                if (vuid != null) {
                    br.setCookie(br.getURL(), "vuid", vuid);
                }

                date = br.getRegex("datetime=\"(\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2})").getMatch(0);
                channelName = br.getRegex("\"Person\",\"name\":\"([^<>\"]*?)\"").getMatch(0);
                if (channelName == null) {
                    channelName = br.getRegex("rel=\"author\" href=\"/[^<>\"]+\">([^<>\"]*?)</a>").getMatch(0);
                }
            }
            title = getTitle(br);
            if (channelName != null) {
                channelName = getFormattedString(channelName);
            }
            title = getFormattedString(title);

            String qualities[][] = getQualities(br, ID);
            if (qualities == null) {
                return null;
            }
            // qx[0] = url
            // qx[1] = extension
            // qx[2] = format (mobile|sd|hd)
            // qx[3] = frameSize (\d+x\d+)
            // qx[4] = bitrate (\d+)
            // qx[5] = fileSize (\d [a-zA-Z]{2})
            // qx[6] = Codec
            // qx[7] = ID
            ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
            HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
            int format = 0;
            for (String quality[] : qualities) {
                String url = quality[0];
                String fmt = quality[2];
                if (fmt != null) {
                    fmt = fmt.toLowerCase(Locale.ENGLISH).trim();
                }
                if (fmt != null) {
                    /* best selection is done at the end */
                    if (fmt.contains("mobile")) {
                        if (cfg.getBooleanProperty(Q_MOBILE, true) == false) {
                            continue;
                        } else {
                            fmt = "mobile";
                            format = 1;
                        }
                    } else if (fmt.contains("hd")) {
                        if (cfg.getBooleanProperty(Q_HD, true) == false) {
                            continue;
                        } else {
                            fmt = "hd";
                            format = 2;
                        }
                    } else if (fmt.contains("sd")) {
                        if (cfg.getBooleanProperty(Q_SD, true) == false) {
                            continue;
                        } else {
                            fmt = "sd";
                            format = 3;
                        }
                    } else if (fmt.contains("original")) {
                        if (cfg.getBooleanProperty(Q_ORIGINAL, true) == false) {
                            continue;
                        } else {
                            fmt = "original";
                            format = 4;
                        }
                    }
                }
                if (url == null) {
                    continue;
                }
                if (!url.startsWith("http")) {
                    if (!url.startsWith("/")) {
                        url = "https://vimeo.com/" + url;
                    } else {
                        url = "https://vimeo.com" + url;
                    }
                }
                // there can be multiple hd/sd etc need to identify with framesize.
                final String linkdupeid = ID + "_" + fmt + "_" + quality[3] + (StringUtils.isNotEmpty(quality[7]) ? "_" + quality[7] : "") + quality[8];
                final DownloadLink link = createDownloadlink(parameter.replaceAll("https?://", "decryptedforVimeoHosterPlugin" + format + "://"));
                link.setProperty("directURL", url);
                // videoTitle is required!
                link.setProperty("videoTitle", title);
                link.setProperty("videoQuality", fmt);
                link.setProperty("videoExt", quality[1]);
                link.setProperty("videoID", ID);
                link.setProperty("videoFrameSize", quality[3]);
                link.setProperty("videoBitrate", quality[4]);
                link.setProperty("videoCodec", quality[6]);
                link.setProperty("videoType", quality[8]);
                link.setLinkID(linkdupeid);
                link.setContentUrl(cleanVimeoURL);
                if (password != null) {
                    link.setProperty("pass", password);
                }
                if (parameter.matches(type_player_private_forced_referer)) {
                    link.setProperty("private_player_link", true);
                }
                if (vimeo_forced_referer != null) {
                    link.setProperty("vimeo_forced_referer", vimeo_forced_referer);
                }
                if (date != null) {
                    link.setProperty("originalDate", date);
                }
                if (channelName != null) {
                    link.setProperty("channel", channelName);
                }
                link.setFinalFileName(getFormattedFilename(link));

                if (quality[5] != null) {
                    link.setDownloadSize(SizeFormatter.getSize(quality[5].trim()));
                }
                link.setAvailable(true);
                final DownloadLink best = bestMap.get(fmt);
                if (best == null || link.getDownloadSize() > best.getDownloadSize()) {
                    bestMap.put(fmt, link);
                }
                newRet.add(link);
            }
            if (newRet.size() > 0) {
                if (cfg.getBooleanProperty(Q_BEST, false)) {
                    /* only keep best quality */
                    DownloadLink keep = bestMap.get("original");
                    if (keep == null) {
                        keep = bestMap.get("hd");
                    }
                    if (keep == null) {
                        keep = bestMap.get("sd");
                    }
                    if (keep == null) {
                        keep = bestMap.get("mobile");
                    }
                    if (keep != null) {
                        newRet.clear();
                        newRet.add(keep);
                    }
                }
                if (newRet.size() > 1) {
                    String fpName = "";
                    if (channelName != null) {
                        fpName += Encoding.htmlDecode(channelName.trim()) + " - ";
                    }
                    if (date != null) {
                        try {
                            final String userDefinedDateFormat = cfg.getStringProperty("CUSTOM_DATE_3", "dd.MM.yyyy_HH-mm-ss");
                            final String[] dateStuff = date.split("T");
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss");
                            Date dateStr = formatter.parse(dateStuff[0] + ":" + dateStuff[1]);
                            String formattedDate = formatter.format(dateStr);
                            Date theDate = formatter.parse(formattedDate);
                            formatter = new SimpleDateFormat(userDefinedDateFormat);
                            formattedDate = formatter.format(theDate);
                            fpName += formattedDate + " - ";
                        } catch (final Throwable e) {
                            LogSource.exception(logger, e);
                        }
                    }
                    fpName += title;

                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName);
                    fp.addLinks(newRet);
                }
                decryptedLinks.addAll(newRet);
            }
        }

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private String containsPass() throws PluginException {
        pluginLoaded();
        return jd.plugins.hoster.VimeoCom.containsPass;
    }

    private String getFormattedString(final String s) throws PluginException {
        pluginLoaded();
        return ((jd.plugins.hoster.VimeoCom) vimeo_hostPlugin).getFormattedString(s);
    }

    private String getTitle(final Browser ibr) throws PluginException {
        pluginLoaded();
        return ((jd.plugins.hoster.VimeoCom) vimeo_hostPlugin).getTitle(ibr);
    }

    private Browser prepBrowser(final Browser ibr) throws PluginException {
        pluginLoaded();
        return ((jd.plugins.hoster.VimeoCom) vimeo_hostPlugin).prepBrGeneral(null, ibr);
    }

    private PluginForHost vimeo_hostPlugin = null;

    private String[][] getQualities(final Browser ibr, final String ID) throws Exception {
        pluginLoaded();
        return jd.plugins.hoster.VimeoCom.getQualities(ibr, ID);
    }

    private String getXsrft(final Browser br) throws Exception {
        pluginLoaded();
        return ((jd.plugins.hoster.VimeoCom) vimeo_hostPlugin).getXsrft(br);
    }

    private String getFormattedFilename(DownloadLink link) throws Exception {
        pluginLoaded();
        return ((jd.plugins.hoster.VimeoCom) vimeo_hostPlugin).getFormattedFilename(link);
    }

    private void pluginLoaded() throws PluginException {
        if (vimeo_hostPlugin == null) {
            vimeo_hostPlugin = JDUtilities.getPluginForHost("vimeo.com");
            if (vimeo_hostPlugin == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private void handlePW(final CryptedLink param, final String videoID, final Browser br) throws Exception {
        // check for a password. Store latest password in DB
        Form pwForm = null;
        /* Try stored password first */
        password = getPluginConfig().getStringProperty("lastusedpass", null);
        final String videourl = "https://player.vimeo.com/video/" + videoID;
        boolean failed = true;

        // lastusedpasswd == null, or lastusedpasswd is wrong
        for (int i = 0; i < 3; i++) {
            if (i > 0) {
                br.getPage(videourl);
            }
            pwForm = getPasswordForm(br);
            pwForm.setAction("https://player.vimeo.com/video/" + videoID + "/check-password");
            /* 2016-06-09: Seems like token is no longer needed! */
            // pwForm.put("token", getXsrft(br));
            if (password == null) {
                password = Plugin.getUserInput("Password for link: " + param.toString() + " ?", param);
            }
            if (password == null || "".equals(password)) {
                // empty pass?? not good...
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            pwForm.put("password", Encoding.urlEncode(password));
            try {
                br.submitForm(pwForm);
            } catch (final Throwable e) {
                /* HTTP/1.1 418 I'm a teapot --> lol */
                if (br.getHttpConnection().getResponseCode() == 401 || br.getHttpConnection().getResponseCode() == 418) {
                    logger.warning("Wrong password for Link: " + param.toString());
                    if (i < 2) {
                        password = null;
                        continue;
                    } else {
                        logger.warning("Exausted password retry count. " + param.toString());
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                }
            }
            if (br.containsHTML(containsPass()) || br.getHttpConnection().getResponseCode() == 405 || "false".equalsIgnoreCase(br.toString())) {
                password = null;
                continue;
            }
            failed = false;
            getPluginConfig().setProperty("lastusedpass", password);
            getPluginConfig().save();
            break;
        }

        if (failed) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
    }

    private Form getPasswordForm(final Browser br) {
        Form pwForm = br.getFormbyProperty("id", "pw_form");
        if (pwForm == null) {
            pwForm = new Form();
            pwForm.setMethod(MethodType.POST);
            pwForm.setAction(br.getURL());
        }
        return pwForm;
    }

    public static String createPrivateVideoUrlWithReferer(final String vimeo_video_id, final String referer_url) {
        final String private_vimeo_url = "https://player.vimeo.com/video/" + vimeo_video_id + "&forced_referer=" + Encoding.Base64Encode(referer_url);
        return private_vimeo_url;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}