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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {}, flags = {})
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
            ret[i] = "(http://[\\w\\.]*?" + names[i].replaceAll("\\.", "\\\\.") + "/(?!\\?ref=|promote|sitemap|reset_password|register_new|mailto|(.*?)\\.php|https:).+)|(http://(?!master)[\\w\\-]{5,16}\\." + names[i].replaceAll("\\.", "\\\\.") + ")";

        }
        return ret;
    }

    /**
     * Returns the annotations flags array
     * 
     * @return
     */
    public static int[] getAnnotationFlags() {
        String[] names = getAnnotationNames();
        int[] ret = new int[names.length];

        for (int i = 0; i < ret.length; i++) {
            ret[i] = 0;

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

        // remove custom rules first!!! As html can change because of generic cleanup rules.

        // generic cleanup
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
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

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        correctBR();
        String link = new Regex(correctedBR, "<META HTTP\\-EQUIV=\"Refresh\" .*? URL=(.*?)\">").getMatch(0);
        if (link == null) {
            link = new Regex(correctedBR, "onClick=\"top\\.location=\\'(.*?)\\'\">").getMatch(0);
            if (link == null) {
                // urlcash fail over.
                link = new Regex(correctedBR, "linkDestUrl = '([^']+)").getMatch(0);
                if (link == null) {
                    link = new Regex(correctedBR, "<iframe name=\\'redirectframe\\' id=\\'redirectframe\\'.*?src=\\'(.*?)\\'.*?></iframe>").getMatch(0);
                    if (link == null) {
                        link = br.getRedirectLocation();
                        if (link == null && br.containsHTML("<title>URLCash\\.net \\- An URL forwarding service where you make money from")) {
                            logger.info("Link offline: " + parameter);
                            return decryptedLinks;
                        } else {
                            logger.warning("Decrypter broken for link:" + parameter);
                            return null;
                        }
                    }
                }
            }
        }

        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}