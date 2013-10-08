//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "apple.com" }, urls = { "http://[\\w\\.]*?apple\\.com/trailers/(disney|dreamworks|filmdistrict|focus_features|fox|fox_searchlight|independent|lions_gate|magnolia|marvel|paramount|picturehouse|sony|sony_pictures|summit|(universial|universal)|wb|(weinstein|weinstien))/[a-zA-Z0-9_]+/" }, flags = { 0 })
public class AppleTrailer extends PluginForDecrypt {

    /**
     * @author raztoki
     * */
    public AppleTrailer(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser                 br1            = new Browser();
    private Browser                 br2            = null;
    private boolean                 itunes         = false;
    private boolean                 poster         = false;
    private boolean                 tryposter      = false;
    private static boolean          loaded         = false;
    private String                  parameter      = null;
    private String                  title          = null;
    private ArrayList<DownloadLink> decryptedLinks = null;
    private ArrayList<DownloadLink> SD             = new ArrayList<DownloadLink>();
    private ArrayList<DownloadLink> HD             = new ArrayList<DownloadLink>();
    private HashSet<String>         dupe           = null;

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // prevent results from carring over when debugging
        decryptedLinks = new ArrayList<DownloadLink>();
        dupe = new HashSet<String>();

        // cleanup required
        parameter = param.toString().replaceAll("://(\\w+\\.)?apple", "://trailers.apple");

        // prevent agent detection, mainly due to the fail over method to the poster retry method..
        if (!loaded) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            loaded = true;
        }
        br1.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());

        // make sure they don't have any stupid redirects here
        br1.setFollowRedirects(true);
        br1.getPage(parameter);
        br1.setFollowRedirects(false);

        if (br1.containsHTML(">Hmm, the page you’re looking for can’t be found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        title = br1.getRegex("var trailerTitle\\s+= '(.*?)';").getMatch(0);
        if (title == null) {
            title = br1.getRegex("name=\"omni_page\" content=\"Movie Trailers - (.*?)\"").getMatch(0);
            // itunes page layout!
            if (title == null) {
                title = br1.getRegex("<title>(.*) - Movie Trailers - iTunes</title>").getMatch(0);
                if (title != null) itunes = true;
            }
        }

        br2 = br1.cloneBrowser();

        if (itunes) {
            processItunes();
        } else {
            processNormal();
        }
        // poster checks required!
        if (tryposter) {
            poster = true;

            title = br1.getRegex("<title>Apple - Trailers - (.*?)( - In Theaters.*)?</title>").getMatch(0);
            if (title == null) title = br1.getRegex("<meta name=\"Keywords\" content=\"(.*?) Trailer").getMatch(0);

            String[] results = br1.getRegex("<a href=\"(\\w+\\.html|([^\"]+)?hd/)\"([^>]+)?><img[^>]+").getColumn(0);

            if (results != null && results.length != 0) {
                for (String result : results) {
                    if (dupe.add(br1.getURL() + result) == false) continue;

                    // goto each page! find the final video link!
                    br2 = br1.cloneBrowser();
                    br2.getPage(result);
                    if (result.endsWith("hd/") && br2.containsHTML("- iTunes</title>")) {
                        processItunes();
                    } else {
                        String url = br2.getRegex("href','(https?://[^/]+apple\\.com/[^\"']+\\d+\\.mov)").getMatch(0);
                        if (url == null) url = br2.getRegex("(https?://[^/]+apple\\.com/[^\"']+\\d+\\.mov)").getMatch(0);
                        if (url != null) {
                            if (dupe.add(url) == false) continue;
                            String name = br2.getRegex("<title>Apple - Trailers - .*? - (.*?)( - (low|medium|high|small|medium|large))?</title>").getMatch(0);
                            if (name != null && name.matches("(?i-)(low|medium|high|small|medium|large)") || name == null) name = "Trailer";
                            String psize = new Regex(url, "(\\d+)\\.mov$").getMatch(0);
                            if (name != null) name = title + " - " + name + " (" + psize + "p_SD).mov";
                            url = url.replace("/trailers.apple.com/", "/trailers.appledecrypted.com/");
                            DownloadLink dlLink = createDownloadlink(url);
                            dlLink.setFinalFileName(name);
                            dlLink.setAvailable(true);
                            dlLink.setProperty("Referer", br1.getURL());
                            SD.add(dlLink);

                        } else {
                            logger.warning("Possible plugin error! Please confirm if videos are present in your browser. If so, please report plugin error to JDownloader Development Team! page : " + br2.getURL() + " parameter : " + parameter);
                        }
                    }
                }
            }
        }

        if (!HD.isEmpty() || !SD.isEmpty()) {
            if (!HD.isEmpty() && SD.isEmpty()) {
                decryptedLinks.addAll(HD);
            } else if (HD.isEmpty() && !SD.isEmpty()) {
                decryptedLinks.addAll(SD);
            } else {
                // prompt user to which format they rather.
                String[] mirrors = new String[] { "High Definition", "Standard Definition" };
                int[] selectedMirrorsIndices = null;
                try {
                    selectedMirrorsIndices = UserIO.getInstance().requestMultiSelectionDialog(0, JDL.L("plugins.decrypter.appletraider.SelectHostersHeadline", "Please select the definition format you desire"), JDL.L("plugins.decrypter.appletrailer.SelectHosters", "Please select the definition formats you prefer\r\n" + parameter), mirrors, null, null, null, null);
                } catch (Throwable e) {
                    // remove when jd2 goes stable
                    int selectedMirror = UserIO.getInstance().requestComboDialog(0, JDL.L("plugins.decrypter.appletraider.SelectHostersHeadline", "Please select the definition format you desire"), JDL.L("plugins.decrypter.appletrailer.SelectHosters", "Please select the definition format you prefer\r\n" + parameter), mirrors, 0, null, null, null, null);
                    if (selectedMirror < 0) return null;
                    selectedMirrorsIndices = new int[] { selectedMirror };
                }
                if (selectedMirrorsIndices == null) {
                    // respect cancel!
                    return new ArrayList<DownloadLink>();
                } else {
                    // select all when they select none!
                    selectedMirrorsIndices = new int[] { 0, 1 };
                }
                for (int i : selectedMirrorsIndices) {
                    if (i == 0) {
                        decryptedLinks.addAll(HD);
                    } else if (i == 1) {
                        decryptedLinks.addAll(SD);
                    }
                }
            }
        }

        if (title != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(title.trim());
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private Browser prepAjax(Browser prepBr) {
        prepBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        prepBr.getHeaders().put("X-Prototype-Version", "1.7");
        prepBr.getHeaders().put("Accept-Charset", null);
        return prepBr;
    }

    private void processNormal() throws IOException, PluginException {
        boolean isNew = false;
        if (!br2.getURL().endsWith("includes/playlists/web.inc")) {
            prepAjax(br2);
            br2.getHeaders().put("Accept", "text/xml");
            br2.getPage("includes/playlists/web.inc");
        }

        if (br2.getHttpConnection().getResponseCode() == 404) {
            tryposter = true;
            return;
        }

        if (title == null) title = br2.getRegex("var trailerTitle = '(.*?)';").getMatch(0);
        if (title == null) {
            logger.warning("Plugin defect, could not find 'title' : " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String[] hits = br2.getRegex("(<li class=('|\")trailer ([a-z]+)?('|\")>.*?</li><)").getColumn(0);
        if (hits == null || hits.length == 0) {
            String test = br2.getRegex("<a href='(includes/large\\.html#videos[^']+)'").getMatch(0);
            if (test != null) {
                // 20131007
                isNew = true;
                br2 = br1.cloneBrowser();
                prepAjax(br2);
                br2.getHeaders().put("Accept", "text/xml");
                br2.getPage(test);
                hits = br2.getRegex("(<li class=('|\")trailer ([a-z0-9]+)?('|\")>.*?</li><)").getColumn(0);
            }
            if (hits == null || hits.length == 0) {
                logger.warning("Plugin defect, could not find 'hits' : " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (hits.length == 1) {
            hits = new String[] { br2.toString() };
        }

        for (String hit : hits) {
            String hitname = new Regex(hit, "<h3[^>]*>(.*?)</h3>").getMatch(0);
            if (hitname == null) {
                logger.warning("Plugin defect, could not find 'hitname' : " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String filename = title + " - " + hitname;

            // mostly the remainder of the old code, only useful when they show 'download links'
            String[] oldHits = new Regex(hit, "class=\"hd\".*?href=\"((http://.*?apple[^<>]*?|/[^<>]*?)_h?\\d+p\\.mov)\"").getColumn(0);
            if (oldHits != null && oldHits.length != 0) {
                for (String oldHit : oldHits) {
                    /* correct url */
                    String url = oldHit.replaceFirst("movies\\.", "www.");
                    if (dupe.add(url) == false) continue;
                    /* get format */
                    String format = new Regex(url, "_h?(\\d+)p").getMatch(0);
                    /* get filename */
                    String fname = filename + " (" + format + "p_HD)" + url.substring(url.lastIndexOf("."));
                    if (fname == null || format == null) continue;
                    /* get size */
                    String size = new Regex(hit, "class=\"hd\".*?>.*?" + oldHit + ".*?" + format + "p \\((\\d+ ?MB)\\)").getMatch(0);
                    /* correct url if its relative */
                    if (!url.startsWith("http")) url = "http://trailers.apple.com" + url;
                    DownloadLink dlLink = createDownloadlink(url.replace(".apple.com", ".appledecrypted.com"));
                    if (size != null) dlLink.setDownloadSize(SizeFormatter.getSize(size));
                    dlLink.setFinalFileName(fname);
                    dlLink.setProperty("Referer", br1.getURL());
                    dlLink.setAvailable(true);
                    HD.add(dlLink);
                }
            } else {
                // new stuff, no need todo this if the provide the download links, this gets it out of js for playing in quicktime
                if (isNew) {
                    // 20131007
                    String url = new Regex(hit, "href=\"([^\"]+)#[^>]+>").getMatch(0);
                    if (url != null) {
                        if (dupe.add(url) == false) continue;
                        br2 = br1.cloneBrowser();
                        prepAjax(br2);
                        br2.getHeaders().put("Accept", "text/xml");
                        br2.getPage(url);
                        url = br2.getRegex("href=\"([^\\?\"]+).*?\">Click to Play</a>").getMatch(0);
                        if (url == null) {
                            logger.warning("Plugin defect, could not find 'url' on page : " + br2.getURL() + " from parameter : " + parameter);
                            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            continue;
                        }
                        if (dupe.add(url) == false) continue;
                        String extension = url.substring(url.lastIndexOf("."));
                        url = url.replace("apple.com/", "appledecrypted.com/");
                        String pSize = new Regex(url, "(\\d+)p?\\.mov").getMatch(0);
                        DownloadLink dlLink = createDownloadlink(url);
                        dlLink.setFinalFileName(filename + " (" + pSize + "_SD)" + extension);
                        dlLink.setAvailable(true);
                        dlLink.setProperty("Referer", br1.getURL());
                        decryptedLinks.add(dlLink);
                    }
                } else {
                    String[] vids = new Regex(hit, "<li class=\"hd\">(.*?)</li>").getColumn(0);
                    if (vids == null || vids.length == 0) {
                        logger.warning("Plugin defect, could not find 'vids' : " + parameter);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    for (String vid : vids) {
                        String[][] matches = new Regex(vid, "href=\"([^\"]+)#[^>]+>(.*?)</a>").getMatches();
                        if (matches == null || matches.length == 0) {
                            logger.warning("Plugin defect, could not find 'matches' : " + parameter);
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        for (String[] match : matches) {
                            String url = match[0];
                            String video_name = filename + " (" + match[1].replaceFirst("<span>", "_").replaceFirst("</span>", "") + ")";
                            br2 = br1.cloneBrowser();
                            url = url.replace("includes/", "includes/" + hitname.toLowerCase().replace(" ", "").replaceAll("[^a-zA-Z0-9]", "") + "/");
                            if (dupe.add(url) == false) continue;
                            br2.getPage(url);
                            url = br2.getRegex("href=\"([^\\?\"]+).*?\">Click to Play</a>").getMatch(0);
                            if (url == null) {
                                logger.warning("Plugin defect, could not find 'url' on page : " + br2.getURL() + " from parameter : " + parameter);
                                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                                continue;
                            }
                            if (dupe.add(url) == false) continue;
                            url = url.replace("apple.com/", "appledecrypted.com/");
                            String extension = url.substring(url.lastIndexOf("."));
                            DownloadLink dlLink = createDownloadlink(url);
                            dlLink.setFinalFileName(video_name + extension);
                            dlLink.setAvailable(true);
                            dlLink.setProperty("Referer", br1.getURL());
                            decryptedLinks.add(dlLink);
                        }
                    }
                }
            }
        }
    }

    private void processItunes() throws IOException, PluginException {

        if (!br2.getURL().endsWith("includes/playlists/web.inc")) br2.getPage("includes/playlists/web.inc");

        String[] names = br2.getRegex("<span class=\"text\">(.*?)</span></li>").getColumn(0);
        // from when it comes from a poster
        if ((names == null || names.length == 0) && poster) {
            names = new String[] { title + "Trailer" };
        } else if (names == null || names.length == 0) {
            // single entries can contain a <h3> value like processNormal
            names = br2.getRegex("<h3>(.*?)</h3>").getColumn(0);
        }
        String[] hits = br2.getRegex("(<div class=\"section.+?</ul></div>)").getColumn(0);

        if ((hits == null || hits.length == 0) || (names == null || names.length == 0)) {
            logger.warning("Plugin defect, could not find 'filters or names' : " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int h = -1;
        for (String hit : hits) {
            h++;
            // standard def
            String[] sdFilter = new Regex(hit, "(<li><a href.*?</li>)").getColumn(0);
            // high def
            String[] hdFilter = new Regex(hit, "(<li><a class=\"hd\".*?</li>)").getColumn(0);
            String[] hdSizes = new Regex(hit, "<li class=\"tag\">(\\d+ MB)</li>").getColumn(0);

            if (sdFilter != null && sdFilter.length != 0) {
                for (String sd : sdFilter) {
                    String url = new Regex(sd, "(https?://[^/]+apple\\.com/[^\\?'\"]+\\.mov)").getMatch(0);
                    if (dupe.add(url) == false) continue;
                    String pSize = new Regex(url, "(\\d+)\\.mov").getMatch(0);
                    String name = title + " - " + names[h] + " (" + pSize + "_SD).mov";
                    DownloadLink dlLink = createDownloadlink(url.replace(".apple.com", ".appledecrypted.com"));
                    dlLink.setFinalFileName(name);
                    dlLink.setProperty("Referer", br1.getURL());
                    dlLink.setAvailable(true);
                    SD.add(dlLink);
                }
            }
            if (hdFilter != null && hdFilter.length != 0) {
                int z = -1;
                for (String hd : hdFilter) {
                    z++;
                    String url = new Regex(hd, "(https?://[^/]+apple\\.com/[^\\?'\"]+\\.mov)").getMatch(0);
                    if (dupe.add(url) == false) continue;
                    String pSize = new Regex(url, "(\\d+)p\\.mov").getMatch(0);
                    String name = title + " - " + names[h] + " (" + pSize + "_HD).mov";
                    String size = hdSizes[z];
                    DownloadLink dlLink = createDownloadlink(url.replace(".apple.com", ".appledecrypted.com"));
                    if (size != null) dlLink.setDownloadSize(SizeFormatter.getSize(size));
                    dlLink.setFinalFileName(name);
                    dlLink.setProperty("Referer", br1.getURL());
                    dlLink.setAvailable(true);
                    HD.add(dlLink);
                }
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}