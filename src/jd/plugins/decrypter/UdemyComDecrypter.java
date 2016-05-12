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
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "udemy.com" }, urls = { "https?://(?:www\\.)?udemy\\.com/.+" }, flags = { 0 })
public class UdemyComDecrypter extends PluginForDecrypt {

    public UdemyComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String decrypter_domain = "udemydecrypted.com";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(jd.plugins.hoster.UdemyCom.TYPE_SINGLE_PREMIUM)) {
            /* Single links --> Host plugin */
            decryptedLinks.add(this.createDownloadlink(parameter.replace(this.getHost() + "/", decrypter_domain + "/")));
            return decryptedLinks;
        }
        final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost("udemy.com"));
        if (aa == null) {
            logger.info("Account needed to download urls of this website");
            return decryptedLinks;
        }
        try {
            jd.plugins.hoster.UdemyCom.login(this.br, aa, false);
        } catch (final Throwable e) {
        }
        jd.plugins.hoster.UdemyCom.prepBRAPI(this.br);
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String courseid = jd.plugins.hoster.UdemyCom.getCourseID(this.br);
        if (courseid == null) {
            logger.info("Could not find any downloadable content");
            return decryptedLinks;
        }
        this.br.getPage("https://www.udemy.com/api-2.0/courses/" + courseid + "/subscriber-curriculum-items?fields%5Basset%5D=@default&fields%5Bchapter%5D=@default,object_index&fields%5Blecture%5D=@default,asset,content_summary,num_discussions,num_external_link_assets,num_notes,num_source_code_assets,object_index,url&fields%5Bquiz%5D=@default,content_summary,object_index,url&page_size=9999");
        if (this.br.getHttpConnection().getResponseCode() == 403) {
            logger.info("User tried to download content which he did not pay for --> Impossible");
            return decryptedLinks;
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = new Regex(parameter, "udemy\\.com/([^/]+)").getMatch(0);
        final String[] links = br.getRegex("\"(/[^/]+/learn/[^<>\"]+/lecture/\\d+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            final String fid = new Regex(singleLink, "(\\d+)$").getMatch(0);
            final String temp_filename = new Regex(singleLink, "^/([^/]+)/").getMatch(0);
            if (fid == null || temp_filename == null) {
                return null;
            }
            final DownloadLink dl = createDownloadlink("http://" + decrypter_domain + singleLink);
            dl.setName(temp_filename + "_" + fid + ".mp4");
            dl.setContentUrl("https://www." + this.getHost() + singleLink);
            dl.setAvailable(true);
            dl.setLinkID(fid);
            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
