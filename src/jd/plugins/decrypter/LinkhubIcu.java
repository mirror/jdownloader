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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linkhub.icu" }, urls = { "https?://(?:www\\.)?linkhub\\.icu/(?:view|get)/([A-Za-z0-9]+)" })
public class LinkhubIcu extends PluginForDecrypt {
    public LinkhubIcu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String lid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(lid)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* 2020-02-04: Captcha = skippable */
        final String continue_url = br.getRegex("(/view/[A-Za-z0-9]+)").getMatch(0);
        if (continue_url != null) {
            br.getPage(continue_url);
        }
        Form pwprotected = br.getFormByInputFieldKeyValue("unlock", "UNLOCK");
        String passCode = null;
        if (pwprotected != null) {
            passCode = getUserInput("Password?", param);
            pwprotected.put("pass", Encoding.urlEncode(passCode));
            br.submitForm(pwprotected);
            if (br.getFormByInputFieldKeyValue("unlock", "UNLOCK") != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        String fpName = br.getRegex("<title>([^<>\"]+) - LinkHub - Protect Your Link In Just One-Click</title>").getMatch(0);
        if (fpName != null && StringUtils.containsIgnoreCase(fpName, "linkhub")) {
            fpName = null;
        }
        final String[] links = br.getRegex("<a href=\"(http[^<>\"]+)\" target=\"_blank\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken fosr link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink(singleLink);
            if (passCode != null) {
                dl.setDownloadPassword(passCode);
            }
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
