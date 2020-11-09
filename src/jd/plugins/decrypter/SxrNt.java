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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sexuria.net" }, urls = { "https?://(?:www\\.)?sexuria\\.net/(\\d+)-([a-z0-9\\-]+)\\.html" })
public class SxrNt extends PluginForDecrypt {
    public SxrNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** This may be the future replacement of {@link #Sxrcm} jd.plugins.decrypter.Sxrcm */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String url_name = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        String fpName = url_name.replace("-", " ");
        final String[] links = br.getRegex("target=\"_blank\" href=\"(https?://[^\"]+)\" class=\"btn vertab\"").getColumn(0);
        final String password = br.getRegex("<strong>Password file</strong></td>\\s*<td>([^<>\"]+)</td>").getMatch(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!StringUtils.isEmpty(password)) {
            param.setDecrypterPassword(password);
        }
        // final Set<String> pws = PasswordUtils.getPasswords(password);
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink(singleLink);
            /* TODO */
            // if (!StringUtils.isEmpty(password)) {
            // dl.setSourcePluginPasswordList(new ArrayList<String>(pws));
            // }
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
