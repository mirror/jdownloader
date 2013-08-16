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
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision: 20458 $", interfaceVersion = 2, names = { "hdd.tomsk.ru" }, urls = { "http://(www|download\\.)?hdd\\.tomsk\\.ru/desk/(?!notfound)[a-z]{8}" }, flags = { 0 })
public class HddTomskRu extends PluginForDecrypt {

    private static final String DESK_ENTER_PASSWORD = "http://hdd.tomsk.ru/?rm=desk_enter_password";
    private static final String PWTEXT              = ">Для доступа к столу необходим пароль<";

    /**
     * @author rnw
     * */
    public HddTomskRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        final SubConfiguration hosterPluginConfig = JDUtilities.getPluginForHost("hdd.tomsk.ru").getPluginConfig();
        String hddSid = hosterPluginConfig.getStringProperty(jd.plugins.hoster.HddTomskRu.HDDSID);
        if (hddSid != null) br.setCookie(jd.plugins.hoster.HddTomskRu.DOMAIN, jd.plugins.hoster.HddTomskRu.HDDSID, hddSid);
        br.getPage(parameter);

        final String deskId = new Regex(parameter, "([a-z]{8})$").getMatch(0);
        final String deskTitle = "hdd.tomsk.ru - Стол " + deskId;

        String redirectLocation = br.getRedirectLocation();

        /* No such desk */
        if (redirectLocation != null && (redirectLocation.equals(jd.plugins.hoster.HddTomskRu.NOTFOUND) || redirectLocation.equals("http://hdd.tomsk.ru/desk/notfound"))) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        /* Password protected desk */
        if (redirectLocation == null && br.containsHTML(PWTEXT)) {
            for (int i = 0; i <= 3; i++) {
                final String passCode = Plugin.getUserInput("Enter password for: " + deskTitle, param);
                br.postPage(DESK_ENTER_PASSWORD, "password=" + Encoding.urlEncode(passCode) + "&signature=" + deskId);
                if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
                if (br.containsHTML(PWTEXT)) continue;
                break;
            }
            if (br.containsHTML(PWTEXT)) throw new DecrypterException(DecrypterException.PASSWORD);
            redirectLocation = br.getRedirectLocation();
        }

        /* Accept TOS */
        if (redirectLocation != null && redirectLocation.equals(jd.plugins.hoster.HddTomskRu.TERMS)) {
            br.postPage(jd.plugins.hoster.HddTomskRu.TERMS_ACCEPT, "accept=1");
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
                final String cookie = br.getCookie(jd.plugins.hoster.HddTomskRu.DOMAIN, jd.plugins.hoster.HddTomskRu.HDDSID);
                if (cookie != null) {
                    hosterPluginConfig.setProperty(jd.plugins.hoster.HddTomskRu.HDDSID, cookie);
                    hosterPluginConfig.save();
                }
            }
        }

        final String[] results = br.getRegex("Window_DeskItem_File[^\n\r]+(\"file_mime_type\"[^\r\n]+)").getColumn(0);
        if (results == null || results.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String result : results) {
            result = result.replaceAll("\\\\/", "/");
            String uid = getRegex("file_signature", result);
            String filename = getRegex("file_original_name", result);
            String filesize = getRegex("size_in_bytes", result);
            String sha1 = getRegex("file_sha1", result);
            if (uid == null) continue;
            DownloadLink dl = createDownloadlink("http://hdd.tomsk.ru/file/" + uid);
            if (filename != null) dl.setFinalFileName(filename);
            if (filesize != null) dl.setDownloadSize(Integer.parseInt(filesize));
            if (sha1 != null) dl.setSha1Hash(sha1);
            if (filename != null) dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Possible plugin error, Please confirm in your web browser and report back to JDownloader Development Team if theirs a problem! : " + parameter);
        }

        FilePackage fp = FilePackage.getInstance();
        fp.setName(deskTitle);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String getRegex(String id, String source) {
        String reg = new Regex(source, "\"" + id + "\":\"?([^:\"]+)\"?,").getMatch(0);
        return reg;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}