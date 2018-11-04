//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "libgen.org" }, urls = { "https?://(www\\.)?(libgen\\.org|gen\\.lib\\.rus\\.ec|libgen\\.io)/book/index\\.php\\?md5=[a-f0-9]{32}" })
public class LibGen extends PluginForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        return new String[] { "gen.lib.rus.ec", "libgen.io" };
    }

    public LibGen(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("libgen.org/", "gen.lib.rus.ec/");
        final String host = new Regex(parameter, "(https?://[^/]+)").getMatch(0);
        br.setCookie(host, "lang", "en");
        br.setCustomCharset("utf-8");
        /* Allow redirects to other of their domains */
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("entry not found in the database")) {
            logger.info("Invalid URL: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (fpName != null) {
            fpName = Encoding.htmlDecode(fpName).trim();
        }
        String[] links = br.getRegex("<url\\d+>(https?://[^<]+)</url\\d+>").getColumn(0);
        // Hmm maybe just try to get all mirrors
        if (links == null || links.length == 0) {
            // links = br.getRegex("<td align='center' width='11,1%'><a href='((?:http|/)[^<>\"]*?)'").getColumn(0);
            links = br.getRegex("<td colspan=2><b><a href='([^<>\"]*?)'>").getColumn(0);
        }
        if (links == null || links.length == 0) {
            return null;
        }
        for (final String link : links) {
            // final String link = Request.getLocation(dl, br.getRequest());
            br.getPage(link);
            String dlink = br.getRegex("<a href=\"([^<>\"]*?)\">GET<").getMatch(0);
            decryptedLinks.add(createDownloadlink(dlink));
        }
        final String cover_url = br.getRegex("(\\'|\")((?:https?:)?(?://libgen\\.(?:in|net))?/covers/\\d+/.*?\\.(?:jpg|jpeg|png|gif))\\1").getMatch(1);
        if (cover_url != null) {
            final DownloadLink dl = createDownloadlink(Request.getLocation(cover_url, br.getRequest()));
            if (fpName != null) {
                final String ext = getFileNameExtensionFromString(cover_url, ".jpg");
                String filename_cover = encodeUnicode(fpName) + ext;
                dl.setFinalFileName(filename_cover);
            }
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}