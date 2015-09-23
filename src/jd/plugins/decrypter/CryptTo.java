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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "crypt.to" }, urls = { "https?://(?:www\\.)?crypt\\.to/fid,[A-Za-z0-9]+" }, flags = { 0 })
public class CryptTo extends PluginForDecrypt {

    public CryptTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("\"images/error\\.gif\"")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String folderid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        final String containerurl = "http://crypt.to/container," + folderid + ",0";
        decryptedLinks = loadcontainer(containerurl);
        // String fpName = br.getRegex("").getMatch(0);
        // final String[] links = br.getRegex("").getColumn(0);
        // if (links == null || links.length == 0) {
        // logger.warning("Decrypter broken for link: " + parameter);
        // return null;
        // }
        // for (final String singleLink : links) {
        // decryptedLinks.add(createDownloadlink(singleLink));
        // }
        //
        // if (fpName != null) {
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(Encoding.htmlDecode(fpName.trim()));
        // fp.addLinks(decryptedLinks);
        // }

        return decryptedLinks;
    }

    @SuppressWarnings("deprecation")
    private ArrayList<DownloadLink> loadcontainer(final String theLink) throws IOException, PluginException {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final Browser brc = br.cloneBrowser();
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(theLink);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/cryptto/" + theLink);
                if (file == null) {
                    return links;
                }
                file.getParentFile().mkdirs();
                file.deleteOnExit();
                brc.downloadConnection(file, con);
                if (file != null && file.exists() && file.length() > 100) {
                    links.addAll(JDUtilities.getController().getContainerLinks(file));
                }
            }
        } catch (Throwable e) {

        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
            if (file.exists()) {
                file.delete();
            }

        }
        return links;
    }

}
