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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "clipsage.com" }, urls = { "https?://(?:www\\.)?clipsage\\.com/(?:embed\\-)?[a-z0-9]{12}" })
public class ClipsageComDecrypter extends PluginForDecrypt {
    public ClipsageComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String fuid = new Regex(param.toString(), "([a-z0-9]{12})$").getMatch(0);
        final String parameter = String.format("http://%s/%s.html", this.getHost(), fuid);
        final FilePackage fp = FilePackage.getInstance();
        String fpName = null;
        try {
            /* Try to find that one mirror URL ... */
            br.getPage(parameter);
            fpName = jd.plugins.hoster.ClipsageCom.getFilename(this.br.toString(), fuid);
            if (fpName == null) {
                fpName = fuid;
            }
            fp.setName(fpName);
            final String finallink = this.br.getRegex("(https?://host\\.hackerbox\\.org/[a-z0-9]{12})").getMatch(0);
            if (finallink != null) {
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } catch (final Throwable e) {
        }
        /* Add main URL to host plugin. */
        decryptedLinks.add(createDownloadlink(parameter.replace("clipsage.com/", "clipsagedecrypted.com/")));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
