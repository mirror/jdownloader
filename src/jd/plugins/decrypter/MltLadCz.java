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
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

//multiload.cz by pspzockerscene
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiload.cz" }, urls = { "http://[\\w\\.]*?multiload\\.cz/(stahnout/[0-9]+/|html/stahnout_process\\.php\\?akce=download&id=[0-9]+&server=[0-9])" }, flags = { 0 })
public class MltLadCz extends PluginForDecrypt {

    public MltLadCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        /* Error handling */
        if (br.containsHTML("soubor neexistuje")) {
            logger.warning("The requested document was not found on this server.");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        /* Single part handling */
        if (parameter.contains("server")) {
            if (br.getRedirectLocation() == null) return decryptedLinks;
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            return decryptedLinks;
        }
        /* File package handling */
        String[] links = br.getRegex("<li><a href=\"(/html/.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            String link0 = "http://www.multiload.cz" + link;
            String link1 = link0.replace("amp;", "");
            br.getPage(link1);
            String finallink = br.getRedirectLocation();
            if (finallink == null) return decryptedLinks;
            DownloadLink dl = createDownloadlink(br.getRedirectLocation());
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    // @Override

}
