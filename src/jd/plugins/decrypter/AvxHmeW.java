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
import jd.http.Browser.BrowserException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * @author typek_pb
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "avaxhome.ws" }, urls = { "http://(www\\.)?(avaxhome\\.(ws|bz|cc)|avaxho\\.me|avaxhm\\.com)/(ebooks|music|software|video|magazines|newspapers|games|graphics|misc|hraphile|comics)/.+" }, flags = { 0 })
public class AvxHmeW extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public AvxHmeW(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String notThis = "https?://(?!(www\\.imdb\\.com|(avaxhome\\.(ws|bz|cc)|avaxho\\.me|avaxhm\\.com)))[\\S&]+";

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.clearCookies(getHost());
        String parameter = cryptedLink.toString().replaceAll("(avaxhow\\.me|avahome\\.bz|avaxhome\\.ws|avaxhm\\.com)", "avaxhome.cc");
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            logger.info("Link offline (server error): " + parameter);
            return decryptedLinks;
        }
        // 1.st try: <a href="LINK" target="_blank" rel="nofollow"> but ignore
        // images/self site refs + imdb refs
        String[] links = br.getRegex("<a href=\"(" + notThis + ")\" target=\"_blank\" rel=\"nofollow\">(?!<img)").getColumn(0);
        if (links != null && links.length != 0) {
            for (String link : links) {
                if (!link.matches(this.getSupportedLinks().pattern())) decryptedLinks.add(createDownloadlink(link));
            }
        }

        // try also LINK</br>, but ignore self site refs + imdb refs
        links = null;
        links = br.getRegex("(" + notThis + ")<br/>").getColumn(0);
        if (links != null && links.length != 0) {
            for (String link : links) {
                if (!link.matches(this.getSupportedLinks().pattern())) decryptedLinks.add(createDownloadlink(link));
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}