//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 * Note: do not decrypt entire series... this decrypter is already threaded by returning back into itself, it will cause high loads for long
 * periods.<br />
 * Using cloudflare!
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "seriesfree.to" }, urls = { "https?://(?:www\\.)?(?:watchseries\\.ag|watchtvseries\\.(?:se|vc)|seriesfree.to)/(episode/.*?\\.html|open/cale/[a-z0-9-]+\\.html)" })
public class WtchSrs extends antiDDoSForDecrypt {
    public WtchSrs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(createOfflinelink(parameter));
            } catch (final Throwable t) {
            }
            return decryptedLinks;
        }
        if (parameter.matches(".+/open/cale/[a-z0-9-]+\\.html")) {
            // final String result = br.getRegex("<a class=(\"|'|)myButton.*?\\1 href=(\"|'\"|)(.*?)\\2").getMatch(2);
            final String result = br.getRegex("<a href=\"([^<>\"]+)\"[^<>]+>Click Here To Play<").getMatch(0);
            if (result != null) {
                logger.info("External link: " + result);
                decryptedLinks.add(createDownloadlink(result));
            }
            return decryptedLinks;
        }
        String fpName = br.getRegex("</h2><h3[^>]*>(.*?)</h3>").getMatch(0);
        final String[] links = br.getRegex("<a[^>]+href=(\"|'|)(/open/cale/[a-z0-9-]+\\.html)\\1").getColumn(1);
        if (links == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        HashSet<String> dupe = new HashSet<String>();
        final String base = new Regex(parameter, "https?://[^/]+").getMatch(-1);
        for (final String link : links) {
            if (!dupe.add(link)) {
                continue;
            }
            // lets thread this task. easiest way to do this is to return back into the decrypter.
            decryptedLinks.add(createDownloadlink(base + link));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}