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
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sta.sh" }, urls = { "http://(www\\.)?sta\\.sh/[a-z0-9]+" }, flags = { 0 })
public class StaShDecrypter extends PluginForDecrypt {

    public StaShDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String INVALIDLINKS = "http://(www\\.)?sta\\.sh/(muro|writer|login)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final DownloadLink main = createDownloadlink(parameter.replace("sta.sh/", "stadecrypted.sh/"));
        if (parameter.matches(INVALIDLINKS)) {
            main.setAvailable(false);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            main.setAvailable(false);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        final String[][] picdata = br.getRegex("class=\"thumb\" href=\"(https?://(www\\.)?sta\\.sh/[a-z0-9]+)\" title=\"([^<>\"]*?)\"").getMatches();
        if (picdata == null || picdata.length == 0) {
            decryptedLinks.add(main);
            return decryptedLinks;
        }

        String fpName = br.getRegex("name=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (fpName == null) fpName = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
        fpName = Encoding.htmlDecode(fpName.trim());

        for (final String singleLinkData[] : picdata) {
            final String url = singleLinkData[0];
            final String name = Encoding.htmlDecode(singleLinkData[2]);
            final DownloadLink dl = createDownloadlink(url.replace("sta.sh/", "stadecrypted.sh/"));
            dl.setName(name + ".png");
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        final String zipLink = br.getRegex("\"(/zip/[a-z0-9]+)\"").getMatch(0);
        if (zipLink != null) {
            final DownloadLink zip = createDownloadlink("http://stadecrypted.sh" + zipLink);
            zip.setName(fpName + ".zip");
            zip.setAvailable(true);
            decryptedLinks.add(zip);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
