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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videobox.com" }, urls = { "https?://(?:www\\.)?videobox\\.com/(?:movie\\-details\\?contentId=|.*?flashPage/)\\d+" })
public class VideoBoxComDecrypter extends PluginForDecrypt {
    public VideoBoxComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private final List<String> all_known_qualities = Arrays.asList("720p", "480p");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Might be useful for quality selection in the future */
        List<String> all_selected_qualities = new ArrayList<String>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // final String[] qualities = { "DVD", "H264_640", "HIGH", "H264_IPOD" };
        /*
         * 2018-05-04: For now we do not have a quality selection so let's add all possible (streaming) resolutions as selected qualities.
         */
        all_selected_qualities.add("720p");
        all_selected_qualities.add("480p");
        if (!getUserLogin()) {
            logger.info("Cannot decrypt without logindata: " + parameter);
            return decryptedLinks;
        }
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        final String encodedUsername = Encoding.urlEncode(aa.getUser());
        final String sessionID = br.getCookie("https://videobox.com/", "JSESSIONID");
        final String videoID = new Regex(parameter, "(\\d+)$").getMatch(0);
        br.getPage("https://www." + this.getHost() + "/content/details/generate/" + videoID + "/content-column.json?x-user-name=" + encodedUsername + "&x-session-key=" + sessionID + "&callback=metai.buildMovieDetails");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.getRegex("\\((.+)\\);$").getMatch(0));
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "details/{0}");
        String fpName = (String) entries.get("name");
        if (StringUtils.isEmpty(fpName)) {
            /* Fallback */
            fpName = videoID;
        }
        final boolean canDownload = ((Boolean) entries.get("canDownload")).booleanValue();
        final ArrayList<Object> scenes = (ArrayList<Object>) entries.get("scenes");
        /* Access downloadURLs to either find downloadurls or at least filesize information for stream download */
        br.getPage("/content/download/options/" + videoID + ".json?x-user-name=" + encodedUsername + "&x-session-key=" + sessionID + "&callback=metai.buildDownloadLinks");
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.getRegex("\\((.+)\\);$").getMatch(0));
        ArrayList<Object> resolutions = (ArrayList<Object>) entries.get("content");
        /* 2018-04-30: Official download possible? / They have "Download" accounts and "Streaming" accounts! */
        final HashMap<String, DownloadLink> all_found_downloadlinks = new HashMap<String, DownloadLink>();
        if (canDownload) {
            /* Download via official download URLs */
            for (final Object resO : resolutions) {
                entries = (LinkedHashMap<String, Object>) resO;
                final String directLink = (String) entries.get("url");
                final String downloadSize = (String) entries.get("size");
                final String quality = (String) entries.get("res");
                if (StringUtils.isEmpty(directLink) || StringUtils.isEmpty(downloadSize) || StringUtils.isEmpty(quality)) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("http://videoboxdecrypted.com/decryptedscene/" + System.currentTimeMillis() + new Random().nextInt(10000));
                final String finalfilename = fpName + "_" + quality + getFileNameExtensionFromString(directLink, ".mp4");
                dl.setAvailable(true);
                dl.setDownloadSize(SizeFormatter.getSize(downloadSize));
                dl.setFinalFileName(finalfilename);
                dl.setProperty("originalurl", parameter);
                dl.setProperty("sceneid", videoID);
                dl.setProperty("directlink", directLink);
                dl.setProperty("quality", quality);
                decryptedLinks.add(dl);
            }
        } else {
            if (scenes != null && scenes.size() > 0) {
                /* TODO: Check this! */
                int currentSceneNumber = 1;
                for (final Object sceneO : scenes) {
                    entries = (LinkedHashMap<String, Object>) sceneO;
                    final String sceneName = (String) entries.get("name");
                    final String sceneID = (String) entries.get("id");
                    if (sceneName == null || sceneID == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    br.getPage("/content/download/options/" + sceneID + ".json?x-user-name=" + encodedUsername + "&x-session-key=" + sessionID + "&callback=metai.buildDownloadLinks");
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.getRegex("\\((.+)\\);$").getMatch(0));
                    resolutions = (ArrayList<Object>) entries.get("content");
                    for (final Object resO : resolutions) {
                        entries = (LinkedHashMap<String, Object>) resO;
                        final String directLink = (String) entries.get("url");
                        final String downloadSize = (String) entries.get("size");
                        final String quality = (String) entries.get("res");
                        if (StringUtils.isEmpty(directLink) || StringUtils.isEmpty(downloadSize) || StringUtils.isEmpty(quality)) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        final DownloadLink dl = createDownloadlink("http://videoboxdecrypted.com/decryptedscene/" + System.currentTimeMillis() + new Random().nextInt(10000));
                        final String finalfilename = fpName + "_" + quality + getFileNameExtensionFromString(directLink, ".mp4");
                        dl.setAvailable(true);
                        dl.setDownloadSize(SizeFormatter.getSize(downloadSize));
                        dl.setFinalFileName(finalfilename);
                        dl.setProperty("originalurl", parameter);
                        dl.setProperty("sceneid", videoID);
                        dl.setProperty("directlink", directLink);
                        dl.setProperty("quality", quality);
                        all_found_downloadlinks.put(quality, dl);
                    }
                    currentSceneNumber++;
                }
            } else {
                /* Download stream */
                br.getPage("/content/download/url/" + videoID + ".json?x-user-name=" + encodedUsername + "&x-session-key=&callback=metai.loadHtml5Video");
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.getRegex("\\((.+)\\);$").getMatch(0));
                final String quality;
                final String quality_website;
                String urlStream = (String) entries.get("urlHD");
                if (!StringUtils.isEmpty(urlStream)) {
                    quality = "720p";
                    quality_website = "H264_HD";
                } else {
                    urlStream = (String) entries.get("url");
                    quality = "480p";
                    quality_website = "H264_640";
                }
                if (StringUtils.isEmpty(urlStream)) {
                    return null;
                }
                if (urlStream.startsWith("//")) {
                    urlStream = "https:" + urlStream;
                }
                final DownloadLink dl = createDownloadlink("http://videoboxdecrypted.com/decryptedscene/" + System.currentTimeMillis() + new Random().nextInt(10000));
                final String finalfilename = fpName + "_" + quality + ".mp4";
                dl.setAvailable(true);
                dl.setFinalFileName(finalfilename);
                dl.setProperty("originalurl", parameter);
                dl.setProperty("sceneid", videoID);
                dl.setProperty("directlink", urlStream);
                dl.setProperty("quality", quality);
                dl.setProperty("finalname", finalfilename);
                if (resolutions != null && resolutions.size() > 0) {
                    for (final Object resO : resolutions) {
                        /* Try to set filesize as the highest downloadable item == stream filesize */
                        entries = (LinkedHashMap<String, Object>) resO;
                        final String downloadSize = (String) entries.get("size");
                        final String quality_this_item = (String) entries.get("res");
                        if (!StringUtils.isEmpty(downloadSize) && !StringUtils.isEmpty(quality_this_item) && quality_this_item.equalsIgnoreCase(quality_website)) {
                            dl.setDownloadSize(SizeFormatter.getSize(downloadSize));
                            break;
                        }
                    }
                }
                all_found_downloadlinks.put(quality, dl);
            }
        }
        final HashMap<String, DownloadLink> all_selected_downloadlinks = handleQualitySelection(all_found_downloadlinks, all_selected_qualities, false, false, true);
        /* Finally add selected URLs */
        final Iterator<Entry<String, DownloadLink>> it_2 = all_selected_downloadlinks.entrySet().iterator();
        while (it_2.hasNext()) {
            final Entry<String, DownloadLink> entry = it_2.next();
            final DownloadLink keep = entry.getValue();
            decryptedLinks.add(keep);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private HashMap<String, DownloadLink> handleQualitySelection(final HashMap<String, DownloadLink> all_found_downloadlinks, final List<String> all_selected_qualities, final boolean grab_best, final boolean grab_best_out_of_user_selection, final boolean grab_unknown) {
        HashMap<String, DownloadLink> all_selected_downloadlinks = new HashMap<String, DownloadLink>();
        final Iterator<Entry<String, DownloadLink>> iterator_all_found_downloadlinks = all_found_downloadlinks.entrySet().iterator();
        if (grab_best) {
            for (final String possibleQuality : this.all_known_qualities) {
                if (all_found_downloadlinks.containsKey(possibleQuality)) {
                    all_selected_downloadlinks.put(possibleQuality, all_found_downloadlinks.get(possibleQuality));
                    break;
                }
            }
            if (all_selected_downloadlinks.isEmpty()) {
                logger.info("Possible issue: Best selection found nothing --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks.put(dl_entry.getKey(), dl_entry.getValue());
                }
            }
        } else {
            boolean atLeastOneSelectedItemExists = false;
            while (iterator_all_found_downloadlinks.hasNext()) {
                final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                final String dl_quality_string = dl_entry.getKey();
                if (all_selected_qualities.contains(dl_quality_string)) {
                    atLeastOneSelectedItemExists = true;
                    all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                } else if (!all_known_qualities.contains(dl_quality_string) && grab_unknown) {
                    logger.info("Found unknown quality: " + dl_quality_string);
                    if (grab_unknown) {
                        logger.info("Adding unknown quality: " + dl_quality_string);
                        all_selected_downloadlinks.put(dl_quality_string, dl_entry.getValue());
                    }
                }
            }
            if (!atLeastOneSelectedItemExists) {
                logger.info("Possible user error: User selected only qualities which are not available --> Adding ALL");
                while (iterator_all_found_downloadlinks.hasNext()) {
                    final Entry<String, DownloadLink> dl_entry = iterator_all_found_downloadlinks.next();
                    all_selected_downloadlinks.put(dl_entry.getKey(), dl_entry.getValue());
                }
            } else {
                if (grab_best_out_of_user_selection) {
                    all_selected_downloadlinks = findBESTInsideGivenMap(all_selected_downloadlinks);
                }
            }
        }
        return all_selected_downloadlinks;
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> map_with_all_qualities) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (map_with_all_qualities.size() > 0) {
            for (final String quality : all_known_qualities) {
                keep = map_with_all_qualities.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = map_with_all_qualities;
        }
        return newMap;
    }

    private boolean getUserLogin() throws Exception {
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("videobox.com");
        Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        if (aa == null) {
            String username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + this.getHost() + " :");
            if (username == null) {
                return false;
            }
            String password = UserIO.getInstance().requestInputDialog("Enter password for " + this.getHost() + " :");
            if (password == null) {
                return false;
            }
            aa = new Account(username, password);
        }
        try {
            ((jd.plugins.hoster.VideoBoxCom) hosterPlugin).login(aa, false, this.br);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(hosterPlugin, aa);
        return true;
    }
}
