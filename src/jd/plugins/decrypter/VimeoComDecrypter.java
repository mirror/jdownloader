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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogSource;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vimeo.com" }, urls = { "https?://((www\\.|player\\.)?vimeo\\.com/((video/)?\\d+(\\&forced_referer=[A-Za-z0-9=]+)?|channels/[a-z0-9\\-_]+/\\d+)|vimeo\\.com/[A-Za-z0-9\\-_]+/videos)" }, flags = { 0 })
public class VimeoComDecrypter extends PluginForDecrypt {

    private static final String type_player_private = "https?://player\\.vimeo\\.com/video/\\d+";
    private static final String Q_MOBILE            = "Q_MOBILE";
    private static final String Q_ORIGINAL          = "Q_ORIGINAL";
    private static final String Q_HD                = "Q_HD";
    private static final String Q_SD                = "Q_SD";
    private static final String Q_BEST              = "Q_BEST";
    private String              password            = null;

    public VimeoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String LINKTYPE_USER = "https?://(www\\.)?vimeo\\.com/[A-Za-z0-9\\-_]+/videos";

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final SubConfiguration cfg = SubConfiguration.getConfig("vimeo.com");
        // when testing and dropping to frame, components will fail without clean browser.
        br = new Browser();
        setBrowserExclusive();
        prepBrowser(br);
        br.setFollowRedirects(true);
        try {
            br.setAllowedResponseCodes(new int[] { 400, 410 });
        } catch (final Throwable t) {
        }
        if (parameter.matches(LINKTYPE_USER)) {
            br.getPage(parameter);
            if (br.containsHTML(">We couldn't find that page")) {
                final DownloadLink link = createDownloadlink("decryptedforVimeoHosterPlugin1://vimeo\\.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
                link.setAvailable(false);
                link.setProperty("offline", true);
                link.setFinalFileName(new Regex(parameter, "vimeo\\.com/(.+)").getMatch(0));
                decryptedLinks.add(link);
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
                try {
                    if (this.isAbort()) {
                        logger.info("vimeo.com: Decrypt process aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                if (i > 1) {
                    br.getPage("http://vimeo.com/" + user_id + "/videos/page:" + i + "/sort:date/format:detail");
                }
                final String[] videoIDs = br.getRegex("id=\"clip_(\\d+)\"").getColumn(0);
                if (videoIDs == null || videoIDs.length == 0) {
                    logger.info("vimeo.com: Found no videos on current page -> Stopping");
                    break;
                }
                for (final String videoID : videoIDs) {
                    final DownloadLink fina = createDownloadlink("http://vimeo.com/" + videoID);
                    decryptedLinks.add(fina);
                }
                logger.info("vimeo.com: Decrypted page: " + i + " of " + totalPages);
                logger.info("vimeo.com: Found " + videoIDs.length + " videolinks on current page");
                logger.info("vimeo.com: Found " + decryptedLinks.size() + " of " + totalVids + " total videolinks");
                if (decryptedLinks.size() >= totalVids) {
                    logger.info("vimeo.com: Decrypted all videos, stopping");
                    break;
                }
            }
            logger.info("vimeo.com: Decrypt done! Total amount of decrypted videolinks: " + decryptedLinks.size() + " of " + totalVids);
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
            final String ID = new Regex(parameter, "(\\d+)$").getMatch(0);
            String date = null;
            String channelName = null;
            String title = null;
            final String cleanVimeoURL = "http://vimeo.com/" + ID;
            /*
             * We used to simply change the vimeo.com/player/XXX links to normal vimeo.com/XXX links but in some cases, videos can only be
             * accessed via their 'player'-link with a specified Referer.
             */
            if (parameter.matches(type_player_private) && new_way_allowed) {
                if (vimeo_forced_referer != null) {
                    br.getHeaders().put("Referer", vimeo_forced_referer);
                }
                br.getPage(parameter);
                if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || "This video does not exist\\.".equals(getJson("message"))) {
                    final DownloadLink link = getOffline(parameter);
                    link.setFinalFileName(ID);
                    return decryptedLinks;
                }
                final String owner_json = br.getRegex("\"owner\":\\{(.*?)\\}").getMatch(0);
                if (owner_json != null) {
                    channelName = getJson(owner_json, "name");
                }
            } else {
                // maybe required
                // br.setCookie(this.getHost(), "player", "");

                parameter = cleanVimeoURL;
                br.getPage(parameter);

                /* Workaround for User from Iran */
                if (br.containsHTML("<body><iframe src=\"http://10\\.10\\.\\d+\\.\\d+\\?type=(Invalid Site)?\\&policy=MainPolicy")) {
                    br.getPage("http://player.vimeo.com/config/" + ID);
                }

                if (br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("Page not found|This video does not exist|>We couldn't find that page|>Sorry, there is no video here\\.<|>Either it was deleted or it never existed in the first place")) {
                    final DownloadLink link = getOffline(parameter);
                    link.setFinalFileName(ID);
                    return decryptedLinks;
                }

                if (br.containsHTML(containsPass())) {
                    try {
                        handlePW(param, br);
                    } catch (final DecrypterException edc) {
                        logger.info("User entered too many wrong passwords --> Cannot decrypt link: " + parameter);
                        final DownloadLink link = getOffline(parameter);
                        link.setFinalFileName(ID);
                        return decryptedLinks;
                    }
                }

                if (br.containsHTML(">There was a problem loading this video")) {
                    final DownloadLink link = getOffline(parameter);
                    link.setFinalFileName(ID);
                    return decryptedLinks;
                }
                // document.cookie = 'vuid=' + encodeURIComponent('35533916.335958829')
                final String vuid = br.getRegex("document\\.cookie\\s*=\\s*'vuid='\\s*\\+\\s*encodeURIComponent\\('(\\d+\\.\\d+)'\\)").getMatch(0);
                if (vuid != null) {
                    br.setCookie(br.getURL(), "vuid", vuid);
                }

                date = br.getRegex("itemprop=\"dateCreated\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2})").getMatch(0);
                channelName = br.getRegex("itemtype=\"http://schema\\.org/Person\">[\t\n\r ]+<meta itemprop=\"name\" content=\"([^<>\"]+)\"").getMatch(0);
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
                        url = "http://vimeo.com/" + url;
                    } else {
                        url = "http://vimeo.com" + url;
                    }
                }
                // there can be multiple hd/sd etc need to identify with framesize.
                final String linkdupeid = ID + "_" + fmt + "_" + quality[3];
                final DownloadLink link = createDownloadlink(parameter.replace("http://", "decryptedforVimeoHosterPlugin" + format + "://"));
                link.setProperty("directURL", url);
                // videoTitle is required!
                link.setProperty("videoTitle", title);
                link.setProperty("videoQuality", fmt);
                link.setProperty("videoExt", quality[1]);
                link.setProperty("videoID", ID);
                link.setProperty("videoFrameSize", quality[3]);
                link.setProperty("videoBitrate", quality[4]);
                link.setProperty("videoCodec", quality[6]);
                try {
                    link.setLinkID(linkdupeid);
                    link.setContentUrl(cleanVimeoURL);
                } catch (final Throwable e) {
                    /* Not available in old 0.9.591 Stable */
                    link.setProperty("LINKDUPEID", linkdupeid);
                    link.setBrowserUrl(cleanVimeoURL);
                }
                if (password != null) {
                    link.setProperty("pass", password);
                }
                if (parameter.matches(type_player_private)) {
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
                DownloadLink best = bestMap.get(fmt);
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

    private DownloadLink getOffline(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
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

    private String[][] getQualities(Browser ibr, String ID) throws Exception {
        pluginLoaded();
        return ((jd.plugins.hoster.VimeoCom) vimeo_hostPlugin).getQualities(ibr, ID);
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

    private String xsrft() throws PluginException {
        final String xsrft = br.getRegex("xsrft: '(.*?)'").getMatch(0);
        if (xsrft != null) {
            br.setCookie(br.getHost(), "xsrft", xsrft);
        } else {
            // is this a problem?
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return xsrft;
    }

    private void handlePW(CryptedLink param, Browser br) throws Exception {
        // check for a password. Store latest password in DB
        Form pwForm = br.getFormbyProperty("id", "pw_form");
        if (pwForm != null) {
            password = getPluginConfig().getStringProperty("lastusedpass", null);
            if (password != null) {
                pwForm.put("token", xsrft());
                pwForm.put("password", password);
                try {
                    br.submitForm(pwForm);
                } catch (Throwable e) {
                    if (br.getHttpConnection().getResponseCode() == 401) {
                        logger.warning("vimeo.com: Wrong password for Link: " + param.toString());
                    }
                    if (br.getHttpConnection().getResponseCode() == 418) {
                        br.getPage(param.toString());
                    }
                }
            }
            // lastusedpasswd == null, or lastusedpasswd is wrong
            for (int i = 0; i < 3; i++) {
                pwForm = br.getFormbyProperty("id", "pw_form");
                if (pwForm == null) {
                    break;
                }
                pwForm.put("token", xsrft());
                password = Plugin.getUserInput("Password for link: " + param.toString() + " ?", param);
                if (password == null || "".equals(password)) {
                    // empty pass?? not good...
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                pwForm.put("password", password);
                try {
                    br.submitForm(pwForm);
                } catch (Throwable e) {
                    if (br.getHttpConnection().getResponseCode() == 401 || br.getHttpConnection().getResponseCode() == 418) {
                        logger.warning("Wrong password for Link: " + param.toString());
                        if (i < 2) {
                            br.getPage(param.toString());
                            continue;
                        } else {
                            logger.warning("Exausted password retry count. " + param.toString());
                            throw new DecrypterException(DecrypterException.PASSWORD);
                        }
                    }
                }
                getPluginConfig().setProperty("lastusedpass", password);
                getPluginConfig().save();
                break;
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}