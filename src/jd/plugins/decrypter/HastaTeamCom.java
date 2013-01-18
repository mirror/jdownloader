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
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hastateam.com" }, urls = { "http://(www\\.)?hastateam\\.com/forum/viewtopic\\.php\\?f=\\d+\\&t=\\d+" }, flags = { 0 })
public class HastaTeamCom extends PluginForDecrypt {

    public HastaTeamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        /** Login process */
        if (!getUserLogin()) {
            logger.info("Logindata invalid, stopping...");
            return decryptedLinks;
        }
        br.getPage(parameter);
        final String fpName = br.getRegex("<h3 class=\"first\"><a href=\"#p\\d+\">([^<>\"]*?) \\- Download</a></h3>").getMatch(0);
        br.getPage("http://hastateam.com/ListDir/KHRDir.php");

        // 'http://hastateam.com/getfile.php?num_link=1&name=Katekyo_Hitman_Reborn/Katekyo_Hitman_Reborn_v01[MS].rar'
        final String[] links = br.getRegex("\\'(http://(www\\.)?hastateam\\.com/getfile\\.php\\?num_link=\\d+\\&name=[^<>\"]*?)\\'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink(singleLink);
            dl.setName(new Regex(singleLink, "/([^<>\"/]+)$").getMatch(0));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private boolean getUserLogin() throws Exception {
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("hastateam.com");
        Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        if (aa == null) {
            String username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + this.getHost() + " :");
            if (username == null) throw new DecrypterException(JDL.L("plugins.decrypter.hastateamcom.nousername", "Username not entered!"));
            String password = UserIO.getInstance().requestInputDialog("Enter password for " + this.getHost() + " :");
            if (password == null) throw new DecrypterException(JDL.L("plugins.decrypter.hastateamcom.nopassword", "Password not entered!"));
            aa = new Account(username, password);
        }
        try {
            ((jd.plugins.hoster.HastaTeamComHoster) hosterPlugin).login(this.br, aa, false);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(hosterPlugin, aa);
        return true;
    }

    private void login(Browser br, Account aa, boolean b) {
    }
}
