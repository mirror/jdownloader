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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "romhustler.net" }, urls = { "http://(www\\.)?romhustler\\.net/rom/[^<>\"/]+/[^<>\"/]+(/[^<>\"/]+)?" }) 
public class RomHustlerNet extends PluginForDecrypt {

    public RomHustlerNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        PluginForHost rhPlugin = JDUtilities.getPluginForHost("romhustler.net");
        ((jd.plugins.hoster.RomHustlerNet) rhPlugin).prepBrowser(br);
        br.getPage(parameter);
        if (br.containsHTML(">404 - Page got lost")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<h1 style=\"font-size: 14pt;\">([^<>\"]*?)</h1>").getMatch(0);
        final String[] results = br.getRegex("(<a[^>]+/(?:file|download)/\\d+/[A-Za-z0-9/\\+=%]+[^>]+)>").getColumn(0);
        if (results == null || results.length == 0 || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        int counter = 1;
        for (final String result : results) {
            final String link = new Regex(result, "href=\"((?:https?://(?:\\w\\.)?romhustler\\.net)?/(?:file|download)/\\d+/[A-Za-z0-9/\\+=%]+[^>]+)\"").getMatch(0);
            final String name = fpName + "_" + counter;
            if (link == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink dl = createDownloadlink(Request.getLocation(link, br.getRequest()));
            dl.setName(name);
            dl.setProperty("decrypterLink", parameter);
            if (counter > 1) {
                dl.setProperty("splitlink", true);
            }
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            counter++;
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}