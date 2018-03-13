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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Application;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "crypt.to" }, urls = { "https?://(?:www\\.)?crypt\\.to/(?:fid|links),[A-Za-z0-9]+" })
public class CryptTo extends PluginForDecrypt {
    public CryptTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> dupelist = new ArrayList<String>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("\"images/error\\.gif\"")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String folderid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        /* First try to load container - this is the quickest way! */
        decryptedLinks = loadcontainer(folderid);
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            /* Some urls do not have containers --> Server will return 0b DLC --> We'll have to check for captcha & password */
            /*
             * TODO: Maybe add support for other captcha types and password protected folders - so far I (psp) only found this one case and
             * was not able to create new links with other captcha types/password ...
             */
            boolean failed = true;
            String code = null;
            for (int i = 0; i <= 5; i++) {
                final Form form = this.br.getFormbyProperty("id", "mainForm");
                if (i == 0 && form == null) {
                    /* 2017-02-06: No Form --> No captcha/password/form-redirect */
                    failed = false;
                    break;
                }
                if (form.containsHTML("captcha\\.inc\\.php")) {
                    code = this.getCaptchaCode("/inc/captcha.inc.php", param);
                    form.put("pruefcode", code);
                }
                this.br.submitForm(form);
                if (br.containsHTML(">Passwort falsch oder das Captcha wurde nicht")) {
                    this.br.getPage(parameter);
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            // String fpName = null;
            final String[] linkkeys = br.getRegex("out\\(\\\\'([^<>\"\\']*?)\\\\'").getColumn(0);
            if (linkkeys == null || linkkeys.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            this.br.setFollowRedirects(false);
            for (final String linkkey : linkkeys) {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return decryptedLinks;
                }
                if (dupelist.contains(linkkey)) {
                    continue;
                }
                /* Add current linkkey to our dupelist */
                dupelist.add(linkkey);
                this.br.getPage("http://crypt.to/iframe.php?row=0&linkkey=" + linkkey);
                final String finallink = this.br.getRedirectLocation();
                if (finallink == null || finallink.matches(".+crypt\\.to/.+")) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
            // if (fpName != null) {
            // final FilePackage fp = FilePackage.getInstance();
            // fp.setName(Encoding.htmlDecode(fpName.trim()));
            // fp.addLinks(decryptedLinks);
            // }
        }
        return decryptedLinks;
    }

    @SuppressWarnings("deprecation")
    private ArrayList<DownloadLink> loadcontainer(final String folderid) throws IOException, PluginException {
        final String theLink = "http://crypt.to/container," + folderid + ",0";
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final Browser brc = br.cloneBrowser();
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(theLink);
            if (con.getResponseCode() == 200) {
                file = Application.getTempResource("/cryptto/" + folderid + ".dlc");
                file.getParentFile().mkdirs();
                brc.downloadConnection(file, con);
                if (file.exists() && file.length() > 100) {
                    links.addAll(loadContainerFile(file));
                }
            }
        } catch (Throwable e) {
            throw new IOException(e);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
            if (file != null && file.exists()) {
                file.delete();
            }
        }
        return links;
    }
}
