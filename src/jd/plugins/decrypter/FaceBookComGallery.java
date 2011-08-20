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
import jd.plugins.hoster.FaceBookComVideos;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "http(s)?://(www\\.)?facebook\\.com/(#\\!/)?media/set/\\?set=a\\.\\d+\\.\\d+\\.\\d+" }, flags = { 0 })
public class FaceBookComGallery extends PluginForDecrypt {

    public FaceBookComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* must be static so all plugins share same lock */
    private static final Object LOCK             = new Object();
    private static String       FACEBOOKMAINPAGE = "http://www.facebook.com";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        synchronized (LOCK) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            String parameter = param.toString().replace("#!/", "");
            br.setCookie(FACEBOOKMAINPAGE, "locale", "en_GB");
            final PluginForHost facebookPlugin = JDUtilities.getPluginForHost("facebook.com");
            Account aa = AccountController.getInstance().getValidAccount(facebookPlugin);
            if (aa == null) {
                String username = UserIO.getInstance().requestInputDialog("Enter Loginname for facebook.com :");
                if (username == null) throw new DecrypterException(JDL.L("plugins.decrypt.facebookcomgallery.nousername", "Username not entered!"));
                String password = UserIO.getInstance().requestInputDialog("Enter password for facebook.com :");
                if (password == null) throw new DecrypterException(JDL.L("plugins.decrypt.facebookcomgallery.nopassword", "Password not entered!"));
                aa = new Account(username, password);
            }
            try {
                ((FaceBookComVideos) facebookPlugin).login(aa, false, this.br);
            } catch (final PluginException e) {
                aa.setEnabled(false);
                aa.setValid(false);
                throw new DecrypterException(JDL.L("plugins.decrypt.facebookcomgallery.invalidaccount", "Account is invalid!"));
            }
            // Account is valid, let's just add it
            AccountController.getInstance().addAccount(facebookPlugin, aa);
            br.getPage(parameter);
            String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            boolean doExtended = false;
            String[] links = br.getRegex("\\&amp;src=(http\\\\u00253A.*?)\\&amp;theater\\\\\"").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("ajaxify=\\\\\"http:\\\\/\\\\/www\\.facebook\\.com\\\\/photo\\.php\\?fbid=(\\d+)\\&amp;").getColumn(0);
                doExtended = true;
            }
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (doExtended) {
                progress.setRange(links.length);
                String setID = new Regex(parameter, "facebook\\.com/media/set/\\?set=(.+)").getMatch(0);
                for (String fbid : links) {
                    br.getPage("http://www.facebook.com/photo.php?fbid=" + fbid + "&set=" + setID + "&type=1");
                    String finallink = br.getRegex("\"Weiter\"><img src=\"(http://.*?)\"").getMatch(0);
                    if (finallink == null) finallink = br.getRegex("\"(http://a\\d+\\.sphotos\\.ak\\.fbcdn\\.net/photos\\-.*?)\"").getMatch(0);
                    if (finallink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                    progress.increase(1);
                }
            } else {
                for (String directlink : links) {
                    directlink = Encoding.htmlDecode(((FaceBookComVideos) facebookPlugin).decodeUnicode(directlink));
                    decryptedLinks.add(createDownloadlink("directhttp://" + directlink));
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
    }
}
