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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tubeviral.com", "whackyvidz.com", "linkbabes.com", "dyo.gs", "filesonthe.net", "cash4files.com", "seriousdeals.net", "any.gs", "goneviral.com", "ultrafiles.net", "miniurls.co", "tinylinks.co", "yyv.co", "realfiles.net", "youfap.com", "linkgalleries.net", "thesefiles.com", "urlpulse.net", "viraldatabase.com", "seriousfiles.com", "ubucks.net", "thesegalleries.com", "seriousurls.com", "baberepublic.com", "qvvo.com", "linkbucks.com", "linkseer.net", "ubervidz.com", "zxxo.net", "ugalleries.net", "picturesetc.net", "allanalpass.com" }, urls = { "http://(([0-9a-fA-F]+(\\d+)?)\\.tubeviral\\.com/?|(www\\.)?tubeviral\\.com/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.whackyvidz\\.com/?|(www\\.)?whackyvidz\\.com/[a-f0-9]{8})",
        "http://(([0-9a-fA-F]+(\\d+)?)\\.linkbabes\\.com/?|(www\\.)?linkbabes\\.com/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.dyo\\.gs/?|(www\\.)?dyo\\.gs/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.filesonthe\\.net/?|(www\\.)?filesonthe\\.net/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.cash4files\\.com/?|(www\\.)?cash4files\\.com/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.seriousdeals\\.net/?|(www\\.)?seriousdeals\\.net/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.any\\.gs/?|(www\\.)?any\\.gs/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.goneviral\\.com/?|(www\\.)?goneviral\\.com/[a-f0-9]{8})", "http://((www\\.)?[a-z0-9]+\\.ultrafiles\\.net/?|(www\\.)?ultrafiles\\.net/[a-f0-9]{8})", "http://((www\\.)?[a-z0-9]+\\.miniurls\\.co/?|(www\\.)?miniurls\\.co/[a-f0-9]{8})",
        "http://([\\w\\.]*?tinylinks\\.co(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?tinylinks\\.co/[a-f0-9]{8})", "http://([0-9a-fA-F]+\\.yyv\\.co/?|(www\\.)?yyv\\.co/[a-f0-9]{8})", "http://([\\w\\.]*?realfiles\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?realfiles\\.net/[a-f0-9]{8})", "http://([\\w\\.]*?youfap\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?youfap\\.com/[a-f0-9]{8})", "http://([\\w\\.]*?linkgalleries\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?linkgalleries\\.net/[a-f0-9]{8})", "http://([\\w\\.]*?thesefiles\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?thesefiles\\.com/[a-f0-9]{8})", "http://([\\w\\.]*?urlpulse\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?urlpulse\\.net/[a-f0-9]{8})", "http://([\\w\\.]*?viraldatabase\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?viraldatabase\\.com/[a-f0-9]{8})",
        "http://([\\w\\.]*?seriousfiles\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?seriousfiles\\.com/[a-f0-9]{8})", "http://([\\w\\.]*?ubucks\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?ubucks\\.net/[a-f0-9]{8})", "http://([\\w\\.]*?thesegalleries\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?thesegalleries\\.com/[a-f0-9]{8})", "http://([\\w\\.]*?seriousurls\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?|(www\\.)?seriousurls\\.com/[a-f0-9]{8})", "http://((www\\.)?(baberepunlic\\.com/(link/[0-9a-zA-Z]+(/\\d+)?|url/.+)|(?!www\\.)[a-z0-9]+\\.baberepunlic\\.com)(www\\.)?baberepunlic\\.com/[a-f0-9]{8})", "http://([\\w\\.]*?qvvo\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?|(www\\.)?qvvo\\.com/[a-f0-9]{8})", "http://(www\\.)?(linkbucks\\.com/(link/[0-9a-zA-Z]+(/\\d+)?|url/.+|[0-9a-f]{8})|(?!www\\.|forums\\.|static\\.)[a-z0-9]+\\.linkbucks\\.com)",
        "http://(([0-9a-fA-F]+(\\d+)?)\\.linkseer\\.net/?|(www\\.)?linkseer\\.net/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.ubervidz\\.com/?|(www\\.)?ubervidz\\.com/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.zxxo\\.net/?|(www\\.)?zxxo\\.net/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.ugalleries\\.net/?|(www\\.)?ugalleries\\.net/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.picturesetc\\.net/?|(www\\.)?picturesetc\\.net/[a-f0-9]{8})", "http://(([0-9a-fA-F]+(\\d+)?)\\.allanalpass\\.com/?|(www\\.)?allanalpass\\.com/[a-f0-9]{8})" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class LnkBcks extends PluginForDecrypt {

    public LnkBcks(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static AtomicReference<String> agent = new AtomicReference<String>(null);

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
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
        sleep(5 * 1000l, param);
        String link = br.getRedirectLocation();
        if ((link != null && link.contains("/notfound/")) || br.containsHTML("(>Link Not Found<|>The link may have been deleted by the owner)")) {
            logger.info("Link offline: " + parameter);
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
            // not needed, sleep in beginning of plugin
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