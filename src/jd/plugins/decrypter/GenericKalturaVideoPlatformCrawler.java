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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.MediathekProperties;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kaltura.com" }, urls = { "https?://[^/]+/html5/html5lib/v\\d+\\.\\d{1,2}/mwEmbedFrame\\.php/p/\\d+/uiconf_id/\\d+/entry_id/[a-z0-9_]+.+" })
public class GenericKalturaVideoPlatformCrawler extends PluginForDecrypt {
    public GenericKalturaVideoPlatformCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final PluginForHost plg = JDUtilities.getPluginForHost("DirectHTTP");
        final LinkedHashMap<String, DownloadLink> all_qualities = crawlEverything(plg, this.br, parameter);
        if (all_qualities == null) {
            return null;
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Iterator<Entry<String, DownloadLink>> iterator = all_qualities.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<String, DownloadLink> entry = iterator.next();
            decryptedLinks.add(entry.getValue());
        }
        // String fpName = br.getRegex("").getMatch(0);
        // if (fpName != null) {
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(Encoding.htmlDecode(fpName.trim()));
        // fp.addLinks(decryptedLinks);
        // }
        return decryptedLinks;
    }

    public static LinkedHashMap<String, DownloadLink> crawlEverything(final PluginForHost plugin, final Browser br, final String player_embed_url) throws Exception {
        final LinkedHashMap<String, DownloadLink> all_qualities = new LinkedHashMap<String, DownloadLink>();
        final String server = new Regex(player_embed_url, "(https?://[^/]+/)").getMatch(0);
        final String partner_id = new Regex(player_embed_url, "/(?:partner_id|p)/(\\d+)").getMatch(0);
        final String uiconf_id = new Regex(player_embed_url, "uiconf_id/(\\d+)").getMatch(0);
        String sp = new Regex(player_embed_url, "/sp/(\\d+)/").getMatch(0);
        String entry_id = new Regex(player_embed_url, "/entry_id/([A-Za-z0-9_]+)").getMatch(0);
        if (entry_id == null) {
            entry_id = new Regex(player_embed_url, "entry_id=([A-Za-z0-9_]+)").getMatch(0);
        }
        if (partner_id == null || uiconf_id == null || entry_id == null) {
            return null;
        }
        /*
         * Will redirect to website containing our important json:
         * http://<server>/html5/html5lib/v2.66/mwEmbedFrame.php/p/102/uiconf_id/23448216/entry_id/<entry_id>?wid=_102&iframeembed=true&
         * playerId=kaltura_player_1469799505&entry_id=<entry_id>
         */
        br.setFollowRedirects(true);
        br.getPage(player_embed_url);
        final String js = br.getRegex("kalturaIframePackageData = (\\{.*?\\}\\}\\});").getMatch(0);
        if (js == null) {
            return null;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(js);
        entries = (LinkedHashMap<String, Object>) entries.get("entryResult");
        final LinkedHashMap<String, Object> meta = (LinkedHashMap<String, Object>) entries.get("meta");
        final String title = (String) meta.get("name");
        final String description = (String) meta.get("description");
        /** dataUrl or downloadUrl might always be the BEST qualities available! */
        /* E.g. http://<server>/p/102/sp/10200/playManifest/entryId/<main_entry_id>/format/url/protocol/http */
        final String dataUrl = (String) meta.get("dataUrl");
        /*
         * Fallback to find 'sp' value if user added an URL which directly leads to our json --> This URL does not contain the required 'sp'
         * value.
         */
        if (sp == null && !StringUtils.isEmpty(dataUrl)) {
            sp = new Regex(dataUrl, "sp/(\\d+)").getMatch(0);
        }
        /* E.g. http://<host>/p/102/sp/10200/raw/entry_id/<main_entry_id>/version/0 */
        // final String downloadUrl = (String) meta.get("downloadUrl");
        // final String thumbnailUrl = (String) meta.get("thumbnailUrl");
        final long date_released = JavaScriptEngineFactory.toLong(meta.get("createdAt"), 0);
        if (StringUtils.isEmpty(title) || date_released == 0 || sp == null) {
            return null;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "contextData/flavorAssets");
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            final long width = JavaScriptEngineFactory.toLong(entries.get("width"), 0);
            final long height = JavaScriptEngineFactory.toLong(entries.get("height"), 0);
            final long bitrate = JavaScriptEngineFactory.toLong(entries.get("bitrate"), 0);
            final String flavourid = (String) entries.get("id");
            final String ext = (String) entries.get("fileExt");
            if (StringUtils.isEmpty(flavourid) || StringUtils.isEmpty(ext) || filesize == 0 || width == 0 || height == 0 || bitrate == 0) {
                /* Skip invalid objects */
                continue;
            }
            final String directurl = server + "p/" + partner_id + "/sp/" + sp + "/playManifest/entryId/" + entry_id + "/flavorId/" + flavourid + "/format/url/protocol/http/a.mp4";
            final DownloadLink dl = new DownloadLink(plugin, title, plugin.getHost(), directurl, true);
            dl._setFilePackage(fp);
            final MediathekProperties data_downloadlink = dl.bindData(MediathekProperties.class);
            data_downloadlink.setHeight((int) height);
            data_downloadlink.setWidth((int) width);
            data_downloadlink.setFileExtension(ext);
            data_downloadlink.setReleaseDate(date_released * 1000);
            data_downloadlink.setTitle(title);
            /* Set bitrate as bandwidth - this is only to differentiate between different resolutions with same bandwidth/bitrate */
            data_downloadlink.setBandwidth(bitrate);
            if (description != null) {
                dl.setComment(description);
            }
            dl.setFinalFileName(jd.plugins.components.MediathekHelper.getMediathekFilename(dl, data_downloadlink, false, true));
            dl.setDownloadSize(filesize * 1024);
            dl.setLinkID(entry_id + "_" + flavourid);
            /* Filename & filesize given --> No need to check directurl */
            dl.setAvailable(true);
            final String quality_identifier = "http_" + width + "_" + height + "_" + bitrate;
            all_qualities.put(quality_identifier, dl);
        }
        return all_qualities;
    }

    public static LinkedHashMap<String, DownloadLink> crawlEverything(final PluginForHost plugin, final Browser br) throws Exception {
        final String player_embed_url = br.getRegex("(https?://[^/]+/p/\\d+/sp/\\d+/embedIframeJs/uiconf_id/\\d+/partner_id/\\d+\\?[^\"\\']+)").getMatch(0);
        if (player_embed_url == null) {
            return null;
        }
        return crawlEverything(plugin, br, player_embed_url);
    }

    public static DownloadLink findBest(final PluginForHost plugin, final Browser br, final LinkedHashMap<String, DownloadLink> all_qualities) throws Exception {
        long max_bitrate = 0;
        long max_bitrate_temp = 0;
        DownloadLink best = null;
        final Iterator<Entry<String, DownloadLink>> iterator = all_qualities.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<String, DownloadLink> entry = iterator.next();
            final DownloadLink current_downloadlink = entry.getValue();
            final MediathekProperties data_downloadlink = current_downloadlink.bindData(MediathekProperties.class);
            max_bitrate_temp = data_downloadlink.getBandwidth();
            if (max_bitrate_temp > max_bitrate) {
                max_bitrate = max_bitrate_temp;
                best = current_downloadlink;
            }
        }
        return best;
    }

    /** Wrapper for findBest(final PluginForHost plugin, final Browser br, final LinkedHashMap<String, DownloadLink> all_qualities) */
    public static DownloadLink findBest(final PluginForHost plugin, final Browser br) throws Exception {
        final LinkedHashMap<String, DownloadLink> all_qualities = crawlEverything(plugin, br);
        return findBest(plugin, br, all_qualities);
    }

    /** Finds best DownloadURL via findBest but returns PluginPatternMatcher for found result instead of DownloadLink. */
    public static String findBestDirecturl(final PluginForHost plugin, final Browser br) throws Exception {
        final DownloadLink best = findBest(plugin, br);
        return best.getPluginPatternMatcher();
    }
}
