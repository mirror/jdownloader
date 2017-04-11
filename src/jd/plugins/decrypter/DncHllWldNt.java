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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

/**
 * NOTE: cloudflare in use.
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dancehallworld.net" }, urls = { "https?://(\\w*\\.)?dancehallworld\\.net/\\d{4}/\\d{1,2}/[\\w\\-]+/" }) 
public class DncHllWldNt extends antiDDoSForDecrypt {

    public DncHllWldNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        getPage(parameter);

        // invalid url
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            return decryptedLinks;
        }

        // packagename
        final String fpName = br.getRegex("<title>(.*?)(?:\\s*(?:\\||-)\\s*Dance\\s*hall\\s*World)?</title>").getMatch(0);

        // all external links pass via there own tracking url
        String[] links = br.getRegex("xurl=(http[^\"]+)").getColumn(0);
        if (links != null) {
            for (String link : links) {
                link = validate(link);
                if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        }

        // audiomac provided via iframe
        links = br.getRegex("<iframe[^>]* src=(\"|')(.*?)\\1").getColumn(1);
        if (links != null) {
            for (String link : links) {
                link = validate(link);
                if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        }

        // set packagename
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
            fp.setProperty("ALLOW_MERGE", true);
        }
        return decryptedLinks;
    }

    /**
     * Validates input URL and corrects protocol if missing prefixs, and also ignores some patterns to prevent false positives
     *
     * @param link
     * @return
     */
    private final String validate(String link) {
        if (link == null) {
            return null;
        }
        final String protocol = new Regex(br.getURL(), "^(https?:)").getMatch(-1);
        // respect current protocol under RFC
        if (link.matches("^//.+") && protocol != null) {
            link = protocol + link;
        }
        // this will construct basic relative path
        else if (link.matches("^/.+") && protocol != null) {
            link = protocol + "//" + Browser.getHost(br._getURL(), true) + link;
        }
        if (new Regex(link, "facebook.com(/|%2F)plugins(/|%2F)|twitter.com(/|%2F)").matches()) {
            return null;
        }
        return link;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}