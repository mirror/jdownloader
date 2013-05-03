//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafire.com" }, urls = { "https?://(?!download|blog)(\\w+\\.)?(mediafire\\.com|mfi\\.re)/(?!select_account_type\\.php|reseller|policies|tell_us_what_you_think\\.php|about\\.php|lost_password\\.php|blank\\.html|js/|common_questions/|software/|error\\.php|favicon|acceptable_use_policy\\.php|privacy_policy\\.php|terms_of_service\\.php)(imageview|i/\\?|\\\\?sharekey=|view/\\?|\\?|(?!download|file|\\?JDOWNLOADER|imgbnc\\.php))[a-z0-9,]+" }, flags = { 0 })
public class MdfrFldr extends PluginForDecrypt {

    public MdfrFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String              SESSIONTOKEN  = null;
    /* keep updated with hoster */
    private final String        APIKEY        = "czQ1cDd5NWE3OTl2ZGNsZmpkd3Q1eXZhNHcxdzE4c2Zlbmt2djdudw==";
    private final String        APPLICATIONID = "27112";
    private String              ERRORCODE     = null;
    private static final String OFFLINE       = ">Unknown or invalid FolderKey<";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("mfi.re/", "mediafire.com/").trim();
        if (parameter.matches("http://(\\w+\\.)?mediafire\\.com/view/\\?.+")) parameter = parameter.replace("/view", "");
        if (parameter.endsWith("mediafire.com") || parameter.endsWith("mediafire.com/")) return decryptedLinks;
        parameter = parameter.replaceAll("(&.+)", "").replaceAll("(#.+)", "");
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.75 Safari/535.7");
        if (parameter.matches("http://download\\d+\\.mediafire.+")) {
            /* direct download */
            String ID = new Regex(parameter, "\\.com/\\?(.+)").getMatch(0);
            if (ID == null) ID = new Regex(parameter, "\\.com/.*?/(.*?)/").getMatch(0);
            if (ID != null) {
                DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
        } else if (parameter.contains("imageview.php")) {
            String ID = new Regex(parameter, "\\.com/.*?quickkey=(.+)").getMatch(0);
            if (ID != null) {
                final DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            return null;
        } else if (parameter.contains("/i/?")) {
            String ID = new Regex(parameter, "\\.com/i/\\?(.+)").getMatch(0);
            if (ID != null) {
                final DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        } else if (parameter.contains(",")) {
            // Multiple files in one link
            final String linksText = new Regex(parameter, "mediafire\\.com/\\?(.+)").getMatch(0);
            if (linksText == null) {
                logger.warning("Unhandled case for link: " + parameter);
                return null;
            }
            final String[] linkIDs = linksText.split(",");
            if (linkIDs == null || linkIDs.length == 0) {
                logger.warning("Unhandled case for link: " + parameter);
                return null;
            }
            for (final String ID : linkIDs) {
                decryptedLinks.add(createDownloadlink("http://www.mediafire.com/download.php?" + ID));
            }
        } else {
            br.setFollowRedirects(false);
            // Private link? Login needed!
            if (getUserLogin()) {
                logger.info("Decrypting with logindata...");
            } else {
                logger.info("Decrypting without logindata...");
            }
            // Check if we have a single link or multiple folders/files
            final String folderKey = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
            apiRequest(this.br, "http://www.mediafire.com/api/file/get_info.php", "?quick_key=" + folderKey);
            boolean offline = false;
            if ("110".equals(this.ERRORCODE)) {
                try {
                    apiRequest(this.br, "http://www.mediafire.com/api/folder/get_content.php?folder_key=", folderKey + "&content_type=folders");
                } catch (final BrowserException e) {
                    offline = true;
                }
                // Offline error or browser exception --> Offline
                if ("112".equals(this.ERRORCODE) || offline) {
                    final DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + folderKey);
                    link.setProperty("offline", true);
                    link.setName(folderKey);
                    decryptedLinks.add(link);
                    return decryptedLinks;
                }
                final String[] subFolders = br.getRegex("<folderkey>([a-z0-9]+)</folderkey>").getColumn(0);
                apiRequest(this.br, "http://www.mediafire.com/api/folder/get_info.php", "?folder_key=" + folderKey);
                String fpName = getXML("name", br.toString());
                if (fpName == null) fpName = folderKey;
                // Decrypt max 100 * 100 = 10 000 links
                for (int i = 1; i <= 100; i++) {
                    apiRequest(this.br, "http://www.mediafire.com/api/folder/get_content.php", "?folder_key=" + folderKey + "&content_type=files&chunk=" + i);
                    final String[] files = br.getRegex("<file>(.*?)</file>").getColumn(0);
                    for (final String fileInfo : files) {
                        final DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + getXML("quickkey", fileInfo));
                        link.setDownloadSize(SizeFormatter.getSize(getXML("size", fileInfo) + "b"));
                        link.setName(getXML("filename", fileInfo));
                        if ("private".equals(getXML("privacy", br.toString()))) {
                            link.setProperty("privatefile", true);
                        }
                        link.setAvailable(true);
                        decryptedLinks.add(link);
                    }
                    if (files == null || files.length < 100) break;
                }
                if ((subFolders == null || subFolders.length == 0) && (decryptedLinks == null || decryptedLinks.size() == 0)) {
                    // Probably private folder which can only be decrypted with
                    // an
                    // active account
                    if ("114".equals(ERRORCODE)) {
                        final DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + new Regex(parameter, "([a-z0-9]+)$").getMatch(0));
                        link.setProperty("privatefolder", true);
                        link.setName(folderKey);
                        link.setAvailable(true);
                        decryptedLinks.add(link);
                        return decryptedLinks;
                    }
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (subFolders != null && subFolders.length != 0) {
                    for (final String folderID : subFolders) {
                        final DownloadLink link = createDownloadlink("http://www.mediafire.com/?" + folderID);
                        decryptedLinks.add(link);
                    }
                }
                if (fpName != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlDecode(fpName.trim()));
                    fp.addLinks(decryptedLinks);
                }

            } else {
                final DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + folderKey);
                link.setName(folderKey);
                decryptedLinks.add(link);
            }
        }
        return decryptedLinks;
    }

    private boolean getUserLogin() throws Exception {
        /*
         * we have to load the plugins first! we must not reference a plugin class without loading it before
         */
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("mediafire.com");
        final Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        if (aa != null) {
            // Try to re-use session token as long as possible (it's valid for
            // 10 minutes)
            final String savedusername = this.getPluginConfig().getStringProperty("username");
            final String savedpassword = this.getPluginConfig().getStringProperty("password");
            final String sessiontokenCreateDateObject = this.getPluginConfig().getStringProperty("sessiontokencreated2");
            long sessiontokenCreateDate = -1;
            if (sessiontokenCreateDateObject != null && sessiontokenCreateDateObject.length() > 0) {
                sessiontokenCreateDate = Long.parseLong(sessiontokenCreateDateObject);
            }
            if ((savedusername != null && savedusername.matches(aa.getUser())) && (savedpassword != null && savedpassword.matches(aa.getPass())) && System.currentTimeMillis() - sessiontokenCreateDate < 600000) {
                SESSIONTOKEN = this.getPluginConfig().getStringProperty("sessiontoken");
            } else {
                // Get token for user account
                apiRequest(br, "https://www.mediafire.com/api/user/get_session_token.php", "?email=" + Encoding.urlEncode(aa.getUser()) + "&password=" + Encoding.urlEncode(aa.getPass()) + "&application_id=" + APPLICATIONID + "&signature=" + JDHash.getSHA1(aa.getUser() + aa.getPass() + APPLICATIONID + Encoding.Base64Decode(APIKEY)) + "&version=1");
                SESSIONTOKEN = getXML("session_token", br.toString());
                this.getPluginConfig().setProperty("username", aa.getUser());
                this.getPluginConfig().setProperty("password", aa.getPass());
                this.getPluginConfig().setProperty("sessiontoken", SESSIONTOKEN);
                this.getPluginConfig().setProperty("sessiontokencreated2", "" + System.currentTimeMillis());
                this.getPluginConfig().save();
            }
        }
        return false;
    }

    private void apiRequest(final Browser br, final String url, final String data) throws IOException {
        if (SESSIONTOKEN == null)
            br.getPage(url + data);
        else
            br.getPage(url + data + "&session_token=" + SESSIONTOKEN);
        ERRORCODE = getXML("error", br.toString());
    }

    private String getXML(final String parameter, final String source) {
        return new Regex(source, "<" + parameter + ">([^<>\"]*?)</" + parameter + ">").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}