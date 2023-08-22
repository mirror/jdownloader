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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spiegel.de", "spon.de" }, urls = { "https?://(?:www\\.)?spiegel\\.de/.+", "https?://(?:www\\.)?spon\\.de/[A-Za-z0-9]+" })
public class SpiegelDe extends PluginForDecrypt {
    private static final Pattern PATTERN_SUPPORTED_FOTOSTRECKE        = Pattern.compile("https?://[^/]+/fotostrecke/([a-z0-9\\-]+)-(\\d+)\\.html", Pattern.CASE_INSENSITIVE);
    private static final String  PATTERN_SUPPORTED_FOTOSTRECKE_SINGLE = "https?://[^/]+/fotostrecke/[a-z0-9\\-]+\\d+\\-\\d+\\.html";
    private static final String  PATTERN_SUPPORTED_SPON_SHORT_URL     = ".+spon\\.de/[A-Za-z0-9]+";
    private static final String  PATTERN_SUPPORTED_REDIRECT_ARTICLE   = ".+/artikel/.+";

    public SpiegelDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        if (new Regex(param.getCryptedUrl(), PATTERN_SUPPORTED_SPON_SHORT_URL).matches() || new Regex(param.getCryptedUrl(), PATTERN_SUPPORTED_REDIRECT_ARTICLE).matches()) {
            final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            /* SpiegelOnline short urls (redirect urls) */
            br.setFollowRedirects(false);
            this.br.getPage(param.getCryptedUrl());
            final String finallink = this.br.getRedirectLocation();
            if (finallink == null) {
                /* Probably offline */
                decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            } else {
                decryptedLinks.add(this.createDownloadlink(finallink));
            }
            return decryptedLinks;
        } else if (new Regex(param.getCryptedUrl(), PATTERN_SUPPORTED_FOTOSTRECKE).matches()) {
            return crawlGallery(param);
        } else {
            return this.crawlSpiegelVideo(param);
        }
    }

    private ArrayList<DownloadLink> crawlGallery(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* Picture galleries */
        br.setFollowRedirects(true);
        this.br.getPage(param.getCryptedUrl());
        final String title = new Regex(param.getCryptedUrl(), PATTERN_SUPPORTED_FOTOSTRECKE).getMatch(0).replace("-", " ").trim();
        if (new Regex(br.getURL(), SpiegelDe.PATTERN_SUPPORTED_FOTOSTRECKE_SINGLE).matches()) {
            // did not find a working *single* link, all are redirected to normal links?
            final String finallink = br.getRegex("<div class=\"biga\\-image\".*?<img src=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + param.toString());
                return null;
            }
            final String finalname = title + ".jpg";
            final DownloadLink dlLink = this.createDownloadlink(finallink);
            dlLink.setFinalFileName(finalname);
            decryptedLinks.add(dlLink);
        } else if (new Regex(br.getURL(), SpiegelDe.PATTERN_SUPPORTED_FOTOSTRECKE).matches()) {
            int index = 0;
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(title.trim());
            final String[] images = br.getRegex("<img \\s*data-image-el[^>]*class=\"[^\"]*mx-auto[^\"]*\"[^>]*src\\s*=\\s*\"(https?://[^\"]+)").getColumn(0);
            for (final String image : images) {
                final String ending = getFileNameExtensionFromString(image);
                final DownloadLink dl = this.createDownloadlink(image);
                filePackage.add(dl);
                dl.setFinalFileName(title + "-" + (index + 1) + ending);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                index++;
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlSpiegelVideo(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        this.br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        String json = br.getRegex("data-settings=\"([^\"]+apiUrl[^\"]+)\"").getMatch(0);
        if (json == null) {
            logger.info("Failed to find any video content");
            return decryptedLinks;
        }
        json = Encoding.htmlDecode(json);
        final String apiBase = PluginJSonUtils.getJson(json, "apiUrl");
        final String mediaID = PluginJSonUtils.getJson(json, "mediaId");
        br.getPage(apiBase + "/v2/media/" + mediaID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final List<Object> ressourcelist = (List<Object>) entries.get("playlist");
        for (final Object videoO : ressourcelist) {
            entries = (Map<String, Object>) videoO;
            String title = (String) entries.get("title");
            final String description = (String) entries.get("description");
            final long publishTimestamp = ((Number) entries.get("pubdate")).longValue();
            final Date date = new Date(publishTimestamp * 1000l);
            final String dateFormatted = new SimpleDateFormat("yyyy-MM-dd").format(date);
            if (StringUtils.isEmpty(title)) {
                /* Fallback */
                title = (String) entries.get("mediaid");
            }
            if (StringUtils.isEmpty(title)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            final List<Object> sourcesO = (List<Object>) entries.get("sources");
            for (final Object sourceO : sourcesO) {
                entries = (Map<String, Object>) sourceO;
                /* 2021-03-11: Skip HLS, only grab HTTP URLs. */
                final String type = (String) entries.get("type");
                if (!type.equals("video/mp4")) {
                    logger.info("Skipping streamtype: " + type);
                }
                final String url = (String) entries.get("file");
                final String label = (String) entries.get("label");
                final DownloadLink dl = this.createDownloadlink(url);
                String finalFilename = dateFormatted + "_" + title;
                if (label != null) {
                    finalFilename += "_" + label;
                }
                dl.setFinalFileName(finalFilename + ".mp4");
                dl.setAvailable(true);
                if (!StringUtils.isEmpty(description)) {
                    dl.setComment(description);
                }
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}