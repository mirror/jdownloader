//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class TurboBitNetFolder extends antiDDoSForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        return getAllSupportedNames();
    }

    public static String[] getAllSupportedNames() {
        /* Different Hostplugins which all extend one class - this crawler can handle all of their folder-links */
        final String[] supportedNamesTurbobit = jd.plugins.hoster.TurboBitNet.domains;
        final String[] supportedNamesHitfile = jd.plugins.hoster.HitFileNet.domains;
        final String[] supportedNamesWayupload = jd.plugins.hoster.WayuploadCom.domains;
        final String[][] supportedNamesArrays = { jd.plugins.hoster.TurboBitNet.domains, jd.plugins.hoster.HitFileNet.domains, jd.plugins.hoster.WayuploadCom.domains };
        final String[] supportedNamesAll = new String[supportedNamesTurbobit.length + supportedNamesHitfile.length + supportedNamesWayupload.length];
        int position = 0;
        for (final String[] supportedNamesOfOneHost : supportedNamesArrays) {
            for (final String supportedName : supportedNamesOfOneHost) {
                supportedNamesAll[position] = supportedName;
                position++;
            }
        }
        return supportedNamesAll;
    }

    public static String[] getAnnotationNames() {
        return new String[] { jd.plugins.hoster.TurboBitNet.domains[0] };
    }

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/download/folder/\\d+" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : getAllSupportedNames()) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = "https?://(?:www\\.)?" + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    public TurboBitNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.setAllowedResponseCodes(new int[] { 400 });
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String id = new Regex(parameter, "download/folder/(\\d+)").getMatch(0);
        if (id == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        // rows = 100 000 makes sure that we only get one page with all links
        // br.getPage("http://turbobit.net/downloadfolder/gridFile?id_folder=" + id + "&_search=false&nd=&rows=100000&page=1");
        final String host = Browser.getHost(parameter);
        getPage(String.format("http://%s/downloadfolder/gridFile?rootId=%s?currentId=%s&_search=false&nd=&rows=100000&page=1&sidx=file_type&sord=asc", host, id, id));
        if (br.containsHTML("\"records\":0,\"total\":0,\"") || br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String[] ids = br.getRegex("\\{\"id\":\"([a-z0-9]+)\"").getColumn(0);
        if (ids == null || ids.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleID : ids) {
            /* Do not add the same folder again. */
            if (!singleID.equals(id)) {
                decryptedLinks.add(createDownloadlink(String.format("https://%s/%s.html", host, singleID)));
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Turbobit_Turbobit;
    }
}