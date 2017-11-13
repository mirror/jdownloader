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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 * So I had this written some time back, just never committed. Here is my original with proper error handling etc. -raz
 *
 * @author raztoki
 * */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "scnlog.eu" }, urls = { "https?://(?:www\\.)?scnlog\\.(#?:eu|me)/(?:[a-z0-9_\\-]+/){2}" })
public class ScnlogEu extends antiDDoSForDecrypt {
    public ScnlogEu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        getPage(parameter);
        if (br.containsHTML("<title>404 Page Not Found</title>|>Sorry, but you are looking for something that isn't here\\.<") || this.br.toString().length() < 200 || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter, "invalidurl", "invalidurl"));
            return decryptedLinks;
        } else if (this.br.getURL().contains("/feed/") || this.br.getURL().contains("/feedback/") || this.br.getURL().contains("/page/")) {
            decryptedLinks.add(createOfflinelink(parameter, "invalidurl", "invalidurl"));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<strong>Release:</strong>\\s*(.*?)<(?:/|\\w*\\s*/)").getMatch(0);
        String download = br.getRegex("<div class=\"download\">.*?</div>").getMatch(-1);
        if (download == null) {
            logger.warning("Can not find 'download table', Please report this to JDownloader Development Team : " + parameter);
            return null;
        }
        String[] results = HTMLParser.getHttpLinks(download, "");
        for (String result : results) {
            // prevent site links from been added.
            if (result.matches("https?://[^/]*scnlog.(?:eu|me)/.+")) {
                continue;
            }
            decryptedLinks.add(createDownloadlink(result));
        }
        if (decryptedLinks.isEmpty()) {
            if (br.containsHTML(">Links have been removed due to DMCA request<")) {
                try {
                    decryptedLinks.add(createOfflinelink(parameter, fpName, null));
                } catch (final Throwable t) {
                    logger.info("Offline Content: " + parameter);
                }
                return decryptedLinks;
            }
            logger.warning("'decrptedLinks' isEmpty!, Please report this to JDownloader Development Team : " + parameter);
            return null;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setProperty("ALLOW_MERGE", true);
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}