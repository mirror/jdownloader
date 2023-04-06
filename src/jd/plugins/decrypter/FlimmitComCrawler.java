//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.GenericM3u8;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.FlimmitComConfig;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flimmit.com" }, urls = { "https?://flimmit\\.(?:at|com)/([a-z0-9\\-]+)/assets/(\\d+)" })
public class FlimmitComCrawler extends PluginForDecrypt {
    public FlimmitComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PROPERTY_COLLECTION_SLUG          = "collectionSlug";
    private static final String PROPERTY_COLLECTION_TITLE         = "collectionTitle";
    private final boolean       premiumAccountRequiredForCrawling = true;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String urlSlug = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String contentID = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        login();
        br.setFollowRedirects(true);
        br.getPage(jd.plugins.hoster.FlimmitCom.getInternalBaseURL() + "dynamically/video/" + contentID + "/parent/" + contentID);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        // final String httpURL = (String) JavaScriptEngineFactory.walkJson(entries, "data/config/progressive");
        // we get hls since thats all we support at this stage.
        final Object errorO = JavaScriptEngineFactory.walkJson(entries, "data/error");
        /*
         * Content is not a video object --> Maybe Series (2022-01-04: website actually handles this the same way and does the request for a
         * single video even for series-overview-pages lol)
         */
        if ((errorO instanceof String) && (errorO.toString().equalsIgnoreCase("Video not found.") || errorO.toString().equalsIgnoreCase("Dieses Video ist derzeit nicht verfügbar."))) {
            logger.info("Content is not a single video --> Maybe series-overview");
            br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String title = br.getRegex("property=\"og:title\" content=\"([^\"]+)\"").getMatch(0);
            final String[] episodesHTMLs = br.getRegex("<div class=\"b-card is-asset has-img-media[^\"]*?js-link-item\" data-jsb=\"(\\{\\&quot;.*?)\">").getColumn(0);
            for (final String episodesHTML : episodesHTMLs) {
                final String episodeJson = Encoding.htmlDecode(episodesHTML);
                final Map<String, Object> episodeInfo = JavaScriptEngineFactory.jsonToJavaMap(episodeJson);
                String url = episodeInfo.get("linkUrl").toString();
                if (url.endsWith(contentID)) {
                    /* Do not add currently processed URL again! */
                    continue;
                } else {
                    url = br.getURL(url).toString();
                    final DownloadLink video = this.createDownloadlink(url);
                    /*
                     * Store some info here which would be harder to get later (e.g. we would have to access HTML page for each single video
                     * -> Takes time -> We want to avoid that)
                     */
                    video.setProperty(PROPERTY_COLLECTION_SLUG, urlSlug);
                    if (title != null) {
                        video.setProperty(PROPERTY_COLLECTION_TITLE, Encoding.htmlDecode(title).trim());
                    }
                    ret.add(video);
                }
            }
            logger.info("Found " + ret.size() + " episodes");
            return ret;
        } else if (errorO != null) {
            if (br.containsHTML("(?i)Sie ein laufendes Abo") || br.containsHTML("\"code\"\\s*:\\s*1006")) {
                /* Active subscription required -> User has either no account or only a free account */
                throw new AccountRequiredException();
            } else {
                /* Offline or GEO-blocked */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        /* Process video object */
        final Map<String, Object> data = (Map<String, Object>) entries.get("data");
        final Map<String, Object> config = (Map<String, Object>) data.get("config");
        final Map<String, Object> modules = (Map<String, Object>) data.get("modules");
        final String m3u = (String) config.get("hls");
        final String videoTitle = (String) JavaScriptEngineFactory.walkJson(modules, "titles/title");
        String seriesTitle = null;
        String seasonNumberStr = null;
        String episodeNumberStr = null;
        String seasonEpisodeInfoFormatted = null;
        String seasonInfoFormatted = null;
        if (param.getDownloadLink() != null) {
            final String collectionSlug = param.getDownloadLink().getStringProperty(PROPERTY_COLLECTION_SLUG);
            final String collectionTitle = param.getDownloadLink().getStringProperty(PROPERTY_COLLECTION_TITLE);
            if (collectionTitle != null) {
                final Regex seriesTitleRegex = new Regex(collectionTitle, "(?i)(.*) . Staffel (\\d+)");
                if (seriesTitleRegex.matches()) {
                    seriesTitle = seriesTitleRegex.getMatch(0);
                    seasonNumberStr = seriesTitleRegex.getMatch(1);
                } else {
                    /* For collections without season numbers */
                    seriesTitle = collectionTitle;
                }
            } else if (collectionSlug != null) {
                /* Fallback */
                final Regex seriesTitleSlugRegex = new Regex(collectionSlug, "(?i)(.*)\\s*staffel-(\\d+)");
                if (seriesTitleSlugRegex.matches()) {
                    seriesTitle = seriesTitleSlugRegex.getMatch(0);
                    seasonNumberStr = seriesTitleSlugRegex.getMatch(1);
                } else {
                    /* For collections without season numbers */
                    seriesTitle = collectionSlug;
                }
            }
        }
        episodeNumberStr = new Regex(videoTitle, "(?i)Folge (\\d+)").getMatch(0);
        if (seasonNumberStr != null && episodeNumberStr != null) {
            final DecimalFormat df = new DecimalFormat("00");
            seasonInfoFormatted = "S" + df.format(Integer.parseInt(seasonNumberStr));
            seasonEpisodeInfoFormatted = seasonInfoFormatted + "E" + df.format(Integer.parseInt(episodeNumberStr));
        }
        if (m3u == null) {
            logger.info("Failed to find any downloadable content");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String baseVideoTitle;
        if (!StringUtils.isEmpty(videoTitle)) {
            baseVideoTitle = videoTitle;
        } else {
            /* Fallback */
            baseVideoTitle = urlSlug;
        }
        final String description = (String) JavaScriptEngineFactory.walkJson(entries, "data/modules/titles/description");
        final FilePackage fp = FilePackage.getInstance();
        if (!StringUtils.isEmpty(seriesTitle) && !StringUtils.isEmpty(seasonInfoFormatted)) {
            fp.setName(seriesTitle + " " + seasonInfoFormatted);
        } else if (!StringUtils.isEmpty(seriesTitle)) {
            fp.setName(seriesTitle);
        } else {
            fp.setName(baseVideoTitle);
        }
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        fp.setAllowMerge(true);
        final String baseFilename;
        if (!StringUtils.isEmpty(seriesTitle) && seasonEpisodeInfoFormatted != null) {
            baseFilename = seriesTitle + " " + seasonEpisodeInfoFormatted;
        } else if (!StringUtils.isEmpty(seriesTitle)) {
            baseFilename = seriesTitle + " - " + baseVideoTitle;
        } else {
            baseFilename = baseVideoTitle;
        }
        br.getPage(m3u);
        // TODO: please rewrite to use the dedicated hoster plugin
        // TODO: store asset ID/contentID and maybe seriesSlug(might be required in future) and hlscontainer id as properties, so hoster
        // plugin can later refresh urls or change quality
        final List<HlsContainer> qualities = HlsContainer.getHlsQualities(br);
        final ArrayList<HlsContainer> selectedQualities = new ArrayList<HlsContainer>();
        final FlimmitComConfig cfg = PluginJsonConfig.get(FlimmitComConfig.class);
        if (cfg.isPreferBest()) {
            selectedQualities.add(HlsContainer.findBestVideoByBandwidth(qualities));
        } else {
            selectedQualities.addAll(qualities);
        }
        for (final HlsContainer quality : selectedQualities) {
            final DownloadLink dl = this.createDownloadlink(GenericM3u8.createURLForThisPlugin(quality.getDownloadurl()));
            dl.setFinalFileName(baseFilename + "_" + quality.getResolution() + "_" + quality.getBandwidth() + ".mp4");
            ret.add(dl);
        }
        if (cfg.isCrawlSubtitle()) {
            final List<Map<String, Object>> subtitles = (List<Map<String, Object>>) modules.get("subtitles");
            if (subtitles != null && !subtitles.isEmpty()) {
                final String subtitleExt = ".vtt";
                final boolean addedMultipleVideoQualities = ret.size() > 1;
                final String filenameOfLastVideoResult = ret.get(0).getFinalFileName();
                final String titleOfLastVideoResult = filenameOfLastVideoResult.substring(0, filenameOfLastVideoResult.lastIndexOf("."));
                for (final Map<String, Object> subtitleInfo : subtitles) {
                    final DownloadLink subtitle = this.createDownloadlink("directhttp://" + subtitleInfo.get("url").toString());
                    if (subtitles.size() == 1 && !addedMultipleVideoQualities) {
                        subtitle.setFinalFileName(titleOfLastVideoResult + subtitleExt);
                    } else {
                        /* Multiple video qualities and/or subtitles will be returned -> Use individual filenames */
                        subtitle.setFinalFileName(titleOfLastVideoResult + "_" + subtitleInfo.get("lang") + subtitleExt);
                    }
                    ret.add(subtitle);
                }
            }
        }
        if (cfg.isCrawlPoster()) {
            /*
             * Download poster of series/movie. For series this will return the same item for each video item resulting in only one result
             * per series/movie.
             */
            final String posterURL = (String) entries.get("imageSource");
            if (!StringUtils.isEmpty(posterURL)) {
                final DownloadLink poster = this.createDownloadlink(posterURL);
                final String ext = Plugin.getFileNameExtensionFromString(posterURL);
                if (ext != null) {
                    if (!StringUtils.isEmpty(seriesTitle)) {
                        poster.setFinalFileName(seriesTitle + ext);
                    } else {
                        /* Probably we have a movie */
                        poster.setFinalFileName(videoTitle + ext);
                    }
                }
                ret.add(poster);
            }
        }
        for (final DownloadLink result : ret) {
            result.setAvailable(true);
            result._setFilePackage(fp);
        }
        return ret;
    }

    public Account login() throws Exception {
        // we need an account
        Account account = null;
        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
        if (accounts != null && accounts.size() != 0) {
            // lets sort, premium > non premium
            Collections.sort(accounts, new Comparator<Account>() {
                @Override
                public int compare(Account o1, Account o2) {
                    final int io1 = o1.getBooleanProperty("free", false) ? 0 : 1;
                    final int io2 = o2.getBooleanProperty("free", false) ? 0 : 1;
                    return io1 <= io2 ? io1 : io2;
                }
            });
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    account = n;
                    break;
                }
            }
        }
        if (account == null) {
            throw new AccountRequiredException();
        } else if (account.getType() != AccountType.PREMIUM) {
            logger.warning("Looks like user does not own a premium account -> Crawler will most likely fail");
            if (premiumAccountRequiredForCrawling) {
                throw new AccountRequiredException();
            }
        }
        final PluginForHost plugin = getNewPluginForHostInstance("flimmit.com");
        ((jd.plugins.hoster.FlimmitCom) plugin).login(account, false);
        return account;
    }

    @Override
    public Class<? extends FlimmitComConfig> getConfigInterface() {
        return FlimmitComConfig.class;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }
}