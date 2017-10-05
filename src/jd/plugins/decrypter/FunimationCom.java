package jd.plugins.decrypter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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

import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision: 37916 $", interfaceVersion = 3, names = { "funimation.com" }, urls = { "http(?:s)://www.funimation.com/(?:shows/)[-0-9a-zA-Z]+/[-0-9a-zA-Z]+/.*" })
public class FunimationCom extends PluginForDecrypt {
    static private final String SHOWEXPERIENCE_API = "https://www.funimation.com/api/showexperience/";
    static private final String EXPERIENCE_API     = "https://www.funimation.com/api/experience/";

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
        // Attempt to login
        this.setBrowserExclusive();
        loadPlugin();
        ((jd.plugins.hoster.FunimationCom) plugin).setBrowser(br);
        final Account account = AccountController.getInstance().getValidAccount(plugin);
        if (account != null) {
            ((jd.plugins.hoster.FunimationCom) plugin).login(account, false);
        }
        // set utf-8
        br.setCustomCharset("utf-8");
        // Load the linked page
        br.setFollowRedirects(true);
        ((jd.plugins.hoster.FunimationCom) plugin).getPage(br, cryptedLink.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 403) {
            decryptedLinks.add(this.createOfflinelink(cryptedLink.getCryptedUrl()));
            return decryptedLinks;
        }
        // Fog: Grab episode information
        final String video_id = br.getRegex("id: \'(\\d+)\'").getMatch(0);
        final String show_name = new Regex(cryptedLink.getCryptedUrl(), "http(?:s)://www.funimation.com/(?:shows/)([-0-9a-zA-Z]+)/").getMatch(0);
        final String episode_name = new Regex(cryptedLink.getCryptedUrl(), "http(?:s)://www.funimation.com/(?:shows/)[-0-9a-zA-Z]+/([-0-9a-zA-Z]+)/").getMatch(0);
        final String episode_number = br.getRegex("episodeNum: (\\d+),").getMatch(0);
        final String season_number = br.getRegex("seasonNum: (\\d+),").getMatch(0);
        final String alpha = br.getRegex("alpha: \"(\\w+)\",").getMatch(0);
        final FilePackage filePackage = FilePackage.getInstance();
        String title = show_name + "-" + episode_number + "-" + episode_name;
        filePackage.setName(title);
        // Fog: Check and see if there are multiple video ids (each language has it's own video id)
        ((jd.plugins.hoster.FunimationCom) plugin).getPage(br, EXPERIENCE_API + video_id);
        String json_source = br.toString();
        json_source = Encoding.htmlDecode(json_source);
        if (json_source.contains("Sorry, but the page you're looking for doesn't exist.")) {
            decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Unable to grab video information"));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
        final ArrayList<Object> seasons_array = (ArrayList<Object>) entries.get("seasons");
        // Fog: Huge messy nested for loop incoming, I do plan on cleaning this up eventually
        for (Object s : seasons_array) {
            entries = (LinkedHashMap<String, Object>) s;
            final String season = entries.get("seasonId").toString();
            if (season_number.equals(season)) {
                final ArrayList<Object> episodes_array = (ArrayList<Object>) entries.get("episodes");
                for (Object e : episodes_array) {
                    entries = (LinkedHashMap<String, Object>) e;
                    final String episode = entries.get("episodeId").toString();
                    final String slug = entries.get("slug").toString();
                    if (episode_number.equals(episode) && episode_name.equals(slug)) {
                        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "languages");
                        String[] languages_array = entries.keySet().toArray(new String[0]);
                        // Fog: Now grab the video and subtitle files for each language
                        for (String l : languages_array) {
                            // Get video links
                            LinkedHashMap<String, Object> languages_entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, l + "/alpha/" + alpha);
                            final String id = languages_entries.get("experienceId").toString();
                            final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                            SecureRandom rnd = new SecureRandom();
                            StringBuilder sb = new StringBuilder(8);
                            for (int i = 0; i < 8; i++) {
                                sb.append(AB.charAt(rnd.nextInt(AB.length())));
                            }
                            final String random = sb.toString();
                            ((jd.plugins.hoster.FunimationCom) plugin).getPage(br, SHOWEXPERIENCE_API + id + "/?pinst_id=" + random);
                            json_source = br.toString();
                            if (json_source.contains("\"errors\"") || json_source.contains("Invalid")) {
                                decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Unable to grab video links"));
                                return decryptedLinks;
                            }
                            json_source = Encoding.htmlDecode(json_source);
                            LinkedHashMap<String, Object> video_entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
                            final ArrayList<Object> items_array = (ArrayList<Object>) video_entries.get("items");
                            for (Object i : items_array) {
                                video_entries = (LinkedHashMap<String, Object>) i;
                                final String src = video_entries.get("src").toString();
                                if (src.contains(".mp4")) {
                                    final String filename = title + "-" + l + "-mobile.mp4";
                                    final DownloadLink dl = createDownloadlink(src);
                                    filePackage.add(dl);
                                    dl.setFinalFileName(filename);
                                    dl.setContentUrl(cryptedLink.getCryptedUrl());
                                    dl.setLinkID(filename);
                                    dl.setAvailable(true);
                                    decryptedLinks.add(dl);
                                } else {
                                    final Browser br = this.br.cloneBrowser();
                                    ((jd.plugins.hoster.FunimationCom) plugin).getPage(br, src);
                                    final List<HlsContainer> qualities = HlsContainer.getHlsQualities(br);
                                    for (final HlsContainer h : qualities) {
                                        final String quality = h.getResolution();
                                        final String filename = title + "-" + l + "-" + quality + h.getFileExtension();
                                        final DownloadLink dl = createDownloadlink(h.getDownloadurl());
                                        filePackage.add(dl);
                                        dl.setFinalFileName(filename);
                                        dl.setContentUrl(cryptedLink.getCryptedUrl());
                                        dl.setLinkID(filename);
                                        dl.setAvailable(true);
                                        decryptedLinks.add(dl);
                                    }
                                }
                            }
                            // Get subtitles if available
                            ((jd.plugins.hoster.FunimationCom) plugin).getPage(br, EXPERIENCE_API + id);
                            json_source = br.toString();
                            json_source = Encoding.htmlDecode(json_source);
                            if (json_source.contains("Sorry, but the page you're looking for doesn't exist.")) {
                                decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Unable to grab subtitle links"));
                                return decryptedLinks;
                            }
                            LinkedHashMap<String, Object> subtitle_entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
                            final ArrayList<Object> seasons_array_sub = (ArrayList<Object>) subtitle_entries.get("seasons");
                            for (Object s2 : seasons_array_sub) {
                                subtitle_entries = (LinkedHashMap<String, Object>) s2;
                                final String season_sub = subtitle_entries.get("seasonId").toString();
                                if (season_number.equals(season_sub)) {
                                    final ArrayList<Object> episodes_array_sub = (ArrayList<Object>) subtitle_entries.get("episodes");
                                    for (Object e2 : episodes_array_sub) {
                                        subtitle_entries = (LinkedHashMap<String, Object>) e2;
                                        final String episode_subs = subtitle_entries.get("episodeId").toString();
                                        final String slug_subs = subtitle_entries.get("slug").toString();
                                        if (episode_number.equals(episode_subs) && episode_name.equals(slug_subs)) {
                                            subtitle_entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(subtitle_entries, "languages/" + l + "/alpha/" + alpha);
                                            final ArrayList<Object> sources_array = (ArrayList<Object>) subtitle_entries.get("sources");
                                            // Only get the first array as the sources are the same between both arrays
                                            subtitle_entries = (LinkedHashMap<String, Object>) sources_array.get(0);
                                            final ArrayList<Object> text_tracks_array = (ArrayList<Object>) subtitle_entries.get("textTracks");
                                            for (Object t : text_tracks_array) {
                                                subtitle_entries = (LinkedHashMap<String, Object>) t;
                                                final String src = subtitle_entries.get("src").toString();
                                                if (src.contains(".srt")) {
                                                    final String filename = title + "-" + l + ".srt";
                                                    final DownloadLink dl = createDownloadlink(src);
                                                    filePackage.add(dl);
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
                        }
                    }
                }
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