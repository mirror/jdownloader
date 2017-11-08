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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "public.upera.co" }, urls = { "https?://(?:www\\.)?public\\.upera\\.co/f/[A-Za-z0-9]+" })
public class PublicUperaCo extends PluginForDecrypt {
    public PublicUperaCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        final Plugin plugin = JDUtilities.getPluginForHost("public.upera.co");
        ((jd.plugins.hoster.PublicUperaCo) plugin).setBrowser(br);
        ((jd.plugins.hoster.PublicUperaCo) plugin).getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/f/") || br.containsHTML(">Invalid or Deleted File\\.<")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex(".+<li class=\"active\">(.*?)</li>\\s*<li class=\"active\">Files").getMatch(0);
        final String[][] links = br.getRegex("<td><a href=\"(https://(?:www\\.)?public\\.upera\\.co/[A-Za-z0-9]+)\"[^>]*>.*?>[\r\n]+(.*?)\\s*</a>.*?([0-9\\.]+ [KMGT]{0,1}B)<").getMatches();
        if (links == null || links.length == 0) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        for (final String singleLink[] : links) {
            final DownloadLink dl = createDownloadlink(singleLink[0]);
            dl.setName(Encoding.htmlDecode(singleLink[1]));
            dl.setDownloadSize(SizeFormatter.getSize(singleLink[2]));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
