//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.RandomUserAgent;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linksave.in" }, urls = { "https?://(www\\.)?linksave\\.in/(view.php\\?id=)?(?!dl\\-)[\\w]+" }, flags = { 0 })
public class Lnksvn extends PluginForDecrypt {

    public Lnksvn(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("https://", "http://");
        br.setRequestIntervalLimit(getHost(), 1000);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://linksave.in/", "Linksave_Language", "german");
        br.setRequestIntervalLimit("linksave.in", 1000);
        br.setFollowRedirects(true);
        br.setFollowRedirects(false);
        br.getPage(param.getCryptedUrl());
        String redirect = br.getRedirectLocation();
        if (redirect != null) {
            decryptedLinks.add(createDownloadlink(redirect));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}