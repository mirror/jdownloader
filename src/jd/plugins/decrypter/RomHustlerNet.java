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
import java.util.Random;

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
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "romhustler.net" }, urls = { "http://(www\\.)?romhustler\\.net/rom/[^<>\"/]+/[^<>\"/]+(/[^<>\"/]+)?" }, flags = { 0 })
public class RomHustlerNet extends PluginForDecrypt {

    public RomHustlerNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        PluginForHost rhPlugin = JDUtilities.getPluginForHost("romhustler.net");
        ((jd.plugins.hoster.RomHustlerNet) rhPlugin).prepBrowser(br);
        br.getPage(parameter);
        if (br.containsHTML(">404 \\- Page got lost")) {
            final DownloadLink offline = createDownloadlink("http://romhustler.net/download/" + new Random().nextInt(10000) + "/58zu09jmng6orhjkrjmtgmt56ojuWUZk8ib" + new Random().nextInt(10000));
            offline.setName(new Regex(parameter, "([^<>\"/]+)$").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<h1 style=\"font\\-size: 14pt;\">([^<>\"]*?)</h1>").getMatch(0);
        final String[] results = br.getRegex("(<a[^>]+/download/\\d+/[A-Za-z0-9/\\+=%]+[^>]+)>").getColumn(0);
        if (results == null || results.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String result : results) {
            String link = new Regex(result, "href=\"(/download/\\d+/[A-Za-z0-9/\\+=%]+[^>]+)\"").getMatch(0);
            String name = new Regex(result, "title[^\"]+>(.*?)</a>").getMatch(0);
            if (name == null) name = new Regex(result, "title=\"([^\"]+)").getMatch(0);
            if (name == null || link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DownloadLink dl = createDownloadlink("http://romhustler.net" + link);
            dl.setName(name);
            dl.setProperty("decrypterLink", parameter);
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