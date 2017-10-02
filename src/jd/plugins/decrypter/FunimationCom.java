package jd.plugins.decrypter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision: 37916 $", interfaceVersion = 3, names = { "funimation.com" }, urls = { "http(?:s)://www.funimation.com/(?:shows/)[-0-9a-zA-Z]+/[-0-9a-zA-Z]+/.*" })
public class FunimationCom extends PluginForDecrypt {
    static private final String PLAYER_URL         = "https://www.funimation.com/player/";
    static private final String SHOWEXPERIENCE_API = "https://www.funimation.com/api/showexperience/";
    static private final String EXPERIENCE_API     = "https://www.funimation.com/api/experience/";
    private final char          SEPARATOR          = '-';
    final FilePackage           filePackage        = FilePackage.getInstance();

    @SuppressWarnings("deprecation")
    public FunimationCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink cryptedLink, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            // Attempt to login
            this.setBrowserExclusive();
            loadPlugin();
            final Account account = AccountController.getInstance().getValidAccount(plugin);
            if (account != null) {
                try {
                    ((jd.plugins.hoster.FunimationCom) plugin).login(account, this.br, false);
                } catch (final Throwable e) {
                    throw e;
                }
            }
            // set utf-8
            this.br.setCustomCharset("utf-8");
            // Load the linked page
            this.br.setFollowRedirects(true);
            br.getPage(cryptedLink.getCryptedUrl());
            final String video_id = this.br.getRegex("id: \'(\\d+)\'").getMatch(0);
            final String show_name = new Regex(cryptedLink.getCryptedUrl(), "http(?:s)://www.funimation.com/(?:shows/)([-0-9a-zA-Z]+)/").getMatch(0);
            final String episode_name = new Regex(cryptedLink.getCryptedUrl(), "http(?:s)://www.funimation.com/(?:shows/)[-0-9a-zA-Z]+/([-0-9a-zA-Z]+)/").getMatch(0);
            final String episode_number = this.br.getRegex("episodeNum: (\\d+),").getMatch(0);
            final String season_number = this.br.getRegex("seasonNum: (\\d+),").getMatch(0);
            final String alpha = this.br.getRegex("alpha: \"(\\w+)\",").getMatch(0);
            final String language = this.br.getRegex("KANE_customdimensions.showLanguage\\s+=\\s+'(\\w+)';").getMatch(0);
            final String title = show_name + "-" + episode_number + "-" + episode_name + "-" + language;
            filePackage.setName(title);
            final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            SecureRandom rnd = new SecureRandom();
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(AB.charAt(rnd.nextInt(AB.length())));
            }
            final String random = sb.toString();
            br.getPage(SHOWEXPERIENCE_API + video_id + "/?pinst_id=" + random);
            String json_source = br.toString();
            if (json_source.contains("\"errors\"") || json_source.contains("Invalid")) {
                decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Unable to grab video links"));
                return decryptedLinks;
            }
            json_source = Encoding.htmlDecode(json_source);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
            final ArrayList<Object> items_array = (ArrayList<Object>) entries.get("items");
            for (Object i : items_array) {
                entries = (LinkedHashMap<String, Object>) i;
                final String src = entries.get("src").toString();
                String filename = null;
                if (src.contains(".mp4")) {
                    filename = title + "-mobile.mp4";
                    final DownloadLink dl = this.createDownloadlink(src);
                    dl._setFilePackage(filePackage);
                    dl.setFinalFileName(filename);
                    dl.setContentUrl(cryptedLink.getCryptedUrl());
                    dl.setLinkID(filename);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                } else {
                    br.getPage(src);
                    List<HlsContainer> qualities = HlsContainer.getHlsQualities(br);
                    for (final HlsContainer h : qualities) {
                        String quality = h.getResolution();
                        filename = title + "-" + quality + h.getFileExtension();
                        final DownloadLink dl = this.createDownloadlink(h.getDownloadurl());
                        dl._setFilePackage(filePackage);
                        dl.setFinalFileName(filename);
                        dl.setContentUrl(cryptedLink.getCryptedUrl());
                        dl.setLinkID(filename);
                        dl.setAvailable(true);
                        decryptedLinks.add(dl);
                    }
                }
            }
            // Get subtitles if available
            br.getPage(EXPERIENCE_API + video_id);
            json_source = br.toString();
            json_source = Encoding.htmlDecode(json_source);
            if (json_source.contains("Sorry, but the page you're looking for doesn't exist.")) {
                decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Unable to grab subtitle links"));
                return decryptedLinks;
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
            final ArrayList<Object> seasons_array = (ArrayList<Object>) entries.get("seasons");
            for (Object s : seasons_array) {
                entries = (LinkedHashMap<String, Object>) s;
                String season = entries.get("seasonId").toString();
                if (season_number.equals(season)) {
                    final ArrayList<Object> episodes_array = (ArrayList<Object>) entries.get("episodes");
                    for (Object e : episodes_array) {
                        entries = (LinkedHashMap<String, Object>) e;
                        String episode = entries.get("episodeId").toString();
                        if (episode_number.equals(episode)) {
                            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "languages/" + language + "/alpha/" + alpha);
                            final ArrayList<Object> sources_array = (ArrayList<Object>) entries.get("sources");
                            // Only get the first array as the sources are the same between both arrays
                            entries = (LinkedHashMap<String, Object>) sources_array.get(0);
                            final ArrayList<Object> text_tracks_array = (ArrayList<Object>) entries.get("textTracks");
                            for (Object t : text_tracks_array) {
                                entries = (LinkedHashMap<String, Object>) t;
                                String src = entries.get("src").toString();
                                if (src.contains(".srt")) {
                                    String filename = title + ".srt";
                                    final DownloadLink dl = this.createDownloadlink(src);
                                    dl._setFilePackage(filePackage);
                                    dl.setFinalFileName(filename);
                                    dl.setContentUrl(cryptedLink.getCryptedUrl());
                                    dl.setLinkID(filename);
                                    dl.setAvailable(true);
                                    decryptedLinks.add(dl);
                                }
                            }
                        }
                    }
                }
            }
        } catch (final IOException e) {
            this.logger.log(e);
            return null;
        } finally {
            try {
                this.br.getHttpConnection().disconnect();
            } catch (final Throwable e) {
            }
        }
        return decryptedLinks;
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 100);
    }

    private PluginForHost plugin = null;

    public void loadPlugin() {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("funimation.com");
            if (plugin == null) {
                throw new IllegalStateException("funimation.com hoster plugin not found!");
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}