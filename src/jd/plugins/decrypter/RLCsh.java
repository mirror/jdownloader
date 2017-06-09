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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.RazStringBuilder;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class RLCsh extends PluginForDecrypt {

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "urlcash.net", "bat5.com", "urlcash.org", "clb1.com", "celebclk.com", "smilinglinks.com", "peekatmygirlfriend.com", "looble.net" };

    }

    /**
     * returns the annotation pattern array
     *
     * @return
     */
    public static String[] getAnnotationUrls() {
        String[] names = getAnnotationNames();
        String[] ret = new String[names.length];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = "(http://[\\w\\.]*?" + Pattern.quote(names[i]) + "/(?!\\?ref=|promote|sitemap|reset_password|register_new|mailto|(.*?)\\.php|https:).+)|(http://(?!master)[\\w\\-]{5,16}\\." + Pattern.quote(names[i]) + ")";

        }
        return ret;
    }

    public RLCsh(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String correctedBR = "";

    /** Remove HTML code which could break the plugin */
    public void correctBR() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> regexStuff = new ArrayList<String>();

        // remove custom rules first!!! As html can change because of generic
        // cleanup rules.

        // generic cleanup
        regexStuff.add("<\\!(--.*?--)>");
        regexStuff.add("(display: ?none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");

        for (String aRegex : regexStuff) {
            String results[] = new Regex(correctedBR, aRegex).getColumn(0);
            if (results != null) {
                for (String result : results) {
                    correctedBR = correctedBR.replace(result, "");
                }
            }
        }
    }

    private final String INVALIDLINKS = "http://images\\..+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.getPage(parameter);
        correctBR();
        String link = new Regex(correctedBR, "<META HTTP-EQUIV=\"Refresh\" .*? URL=(.*?)\">").getMatch(0);
        if (link == null) {
            link = new Regex(correctedBR, "onClick=\"top\\.location='(.*?)'\">").getMatch(0);
            if (link == null) {
                // urlcash fail over.
                link = new Regex(correctedBR, "linkDestUrl = '([^']+)").getMatch(0);
                if (link == null) {
                    link = new Regex(correctedBR, "<iframe name='redirectframe' id='redirectframe'.*?src='(.*?)'.*?></iframe>").getMatch(0);
                }
            }
        }
        if (link == null) {
            link = br.getRedirectLocation();
            if (link == null && br.containsHTML("<title>URLCash\\.net - An URL forwarding service where you make money from|register_new\\.php\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
        }
        if (link != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            return decryptedLinks;
        }

        // image galleries.
        final String fpName = br.getRegex("<strong>(.*?)</strong>").getMatch(0);
        final FilePackage fp;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlOnlyDecode(fpName));
        } else {
            fp = null;
        }
        final String pattern = RazStringBuilder.buildString(getAnnotationNames(), "|", true);
        // lets find all images based on error...
        final String[] picshtml = br.getRegex("<a\\s+[^>]+><\\s*img\\s+[^>]*/img/imagehost_unavailable[^>]+>").getColumn(-1);
        if (picshtml != null && picshtml.length != 0) {
            for (final String pic : picshtml) {
                // src could be a thumb nail! we don't want that persay!
                final String href = new Regex(pic, "href=('|\")(.*?)\\1").getMatch(1);
                final String src = new Regex(pic, "src=('|\")(.*?)\\1").getMatch(1);
                // if href is linking back into plugin.. then treat it as full size image!
                final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(href != null && new Regex(href, pattern).matches() ? href : src));
                if (fp != null) {
                    fp.add(dl);
                }
                decryptedLinks.add(dl);
            }
        }
        // fail over, with generic regex... this has downside, as it can pickup thumbnails...
        if (decryptedLinks.isEmpty()) {
            final String[] piclinks = br.getRegex("'(http://[a-z0-9]+\\.(?:image[^/]+|img[^/]+)/[^<>\"]*?)'").getColumn(0);
            if (piclinks != null && piclinks.length != 0) {
                for (final String piclink : piclinks) {
                    final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(piclink));
                    if (fp != null) {
                        fp.add(dl);
                    }
                    decryptedLinks.add(dl);
                }
            }
        }
        if (decryptedLinks.isEmpty()) {
            logger.warning("Decrypter broken for link:" + parameter);
            return null;
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}