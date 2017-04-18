//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.SiteType.SiteTemplate;

/**
 * DEV NOTE: uid are 1 digit, tested with uplds.com/1 /2 /20 /100 etc<br/>
 * <br/>
 *
 * NOTE: DO NOT DELETE, this is a template.
 *
 * @author psp
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uplds.com" }, urls = { "http://(www\\.)?uplds\\.com/\\d+" })
public class UpldsCom extends antiDDoSForDecrypt {

    public UpldsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setReadTimeout(3 * 60 * 1000);
        getPage(parameter);
        if (br.containsHTML("Link Not Found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final Form decryptform = br.getFormbyProperty("name", "F1");
        if (decryptform == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        submitForm(decryptform);
        final String finallink = br.getRegex("<span.*?>\\s*<a href=\"(http[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XLinker;
    }

}