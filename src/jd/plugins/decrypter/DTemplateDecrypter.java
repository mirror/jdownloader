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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "DTemplateDecrypter" }, urls = { "http://(www\\.)?(cloudyload\\.com|filebeer\\.info)/(\\d+~f|[A-Za-z0-9]+)" }, flags = { 0 })
public class DTemplateDecrypter extends PluginForDecrypt {

    public DTemplateDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** This is a decrypter for all hosters which are using the DTemplate */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString();
        final String host = new Regex(parameter, "http://(www\\.)?([^<>\"/]*?)/\\d+(~f)?").getMatch(1);
        if (parameter.contains("~f")) {
            br.getPage(parameter);
            if (br.getURL().contains("/index.html")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            ArrayList<String> allPages = new ArrayList<String>();
            allPages.add("1");
            final String[] tempPages = br.getRegex("\"\\?page=(\\d+)\"").getColumn(0);
            if (tempPages != null && tempPages.length != 0) {
                for (final String aPage : tempPages)
                    if (!allPages.contains(aPage)) allPages.add(aPage);
            }

            for (final String currentPage : allPages) {
                if (!currentPage.equals("1")) br.getPage(parameter + "?page=" + currentPage);
                final String[] links = br.getRegex("<a href=\"(http://(www\\.)?" + host + "/[a-z0-9]+)").getColumn(0);
                if (links == null || links.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (String singleLink : links)
                    decryptedLinks.add(createDownloadlink(singleLink.replace(host, host + "decrypted")));
            }
        } else {
            decryptedLinks.add(createDownloadlink(parameter.replace(host, host + "decrypted")));
        }
        return decryptedLinks;
    }
}
