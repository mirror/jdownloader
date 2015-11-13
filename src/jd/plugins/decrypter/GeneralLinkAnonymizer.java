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
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "2ch.io", "l.moapi.net" }, urls = { "https?://(?:www\\.)?2ch\\.io/.+", "https?://(?:www\\.)?l\\.moapi\\.net/.+" }, flags = { 0, 0 })
public class GeneralLinkAnonymizer extends PluginForDecrypt {

    public GeneralLinkAnonymizer(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String real_url_without_protocol = new Regex(parameter, "https?://(?:www\\.)?[^/]+/(.+)").getMatch(0);
        decryptedLinks.add(createDownloadlink("http://" + real_url_without_protocol));

        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return null; // GeneralLinkAnonymizer
    }

}
