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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xboxisozone.com" }, urls = { "http://(www\\.)?((xboxisozone|dcisozone|gcisozone|theisozone)\\.com/downloads/[^<>\"/]+/[^<>\"/]+/[^<>\"/]+/|psisozone\\.com/downloads/\\d+/.*?/|romgamer\\.com/roms/.*?/\\d+/)" }, flags = { 0 })
public class XboxSoZoneCom extends PluginForDecrypt {

    public XboxSoZoneCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("(g> The page you requested could not be found\\. </strong>|It may have been moved or deleted\\. <br)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("class=\"row_link\" >([^<>\"]*?)</a>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>([^<>\"]*?) \\&bull; .*?</title>").getMatch(0);
        }
        final String[] links = br.getRegex("\"(/dl\\-start/\\d+/(\\d+/)?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String host = new Regex(parameter, "(http://.*?\\.com)").getMatch(0);
        for (String finallink : links) {
            final DownloadLink finaldownloadlink = createDownloadlink(host + finallink);
            finaldownloadlink.setName(String.valueOf(new Random().nextInt(100000)));
            // Needed to download it later
            finaldownloadlink.setProperty("mainlink", parameter);
            decryptedLinks.add(finaldownloadlink);
        }
        final DownloadLink finaldownloadlink = createDownloadlink(parameter.replace("http://", "xboxisopremiumonly://"));
        finaldownloadlink.setProperty("premiumonly", true);
        finaldownloadlink.setName(String.valueOf(new Random().nextInt(100000)));
        // Needed to download it later
        finaldownloadlink.setProperty("mainlink", parameter);
        decryptedLinks.add(finaldownloadlink);
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
