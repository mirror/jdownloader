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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagevenue.com" }, urls = { "http://[\\w\\.]*?img[0-9]+\\.imagevenue\\.com/img\\.php\\?image=.+" }, flags = { 0 })
public class ImgVenueCm extends PluginForDecrypt {

    public ImgVenueCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        /* Error handling */
        if (br.containsHTML("This image does not exist on this server")) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        String links = br.getRegex("scaleImg\\(\\)\"  SRC=\"(.*?)\"").getMatch(0);
        if (links == null) return null;
        String server = new Regex(parameter, "(img[0-9]+\\.imagevenue\\.com/)").getMatch(0);
        links = "directhttp://http://" + server + links;
        String decryptedlink = links;
        DownloadLink dl = createDownloadlink(decryptedlink);
        dl.setName("pic");
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    // @Override

}
