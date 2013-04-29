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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "scribd.com" }, urls = { "http://(www\\.)?scribd\\.com/collections/\\d+/[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class ScribdCom extends PluginForDecrypt {

    public ScribdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().equals("http://www.scribd.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("http://www.scribd.com/profiles/collections/get_collection_documents?page=1&id=" + new Regex(parameter, "/collections/(\\d+)/").getMatch(0));
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        final String[] links = br.getRegex("data\\-object_id=\"(\\d+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links)
            decryptedLinks.add(createDownloadlink("http://www.scribd.com/doc/" + singleLink));
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("scribd.com collection - " + new Regex(parameter, "scribd\\.com/collections/\\d+/([A-Za-z0-9\\-_]+)").getMatch(0));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
