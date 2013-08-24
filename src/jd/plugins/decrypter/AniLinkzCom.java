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
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anilinkz.com" }, urls = { "http://(www\\.)?anilinkz\\.com/[^<>\"/]+(/[^<>\"/]+)?" }, flags = { 0 })
@SuppressWarnings("deprecation")
public class AniLinkzCom extends PluginForDecrypt {

    private final String                   supported_hoster  = "(4shared\\.com|4vid\\.me|animeuploads\\.com|auengine\\.com|chia\\-anime\\.com|cizgifilmlerizle\\.com|dailymotion\\.com|gogoanime\\.com|gorillavid\\.in|mp4upload\\.com|movreel\\.com|myspace\\.com|nowvideo\\.eu|novamov\\.com|putlocker\\.com|rutube\\.ru|sockshare\\.com|stagevu\\.com|upload2\\.com|uploadc\\.com|veevr\\.com|veoh\\.com|vidbox\\.yt|video44\\.net|videobb\\.com|videobam\\.com|videofun\\.me|videonest\\.net|videoweed\\.com|videozer\\.com|vidzur\\.com|vk\\.com|yourupload\\.com|youtube\\.com|zshare\\.net|player\\.vimeo\\.com)";
    private final String                   invalid_links     = "http://(www\\.)?anilinkz\\.com/(search|affiliates|get|img|dsa|forums|files|category|\\?page=|faqs|.*?-list|.*?-info|\\?random).*?";
    private String                         parameter         = null;
    private String                         fpName            = null;
    private String                         escapeAll         = null;
    private int                            spart             = 1;
    private Browser                        br2               = new Browser();
    private ArrayList<DownloadLink>        decryptedLinks    = null;
    private boolean                        cloudflare        = false;
    private static HashMap<String, String> cloudflareCookies = new HashMap<String, String>();
    private static Object                  LOCK              = new Object();

    /**
     * @author raztoki
     * */
    public AniLinkzCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private static StringContainer agent = new StringContainer();

    public static class StringContainer {
        public String string = null;
    }

    private Browser prepBrowser(Browser prepBr) {
        if (!cloudflareCookies.isEmpty()) {
            for (final Map.Entry<String, String> cookieEntry : cloudflareCookies.entrySet()) {
                final String key = cookieEntry.getKey();
                final String value = cookieEntry.getValue();
                prepBr.setCookie(this.getHost(), key, value);
            }
        }
        if (agent.string == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent.string);
        prepBr.getHeaders().put("Referer", null);
        return prepBr;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        // testing purpose lets null/zero/false storables
        decryptedLinks = new ArrayList<DownloadLink>();
        escapeAll = null;
        spart = 1;

        parameter = param.toString();
        if (parameter.matches(invalid_links)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        // only allow one thread! To minimise/reduce loads.
        synchronized (LOCK) {
            prepBrowser(br);
            // start of cloudflare
            // this hoster uses cloudflare as it's webhosting service provider, which invokes anti DDoS measures (at times)
            br.setFollowRedirects(true);
            // try like normal, on failure try with cloudflare(link)
            try {
                br.getPage(parameter);
            } catch (Exception e) {
                // typical header response code for anti ddos event is 503
                if (e.getMessage().contains("503")) {
                    cloudflare = true;
                }
            }
            if (cloudflare) {
                try {
                    cloudflare(parameter);
                } catch (Exception e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                    if (e.getMessage().contains("503")) {
                        logger.warning("Cloudflare anti DDoS measures enabled, your version of JD can not support this. In order to go any further you will need to upgrade to JDownloader 2");
                        return decryptedLinks;
                    }
                }
            }
            br.setFollowRedirects(false);
            // end of cloudflare
            if (br.containsHTML(">Page Not Found<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (parameter.contains(".com/series/")) {
                int p = 1;
                String page = new Regex(parameter, "\\?page=(\\d+)").getMatch(0);
                if (page != null) p = Integer.parseInt(page);
                for (int i = 0; i != p; i++) {
                    String host = new Regex(br.getURL(), "(https?://[^/]+)").getMatch(0);
                    String nextPage = br.getRegex("class=\"page\" href=\"(/series/[^\\?]+\\?page=" + (p + 1) + ")\">\\d+</a>").getMatch(0);
                    String[] links = br.getRegex("href=\"(/[^\"]+)\">[^<]+</a>[\r\n\t ]+Series:").getColumn(0);
                    if (links == null || links.length == 0) {
                        logger.warning("Could not find series 'links' : " + parameter);
                        return null;
                    }
                    for (String link : links) {
                        decryptedLinks.add(createDownloadlink(host + link));
                    }
                    // if page is provided within parameter only add that page
                    if (nextPage != null && !parameter.contains("?page=")) {
                        p++;
                        br.getPage(nextPage);
                    } else {
                        break;
                    }
                }
            } else {
                // set filepackage
                fpName = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
                if (fpName == null) {
                    logger.warning("filepackage == null: " + parameter);
                    logger.warning("Please report issue to JDownloader Development team!");
                    return null;
                }

                if (parameter.matches(".+\\?src=\\d+")) {
                    // if the user imports src link, just return that link
                    br2 = br.cloneBrowser();
                    parsePage();
                } else {
                    // if the user imports src link, just return that link
                    br2 = br.cloneBrowser();
                    parsePage();
                    // grab src links and process
                    String[] links = br.getRegex("<a rel=\"nofollow\" title=\"[^\"]+(?!dead) Source\" href=\"(/[^\"]+\\?src=\\d+)\">").getColumn(0);
                    if (links != null && links.length != 0) {
                        for (String link : links) {
                            br2 = br.cloneBrowser();
                            br2.getPage(link);
                            parsePage();
                        }
                    }
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.setProperty("ALLOW_MERGE", true);
                fp.addLinks(decryptedLinks);
            }
            if (decryptedLinks.isEmpty()) {
                // not necessarily an error...
                // logger.warning("Decrypter out of date for link: " + parameter);
                // return null;
            }
        }
        return decryptedLinks;
    }

    private boolean parsePage() throws Exception {
        if (escapeAll != null && spart > 1) {
            // to prevent over write of escapeAll storable, for split parts within escapeAll
        } else {
            escapeAll = br2.getRegex("escapeall\\('(.*)'\\)\\)\\);").getMatch(0);
            if (!inValidate(escapeAll)) {
                escapeAll = escapeAll.replaceAll("[A-Z~!@#\\$\\*\\{\\}\\[\\]\\-\\+\\.]?", "");
                escapeAll = Encoding.htmlDecode(escapeAll);
                escapeAll = Encoding.urlDecode(escapeAll, false);
                // cleanup crap
                if (new Regex(escapeAll, "https?://[^\"]+(cizgifilmlerizle\\.com|animeuploads\\.com)/[^\"]+<div[^>]+").matches()) escapeAll = escapeAll.replaceAll("<div[^>]+>", "");
            }
            if (inValidate(escapeAll) || new Regex(escapeAll, "(/img/\\w+dead\\.jpg|http://www\\./media)").matches()) {
                // escapeAll == null / not online yet... || offline results within escapeAll
                if (br.containsHTML("This page will be updated as soon as"))
                    logger.info("Not been release yet... : " + br2.getURL());
                else if (inValidate(escapeAll))
                    logger.info("DeadLink!... : " + br2.getURL());
                else
                    logger.warning("Decrypter out of date for link: " + br2.getURL());
                DownloadLink dl = createDownloadlink("directhttp://" + br2.getURL());
                dl.setProperty("OFFLINE", true);
                dl.setAvailable(false);
                decryptedLinks.add(dl);
                return false;
            }
        }

        // embed links that are not found by generic's
        String link = new Regex(escapeAll, "(https?://(\\w+\\.)?vureel\\.com/playwire\\.php\\?vid=\\d+)").getMatch(0);
        // with stagevu they are directly imported finallink and not embed player. We want the image for the uid, return to hoster.
        if (inValidate(link) && escapeAll.contains("stagevu.com/")) {
            String stagevu = new Regex(escapeAll, "previewImage=\"https?://stagevu\\.com/img/thumbnail/([a-z]{12})").getMatch(0);
            if (!inValidate(stagevu)) {
                link = "http://stagevu.com/video/" + stagevu;
            } else {
                // error
            }
        } else if (inValidate(link) && escapeAll.contains("smotri.com/")) {
            String smotri = new Regex(escapeAll, "file=(v\\d+)").getMatch(0);
            if (!inValidate(smotri)) {
                link = "http://smotri.com/video/view/?id=" + smotri;
            } else {
                // error
            }
        }

        // generic fail overs
        if (inValidate(link)) link = new Regex(escapeAll, "<iframe src=\"(https?://([^<>\"]+)?" + supported_hoster + "/[^<>\"]+)\"").getMatch(0);
        if (inValidate(link)) link = new Regex(escapeAll, "(href|url|file)=\"?(https?://([^<>\"]+)?" + supported_hoster + "/[^<>\"]+)\"").getMatch(1);
        if (inValidate(link)) link = new Regex(escapeAll, "src=\"(https?://([^<>\"]+)?" + supported_hoster + "/[^<>\"]+)\"").getMatch(0);
        if (!inValidate(link))
            decryptedLinks.add(createDownloadlink(link));
        else if (inValidate(link) && new Regex(escapeAll, "(anilinkz\\.com/get/|chia\\-anime\\.com)").matches()) {
            String[] aLinks = new Regex(escapeAll, "(http[^\"]+/get/[^\"]+)").getColumn(0);
            // chia-anime can't be redirected back into dedicated plugin
            if ((aLinks == null || aLinks.length == 0) && escapeAll.contains("chia-anime.com")) aLinks = new Regex(escapeAll, "url\":\"(https?[^\"]+chia-anime\\.com[^\"]+)").getColumn(0);
            if (aLinks != null && aLinks.length != 0) {
                for (String aLink : aLinks) {
                    DownloadLink downloadLink = createDownloadlink("directhttp://" + aLink);
                    downloadLink.setFinalFileName(fpName + aLink.substring(aLink.lastIndexOf(".")));

                    Browser br2 = br.cloneBrowser();
                    // In case the link redirects to the finallink
                    br2.setFollowRedirects(true);
                    URLConnectionAdapter con = null;
                    try {
                        con = br2.openGetConnection(aLink);
                        // only way to check for made up links... or offline is here
                        if (!con.getContentType().contains("html")) {
                            downloadLink.setName(fpName + getFileNameFromHeader(con).substring(getFileNameFromHeader(con).lastIndexOf(".")));
                            downloadLink.setDownloadSize(con.getLongContentLength());
                            downloadLink.setAvailable(true);
                        } else {
                            downloadLink.setAvailable(false);
                        }
                        decryptedLinks.add(downloadLink);
                    } finally {
                        try {
                            con.disconnect();
                        } catch (Throwable e) {
                        }
                    }

                }
            }
        }
        // logic to deal with split parts within escapeAll. Uses all existing code within parsePage (see #9373)
        String[] sprt = new Regex(escapeAll, "(<div class=\"spart\".*?</div>|Part \\d+\r\n)").getColumn(0);
        if (sprt != null && sprt.length > 1) {
            spart = sprt.length;
            // lets remove previous results from escape all
            if (spart > 1 && link != null) {
                escapeAll = escapeAll.replace(link, "");
                parsePage();
            }
        }
        return true;
    }

    /**
     * performs silly cloudflare anti DDoS crapola
     * 
     * @author raztoki
     */
    private boolean cloudflare(String url) throws Exception {
        try {
            /* not available in old stable */
            br.setAllowedResponseCodes(new int[] { 503 });
        } catch (Throwable e) {
        }
        // we need to reload the page, as first time it failed and allowedResponseCodes wasn't set to allow 503
        br.getPage(url);
        final Form cloudflare = br.getFormbyProperty("id", "ChallengeForm");
        if (cloudflare != null) {
            String math = br.getRegex("\\$\\(\\'#jschl_answer\\'\\)\\.val\\(([^\\)]+)\\);").getMatch(0);
            if (math == null) {
                String variableName = br.getRegex("(\\w+)\\s*=\\s*\\$\\(\'#jschl_answer\'\\);").getMatch(0);
                if (variableName != null) variableName = variableName.trim();
                math = br.getRegex(variableName + "\\.val\\(([^\\)]+)\\)").getMatch(0);
            }
            if (math == null) {
                logger.warning("Couldn't find 'math'");
                return false;
            }
            // use js for now, but change to Javaluator as the provided string doesn't get evaluated by JS according to Javaluator author.
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            cloudflare.put("jschl_answer", String.valueOf(((Double) engine.eval("(" + math + ") + 12")).longValue()));
            Thread.sleep(5500);
            br.submitForm(cloudflare);
            if (br.getFormbyProperty("id", "ChallengeForm") != null) {
                logger.warning("Possible plugin error within cloudflare(). Continuing....");
                return false;
            }
            // lets save cloudflare cookie to reduce the need repeat cloudFlare()
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = br.getCookies(this.getHost());
            for (final Cookie c : add.getCookies()) {
                if (c.getKey().contains("cfduid")) cookies.put(c.getKey(), c.getValue());
            }
            cloudflareCookies = cookies;
        }
        // remove the setter
        try {
            /* not available in old stable */
            br.setAllowedResponseCodes(new int[] {});
        } catch (Throwable e) {
        }
        return true;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     * 
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals("")))
            return true;
        else
            return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}