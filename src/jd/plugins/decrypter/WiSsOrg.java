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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

//by Maniac+pspzockerscene
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wiiisos.org" }, urls = { "http://[\\w\\.]*?wiiisos\\.org/(d\\d\\d?wn|d3741l5)/.+(/\\d+)?" }, flags = { 0 })
public class WiSsOrg extends PluginForDecrypt {

    public WiSsOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        /* Error handling */
        if (br.containsHTML("requested document was not found") || (br.getRedirectLocation() != null && br.getRedirectLocation().contentEquals("http://pspisos.org/"))) {
            logger.warning("The requested document was not found on this server.");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        /* Single part handling */
        if (br.getRedirectLocation() != null) {
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            return decryptedLinks;
        }

        /* File package handling */
        String fpName0 = br.getRegex("<div class=\"content\">.*?<fieldset>.*?<legend>(.*?)</legend>.*?<a href=\"javascript:history\\.back").getMatch(0).trim();
        String fpName = fpName0.replace(" ", "");
        fp.setName(fpName);
        String[] links = br.getRegex("<a href=\"(http://wiiisos\\.org/d\\d\\d?wn/.+?/\\d+?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            br.getPage(link);
            if (br.getRedirectLocation() == null) continue;
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            progress.increase(1);
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    // @Override

}
