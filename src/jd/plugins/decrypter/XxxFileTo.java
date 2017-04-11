//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xfull.net" }, urls = { "http://(?:www\\.)?(?:xfull\\.net|xxxfile\\.to|x3\\.to)/download/clips(?:_[sh]d)?/[a-z0-9\\-]+\\.html" }) 
public class XxxFileTo extends PluginForDecrypt {

    /**
     * @author ohgod
     * @author raztoki
     * */
    public XxxFileTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceFirst("xxxfile\\.to|x3.to", getHost());
        br.setCookie(getHost(), "hasVisitedSite", "Yes");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        correctBR();
        final String content = new Regex(correctedBR, "<div class=\"news-text clearfix\">(.*?)<div align=\"center\">").getMatch(0);

        if (content == null) {
            return null;
        }

        // test zum extracten des releasenames

        final String packageName = new Regex(content, "alt=\"(.*?)\"").getMatch(0);

        final String[] links = new Regex(content, "<a href=\"(https?://.*?)\"").getColumn(0);

        if (links == null || links.length == 0) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        for (final String link : links) {
            if (!link.matches("https?://(?:www\\.)?(?:xxxfile\\.to|x3\\.to|xfull\\.net)/.+")) {
                decryptedLinks.add(createDownloadlink(link));
            }
        }

        final String[] imgs = new Regex(content, "https?://([\\w\\.]+)?(?:pixhost\\.org/show/|picsee\\.net/)[^\"]+").getColumn(-1);
        if (links != null && links.length != 0) {
            for (final String img : imgs) {
                if (!img.matches("(?i-).+thumbnails?.+")) {
                    decryptedLinks.add(createDownloadlink(img));
                }
            }
        }

        if (packageName != null) {
            FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(packageName);
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String correctedBR = null;

    /** Remove HTML code which could break the plugin */
    public void correctBR() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();

        // remove custom rules first!!! As html can change because of generic cleanup rules.

        // generic cleanup
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(<\\s*(\\w+)\\s+[^>]*style\\s*=\\s*(\"|')(?:(?:[\\w:;\\s#-]*(visibility\\s*:\\s*hidden;|display\\s*:\\s*none;|font-size\\s*:\\s*0;)[\\w:;\\s#-]*)|font-size\\s*:\\s*0|visibility\\s*:\\s*hidden|display\\s*:\\s*none)\\3[^>]*(>.*?<\\s*/\\2[^>]*>|/\\s*>))");
        for (String aRegex : regexStuff) {
            String results[] = new Regex(correctedBR, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    correctedBR = correctedBR.replace(result, "");
                }
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}