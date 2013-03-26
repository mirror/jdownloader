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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkbabes.com", "dyo.gs", "filesonthe.net", "cash4files.com", "seriousdeals.net", "any.gs", "goneviral.com", "ultrafiles.net", "miniurls.co", "tinylinks.co", "yyv.co", "realfiles.net", "youfap.com", "linkgalleries.net", "thesefiles.com", "urlpulse.net", "viraldatabase.com", "seriousfiles.com", "ubucks.net", "thesegalleries.com", "seriousurls.com", "baberepublic.com", "qvvo.com", "linkbucks.com", "linkseer.net", "ubervidz.com", "uberpicz.com", "zxxo.net", "ugalleries.net", "picturesetc.net", "allanalpass.com" }, urls = { "http://([0-9a-fA-F]+(\\d+)?)\\.linkbabes\\.com/?", "http://([0-9a-fA-F]+(\\d+)?)\\.dyo\\.gs/?", "http://([0-9a-fA-F]+(\\d+)?)\\.filesonthe\\.net/?", "http://([0-9a-fA-F]+(\\d+)?)\\.cash4files\\.com/?", "http://([0-9a-fA-F]+(\\d+)?)\\.seriousdeals\\.net/?",
        "http://([0-9a-fA-F]+(\\d+)?)\\.any\\.gs/?", "http://([0-9a-fA-F]+(\\d+)?)\\.goneviral\\.com/?", "http://(www\\.)?[a-z0-9]+\\.ultrafiles\\.net", "http://(www\\.)?[a-z0-9]+\\.miniurls\\.co", "http://[\\w\\.]*?tinylinks\\.co(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[0-9a-fA-F]+\\.yyv\\.co/?", "http://[\\w\\.]*?realfiles\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?youfap\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?linkgalleries\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?thesefiles\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?urlpulse\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?viraldatabase\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?seriousfiles\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?ubucks\\.net(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://[\\w\\.]*?thesegalleries\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?",
        "http://[\\w\\.]*?seriousurls\\.com(/link/[0-9a-fA-F]+(/\\d+)?)?", "http://(www\\.)?(baberepunlic\\.com/(link/[0-9a-zA-Z]+(/\\d+)?|url/.+)|(?!www\\.)[a-z0-9]+\\.baberepunlic\\.com)", "http://[\\w\\.]*?qvvo\\.com(/link/[0-9a-zA-Z]+(/\\d+)?)?", "http://(www\\.)?(linkbucks\\.com/(link/[0-9a-zA-Z]+(/\\d+)?|url/.+)|(?!www\\.|forums\\.)[a-z0-9]+\\.linkbucks\\.com)", "http://([0-9a-fA-F]+(\\d+)?)\\.linkseer\\.net/?", "http://([0-9a-fA-F]+(\\d+)?)\\.ubervidz\\.com/?", "http://([0-9a-fA-F]+(\\d+)?)\\.uberpicz\\.com/?", "http://([0-9a-fA-F]+(\\d+)?)\\.zxxo\\.net/?", "http://([0-9a-fA-F]+(\\d+)?)\\.ugalleries\\.net/?", "http://([0-9a-fA-F]+(\\d+)?)\\.picturesetc\\.net/?", "http://([0-9a-fA-F]+(\\d+)?)\\.allanalpass\\.com/?"

}, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class LnkBcks extends PluginForDecrypt {

    public LnkBcks(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String link = br.getRedirectLocation();
        if ((link != null && link.contains("/notfound/")) || br.containsHTML("(>Link Not Found<|>The link may have been deleted by the owner)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (link == null) {
            link = br.getRegex(Pattern.compile("<div id=\"lb_header\">.*?/a>.*?<a.*?href=\"(.*?)\".*?class=\"lb", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (link == null) {
            link = br.getRegex(Pattern.compile("AdBriteInit(\"(.*?)\")", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (link == null) {
            link = br.getRegex(Pattern.compile("Linkbucks\\.TargetUrl = '(.*?)';", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (link == null) {
            link = br.getRegex(Pattern.compile("Lbjs\\.TargetUrl = \\'(http://[^<>\"]*?)\\'", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (link == null) {
            link = br.getRegex(Pattern.compile("src=\"http://static\\.linkbucks\\.com/tmpl/mint/img/lb\\.gif\" /></a>.*?<a href=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (link == null) {
            link = br.getRegex(Pattern.compile("noresize=\"[0-9+]\" src=\"(http.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (link == null) {
                link = br.getRegex(Pattern.compile("\"frame2\" frameborder.*?src=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            }
        }
        if (link == null) {
            link = br.getRegex(Pattern.compile("id=\"content\" src=\"([^\"]*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (link == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(link));

        return decryptedLinks;
    }

}
