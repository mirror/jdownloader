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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {}, flags = {})
public class RLCsh extends PluginForDecrypt {
    private static final String[] ANNOTATION_NAMES = new String[] { "urlcash.net", "bat5.com", "urlcash.org", "clb1.com", "celebclk.com", "smilinglinks.com", "peekatmygirlfriend.com", "looble.net" };

    /**
     * Returns the annotations names array
     * 
     * @return
     */
    public static String[] getAnnotationNames() {
        return ANNOTATION_NAMES;
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
            ret[i] = "(http://[\\w\\.]*?" + names[i].replaceAll("\\.", "\\\\.") + "/(?!\\?ref=|promote|reset_password|register_new|(index|advertise|create_links|index_new|learnmore)\\.php).+)|(http://(?!master)[\\w\\-]{5,16}\\." + names[i].replaceAll("\\.", "\\\\.") + ")";

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

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        String link = br.getRegex("<META HTTP\\-EQUIV=\"Refresh\" .*? URL=(.*?)\">").getMatch(0);
        if (link == null) link = br.getRegex("onClick=\"top\\.location=\\'(.*?)\\'\">").getMatch(0);
        if (link == null) link = br.getRegex("<iframe name=\\'redirectframe\\' id=\\'redirectframe\\'.*?src=\\'(.*?)\\'.*?></iframe>").getMatch(0);
        if (link == null) link = br.getRedirectLocation();
        if (link == null) {
            logger.warning("Decrypter broken for link:" + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
        return decryptedLinks;
    }

}
