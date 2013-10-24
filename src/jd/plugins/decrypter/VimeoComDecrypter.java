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
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogSource;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vimeo.com" }, urls = { "https?://(www\\.|player\\.)?vimeo\\.com/((video/)?\\d+|channels/[a-z0-9\\-_]+/\\d+)" }, flags = { 0 })
public class VimeoComDecrypter extends PluginForDecrypt {

    private static final String Q_MOBILE   = "Q_MOBILE";
    private static final String Q_ORIGINAL = "Q_ORIGINAL";
    private static final String Q_HD       = "Q_HD";
    private static final String Q_SD       = "Q_SD";
    private static final String Q_BEST     = "Q_BEST";
    private String              password   = null;

    public VimeoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final SubConfiguration cfg = SubConfiguration.getConfig("vimeo.com");
        final String ID = new Regex(parameter, "/(\\d+)").getMatch(0);
        parameter = "http://vimeo.com/" + ID;

        final Browser br = new Browser();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("vimeo.com", "v6f", "1");
        br.getPage(parameter);

        /* Workaround for User from Iran */
        if (br.containsHTML("<body><iframe src=\"http://10\\.10\\.\\d+\\.\\d+\\?type=(Invalid Site)?\\&policy=MainPolicy")) br.getPage("http://player.vimeo.com/config/" + ID);

        if (br.containsHTML("Page not found|This video does not exist|>We couldn\\'t find that page")) {
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decryptedforVimeoHosterPlugin1" + "://"));
            link.setAvailable(false);
            link.setProperty("offline", true);
            decryptedLinks.add(link);
            return decryptedLinks;
        }
        if (br.containsHTML("This is a private video")) {
            if (!br.containsHTML("If so please provide the correct password")) {
                final DownloadLink link = createDownloadlink(parameter.replace("http://", "decryptedforVimeoHosterPlugin1" + "://"));
                link.setAvailable(false);
                link.setProperty("offline", true);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
        }
        if (br.containsHTML(">There was a problem loading this video")) {
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decryptedforVimeoHosterPlugin1" + "://"));
            link.setAvailable(false);
            link.setProperty("offline", true);
            decryptedLinks.add(link);
            return decryptedLinks;
        }

        handlePW(param, br);

        final String date = br.getRegex("itemprop=\"dateCreated\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2})").getMatch(0);
        final String channelName = br.getRegex("itemtype=\"http://schema\\.org/Person\">[\t\n\r ]+<meta itemprop=\"name\" content=\"([^<>\"]*?)\"").getMatch(0);
        String title = br.getRegex("\"title\":\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) title = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\">").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        title = Encoding.htmlDecode(title.replaceAll("(\\\\|/)", "_").replaceAll("_+", "_").trim());
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("vimeo.com");

        String qualities[][] = getQualities(br, ID, title);
        if (qualities == null) return null;
        ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
        HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
        int format = 0;
        for (String quality[] : qualities) {
            String url = quality[0];
            String name = Encoding.htmlDecode(quality[1]);
            String fmt = quality[2];
            name = name.replaceFirst("\\.mp4$", "") + "_" + fmt.replaceAll(" ?\\.MP4 file$", "");
            if (fmt != null) fmt = fmt.toLowerCase(Locale.ENGLISH).trim();
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
            if (url == null || name == null) continue;
            if (!url.startsWith("http://")) url = "http://vimeo.com" + url;
            final DownloadLink link = createDownloadlink(parameter.replace("http://", "decryptedforVimeoHosterPlugin" + format + "://"));
            link.setProperty("directURL", url);
            link.setProperty("directQuality", fmt);
            link.setProperty("LINKDUPEID", "vimeo" + ID + name + fmt);
            link.setProperty("pass", password);

            if (date != null) link.setProperty("originaldate", date);
            if (channelName != null) link.setProperty("channel", Encoding.htmlDecode(channelName.trim()));
            link.setProperty("plainfilename", name);
            /* make sure the plugin is loaded! */
            JDUtilities.getPluginForHost("vimeo.com");
            final String formattedFilename = ((jd.plugins.hoster.VimeoCom) hostPlugin).getFormattedFilename(link);
            link.setFinalFileName(formattedFilename);
            link.setProperty("directName", formattedFilename);

            if (quality[3] != null) {
                link.setDownloadSize(SizeFormatter.getSize(quality[3].trim()));
                link.setAvailable(true);
            }
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
                if (keep == null) keep = bestMap.get("hd");
                if (keep == null) keep = bestMap.get("sd");
                if (keep == null) keep = bestMap.get("mobile");
                if (keep != null) {
                    newRet.clear();
                    newRet.add(keep);
                }
            }
            /*
             * only replace original found links by new ones, when we have some
             */
            if (newRet.size() > 1) {
                String fpName = "";
                if (channelName != null) fpName += Encoding.htmlDecode(channelName.trim()) + " - ";
                if (date != null) {
                    try {
                        final String userDefinedDateFormat = cfg.getStringProperty("CUSTOM_DATE_2", "dd.MM.yyyy_HH-mm-ss");
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

        if (decryptedLinks == null || decryptedLinks.size() == 0) return null;
        return decryptedLinks;
    }

    private String[][] getQualities(Browser br, String ID, String title) throws Exception {
        /*
         * little pause needed so the next call does not return trash
         */
        Thread.sleep(1000);
        String qualities[][] = null;
        String configURL = br.getRegex("data-config-url=\"(https?://player.vimeo.com/v2/video/\\d+/config.*?)\"").getMatch(0);
        if (configURL != null) {
            configURL = configURL.replaceAll("&amp;", "&");
            br.getPage(configURL);
            String fmts = br.getRegex("\"files\":\\{\"h264\":\\{(.*?)\\}\\}").getMatch(0);
            if (fmts != null) {
                String quality[][] = new Regex(fmts, "\"(.*?)\":\\{(.*?)(\\}|$)").getMatches();
                qualities = new String[quality.length][4];
                for (int i = 0; i < quality.length; i++) {
                    String url = new Regex(quality[i][1], "\"url\":\"(http.*?)\"").getMatch(0);
                    qualities[i][0] = url;
                    qualities[i][1] = title;
                    qualities[i][2] = quality[i][0];
                    qualities[i][3] = null;
                }
            }
        } else if (br.containsHTML("iconify_down_b")) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("http://vimeo.com/" + ID + "?action=download");
            qualities = br.getRegex("href=\"[^\"]+(/\\d+/download.*?)\" download=\"(.*?)\" .*?>(.*? file)<.*?\\d+x\\d+ /(.*?)\\)").getMatches();
        } else {
            /* withoutDlBtn */
            String sig = br.getRegex("\"signature\":\"([0-9a-f]+)\"").getMatch(0);
            String time = br.getRegex("\"timestamp\":(\\d+)").getMatch(0);
            if (sig != null && time != null) {
                String fmts = br.getRegex("\"files\":\\{\"h264\":\\[(.*?)\\]\\}").getMatch(0);
                if (fmts != null) {
                    String quality[] = fmts.replaceAll("\"", "").split(",");
                    qualities = new String[quality.length][4];
                    for (int i = 0; i < quality.length; i++) {
                        qualities[i][0] = "http://player.vimeo.com/play_redirect?clip_id=" + ID + "&sig=" + sig + "&time=" + time + "&quality=" + quality[i];
                        qualities[i][1] = title;
                        qualities[i][2] = quality[i];
                        qualities[i][3] = null;
                    }
                } else {
                    // Nothing found so SD should be available at least...
                    qualities = new String[1][4];
                    qualities[0][0] = "http://player.vimeo.com/play_redirect?clip_id=" + ID + "&sig=" + sig + "&time=" + time + "&quality=sd&codecs=H264,VP8,VP6&type=moogaloop_local&embed_location=&seek=0";
                    qualities[0][1] = title;
                    qualities[0][2] = "sd";
                    qualities[0][3] = null;
                }
            }
        }
        return qualities;
    }

    private void handlePW(CryptedLink param, Browser br) throws Exception {
        // check for a password. Store latest password in DB
        Form pwForm = br.getFormbyProperty("id", "pw_form");
        if (pwForm != null) {
            String xsrft = br.getRegex("xsrft: '(.*?)'").getMatch(0);
            br.setCookie(br.getHost(), "xsrft", xsrft);
            String latestPassword = getPluginConfig().getStringProperty("PASSWORD");
            if (latestPassword != null) {
                pwForm.put("password", latestPassword);
                pwForm.put("token", xsrft);
                try {
                    br.submitForm(pwForm);
                } catch (Throwable e) {
                    if (br.getHttpConnection().getResponseCode() == 401) logger.warning("vimeo.com: Wrong password for Link: " + param.toString());
                    if (br.getHttpConnection().getResponseCode() == 418) {
                        br.getPage(param.toString());
                        xsrft = br.getRegex("xsrft: '(.*?)'").getMatch(0);
                        br.setCookie(br.getHost(), "xsrft", xsrft);
                    }
                }
            }
            // no defaultpassword, or defaultpassword is wrong
            for (int i = 0; i < 3; i++) {
                pwForm = br.getFormbyProperty("id", "pw_form");
                if (pwForm == null) break;
                latestPassword = Plugin.getUserInput("Password?", param);
                pwForm.put("password", latestPassword);
                pwForm.put("token", xsrft);
                try {
                    br.submitForm(pwForm);
                } catch (Throwable e) {
                    if (br.getHttpConnection().getResponseCode() == 401) {
                        logger.warning("vimeo.com: Wrong password for Link: " + param.toString());
                        if (i < 2) br.getPage(param.toString());
                        xsrft = br.getRegex("xsrft: '(.*?)'").getMatch(0);
                        br.setCookie(br.getHost(), "xsrft", xsrft);
                        continue;
                    }
                }
                password = latestPassword;
                getPluginConfig().setProperty("PASSWORD", latestPassword);
                getPluginConfig().save();
                break;
            }
            if (br.getHttpConnection().getResponseCode() == 401) throw new DecrypterException(DecrypterException.PASSWORD);
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}