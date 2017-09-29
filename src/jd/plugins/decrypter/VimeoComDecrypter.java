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

import org.jdownloader.plugins.components.containers.VimeoVideoContainer;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vimeo.com" }, urls = { "https?://(?:www\\.)?vimeo\\.com/(\\d+|channels/[a-z0-9\\-_]+/\\d+|[A-Za-z0-9\\-_]+/videos|ondemand/[A-Za-z0-9\\-_]+|groups/[A-Za-z0-9\\-_]+(?:/videos/\\d+)?)|https?://player\\.vimeo.com/(?:video|external)/\\d+.+" })
public class VimeoComDecrypter extends PluginForDecrypt {

    private static final String type_player_private_external_direct = "https?://player\\.vimeo.com/external/\\d+\\.[A-Za-z]{1,5}\\.mp4.+";
    private static final String type_player_private_external_m3u8   = "https?://player\\.vimeo.com/external/\\d+\\.*?\\.m3u8.+";
    private static final String type_player_private_external        = "https?://player\\.vimeo.com/external/\\d+(\\&forced_referer=[A-Za-z0-9=]+)?";
    private static final String type_player_private_forced_referer  = "https?://player\\.vimeo.com/video/\\d+.*?(\\&|\\?)forced_referer=[A-Za-z0-9=]+";
    public static final String  type_player                         = "https?://player\\.vimeo.com/video/\\d+.+";

    public VimeoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String LINKTYPE_USER  = "https?://(?:www\\.)?vimeo\\.com/[A-Za-z0-9\\-_]+/videos";
    private static final String LINKTYPE_GROUP = "https?://(?:www\\.)?vimeo\\.com/groups/[A-Za-z0-9\\-_]+(?!videos/\\d+)";

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = SubConfiguration.getConfig("vimeo.com");
        init(cfg);
        int skippedLinks = 0;
        String parameter = param.toString().replace("http://", "https://");
        if (parameter.matches(type_player_private_external_m3u8)) {
            parameter = parameter.replaceFirst("(p=.*?)($|&)", "");
            final DownloadLink link = this.createDownloadlink(parameter);
            decryptedLinks.add(link);
            return decryptedLinks;
        } else if (parameter.matches(type_player_private_external_direct)) {
            final DownloadLink link = this.createDownloadlink("directhttp://" + parameter.replaceFirst("%20.+$", ""));
            decryptedLinks.add(link);
            final String fileName = Plugin.getFileNameFromURL(new URL(parameter));
            link.setForcedFileName(fileName);
            link.setFinalFileName(fileName);
            return decryptedLinks;
        } else if (parameter.matches(type_player_private_external)) {
            parameter = parameter.replace("/external/", "/video/");
        } else if (!parameter.matches(type_player_private_forced_referer) && parameter.matches(type_player)) {
            parameter = "https://vimeo.com/" + parameter.substring(parameter.lastIndexOf("/") + 1);
        }
        // when testing and dropping to frame, components will fail without clean browser.
        br = new Browser();
        setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400, 410 });
        String password = null;
        if (parameter.matches(LINKTYPE_USER) || parameter.matches(LINKTYPE_GROUP)) {
            /* Decrypt all videos of a user- or group. */
            br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(createOfflinelink(parameter, "Could not find that page"));
                return decryptedLinks;
            }
            final String urlpart_pagination;
            final String user_or_group_id;
            String userName = null;
            if (parameter.matches(LINKTYPE_USER)) {
                user_or_group_id = new Regex(parameter, "vimeo\\.com/([A-Za-z0-9\\-_]+)/videos").getMatch(0);
                userName = br.getRegex(">Here are all of the videos that <a href=\"/user\\d+\">([^<>\"]*?)</a> has uploaded to Vimeo").getMatch(0);
                urlpart_pagination = "/" + user_or_group_id + "/videos";
            } else {
                user_or_group_id = new Regex(parameter, "vimeo\\.com/groups/([A-Za-z0-9\\-_]+)").getMatch(0);
                urlpart_pagination = "/groups/" + user_or_group_id;
            }
            if (userName == null) {
                userName = user_or_group_id;
            }
            final String totalVideoNum = br.getRegex(">(\\d+(,\\d+)?) Total</a>").getMatch(0);
            int numberofPages = 1;
            final String[] pages = br.getRegex("/page:(\\d+)/").getColumn(0);
            if (pages != null && pages.length != 0) {
                for (final String apage : pages) {
                    final int currentp = Integer.parseInt(apage);
                    if (currentp > numberofPages) {
                        numberofPages = currentp;
                    }
                }
            }
            final int totalVids;
            if (totalVideoNum != null) {
                totalVids = Integer.parseInt(totalVideoNum.replace(",", ""));
            } else {
                /* Assume number of videos. */
                totalVids = numberofPages * 12;
            }
            for (int i = 1; i <= numberofPages; i++) {
                if (this.isAbort()) {
                    logger.info("Decrypt process aborted by user: " + parameter);
                    return decryptedLinks;
                }
                if (i > 1) {
                    br.getPage(urlpart_pagination + "/page:" + i + "/sort:date/format:detail");
                }
                final String[] videoIDs = br.getRegex("id=\"clip_(\\d+)\"").getColumn(0);
                if (videoIDs == null || videoIDs.length == 0) {
                    logger.info("Found no videos on current page -> Stopping");
                    break;
                }
                for (final String videoID : videoIDs) {
                    decryptedLinks.add(createDownloadlink("http://vimeo.com/" + videoID));
                }
                logger.info("Decrypted page: " + i + " of " + numberofPages);
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
            final String videoID = new Regex(parameter, "/(\\d+)").getMatch(0);
            String date = null;
            String channelName = null;
            String title = null;
            final String cleanVimeoURL;
            if (videoID != null && !StringUtils.containsIgnoreCase(parameter, "/ondemand/")) {
                cleanVimeoURL = "https://vimeo.com/" + videoID;
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
                br.getPage("https://player.vimeo.com/video/" + videoID);
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
                        br.getPage("https://player.vimeo.com/video/" + videoID);
                    }
                }
                if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || "This video does not exist\\.".equals(PluginJSonUtils.getJsonValue(br, "message"))) {
                    decryptedLinks.add(createOfflinelink(orgParam, videoID, null));
                    return decryptedLinks;
                }
                if (br.containsHTML(containsPass())) {
                    try {
                        password = handlePW(param, videoID, this.br);
                    } catch (final DecrypterException edc) {
                        logger.info("User entered too many wrong passwords --> Cannot decrypt link: " + parameter);
                        decryptedLinks.add(createOfflinelink(parameter, videoID, null));
                        return decryptedLinks;
                    }
                }
                parameter = orgParam;
                final String owner_json = br.getRegex("\"owner\":\\{(.*?)\\}").getMatch(0);
                if (owner_json != null) {
                    channelName = PluginJSonUtils.getJsonValue(owner_json, "name");
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
                        decryptedLinks.add(createOfflinelink(parameter, videoID, null));
                        return decryptedLinks;
                    }
                    throw e;
                }
                /* Workaround for User from Iran */
                if (br.containsHTML("<body><iframe src=\"http://10\\.10\\.\\d+\\.\\d+\\?type=(Invalid Site)?\\&policy=MainPolicy")) {
                    br.getPage("//player.vimeo.com/config/" + videoID);
                }
                if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("Page not found|This video does not exist|>We couldn't find that page|>Sorry, there is no video here\\.<|>Either it was deleted or it never existed in the first place")) {
                    decryptedLinks.add(createOfflinelink(parameter, videoID, null));
                    return decryptedLinks;
                }
                if (br.containsHTML(containsPass())) {
                    try {
                        password = handlePW(param, videoID, br);
                        /*
                         * After successful password input we'll get json but we want the "normal" html which suits the code below -->
                         * Access main video url again!
                         */
                        br.getPage(parameter);
                    } catch (final DecrypterException edc) {
                        logger.info("User entered too many wrong passwords --> Cannot decrypt link: " + parameter);
                        decryptedLinks.add(createOfflinelink(parameter, videoID, null));
                        return decryptedLinks;
                    }
                }
                if (br.containsHTML(">There was a problem loading this video")) {
                    decryptedLinks.add(createOfflinelink(parameter, videoID, null));
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
            final List<VimeoVideoContainer> qualities = getQualities(br, videoID);
            if (qualities == null) {
                return null;
            }
            final HashMap<String, DownloadLink> dedupeMap = new HashMap<String, DownloadLink>();
            for (final VimeoVideoContainer quality : qualities) {
                if (!qualityAllowed(quality) || !pRatingAllowed(quality)) {
                    skippedLinks++;
                    continue;
                }
                // there can be multiple hd/sd etc need to identify with framesize.
                final String linkdupeid = quality.createLinkID(videoID);
                final DownloadLink link = createDownloadlink(parameter.replaceAll("https?://", "decryptedforVimeoHosterPlugin://"));
                link.setLinkID(linkdupeid);
                link.setProperty("videoID", videoID);
                // videoTitle is required!
                link.setProperty("videoTitle", title);
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
                link.setProperty(jd.plugins.hoster.VimeoCom.VVC, quality);
                link.setFinalFileName(getFormattedFilename(link));
                if (quality.getFilesize() > -1) {
                    link.setDownloadSize(quality.getFilesize());
                }
                link.setAvailable(true);
                final DownloadLink best = dedupeMap.get(quality.bestString());
                // we wont use size as its not always shown for different qualities. use quality preference
                if (best == null || quality.getSource().ordinal() > ((VimeoVideoContainer) best.getProperty(jd.plugins.hoster.VimeoCom.VVC)).getSource().ordinal()) {
                    dedupeMap.put(quality.bestString(), link);
                }
            }
            if (dedupeMap.size() > 0) {
                if (cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_BEST, false)) {
                    decryptedLinks.add(determineBest(dedupeMap));
                } else {
                    for (final Map.Entry<String, DownloadLink> best : dedupeMap.entrySet()) {
                        decryptedLinks.add(best.getValue());
                    }
                }
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
                    fpName += title;
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName);
                    fp.addLinks(decryptedLinks);
                }
            }
        }
        if ((decryptedLinks == null || decryptedLinks.size() == 0) && skippedLinks == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private DownloadLink determineBest(HashMap<String, DownloadLink> bestMap) {
        DownloadLink bestLink = null;
        int bestHeight = -1;
        for (final Map.Entry<String, DownloadLink> best : bestMap.entrySet()) {
            final DownloadLink link = best.getValue();
            final int height = ((VimeoVideoContainer) link.getProperty(jd.plugins.hoster.VimeoCom.VVC)).getHeight();
            if (height > bestHeight) {
                bestLink = link;
                bestHeight = height;
            }
        }
        return bestLink;
    }

    private boolean qMOBILE;
    private boolean qHD;
    private boolean qSD;
    private boolean qORG;
    private boolean qALL;
    private boolean p240;
    private boolean p360;
    private boolean p480;
    private boolean p540;
    private boolean p720;
    private boolean p1080;
    private boolean p1440;
    private boolean p2560;
    private boolean pALL;

    public void init(final SubConfiguration cfg) {
        //
        qMOBILE = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_MOBILE, true);
        qHD = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_HD, true);
        qSD = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_SD, true);
        qORG = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.Q_ORIGINAL, true);
        qALL = !qMOBILE && !qHD && !qSD && !qORG;
        // p ratings
        p240 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_240, true);
        p360 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_360, true);
        p480 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_480, true);
        p540 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_540, true);
        p720 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_720, true);
        p1080 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_1080, true);
        p1440 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_1440, true);
        p2560 = cfg.getBooleanProperty(jd.plugins.hoster.VimeoCom.P_2560, true);
        pALL = !p240 && !p360 && !p480 && !p540 && !p720 && !p1080 && !p1440 && !p1440;
    }

    private boolean qualityAllowed(final VimeoVideoContainer vvc) {
        if (qALL) {
            return true;
        }
        switch (vvc.getQuality()) {
        case ORIGINAL:
            return qORG;
        case HD:
            return qHD;
        case SD:
            return qSD;
        case MOBILE:
            return qMOBILE;
        }
        return false;
    }

    private boolean pRatingAllowed(final VimeoVideoContainer quality) {
        if (pALL) {
            return true;
        }
        final int height = quality.getHeight();
        // max down
        if (height >= 2560) {
            return p2560;
        }
        if (height >= 1140) {
            return p1440;
        }
        if (height >= 1080) {
            return p1080;
        }
        if (height >= 720) {
            return p720;
        }
        if (height >= 540) {
            return p540;
        }
        if (height >= 480) {
            return p480;
        }
        if (height >= 360) {
            return p360;
        }
        if (height >= 240) {
            return p240;
        }
        return false;
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

    private List<VimeoVideoContainer> getQualities(final Browser ibr, final String ID) throws Exception {
        return jd.plugins.hoster.VimeoCom.getQualities(ibr, ID);
    }

    private String getXsrft(final Browser br) throws Exception {
        return jd.plugins.hoster.VimeoCom.getXsrft(br);
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

    private String handlePW(final CryptedLink param, final String videoID, final Browser br) throws Exception {
        final List<String> passwords = getPreSetPasswords();
        // check for a password. Store latest password in DB
        /* Try stored password first */
        final String lastUsedPass = getPluginConfig().getStringProperty("lastusedpass", null);
        if (StringUtils.isNotEmpty(lastUsedPass) && !passwords.contains(lastUsedPass)) {
            passwords.add(lastUsedPass);
        }
        final String videourl = "https://player.vimeo.com/video/" + videoID;
        retry: for (int i = 0; i < 3; i++) {
            final Form pwForm = getPasswordForm(br);
            pwForm.setAction("https://player.vimeo.com/video/" + videoID + "/check-password");
            /* 2016-06-09: Seems like token is no longer needed! */
            // pwForm.put("token", getXsrft(br));
            final String password;
            if (passwords.size() > 0) {
                i -= 1;
                password = passwords.remove(0);
            } else {
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
                        br.getPage(videourl);
                        continue retry;
                    } else {
                        logger.warning("Exausted password retry count. " + param.toString());
                        throw new DecrypterException(DecrypterException.PASSWORD);
                    }
                }
            }
            if (br.containsHTML(containsPass()) || br.getHttpConnection().getResponseCode() == 405 || "false".equalsIgnoreCase(br.toString())) {
                br.getPage(videourl);
                continue retry;
            }
            getPluginConfig().setProperty("lastusedpass", password);
            return password;
        }
        throw new DecrypterException(DecrypterException.PASSWORD);
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