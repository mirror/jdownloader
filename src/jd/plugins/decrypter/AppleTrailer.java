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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "trailers.apple.com" }, urls = { "https?://[\\w\\.]*?apple\\.com/trailers/(?:disney|dreamworks|entertainmentone|filmdistrict|focus_features|fox|fox_searchlight|independent|ifcfilms|lions_gate|lucasfilm|magnolia|marvel|mgm|oscilloscope|paramount|picturehouse|relativity|roadsideattractions|sony|sony_pictures|summit|(?:universial|universal)|wb|(?:weinstein|weinstien))/([a-zA-Z0-9_\\-]+)/" })
public class AppleTrailer extends PluginForDecrypt {

    public AppleTrailer(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    // public int getMaxConcurrentProcessingInstances() {
    // return 1;
    // }

    private String                        parameter      = null;
    private String                        title          = null;
    private final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();;
    private final HashSet<String>         dupe           = new HashSet<String>();

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // prevent results from carying over when debugging
        decryptedLinks.clear();
        dupe.clear();

        // cleanup required
        parameter = param.toString().replaceAll("://(\\w+\\.)?apple", "://trailers.apple");
        br = new Browser();
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());

        // make sure they don't have any stupid redirects here
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter, new Regex(parameter, this.getSupportedLinks()).getMatch(0), null));
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        // this is version 4
        if (isVersion4()) {
            // 02-2016-2017+ (first in jd)}
            // http://trailers.apple.com/trailers/independent/valerian-and-the-city-of-a-thousand-planets/
            processVersion4();

        } else if (isVersion3()) {
            // http://trailers.apple.com/trailers/fox/thefantasticfour/
            // 2015(date made) http://www.imdb.com/title/tt1502712/
            processVersion3();

        } else if (isItunes()) {
            // http://trailers.apple.com/trailers/independent/myoneandonly/
            // 2009(date made) http://www.imdb.com/title/tt1185431/
            // 25 September 2009
            processItunes(br);

        } else if (isDropdownTrigger()) {
            // http://trailers.apple.com/trailers/disney/walle/
            // 2008(date made) http://www.imdb.com/title/tt0910970/
            // 27 June 2008
            processDropdownTrigger();

        } else if (isAreaPoster()) {
            // https://trailers.apple.com/trailers/paramount/tropicthunder/
            // 2008(date made) http://www.imdb.com/title/tt0942385/
            // 15 August 2008
            processAreaPoster();

        } else if (isDivPoster()) {
            // http://trailers.apple.com/trailers/lions_gate/slowburn/
            // 2005(date made) http://www.imdb.com/title/tt0376196
            // 13 April 2007
            processDivPoster();

        } else if (isPoster()) {
            // http://trailers.apple.com/trailers/universal/curious_george/
            // 2006(date made) http://www.imdb.com/title/tt0381971/
            // 10 February 2006
            processPoster();

        } else {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        if (decryptedLinks.isEmpty()) {
            System.out.println("debug");
        }

        if (StringUtils.isNotEmpty(title)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title.trim());
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private void processItunes(final Browser br) throws Exception {
        if (title == null) {
            // when coming from other methods this is already determined.
            title = br.getRegex("<title>(.*)\\s*-\\s*Movie Trailers\\s*-\\s*iTunes</title>").getMatch(0);
            // title can have htmlEntities
            if (title == null) {
                throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
            }
            title = HTMLEntities.unhtmlentities(title);
        }
        // TODO: find one that will trigger this old piece of code.
        boolean poster = false;
        Browser br2 = br.cloneBrowser();
        {
            final String url = Request.getLocation("includes/playlists/web.inc", br2.getRequest());
            if (dupe.add(url) == false) {
                return;
            }
            br2.getPage(url);
        }
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
            final ArrayList<DownloadLink> temp = new ArrayList<DownloadLink>();
            h++;
            // standard def
            String[] sdFilter = new Regex(hit, "(<li><a href.*?</li>)").getColumn(0);
            // high def
            String[] hdFilter = new Regex(hit, "(<li><a class=\"hd\".*?</li>)").getColumn(0);
            String[] hdSizes = new Regex(hit, "<li class=\"tag\">(\\d+ MB)</li>").getColumn(0);
            // hd first, they have 2 480p. 1 within sd 1 within hd. if you place hd in temp array it will be preferred by
            // analyseusersettings
            if (hdFilter != null && hdFilter.length != 0) {
                int z = -1;
                for (String hd : hdFilter) {
                    z++;
                    String[] url = new Regex(hd, "(https?://[^/]+apple\\.com/[^\\?'\"]+\\.(mov|m4v))").getRow(0);
                    if (dupe.add(url[0]) == false) {
                        continue;
                    }
                    String pSize = new Regex(url[0], "(\\d+)p\\." + url[1]).getMatch(0);
                    String name = title + " - " + names[h] + " (" + p_q(pSize) + ")." + url[1];
                    String size = hdSizes[z];
                    DownloadLink dlLink = createDownloadlink(url[0].replace(".apple.com", ".appledecrypted.com"));
                    if (size != null) {
                        dlLink.setDownloadSize(SizeFormatter.getSize(size));
                    }
                    dlLink.setFinalFileName(name);
                    dlLink.setProperty("Referer", br.getURL());
                    dlLink.setProperty("pSize", pSize);
                    dlLink.setAvailable(true);
                    temp.add(dlLink);
                }
            }

            if (sdFilter != null && sdFilter.length != 0) {
                for (String sd : sdFilter) {
                    String[] url = new Regex(sd, "(https?://[^/]+apple\\.com/[^\\?'\"]+\\.(mov|m4v))").getRow(0);
                    if (dupe.add(url[0]) == false) {
                        continue;
                    }
                    String pSize = new Regex(url[0], "(\\d+)\\." + url[1]).getMatch(0);
                    String name = title + " - " + names[h] + " (" + p_q(pSize) + ")." + url[1];
                    DownloadLink dlLink = createDownloadlink(url[0].replace(".apple.com", ".appledecrypted.com"));
                    dlLink.setFinalFileName(name);
                    dlLink.setProperty("Referer", br.getURL());
                    dlLink.setProperty("pSize", pSize);

                    dlLink.setAvailable(true);
                    temp.add(dlLink);
                }
            }
            decryptedLinks.addAll(analyseUserSettings(temp));
        }
    }

    // only ever seen this as a single trailer result, not named within souce either. some times within the image.
    // this method and layout is a lot like divposter. this ive seen hd, though not in div.
    private void processAreaPoster() throws Exception {
        title = br.getRegex("<title>Apple\\s*-\\s*Trailers\\s*-\\s*(.*?)</title>").getMatch(0);
        // title can have htmlEntities
        if (title == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        title = HTMLEntities.unhtmlentities(title);
        final ArrayList<DownloadLink> temp = new ArrayList<DownloadLink>();
        final String[] results = getAreaPoster();
        // cant be null as isAreaPoster uses same method
        for (final String result : results) {
            String href = new Regex(result, "href\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
            href = Request.getLocation(href, br.getRequest());
            // hd/ is mentioned multiple times on page!
            if (dupe.add(href) == false) {
                continue;
            }
            final Browser br2 = br.cloneBrowser();
            br2.getPage(href);
            if (href.endsWith("/hd/")) {
                processItunes(br2);
                continue;
            }
            String url = br2.getRegex("('|\")(https?.*?\\.mov)\\1").getMatch(1);
            if (dupe.add(url) == false) {
                continue;
            }
            String psize = new Regex(url, "(\\d+)\\.mov$").getMatch(0);
            // could not see a name reference in html
            final String name = title + " - " + "Trailer" + " (" + p_q(psize) + ").mov";
            url = url.replace("/trailers.apple.com/", "/trailers.appledecrypted.com/");
            DownloadLink dlLink = createDownloadlink(url);
            dlLink.setProperty("pSize", psize);
            dlLink.setFinalFileName(name);
            dlLink.setAvailable(true);
            dlLink.setProperty("Referer", br2.getURL());
            temp.add(dlLink);
        }
        decryptedLinks.addAll(analyseUserSettings(temp));
    }

    private void processDropdownTrigger() throws Exception {
        title = br.getRegex("<title>Apple\\s*-\\s*Trailers\\s*-\\s*(.*?)</title>").getMatch(0);
        if (title == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        title = HTMLEntities.unhtmlentities(title);
        final String[] results = br.getRegex("<div[^>]+class=(\"|')dropdown-trigger[^>]+>").getColumn(-1);
        if (results != null && results.length != 0) {
            // var sizes = ['small', 'medium', 'large', 'iphone'];
            final String[] sizes = { "large", "medium", "small" };
            for (String result : results) {
                final String t_name = new Regex(result, "id\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                final String clazz = new Regex(result, "class\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                final boolean containsHD = StringUtils.contains(clazz, "dropdown-trigger-hd");
                final ArrayList<DownloadLink> temp = new ArrayList<DownloadLink>();
                for (final String size : sizes) {
                    Browser br2 = br.cloneBrowser();
                    br2.getPage(t_name + "_" + size + ".html");
                    // .mov is within javascript reference
                    String url = br2.getRegex(",\\s*('|\")(https?.*?\\.mov)\\1").getMatch(1);
                    if (dupe.add(url) == false) {
                        continue;
                    }
                    String psize = new Regex(url, "(\\d+)\\.mov$").getMatch(0);
                    final String name = title + " - " + t_name + " (" + p_q(psize) + ").mov";
                    url = url.replace("/trailers.apple.com/", "/trailers.appledecrypted.com/");
                    DownloadLink dlLink = createDownloadlink(url);
                    dlLink.setProperty("pSize", psize);
                    dlLink.setFinalFileName(name);
                    dlLink.setAvailable(true);
                    dlLink.setProperty("Referer", br2.getURL());
                    temp.add(dlLink);
                }
                if (containsHD) {
                    final Browser br2 = br.cloneBrowser();
                    // you shouldn't have dupes here
                    final String url = Request.getLocation("hd/", br2.getRequest());
                    if (dupe.add(url) == false) {
                        continue;
                    }
                    br2.getPage("hd/");
                    processItunes(br2);
                }
                decryptedLinks.addAll(analyseUserSettings(temp));
            }
        }
    }

    // should cover version 3 and 3.1
    private boolean isVersion3() {
        final boolean test = br.containsHTML("<script [^>]*src=('|\")(?:https?:)?(?://trailers\\.apple\\.com)?/trailers/global/v3(?:\\.1)?/scripts/.+?\\.js\\1");
        return test;
    }

    private boolean isDropdownTrigger() {
        // url + /code.js also
        final boolean result = br.containsHTML("<div[^>]+class=(\"|')dropdown-trigger");
        return result;
    }

    private boolean isVersion4() {
        final boolean result = br.containsHTML("<script [^>]+/trailers/global/v4/scripts/");
        return result;
    }

    private boolean isDivPoster() {
        // cant use this as its shared between different templates/cms.
        // final boolean result = br.containsHTML("//images\\.apple\\.com/global/metrics/js/s_code_h\\.js");
        final String[] results = getDivPoster();
        final boolean result = results != null && results.length > 0 ? true : false;
        return result;
    }

    final String[] getDivPoster() {
        final String[] results = br.getRegex("<div class=('|\")(\\w+)\\1><a href=('|\")(\\2\\.html)\\3>\\2</a>").getColumn(3);
        return results;
    }

    private boolean isAreaPoster() {
        // only single js again https://images.apple.com/global/metrics/js/s_code_h.js"
        final String[] results = getAreaPoster();
        final boolean result = results != null && results.length > 0 ? true : false;
        return result;
    }

    final String[] getAreaPoster() {
        final String[] results = br.getRegex("<area [^>]+href=('|\")(hd/|(?:trailer_)?\\w+\\.html)\\1[^>]*").getColumn(-1);
        return results;
    }

    private String[] getPoster() {
        final String[] results = br.getRegex("<a href=\"(\\w+\\.html|([^\"]+)?hd/)\"([^>]+)?>\\s*<img[^>]+").getColumn(0);
        return results;
    }

    private boolean isPoster() {
        final String[] results = getPoster();
        final boolean result = results != null && results.length > 0 ? true : false;
        return result;
    }

    private boolean isItunes() {
        // use the topic as its always static
        if (br.containsHTML(" - iTunes</title>")) {
            return true;
        }
        return false;
    }

    /**
     * could be a change of poster? copy paste of poster and changed to match the test url
     *
     * @throws Exception
     *             *
     */
    private void processDivPoster() throws Exception {
        title = br.getRegex("<title>Apple - Trailers - (.*?)</title>").getMatch(0);
        if (title == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        title = HTMLEntities.unhtmlentities(title);
        // could have multiples
        final String[] results = getDivPoster();
        Browser br2 = br.cloneBrowser();
        final ArrayList<DownloadLink> temp = new ArrayList<DownloadLink>();
        String name = br2.getRegex("<title>Apple\\s*-\\s*Trailers\\s*-\\s*.*? - (.*?)( - (low|medium|high|small|medium|large))?</title>").getMatch(0);
        if (name != null && name.matches("(?i-)(low|medium|high|small|medium|large)") || name == null) {
            name = "Trailer";
        }
        for (String result : results) {
            result = Request.getLocation(result, br.getRequest());
            if (dupe.add(result) == false) {
                continue;
            }
            // goto each page! find the final video link!
            br2 = br.cloneBrowser();
            br2.getPage(result);
            String url = br2.getRegex("href','(https?://[^/]+apple\\.com/[^\"']+\\d+\\.mov)").getMatch(0);
            if (url == null) {
                url = br2.getRegex("(https?://[^/]+apple\\.com/[^\"']+\\d+\\.mov)").getMatch(0);
            }
            if (url != null) {
                if (dupe.add(url) == false) {
                    continue;
                }
                String psize = new Regex(url, "(\\d+)\\.mov$").getMatch(0);
                if (name != null) {
                    name = title + " - " + name + " (" + p_q(psize) + ").mov";
                }
                url = url.replace("/trailers.apple.com/", "/trailers.appledecrypted.com/");
                DownloadLink dlLink = createDownloadlink(url);
                dlLink.setProperty("pSize", psize);
                dlLink.setFinalFileName(name);
                dlLink.setAvailable(true);
                dlLink.setProperty("Referer", br.getURL());
                temp.add(dlLink);

            } else {
                logger.warning("Possible plugin error! Please confirm if videos are present in your browser. If so, please report plugin error to JDownloader Development Team! page : " + br2.getURL() + " parameter : " + parameter);
            }
        }
        decryptedLinks.addAll(analyseUserSettings(temp));
    }

    private void processPoster() throws Exception {
        title = br.getRegex("<title>Apple\\s*-\\s*Trailers\\s*-\\s*(.*?)( - In Theaters.*)?</title>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<meta name=\"Keywords\" content=\"(.*?) Trailer").getMatch(0);
            // final fail over
            {
                // http://trailers.apple.com/trailers/paramount/team_america/
                final String a = br.getRegex("<title>(.*?)</title>").getMatch(0);
                final String b = br.getRegex("<meta name=\"Keywords\" content=\"(.*?),").getMatch(0);
                if (a != null && b != null && a.equals(b)) {
                    title = a;
                }
            }
            if (title == null) {
                throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
            }
        }
        title = HTMLEntities.unhtmlentities(title);
        // once agian no reference to video name, the new code for determinign best, requires to be run in arraylist of there own titles...
        // otherwise just one video shows.
        final String[] results = getPoster();
        if (results != null && results.length != 0) {
            final HashMap<String, ArrayList<DownloadLink>> temp = new HashMap<String, ArrayList<DownloadLink>>();
            for (String result : results) {
                result = Request.getLocation(result, br.getRequest());
                if (dupe.add(result) == false) {
                    continue;
                }
                // goto each page! find the final video link!
                Browser br2 = br.cloneBrowser();
                br2.getPage(result);
                if (result.endsWith("hd/") && br2.containsHTML("- iTunes</title>")) {
                    processItunes(br2);
                    continue;
                }
                String url = br2.getRegex("href','(https?://[^/]+apple\\.com/[^\"']+\\d+\\.mov)").getMatch(0);
                if (url == null) {
                    url = br2.getRegex("(https?://[^/]+apple\\.com/[^\"']+\\d+\\.mov)").getMatch(0);
                }
                if (url != null) {
                    if (dupe.add(url) == false) {
                        continue;
                    }
                    String trailerName = br2.getRegex("<title>Apple - Trailers - .*?(?: -){1,2} (.*?)(?: - (?:low|medium|high|small|medium|large))?</title>").getMatch(0);
                    if (trailerName != null && trailerName.matches("(?i-)(low|medium|high|small|medium|large)") || trailerName == null) {
                        trailerName = "Trailer";
                    }
                    String psize = new Regex(url, "(\\d+)\\.mov$").getMatch(0);
                    final String name = title + " - " + trailerName + " (" + p_q(psize) + ").mov";
                    url = url.replace("/trailers.apple.com/", "/trailers.appledecrypted.com/");
                    DownloadLink dlLink = createDownloadlink(url);
                    dlLink.setProperty("pSize", psize);
                    dlLink.setFinalFileName(name);
                    dlLink.setAvailable(true);
                    dlLink.setProperty("Referer", br.getURL());
                    final ArrayList<DownloadLink> holder;
                    if (temp.containsKey(trailerName)) {
                        holder = temp.get(trailerName);
                    } else {
                        holder = new ArrayList<DownloadLink>();
                    }
                    holder.add(dlLink);
                    temp.put(trailerName, holder);
                } else {
                    logger.warning("Possible plugin error! Please confirm if videos are present in your browser. If so, please report plugin error to JDownloader Development Team! page : " + br2.getURL() + " parameter : " + parameter);
                }
            }
            // for each entry in HashMap we need analyse.
            for (final Entry<String, ArrayList<DownloadLink>> entry : temp.entrySet()) {
                decryptedLinks.addAll(analyseUserSettings(entry.getValue()));
            }
        }
    }

    private Browser prepAjax(Browser prepBr) {
        prepBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        prepBr.getHeaders().put("X-Prototype-Version", "1.7");
        prepBr.getHeaders().put("Accept-Charset", null);
        return prepBr;
    }

    private void processVersion3() throws Exception {
        boolean isNew = false;
        Browser br2 = br.cloneBrowser();
        prepAjax(br2);
        br2.getHeaders().put("Accept", "text/xml");
        br2.getPage("includes/playlists/web.inc");

        if (br2.getHttpConnection().getResponseCode() == 404) {
            // tryposter = true;
            return;
        }
        title = br.getRegex("var trailerTitle\\s*=\\s*'(.*?)';").getMatch(0);
        if (title == null) {
            logger.warning("Plugin defect, could not find 'title' : " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = HTMLEntities.unhtmlentities(title);
        String[] hits = br2.getRegex("(<li class=('|\")trailer ([a-z]+)?('|\")>.*?</li><)").getColumn(0);
        if (hits == null || hits.length == 0) {
            String test = br2.getRegex("<a href='(includes/large\\.html#videos[^']+)'").getMatch(0);
            if (test != null) {
                // 20131007
                isNew = true;
                br2 = br.cloneBrowser();
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
            final ArrayList<DownloadLink> temp = new ArrayList<DownloadLink>();
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
                    if (dupe.add(url) == false) {
                        continue;
                    }
                    /* get format */
                    String format = new Regex(url, "_h?(\\d+)p").getMatch(0);
                    /* get filename */
                    String fname = filename + " (" + p_q(format) + ")" + url.substring(url.lastIndexOf("."));
                    if (fname == null || format == null) {
                        continue;
                    }
                    /* get size */
                    String size = new Regex(hit, "class=\"hd\".*?>.*?" + oldHit + ".*?" + format + "p \\((\\d+ ?MB)\\)").getMatch(0);
                    url = Request.getLocation(url, br2.getRequest());
                    DownloadLink dlLink = createDownloadlink(url.replace(".apple.com", ".appledecrypted.com"));
                    if (size != null) {
                        dlLink.setDownloadSize(SizeFormatter.getSize(size));
                    }
                    dlLink.setProperty("pSize", format);
                    dlLink.setFinalFileName(fname);
                    dlLink.setProperty("Referer", br.getURL());
                    dlLink.setAvailable(true);
                    temp.add(dlLink);
                }
            } else {
                // new stuff, no need todo this if the provide the download links, this gets it out of js for playing in quicktime
                if (isNew) {
                    // 20131007
                    String url = new Regex(hit, "href=\"([^\"]+)#[^>]+>").getMatch(0);
                    if (url != null) {
                        if (dupe.add(url) == false) {
                            continue;
                        }
                        br2 = br.cloneBrowser();
                        prepAjax(br2);
                        br2.getHeaders().put("Accept", "text/xml");
                        br2.getPage(url);
                        url = br2.getRegex("href=\"([^\\?\"]+).*?\">Click to Play</a>").getMatch(0);
                        if (url == null) {
                            logger.warning("Plugin defect, could not find 'url' on page : " + br2.getURL() + " from parameter : " + parameter);
                            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            continue;
                        }
                        if (dupe.add(url) == false) {
                            continue;
                        }
                        // here website has another requests which gets the url found above.
                        // url = videoSrc.substring(0, videoSrc.lastIndexOf('_')) + '.json';
                        // but its always the same 3 p' so lets just construct.
                        String extension = url.substring(url.lastIndexOf("."));
                        url = url.replace("apple.com/", "appledecrypted.com/");
                        String pSize = new Regex(url, "(\\d+)p?\\.(mov|m4v)").getMatch(0);
                        DownloadLink dlLink = createDownloadlink(url);
                        dlLink.setFinalFileName(filename + " (" + p_q(pSize) + ")" + extension);
                        dlLink.setAvailable(true);
                        dlLink.setProperty("Referer", br.getURL());
                        dlLink.setProperty("pSize", pSize);
                        temp.add(dlLink);
                        // lets see if we can add the other formats 20140224, generally found links are 480 others are cock blocked.
                        ArrayList<String> p = new ArrayList<String>(Arrays.asList(new String[] { "480", "720", "1080" }));
                        // remove what we have already added, maybe they change the default qual from 480
                        p.remove(pSize);
                        while (p.size() != 0) {
                            final String n = p.get(0);
                            final String u = url.replace(pSize, n);
                            if (dupe.add(u) == false) {
                                continue;
                            }
                            DownloadLink d = createDownloadlink(u);
                            d.setFinalFileName(filename + " (" + p_q(n) + ")" + extension);
                            d.setAvailable(true);
                            d.setProperty("Referer", br.getURL());
                            d.setProperty("pSize", n);
                            temp.add(d);
                            p.remove(n);
                        }
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
                            br2 = br.cloneBrowser();
                            url = url.replace("includes/", "includes/" + hitname.toLowerCase().replace(" ", "").replaceAll("[^a-zA-Z0-9]", "") + "/");
                            if (dupe.add(url) == false) {
                                continue;
                            }
                            br2.getPage(url);
                            url = br2.getRegex("href=\"([^\\?\"]+).*?\">Click to Play</a>").getMatch(0);
                            if (url == null) {
                                logger.warning("Plugin defect, could not find 'url' on page : " + br2.getURL() + " from parameter : " + parameter);
                                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                                continue;
                            }
                            if (dupe.add(url) == false) {
                                continue;
                            }
                            url = url.replace("apple.com/", "appledecrypted.com/");
                            String extension = url.substring(url.lastIndexOf("."));
                            DownloadLink dlLink = createDownloadlink(url);
                            dlLink.setFinalFileName(video_name + extension);
                            dlLink.setAvailable(true);
                            dlLink.setProperty("Referer", br.getURL());
                            temp.add(dlLink);
                        }
                    }
                }
            }
            decryptedLinks.addAll(analyseUserSettings(temp));
        }

    }

    private void processVersion4() throws Exception {
        final String filmID = br.getRegex("var\\s*FilmId\\s*=\\s*'(\\d+)'").getMatch(0);
        if (filmID == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        Browser br2 = br.cloneBrowser();
        br2.getPage("//trailers.apple.com/trailers/feeds/data/" + filmID + ".json");
        final LinkedHashMap<String, Object> json;
        try {
            if (br2.containsHTML("404 - Page Not Found")) {
                return;
            }
            json = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br2.toString());
        } catch (Exception e) {
            return;
        }
        final Map<String, Object> page = (Map<String, Object>) json.get("page");
        if (page != null) {
            title = (String) page.get("movie_title");
            final List<Map<String, Object>> clips = (List<Map<String, Object>>) json.get("clips");
            for (final Map<String, Object> clip : clips) {
                final String clipTitle = (String) clip.get("title");
                final String[] sizes = new Regex(clip.toString(), "src=((https?://.*?apple[^<>]*?|/[^<>]*?)_h?\\d+p\\.mov)").getColumn(0);
                if (sizes != null) {
                    // temp arraylist
                    final ArrayList<DownloadLink> temp = new ArrayList<DownloadLink>();
                    for (final String size : sizes) {
                        /* correct url */
                        String url = size.replaceFirst("movies\\.", "www.");
                        if (dupe.add(url) == false) {
                            continue;
                        }
                        /* get format */
                        String format = new Regex(url, "_h?(\\d+)p").getMatch(0);
                        /* get filename */
                        String fname = title + "-" + clipTitle + " (" + p_q(format) + ")" + getFileNameExtensionFromString(url, ".mov");
                        if (fname == null || format == null) {
                            continue;
                        }
                        final DownloadLink dlLink = createDownloadlink(url.replace(".apple.com", ".appledecrypted.com"));
                        dlLink.setLinkID(getHost() + "://" + filmID + "/" + Hash.getMD5(clipTitle) + "/" + format);
                        dlLink.setFinalFileName(fname);
                        dlLink.setProperty("pSize", format);
                        dlLink.setProperty("Referer", br.getURL());
                        dlLink.setAvailable(true);
                        temp.add(dlLink);
                    }
                    decryptedLinks.addAll(analyseUserSettings(temp));
                }
            }
        }
    }

    private ArrayList<DownloadLink> analyseUserSettings(final ArrayList<DownloadLink> links) {
        if (this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.preferBest, jd.plugins.hoster.TrailersAppleCom.preferBest_default)) {
            int bestest = 0;
            DownloadLink bdlink = null;
            for (final DownloadLink dl : links) {
                if (dl.getStringProperty("pSize", null) != null) {
                    int p = Integer.parseInt(dl.getStringProperty("pSize"));
                    if (!isPqualityEnabled(p)) {
                        continue;
                    }
                    if (p > bestest) {
                        bestest = p;
                        bdlink = dl;
                    }
                }
            }
            if (bdlink != null) {
                final ArrayList<DownloadLink> b = new ArrayList<DownloadLink>();
                b.add(bdlink);
                return b;
            }
        }
        // we need code to still respect checkboxes when best isn't enabled
        final ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
        for (final DownloadLink dl : links) {
            if (dl.getStringProperty("pSize", null) != null) {
                int p = Integer.parseInt(dl.getStringProperty("pSize"));
                if (isPqualityEnabled(p)) {
                    results.add(dl);
                }
            }
        }
        return results.isEmpty() ? links : results;
    }

    private final boolean isPqualityEnabled(final int p) {
        if (p == 1080) {
            return this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.p1080, jd.plugins.hoster.TrailersAppleCom.p1080_default);
        } else if (p == 720) {
            return this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.p720, jd.plugins.hoster.TrailersAppleCom.p720_default);
        } else if (p == 640) {
            return this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.p640, jd.plugins.hoster.TrailersAppleCom.p720_default);
        } else if (p == 480) {
            return this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.p480, jd.plugins.hoster.TrailersAppleCom.p480_default);
        } else if (p == 360) {
            return this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.TrailersAppleCom.p360, jd.plugins.hoster.TrailersAppleCom.p360_default);
        } else {
            return true;
        }
    }

    private String p_q(final String p) {
        final int dd = Integer.parseInt(p);
        if (dd >= 720) {
            return p + "p_HD";
        } else {
            return p + "p_SD";
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}