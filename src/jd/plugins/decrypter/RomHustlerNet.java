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
        final PluginForHost rhPlugin = JDUtilities.getPluginForHost(this.getHost());
        ((jd.plugins.hoster.RomHustlerNet) rhPlugin).prepBrowser(this.br);
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">404 \\- Page got lost|>\\s*This is a ESA protected rom")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String url_name = new Regex(parameter, "/rom/(.+)").getMatch(0);
        String fpName = br.getRegex("<h1 [^>]*?itemprop=\"name\"[^>]*?>([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = url_name;
        }
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
                return null;
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