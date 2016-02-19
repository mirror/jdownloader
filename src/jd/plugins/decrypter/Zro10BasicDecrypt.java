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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save-link.info" }, urls = { "https?://(?:www\\.)?(?:save\\-link\\.info|share\\-link\\.info|h\\-link\\.us|forexurls\\.net|zmelody\\.com|filmey\\.co)/[0-9]+" }, flags = { 0 })
public class Zro10BasicDecrypt extends PluginForDecrypt {

    public Zro10BasicDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String[] siteSupportedNames() {
        return new String[] { "save-link.info", "share-link.info", "h-link.us", "forexurls.net", "zmelody.com", "forexshare.net", "filmey.co" };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        // finallink2 is used for unusual zero10 crypters like arbforce
        String finallink2 = null;
        String finallink = null;
        String ID = new Regex(parameter, "/([0-9A-Z]+)$").getMatch(0);
        String domain = new Regex(parameter.replaceAll("(www\\.|http://)", ""), "(.+)/" + ID).getMatch(0);
        String m1link = "http://" + domain + "/m1.php?id=" + ID;
        br.getPage(m1link);
        // little errorhandling
        if (br.getRedirectLocation() != null && !br.getRedirectLocation().contains(ID)) {
            return decryptedLinks;
        }
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        finallink = br.getRegex("onclick=\"NewWindow\\(\\'(.*?)\\',\\'name\\'").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("a href=\"(htt.*?)\"").getMatch(0);
        }
        if (finallink == null) {
            finallink = finallink2;
        }
        if (finallink == null) {
            return null;
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
        /* TODO */
        return null;
    }

}