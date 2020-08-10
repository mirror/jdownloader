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

import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "shotzon.com" }, urls = { "https?://(?:www\\.)?(?:enagato\\.com|shotzon\\.com)/(?:full/\\?api=[a-f0-9]+\\&url=[a-zA-Z0-9_/\\+\\=\\-%]+|[A-Za-z0-9]{2,})" })
public class ShotzonCom extends PluginForDecrypt {
    public ShotzonCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String base64Str = UrlQuery.parse(parameter).get("url");
        if (base64Str != null) {
            /* 2020-08-10: Special: no http request required at all! */
            final String finallink = Encoding.Base64Decode(Encoding.htmlDecode(base64Str));
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        } else {
            final PluginForDecrypt templateCrawler = JDUtilities.getPluginForDecrypt("cut-urls.com");
            templateCrawler.setBrowser(this.br);
            decryptedLinks = templateCrawler.decryptIt(param, null);
            return decryptedLinks;
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MightyScript_AdLinkFly;
    }
}
