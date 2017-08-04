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
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mangahost.com" }, urls = { "https?://(?:www\\.)?(?:br\\.)?(mangahost\\.(com|net|me|org)|yesmangas\\.net)/manga/[^/]+/([^\\s]*\\d+(\\.\\d+|[a-z])?|one-shot)" })
public class MangahostCom extends antiDDoSForDecrypt {

    public MangahostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "mangahost.com", "mangahost.net", "mangahost.me", "mangahost.org", "yesmangas.net" };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 403) {
            logger.info("GEO-blocked!!");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String host = br.getHost();
        final String fpName = br.getRegex("<title>(.*?)(?:\\s*\\|\\s*[^<]*)?</title>").getMatch(0);
        String[] links = null;
        if (br.containsHTML("var images")) {
            if (br.containsHTML("(jpe?g|png)\\.webp")) {
                links = br.getRegex("(https?://(?:img\\." + Pattern.quote(host) + "|img-host\\.filestatic\\.xyz)/br/images/[^<>\"\\']+\\.webp)").getColumn(0);
            } else {
                links = br.getRegex("(https?://(?:img\\." + Pattern.quote(host) + "|img-host\\.filestatic\\.xyz)/br/mangas_files/[^<>\"\\']+(jpe?g|png))").getColumn(0);
            }
        } else {
            // this is JSON, DO NOT universally unescape it.
            String pages = br.getRegex("var pages\\s*=\\s*(\\[\\{[^<>]+\\}\\])\\;").getMatch(0);
            final ArrayList<Object> resource = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(pages);
            if (links == null) {
                links = new String[resource.size()];
            }
            int i = 0;
            for (final Object page : resource) {
                links[i++] = (String) JavaScriptEngineFactory.walkJson(page, "url");
            }
        }
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links) {
            /* Correct final urls */
            singleLink = singleLink.replace("/images/", "/mangas_files/").replace(".webp", "");
            singleLink = "directhttp://" + singleLink;
            final DownloadLink dl = createDownloadlink(singleLink);
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
}
