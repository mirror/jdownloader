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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pururin.com" }, urls = { "http://(www\\.)?pururin\\.com/(gallery|thumbs)/\\d+/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class PururinCom extends PluginForDecrypt {

    /**
     * @author raztoki
     * */
    public PururinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replace("/gallery/", "/thumbs/");
        getPage(parameter);
        if (br.containsHTML(">Page not found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<h1>([^<>\"]*?) Thumbnails</h1>").getMatch(0);
        final String uid = new Regex(parameter, "/thumbs/(\\d+)").getMatch(0);
        final String[] links = br.getRegex("\"(/view/" + uid + "/\\d+/[a-z0-9\\-_]+\\.html)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        DecimalFormat df_links = new DecimalFormat("00");
        if (links.length > 999) {
            df_links = new DecimalFormat("0000");
        } else if (links.length > 99) {
            df_links = new DecimalFormat("000");
        }
        for (String link : links) {
            final DownloadLink dl = createDownloadlink("http://pururin.com" + link);
            dl.setAvailable(true);
            final String[] fn = new Regex(link, "/([^/]+)(_|-)(\\d+)(\\.[a-z0-9]{3,4})$").getRow(0);
            // not final as this hasn't been confirmed. We give image extension so package customiser rules are easier to work with!
            dl.setName(fn[0] + "-" + df_links.format(Integer.parseInt(fn[2])) + fn[3].replace(".html", ".jpg"));
            dl.setProperty("links_length", links.length);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private PluginForHost plugin = null;

    private void getPage(final String parameter) throws Exception {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("pururin.com");
            if (plugin == null) {
                throw new IllegalStateException("pururin.com hoster plugin not found!");
            }
            // set cross browser support
            ((jd.plugins.hoster.PururinCom) plugin).setBrowser(br);
        }
        ((jd.plugins.hoster.PururinCom) plugin).getPage(parameter);
    }

}
