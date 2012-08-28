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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1channel.ch" }, urls = { "http://(www\\.)?1channel\\.(ch|li)/(watch\\-\\d+|tv\\-\\d+[A-Za-z0-9\\-_]+/season\\-\\d+\\-episode\\-\\d+)" }, flags = { 0 })
public class OneChannelCh extends PluginForDecrypt {

    public OneChannelCh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("1channel.li/", "1channel.ch/");
        br.getPage(parameter);
        String fpName = br.getRegex("<title>Watch ([^<>\"]*?) online \\-  on 1Channel \\| [^<>\"]*?</title>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\">").getMatch(0);
        String[] links = br.getRegex("\\&url=([^<>\"]*?)\\&domain=").getColumn(0);
        if (links == null || links.length == 0) {
            if (br.containsHTML("\\'HD Sponsor\\'")) {
                logger.info("Found no downloadlink in link: " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links)
            decryptedLinks.add(createDownloadlink(Encoding.Base64Decode(singleLink)));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
