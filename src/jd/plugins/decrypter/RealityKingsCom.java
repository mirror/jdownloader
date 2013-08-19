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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "realitykings.com" }, urls = { "http://(www\\.)?members\\.rk\\.com/\\?a=update\\.download\\&site=[a-z0-9]+\\&id=\\d+.+" }, flags = { 0 })
public class RealityKingsCom extends PluginForDecrypt {

    public RealityKingsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        if (parameter.matches(jd.plugins.hoster.RealityKingsCom.VIDEOLINK)) {
            decryptedLinks.add(createDownloadlink(parameter.replace(".rk.com/", ".rkdecrypted.com/")));
            return decryptedLinks;
        }

        if (!getUserLogin(false)) {
            logger.info("Can only decrypt link with account: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("class=\"ppp\\-post\\-login\\-body\"")) br.getPage(parameter);

        final String fpName = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);

        if (br.getURL().equals("http://members.rk.com/?a=user.home")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String[] downloadlinks = br.getRegex("\"(/\\?a=update\\.download\\&site=[a-z0-9]+\\&id=\\d+\\&download=[A-Za-z0-9%=\\+]+)\"").getColumn(0);
        final String[] piclinks = br.getRegex("(http://(www\\.)?imagesr\\.rk\\.com/content/[a-z0-9]+/pictures/[a-z0-9\\-_]+/[a-z0-9\\-_]+\\.zip\\?nvb=\\d+\\&nva=\\d+&hash=[a-z0-9]+)").getColumn(0);
        if ((downloadlinks == null || downloadlinks.length == 0) && (piclinks == null || piclinks.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (downloadlinks != null && downloadlinks.length != 0) {
            for (final String videolink : downloadlinks) {
                final DownloadLink dl = createDownloadlink("http://members.rkdecrypted.com" + videolink);
                dl.setName(new Regex(videolink, "([A-Za-z0-9%=\\+]+)$").getMatch(0));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        if (piclinks != null && piclinks.length != 0) {
            for (final String piclink : piclinks) {
                final DownloadLink dl = createDownloadlink(piclink.replace("imagesr.rk.com/", "imagesr.rkdecrypted.com/"));
                dl.setName(new Regex(piclink, "([A-Za-z0-9%=\\+]+)$").getMatch(0));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("realitykings.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.RealityKingsCom) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            return false;
        }
        return true;
    }

}
