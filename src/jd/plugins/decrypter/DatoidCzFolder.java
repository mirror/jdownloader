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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datoid.cz" }, urls = { "http://(www\\.)?datoid\\.(cz|sk)/slozka/[A-Za-z0-9]+/[A-Za-z0-9]+" }, flags = { 0 })
public class DatoidCzFolder extends PluginForDecrypt {

    public DatoidCzFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> allPages = new ArrayList<String>();
        allPages.add("1");
        final String parameter = param.toString().replace("datoid.sk/", "datoid.cz/");
        br.getPage(parameter);

        final String[] pages = br.getRegex("class=\"ajax\">(\\d+)</a>").getColumn(0);
        if (pages != null) {
            for (final String aPage : pages) {
                if (!allPages.contains(aPage)) allPages.add(aPage);
            }
        }
        logger.info("Found " + allPages.size() + " pages, starting to decrypt...");
        for (final String currentPage : allPages) {
            logger.info("Decrypting page " + currentPage + " / " + allPages.size());
            if (!currentPage.equals("1")) br.getPage(parameter + "?current-page=" + currentPage);

            // br.getPage("http://api.datoid.cz/v1/getfilesoffolder?url=" + Encoding.urlEncode(parameter));
            //
            // final String[] links = br.getRegex("\"(http:[^<>\"]*?)\"").getColumn(0);
            // if (links == null || links.length == 0) {
            // logger.warning("Decrypter broken for link: " + parameter);
            // return null;
            // }

            final String[] links = br.getRegex("\"(/[^<>\"]*?)\">[\t\n\r ]+<div class=\"thumb").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links)
                decryptedLinks.add(createDownloadlink("http://datoid.cz" + singleLink));
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(parameter, "/slozka/[A-Za-z0-9]+/(.+)").getMatch(0));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
