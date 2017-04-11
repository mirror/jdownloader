//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialogInterface;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rajce.idnes.cz" }, urls = { "https?://.*\\.rajce\\.idnes\\.cz/([^/]+/?#.*|[^/]+/?$|[^/]*\\?.+|/?$)" })
public class DnsCz extends PluginForDecrypt {
    public DnsCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://img\\d+\\.rajce.*";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        final HashSet<String> dupCheck = new HashSet<String>();
        while (parameter != null && dupCheck.add(parameter)) {
            if (parsePage(decryptedLinks, parameter)) {
                parameter = br.getRegex("<a href=\"([^\"<>]*?page=\\d+[^\"<>]*?)\">[^<>]*?\\»</a>").getMatch(0);
            } else {
                break;
            }
        }
        return decryptedLinks;
    }

    /**
     * @param decryptedLinks
     * @param parameter
     * @throws Exception
     */
    protected boolean parsePage(ArrayList<DownloadLink> decryptedLinks, String parameter) throws Exception {
        br.getPage(parameter);
        while (br.containsHTML("<div class=\"album-login\">")) {
            Form form = br.getFormbyKey("login");
            String error = br.getRegex("<div class=\"error\">([^<]+)").getMatch(0);
            String userName = new Regex(parameter, "http://([^\\.]+)").getMatch(0);
            String album = new Regex(parameter, "\\.cz/([^/]+)").getMatch(0);
            LoginDialogInterface d = UIOManager.I().show(LoginDialogInterface.class, new LoginDialog(LoginDialog.DISABLE_REMEMBER, "Gallery Password for " + album + " by " + userName, "The gallery " + album + " by " + userName + " requires logins." + (StringUtils.isEmpty(error) ? "" : "  Error: " + error), null));
            if (d.getCloseReason() != CloseReason.OK) {
                return false;
            }
            final String user = d.getUsername();
            final String pass = d.getPassword();
            if (StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
                return false;
            }
            form.getInputField("login").setValue(Encoding.urlEncode(user));
            form.getInputField("password").setValue(Encoding.urlEncode(pass));
            br.submitForm(form);
        }
        decryptAlbumPage(decryptedLinks, br);
        final String[] albums = br.getRegex("<a class=\"albumName\" href=\"(https?://.*?.rajce.idnes.cz/[^/]+/)\">").getColumn(0);
        if (albums != null) {
            for (final String albumUrl : albums) {
                final Browser clone = br.cloneBrowser();
                clone.getPage(albumUrl);
                decryptAlbumPage(decryptedLinks, clone);
            }
        }
        return true;
    }

    /**
     * @param decryptedLinks
     * @param br
     * @throws IOException
     */
    protected void decryptAlbumPage(ArrayList<DownloadLink> decryptedLinks, Browser br) throws IOException {
        String albumUserName = br.getRegex("<h1.*?id=\"albumlistUserName\">(.*?)<").getMatch(0);
        String albumName = br.getRegex("<h2 id=\"albumName\">(.*?)</h2>").getMatch(0);
        if (albumName != null && albumUserName != null) {
            albumName = albumName.trim();
            albumUserName = albumUserName.trim();
            String[][] galleryLinks = br.getRegex("<a id=\"p_(\\d+)\" href=\"(https?://img\\d+.rajce.idnes.cz[^\"]+)\".*?title=\"([^\"]*)").getMatches();
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(albumName) + " by " + Encoding.htmlDecode(albumUserName));
            for (String[] link : galleryLinks) {
                DownloadLink dl = createDownloadlink(link[1]);
                dl.setProperty("album", albumName);
                dl.setProperty("user", albumUserName);
                dl.setProperty("title", link[2]);
                dl.setAvailableStatus(AvailableStatus.TRUE);
                fp.add(dl);
                decryptedLinks.add(dl);
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}