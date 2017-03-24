//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "links-secure.com" }, urls = { "https?://(?:www\\.)?(?:links-secure\\.com|h-link\\.us|foxmoviez\\.com|zmelody\\.com|filmey\\.co|wwenews\\.us)/[0-9]+" })
public class Zro10BasicDecrypt extends PluginForDecrypt {

    public Zro10BasicDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // alot of these sites are all on the same ip http://samesites.com/ip/5.255.64.5

    public String[] siteSupportedNames() {
        return new String[] { "links-secure.com", "h-link.us", "zmelody.com", "foxmoviez.com", "filmey.co", "wwenews.us" };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        final String ID = new Regex(parameter, "/([0-9A-Z]+)$").getMatch(0);
        final String m1link = Request.getLocation("/m1.php?id=" + ID, br.createGetRequest(parameter));
        br.getPage(m1link);
        // little errorhandling
        if (br.getRedirectLocation() != null && !br.getRedirectLocation().contains(ID)) {
            return decryptedLinks;
        }
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        String finallink = br.getRegex("onclick=\"NewWindow\\('([^']+)','name'").getMatch(0);
        if (finallink == null) {
            // links-secure.com
            finallink = br.getRegex("<a id=(\"|')download\\1 href=(\"|')(.*?)\\2").getMatch(2);
            if (finallink == null) {
                // generic failover, this will bring up false positives
                finallink = br.getRegex("a href=\"(htt.*?)\"").getMatch(0);
                if (finallink == null) {
                    return null;
                }
            }
        }
        if ("".equals(finallink)) {
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Zero10BasicDecrypt;
    }

}