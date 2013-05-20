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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

//This decrypter is there to seperate folder- and hosterlinks as hosterlinks look the same as folderlinks
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "i-filez.com", "depfile.com" }, urls = { "rfh5ujnthUNUSED_REGEX_HAHHAHAHAHAdcj43z8hgto9vhr", "https?://(www\\.)?(i\\-filez|depfile)\\.com/(downloads/i/\\d+/f/[^\"\\']+|(?!downloads)[a-zA-Z0-9]+)" }, flags = { 0, 0 })
public class IFilezComDecrypter extends PluginForDecrypt {

    public IFilezComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DEPFILEDECRYPTED = "depfiledecrypted.com/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("i-filez", "depfile");
        // Set English language
        br.setCookie(this.getHost(), "sdlanguageid", "2");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        handleErrors();
        String[] links = br.getRegex("(https?://(www\\.)?depfile\\.com/downloads/i/\\d+/f/[^\"' ><]+|https?://(www\\.)?depfile\\.com/[a-zA-Z0-9]{8}\\?cid=[a-z0-9]{32})").getColumn(0);
        if (links != null && links.length != 0) {
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl.replace("depfile.com/", DEPFILEDECRYPTED)));
        } else {
            if (br.containsHTML(">Description of the downloaded folder")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(parameter.replace("depfile.com/", DEPFILEDECRYPTED)));
        }
        return decryptedLinks;
    }

    private void handleErrors() throws Exception {
        PluginForHost DeviantArtPlugin = JDUtilities.getPluginForHost("depfile.com");
        try {
            ((jd.plugins.hoster.IFilezCom) DeviantArtPlugin).handleErrors();
        } catch (final Exception e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}