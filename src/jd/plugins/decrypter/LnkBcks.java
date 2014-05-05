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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

// DEV NOTES:
// 162.158.0.0/15 cloudflare
// 141.101.64.0 - 141.101.127.255 cloudflare
// 190.93.240/20 cloudflare
// 198.41.128.0/17 cloudflare
// INFORELAY-LAX2-02   (NET-173-205-128-0-1) 173.205.128.0 - 173.205.255.255
// LINKBUCKS LINKBUCKS (NET-173-205-185-80-1) 173.205.185.80 - 173.205.185.95
// www.uid.domain redirects to linkbucks homepage on https.

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2,

names = { "cash4files.com", "megaline.co", "qqc.co", "theseblogs.com", "theseforums.com", "ultrafiles.net", "urlbeat.net", "whackyvidz.com", "yyv.co", "amy.gs", "deb.gs", "drstickyfingers.com", "fapoff.com", "freean.us", "freegaysitepass.com", "galleries.bz", "hornywood.tv", "picbucks.com", "poontown.net", "rqq.co", "sexpalace.gs", "youfap.me", "zff.co", "tubeviral.com", "whackyvidz.com", "linkbabes.com", "dyo.gs", "filesonthe.net", "cash4files.com", "seriousdeals.net", "any.gs", "goneviral.com", "ultrafiles.net", "miniurls.co", "tinylinks.co", "yyv.co", "allanalpass.com", "linkbucks.com" },

urls = { "http://([a-f0-9]{8}\\.cash4files\\.com|(www\\.)?cash4files\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.megaline\\.co|(www\\.)?megaline\\.co/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.qqc\\.co|(www\\.)?qqc\\.co/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.theseblogs\\.com|(www\\.)?theseblogs\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.theseforums\\.com|(www\\.)?theseforums\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.ultrafiles\\.net|(www\\.)?ultrafiles\\.net/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.urlbeat\\.net|(www\\.)?urlbeat\\.net/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.whackyvidz\\.com|(www\\.)?whackyvidz\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.yyv\\.co|(www\\.)?yyv\\.co/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.amy\\.gs|(www\\.)?amy\\.gs/(link/)?[a-zA-Z0-9]{4,8})",
        "http://([a-f0-9]{8}\\.deb\\.gs|(www\\.)?deb\\.gs/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.drstickyfingers\\.com|(www\\.)?drstickyfingers\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.fapoff\\.com|(www\\.)?fapoff\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.freean\\.us|(www\\.)?freean\\.us/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.freegaysitepass\\.com|(www\\.)?freegaysitepass\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.galleries\\.bz|(www\\.)?galleries\\.bz/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.hornywood\\.tv|(www\\.)?hornywood\\.tv/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.picbucks\\.com|(www\\.)?picbucks\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.poontown\\.net|(www\\.)?poontown\\.net/(link/)?[a-zA-Z0-9]{4,8})",
        "http://([a-f0-9]{8}\\.rqq\\.co|(www\\.)?rqq\\.co/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.sexpalace\\.gs|(www\\.)?sexpalace\\.gs/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.youfap\\.me|(www\\.)?youfap\\.me/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.zff\\.co|(www\\.)?zff\\.co/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.tubeviral\\.com|(www\\.)?tubeviral\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.whackyvidz\\.com|(www\\.)?whackyvidz\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.linkbabes\\.com|(www\\.)?linkbabes\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.dyo\\.gs|(www\\.)?dyo\\.gs/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.filesonthe\\.net|(www\\.)?filesonthe\\.net/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.cash4files\\.com|(www\\.)?cash4files\\.com/(link/)?[a-zA-Z0-9]{4,8})",
        "http://([a-f0-9]{8}\\.seriousdeals\\.net|(www\\.)?seriousdeals\\.net/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.any\\.gs|(www\\.)?any\\.gs/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.goneviral\\.com|(www\\.)?goneviral\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.ultrafiles\\.net|(www\\.)?ultrafiles\\.net/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.miniurls\\.co|(www\\.)?miniurls\\.co/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.tinylinks\\.co|(www\\.)?tinylinks\\.co/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-f0-9]{8}\\.yyv\\.co|(www\\.)?yyv\\.co/(link/)?[a-zA-Z0-9]{4,8})", "http://([a-z0-9]{8}\\.allanalpass\\.com|(www\\.)?allanalpass\\.com/(link/)?[a-zA-Z0-9]{4,8})", "http://((www\\.)?linkbucks\\.com/(url/.+|[0-9a-zA-Z]{4,8}|\\d+)|[a-z0-9]{8}\\.linkbucks\\.com)" },

flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class LnkBcks extends PluginForDecrypt {

    public LnkBcks(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static AtomicReference<String> agent      = new AtomicReference<String>(null);
    private final String                   surveyLink = "To access the content, you must complete a quick survey\\.";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // urls containing /link/ are no longer valid, but uid seems to be transferable.
        String parameter = param.toString().replace("/link/", "");
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        br.getHeaders().put("User-Agent", agent.get());
        br.getHeaders().put("Accept-Language", "en,en-GB;q=0.8");
        br.getHeaders().put("Accept-Charset", null);

        br.setFollowRedirects(false);
        br.getPage(parameter);
        long firstGet = System.currentTimeMillis();
        String link = br.getRedirectLocation();
        if ((link != null && link.contains("/notfound/")) || br.containsHTML("(>Link Not Found<|>The link may have been deleted by the owner|" + surveyLink + ")")) {
            DownloadLink dl = createDownloadlink("directhttp://" + parameter);
            dl.setAvailable(false);
            dl.setProperty("OFFLINE", true);
            if (br.containsHTML(surveyLink)) {
                dl.setName("JD NOTE - We don't support surveys");
                logger.info("Survery Link!: " + parameter);
            } else {
                logger.info("Link offline: " + parameter);
            }
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("<div id=\"lb_header\">.*?/a>.*?<a.*?href=\"(.*?)\".*?class=\"lb", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("AdBriteInit\\(\"(.*?)\"\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("Linkbucks\\.TargetUrl = '(.*?)';", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("Lbjs\\.TargetUrl = '(http://[^<>\"]*?)'", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("src=\"http://static\\.linkbucks\\.com/tmpl/mint/img/lb\\.gif\" /></a>.*?<a href=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("noresize=\"[0-9+]\" src=\"(http.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (inValidate(link)) {
                link = br.getRegex(Pattern.compile("\"frame2\" frameborder.*?src=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            }
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("id=\"content\" src=\"([^\"]*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            // thx FRD, slightly adapted to JD
            // scan for js, they repeat, usually last wins (in browser...)
            String[] jss = br.getRegex("(<script type=\"text/javascript\">[^<]+</script>)").getColumn(0);
            if (jss == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String js = null;
            for (String j : jss) {
                // cleanup
                j = j.replaceAll("[\r\n\\s]+\\/\\/\\s*[^\r\n]+", "");
                if (new Regex(j, "\\s*var\\s*f\\s*=\\s*window\\['init'\\s*\\+\\s*'Lb'\\s*\\+\\s*'js'\\s*\\+\\s*''\\];[\r\n\\s]+").matches()) js = j;
            }
            if (js == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String token = new Regex(js, "Token\\s*:\\s*'([a-f0-9]{40})'").getMatch(0);
            if (token == null) token = new Regex(js, "\\?t=([a-f0-9]{40})").getMatch(0);
            final String authKeyMatchStr = "A(?:'\\s*\\+\\s*')?u(?:'\\s*\\+\\s*')?t(?:'\\s*\\+\\s*')?h(?:'\\s*\\+\\s*')?K(?:'\\s*\\+\\s*')?e(?:'\\s*\\+\\s*')?y";
            final String l1 = new Regex(js, "\\s*params\\['" + authKeyMatchStr + "'\\]\\s*=\\s*(\\d+?);").getMatch(0);
            final String l2 = new Regex(js, "\\s*params\\['" + authKeyMatchStr + "'\\]\\s*=\\s?params\\['" + authKeyMatchStr + "'\\]\\s*\\+\\s*(\\d+?);").getMatch(0);
            if (l1 == null || l2 == null || token == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final long authKey = Long.parseLong(l1) + Long.parseLong(l2);
            Browser br2 = br.cloneBrowser();
            br2.getPage("/director/?t=" + token);
            final long timeLeft = 5033 - (System.currentTimeMillis() - firstGet);
            if (timeLeft > 0) sleep(timeLeft, param);
            Browser br3 = br.cloneBrowser();
            br3.getPage("/intermission/loadTargetUrl?t=" + token + "&aK=" + authKey);
            link = br3.getRegex("Url\":\"([^\"]+)").getMatch(0);
        }
        if (inValidate(link)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(link));

        return decryptedLinks;
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
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals("") || s.equalsIgnoreCase("about:blank")))
            return true;
        else
            return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}