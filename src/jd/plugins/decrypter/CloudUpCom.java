//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;

/**
 * Collections!
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cloudup.com" }, urls = { "https://(www\\.)?cloudup\\.com/c[a-zA-Z0-9_\\-]{10}" })
public class CloudUpCom extends PluginForDecrypt {

    private String  csrfToken      = null;
    private String  mydb_socket_id = null;
    private Browser ajax           = null;

    private void ajaxGetPage(final String page) throws IOException {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        if (mydb_socket_id != null) {
            ajax.getHeaders().put("X-MyDB-SocketId", mydb_socket_id);
        }
        if (csrfToken != null) {
            ajax.getHeaders().put("X-CSRF-Token", csrfToken);
        }
        ajax.getPage(page);
    }

    public CloudUpCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        // get the json url coded info
        final String preloader = br.getRegex("JSON\\.parse\\(decodeURIComponent\\('(.*?)'\\)\\)").getMatch(0);
        if (preloader == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        // we need some session info here, these are not actually verified....
        mydb_socket_id = br.getRegex("mydb_socket_id\\s*=\\s*('|\")(.*?)\\1").getMatch(1);
        csrfToken = br.getRegex("csrfToken\\s*=\\s*('|\")(.*?)\\1").getMatch(1);

        final String de_loaded = Encoding.urlDecode(preloader, false);
        final String[] ids = PluginJSonUtils.getJsonResultsFromArray(PluginJSonUtils.getJsonArray(de_loaded, "items"));
        String fpName = PluginJSonUtils.getJsonValue(de_loaded, "title");
        if (ids != null) {
            if (StringUtils.startsWithCaseInsensitive(fpName, "Video Stream - ")) {
                // these are annoying as they stream from youtube etc.
                for (final String id : ids) {
                    Browser br = this.br.cloneBrowser();
                    br.getPage("/" + id);
                    // ("/files/" + id + "?mydb=1");
                    // do they host this themselves?
                    final String pl = br.getRegex("JSON\\.parse\\(decodeURIComponent\\('(.*?)'\\)\\)").getMatch(0);
                    final String dl = Encoding.urlDecode(pl, false);
                    // youtube, vimeo send back to proper plugins
                    final String iframeJson = PluginJSonUtils.getJsonValue(dl, "oembed_html");
                    String iframe = iframeJson == null ? null : new Regex(iframeJson, "<iframe\\s+[^>]*\\s*src=(\"|')(.*?)\\1").getMatch(1);
                    if (iframe != null) {
                        decryptedLinks.add(createDownloadlink(iframe));
                    } else {
                        // send back into the hoster plugin
                        decryptedLinks.add(createDownloadlink("https://cloudup.com/" + id));
                    }
                }
            } else {
                // return indexes!
                for (final String id : ids) {
                    decryptedLinks.add(createDownloadlink("https://cloudup.com/" + id));
                }
            }
        }
        if (decryptedLinks.isEmpty()) {
            logger.warning("'decrptedLinks' isEmpty!, Please report this to JDownloader Development Team : " + parameter);
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        if (fpName != null) {
            fpName = fpName.replaceAll("share your \\w+ with family and friends|share clips and home movies", "") + new Regex(parameter, "/([^/]+)$").getMatch(0);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}