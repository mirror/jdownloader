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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "efukt.com" }, urls = { "http://(www\\.)?efukt\\.com/(\\d+[A-Za-z0-9_\\-]+\\.html|out\\.php\\?id=\\d+)" }, flags = { 0 })
public class EfuktComDecrypter extends PluginForDecrypt {

    public EfuktComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_redirect = "http://(www\\.)?efukt\\.com/out\\.php\\?id=\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final String redirect = br.getRedirectLocation();
        if (parameter.matches(type_redirect) && redirect != null && !redirect.contains("efukt.com/")) {
            decryptedLinks.add(createDownloadlink(redirect));
            return decryptedLinks;
        }
        final DownloadLink main = createDownloadlink(parameter.replace("efukt.com/", "efuktdecrypted.com/"));
        if (br.getURL().equals("http://efukt.com/")) {
            main.setFinalFileName(new Regex(parameter, "https?://efukt\\.com/(.+)").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        decryptedLinks.add(main);

        return decryptedLinks;
    }

}
