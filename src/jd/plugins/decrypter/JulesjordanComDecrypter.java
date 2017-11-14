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
import java.util.List;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.JulesjordanCom.JulesjordanComConfigInterface;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "julesjordan.com" }, urls = { "https?://(?:www\\.)?julesjordan\\.com/(?:trial|members)/(?:movies|scenes)/[^/]+\\.html" })
public class JulesjordanComDecrypter extends PluginForDecrypt {
    public JulesjordanComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String> all_known_qualities = Arrays.asList("mp4_4k", "mp4_1080", "mp4_720", "mp4_mobile");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        List<String> all_selected_qualities = new ArrayList<String>();
        final JulesjordanComConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.JulesjordanCom.JulesjordanComConfigInterface.class);
        final String parameter = param.toString();
        final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(plg);
        final String url_name = jd.plugins.hoster.JulesjordanCom.getURLName(parameter);
        final boolean grabBest = cfg.isGrabBESTEnabled();
        final boolean grabBestWithinUserSelection = cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled();
        final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
        final boolean grab4k = cfg.isGrabHTTPMp4_4kEnabled();
        final boolean grab1080p = cfg.isGrabHTTPMp4_1080pEnabled();
        final boolean grab720p = cfg.isGrabHTTPMp4_720pHDEnabled();
        final boolean grabMobileSD = cfg.isGrabHTTPMp4_MobileSDEnabled();
        final boolean fastLinkcheck = cfg.isFastLinkcheckEnabled();
        if (grab4k) {
            all_selected_qualities.add("mp4_4k");
        }
        if (grab1080p) {
            all_selected_qualities.add("mp4_1080");
        }
        if (grab720p) {
            all_selected_qualities.add("mp4_720");
        }
        if (grabMobileSD) {
            all_selected_qualities.add("mp4_mobile");
        }
        if (all_selected_qualities.isEmpty()) {
            all_selected_qualities = all_known_qualities;
        }
        if (aa != null) {
            ((jd.plugins.hoster.JulesjordanCom) plg).login(this.br, aa, false);
        }
        if (aa == null) {
            /* Only MOCH download and / or trailer download --> Add link for hostplugin */
            final DownloadLink dl = this.createDownloadlink(parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else {
            /* Normal host account is available */
            this.br.getPage(jd.plugins.hoster.JulesjordanCom.getURLPremium(parameter));
        }
        if (isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String title = jd.plugins.hoster.JulesjordanCom.getTitle(this.br);
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = url_name;
        }
        title = Encoding.htmlDecode(title).trim();
        final HashMap<String, DownloadLink> all_found_downloadlinks = new HashMap<String, DownloadLink>();
        final HashMap<String, String> allQualities = findAllQualities(this.br);
        final Iterator<Entry<String, String>> it = allQualities.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, String> entry = it.next();
            final String quality_url = entry.getKey();
            final String dlurl = entry.getValue();
            final DownloadLink dl = this.createDownloadlink(dlurl);
            final String decrypter_filename = title + "_" + quality_url + ".mp4";
            dl.setName(decrypter_filename);
            if (fastLinkcheck) {
                dl.setAvailable(true);
            }
            dl.setProperty("fid", url_name);
            dl.setProperty("quality", quality_url);
            dl.setProperty("decrypter_filename", decrypter_filename);
            dl.setProperty("mainlink", parameter);
            all_found_downloadlinks.put("mp4_" + quality_url, dl);
        }
        final HashMap<String, DownloadLink> all_selected_downloadlinks = handleQualitySelection(all_found_downloadlinks, all_selected_qualities, grabBest, grabBestWithinUserSelection, grabUnknownQualities);
        /* Finally add selected URLs */
        final Iterator<Entry<String, DownloadLink>> it_2 = all_selected_downloadlinks.entrySet().iterator();
        while (it_2.hasNext()) {
            final Entry<String, DownloadLink> entry = it_2.next();
            final DownloadLink keep = entry.getValue();
            decryptedLinks.add(keep);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(title.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    public static HashMap<String, String> findAllQualities(final Browser br) {
        final HashMap<String, String> allQualities = new HashMap<String, String>();
        final String[] dlinfo = br.getRegex("<option value=\"(https?://dl\\d+\\.julesjordan\\.com/dl/[^<>\"]+\\.mp4)\"").getColumn(0);
        for (final String dlurl : dlinfo) {
            final String quality_url = new Regex(dlurl, "([A-Za-z0-9]+)\\.mp4$").getMatch(0);
            if (dlurl == null || quality_url == null) {
                /* Skip URLs which do nit fit our pattern. */
                continue;
            }
            allQualities.put(quality_url, dlurl);
        }
        return allQualities;
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

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> map_with_downloadlinks) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (map_with_downloadlinks.size() > 0) {
            for (final String quality : all_known_qualities) {
                keep = map_with_downloadlinks.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = map_with_downloadlinks;
        }
        return newMap;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }
}
