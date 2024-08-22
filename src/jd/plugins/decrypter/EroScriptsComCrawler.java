package jd.plugins.decrypter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.utils.Files;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "discuss.eroscripts.com" }, urls = { "https?://discuss\\.eroscripts\\.com/t/([\\w\\-/]+)" })
public class EroScriptsComCrawler extends antiDDoSForDecrypt {
    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    class SmartGroup {
        EroScriptsComCrawler              plugin;
        protected String                  title;
        protected ArrayList<DownloadLink> links;
        protected ArrayList<DownloadLink> scripts;
        protected ArrayList<DownloadLink> videos;
        protected ArrayList<DownloadLink> matched;
        protected FilePackage             fp;
        protected FilePackage             myfp;
        protected int                     foundPackages = 0;

        public SmartGroup(EroScriptsComCrawler plugin, String title, ArrayList<DownloadLink> links) throws MalformedURLException {
            this.plugin = plugin;
            this.title = title;
            this.links = links;
            preloadLinks();
            findScripts();
            findVideos();
            matchMaking();
        }

        int compareString(String str1, String str2) {
            String clean1 = str1.split("\\.", 2)[0].toLowerCase().replaceAll("[',\\-_\\(\\)\\[\\]]", " ").replaceAll("\\s{2,}", " ").trim();
            String clean2 = str2.split("\\.", 2)[0].toLowerCase().replaceAll("[',\\-_\\(\\)\\[\\]]", " ").replaceAll("\\s{2,}", " ").trim();
            if (clean1.equals(clean2)) {
                return clean1.length();
            }
            if (clean1.startsWith(clean2)) {
                return clean2.length();
            }
            if (clean2.startsWith(clean1)) {
                return clean1.length();
            }
            int score = 0;
            String[] pts1 = clean1.split("\\s");
            String[] pts2 = clean2.split("\\s");
            for (String p1 : pts1) {
                for (String p2 : pts2) {
                    if (p1.equals(p2)) {
                        score += p2.length();
                    }
                }
            }
            for (String p1 : pts1) {
                for (String p2 : pts2) {
                    if (p2.equals(p1)) {
                        score += p1.length();
                    }
                }
            }
            return score;
        }

        void renameBase(DownloadLink l, String packageName, String fileBase) {
            String[] pts = l.getName().split("\\.", 2);
            if (pts.length != 2) {
                return;
            }
            l.forceForcedFileName(fileBase + "." + pts[1]);
            l.setName(fileBase + "." + pts[1]);
            l.setFinalFileName(fileBase + "." + pts[1]);
            if (!packageName.equals(title)) {
                if (myfp == null || !myfp.getName().equals(packageName)) {
                    myfp = FilePackage.getInstance();
                    myfp.setAllowMerge(true);
                    myfp.setName(packageName);
                    foundPackages += 1;
                }
            } else {
                if (fp == null) {
                    fp = FilePackage.getInstance();
                    fp.setAllowMerge(true);
                    fp.setName(title);
                    myfp = fp;
                }
            }
            myfp.add(l);
            matched.add(l);
        }

        List<Entry<DownloadLink[], Integer>> sortedMatches() {
            Map<DownloadLink[], Integer> matchScores = new HashMap<DownloadLink[], Integer>();
            for (DownloadLink v : videos) {
                for (DownloadLink s : scripts) {
                    DownloadLink[] key = { v, s };
                    matchScores.put(key, compareString(v.getName(), s.getName()));
                }
            }
            Comparator<Entry<DownloadLink[], Integer>> valueComparator = new Comparator<Entry<DownloadLink[], Integer>>() {
                @Override
                public int compare(Entry<DownloadLink[], Integer> e1, Entry<DownloadLink[], Integer> e2) {
                    return e2.getValue() - e1.getValue();
                }
            };
            List<Entry<DownloadLink[], Integer>> listOfEntries = new ArrayList<Entry<DownloadLink[], Integer>>(matchScores.entrySet());
            Collections.sort(listOfEntries, valueComparator);
            return listOfEntries;
        }

        void matchMaking() {
            matched = new ArrayList<DownloadLink>();
            if (scripts.size() == 1 && videos.size() == 1) {
                renameBase(scripts.get(0), title, title);
                renameBase(videos.get(0), title, title);
            } else {
                List<Entry<DownloadLink[], Integer>> matchScores = sortedMatches();
                Map<DownloadLink, List<DownloadLink>> videoScripts = new HashMap<DownloadLink, List<DownloadLink>>();
                for (Entry<DownloadLink[], Integer> e : matchScores) {
                    DownloadLink v = e.getKey()[0];
                    DownloadLink s = e.getKey()[1];
                    if (e.getValue() <= 2 || matched.contains(s)) {
                        continue;
                    }
                    if (!videoScripts.containsKey(v)) {
                        videoScripts.put(v, new ArrayList<DownloadLink>());
                    }
                    if (videoScripts.get(v).size() == 0 || videos.size() == 1) {
                        videoScripts.get(v).add(s);
                        matched.add(s);
                    }
                }
                for (Entry<DownloadLink, List<DownloadLink>> v : videoScripts.entrySet()) {
                    if (v.getValue().size() == 1) {
                        String scriptBase = v.getValue().get(0).getName().split("\\.", 2)[0];
                        renameBase(v.getKey(), scriptBase, scriptBase);
                        renameBase(v.getValue().get(0), scriptBase, scriptBase);
                    } else {
                        String scriptBase = "";
                        int charIndex = 0;
                        boolean nextChar = true;
                        do {
                            char c = v.getValue().get(0).getName().charAt(charIndex);
                            for (DownloadLink s : v.getValue()) {
                                if (s.getName().charAt(charIndex) != c) {
                                    nextChar = false;
                                    break;
                                }
                            }
                            if (nextChar) {
                                scriptBase += c;
                                charIndex++;
                            }
                        } while (nextChar);
                        if (charIndex > 1) {
                            renameBase(v.getKey(), scriptBase, scriptBase);
                            for (DownloadLink s : v.getValue()) {
                                String myScriptBase = s.getName().split("\\.", 2)[0];
                                renameBase(s, scriptBase, myScriptBase + scriptBase.substring(scriptBase.length()));
                            }
                        }
                    }
                }
            }
            if (fp == null) {
                if (foundPackages == 1) {
                    fp = myfp;
                } else {
                    fp = FilePackage.getInstance();
                    fp.setAllowMerge(true);
                    fp.setName(title);
                }
            }
            for (DownloadLink l : links) {
                if (!matched.contains(l)) {
                    fp.add(l);
                }
            }
        }

        public void preloadLinks() throws MalformedURLException {
            List<LazyCrawlerPlugin> decrypters = plugin.getCrawler().getSortedLazyCrawlerPlugins();
            List<LazyHostPlugin> hosters = plugin.getCrawler().getSortedLazyHostPlugins();
            // TODO: rewrite to use LinkCrawler , see FolderWatchExtension, line 539, that way filters/abort still work and everything can
            // make use of multithreading
            // let the LinkCrawler process all found links, then you can requestFileInformation them
            for (DownloadLink l : links) {
                for (LazyCrawlerPlugin p : decrypters) {
                    boolean can = false;
                    if (p.canHandle(l.getPluginPatternMatcher())) {
                        can = true;
                    } else {
                        // Attempt fix for exmaple issue: spankbang.party -> spankbang.com
                        int ih = p.getDisplayName().lastIndexOf(".");
                        URL u = new URL(l.getPluginPatternMatcher());
                        int il = u.getHost().lastIndexOf(".");
                        if (ih > 0 && il > 0) {
                            String hostBase = p.getDisplayName().substring(0, ih);
                            String linkBase = u.getHost().substring(0, il);
                            String hostPost = p.getDisplayName().substring(ih + 1);
                            String linkPost = u.getHost().substring(il + 1);
                            if (hostBase.equals(linkBase) && !hostPost.equals(linkPost)) {
                                String testUrl = l.getPluginPatternMatcher().replace(linkBase + "." + linkPost, hostBase + "." + hostPost);
                                if (p.canHandle(testUrl)) {
                                    l.setPluginPatternMatcher(testUrl);
                                    can = true;
                                }
                            }
                        }
                    }
                    if (!can) {
                        continue;
                    }
                    try {
                        final PluginForDecrypt dec = getNewPluginInstance(p);
                        CrawledLink cl = new CrawledLink(new CryptedLink(l.getPluginPatternMatcher(), l.getPluginPatternMatcher()));
                        dec.setCurrentLink(cl);
                        List<DownloadLink> result = dec.decryptIt(cl);
                        if (result.size() > 1) {
                            l.setEnabled(false);
                        } else if (result.size() == 1) {
                            DownloadLink dl = result.get(0).getDownloadLink();
                            l.setPluginPatternMatcher(dl.getPluginPatternMatcher());
                            l.setContentUrl(dl.getContentUrl());
                            l.setLinkID(dl.getLinkID());
                            l.setProperties(dl.getProperties());
                            l.setPluginPatternMatcher(dl.getPluginPatternMatcher());
                            l.setName(dl.getName());
                            break;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(e);
                    }
                }
                for (LazyHostPlugin p : hosters) {
                    if (p.canHandle(l.getPluginPatternMatcher())) {
                        try {
                            final PluginForHost host = getNewPluginInstance(p);
                            host.requestFileInformation(l);
                        } catch (Exception e) {
                            plugin.getLogger().log(e);
                        }
                        break;
                    }
                }
            }
        }

        public void findScripts() {
            scripts = new ArrayList<DownloadLink>();
            for (DownloadLink l : links) {
                if (l.getName().endsWith(".funscript")) {
                    scripts.add(l);
                }
            }
        }

        public void findVideos() {
            videos = new ArrayList<DownloadLink>();
            for (DownloadLink l : links) {
                if (!l.isEnabled()) {
                    continue;
                }
                if (l.getName() == null || l.getName().length() == 0) {
                    continue;
                }
                String ext = Files.getExtension(l.getName(), true);
                if (ext == null) {
                    continue;
                }
                if (ext.equalsIgnoreCase("mp4") || ext.equalsIgnoreCase("mkv") || ext.equalsIgnoreCase("wmv") || ext.equalsIgnoreCase("mov")) {
                    videos.add(l);
                }
            }
        }
    }

    public EroScriptsComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected void handleMatches(String[][] matches, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        for (String[] m : matches) {
            String match = br.getURL(Encoding.htmlDecode(m[0])).toString();
            if (match.contains("discuss.eroscripts.com") && !match.contains("/uploads")) {
                continue;
            }
            if (match.contains("/model/")) {
                continue;
            }
            if (match.contains("xhamster.") && !match.contains("/videos/")) {
                continue;
            }
            if (match.contains("pornhub.") && !match.contains("view_video")) {
                continue;
            }
            if (match.contains("spankbang.") && !match.contains("/video/")) {
                continue;
            }
            if (match.contains("pixiv.net")) {
                continue;
            }
            String ext = Files.getExtension(match, true);
            boolean useImages = getPluginConfig().getBooleanProperty(jd.plugins.hoster.EroScriptsCom.FETCH_IMAGES, true);
            if (!useImages) {
                if (ext != null && (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("gif") || ext.equalsIgnoreCase("bmp"))) {
                    continue;
                }
                if (match.contains("imgur.")) {
                    continue;
                }
            }
            boolean dupe = false;
            for (DownloadLink l : decryptedLinks) {
                if (l.getPluginPatternMatcher().equals(match)) {
                    dupe = true;
                    break;
                }
            }
            if (dupe) {
                continue;
            }
            DownloadLink link = this.createDownloadlink(match);
            if (m[2] != null && !m[2].isEmpty()) {
                String name = Encoding.htmlDecode(m[2]);
                if (ext != null) {
                    name += "." + ext;
                    link.setFinalFileName(name);
                } else {
                    link.setName(name);
                }
            }
            decryptedLinks.add(link);
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink parameter, ProgressController progress) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        if (account != null) {
            jd.plugins.hoster.EroScriptsCom hoster = (jd.plugins.hoster.EroScriptsCom) this.getNewPluginForHostInstance("discuss.eroscripts.com");
            hoster.login(br, account, false);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = parameter.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        br.getPage(contenturl);
        // final Storage CFG = JSonStorage.getPlainStorage("quickfilters");
        String[][] linkMatches = br.getRegex("<a[^<>]*\\shref\\s*=\\s*(?:\"|')([^#][^\\s]+)(?:\"|')([^<>]+title\\s*=\\s*(?:\\\"|')([^\\'\\\"]*)(?:\\\"|'))?").getMatches();
        handleMatches(linkMatches, ret);
        if (getPluginConfig().getBooleanProperty(jd.plugins.hoster.EroScriptsCom.FETCH_IMAGES, true)) {
            String[][] imgMatches = br.getRegex("<img[^<>]*\\ssrc\\s*=\\s*(?:\"|')([^#][^\\s]+)(?:\"|')([^<>]+alt\\s*=\\s*(?:\\\"|')([^\\'\\\"]*)(?:\\\"|'))?").getMatches();
            handleMatches(imgMatches, ret);
        }
        if (ret.size() == 0) {
            return ret;
        }
        String[][] titleMatches = br.getRegex("<meta property=\"og:title\" content=\"([^\"]+)\"").getMatches();
        if (titleMatches == null || titleMatches.length == 0) {
            return ret;
        }
        String title = Encoding.htmlDecode(titleMatches[0][0]);
        // SMART_FILENAMES: disabled since it isn't working yet
        if (getPluginConfig().getBooleanProperty(jd.plugins.hoster.EroScriptsCom.SMART_FILENAMES, false)) {
            // runSmartFileNames(title, decryptedLinks);
            SmartGroup sg = new SmartGroup(this, title, ret);
        } else {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.setAllowMerge(true);
            fp.setAllowInheritance(true);
            fp.addLinks(ret);
        }
        return ret;
    }
}
