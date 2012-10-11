//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.hoster.MediafireCom;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafire.com" }, urls = { "http://(?!download|blog)(\\w+\\.)?(mediafire\\.com|mfi\\.re)/(?!select_account_type\\.php|reseller|policies|tell_us_what_you_think\\.php|about\\.php|lost_password\\.php|blank\\.html|js/|common_questions/|software/|error\\.php|favicon|acceptable_use_policy\\.php|privacy_policy\\.php|terms_of_service\\.php)(imageview|i/\\?|\\\\?sharekey=|view/\\?|(?!download|file|\\?JDOWNLOADER|imgbnc\\.php)).+" }, flags = { 0 })
public class MdfrFldr extends PluginForDecrypt {

    public MdfrFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("mfi.re/", "mediafire.com/").trim();
        if (parameter.matches("http://(\\w+\\.)?mediafire\\.com/view/\\?.+")) parameter = parameter.replace("/view", "");
        if (parameter.endsWith("mediafire.com") || parameter.endsWith("mediafire.com/")) return decryptedLinks;
        parameter = parameter.replaceAll("(&.+)", "").replaceAll("(#.+)", "");
        String fpName = null;
        this.setBrowserExclusive();
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
        }
        if (parameter.contains("imageview.php")) {
            String ID = new Regex(parameter, "\\.com/.*?quickkey=(.+)").getMatch(0);
            if (ID != null) {
                DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            return null;
        }
        if (parameter.contains("/i/?")) {
            String ID = new Regex(parameter, "\\.com/i/\\?(.+)").getMatch(0);
            if (ID != null) {
                DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            return null;
        }
        br.setFollowRedirects(false);
        br.getPage(parameter);
        // Private link? Login needed!
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("error.php?errno=999")) {
            if (!getUserLogin()) {
                logger.info("Wrong logindata entered, stopping...");
                return decryptedLinks;
            }
            br.getPage(parameter);
        }
        if (br.getRedirectLocation() != null) {
            /* check for direct download stuff */
            String red = br.getRedirectLocation();
            if (red.matches("http://download\\d+\\.mediafire.+")) {
                /* direct download */
                String ID = new Regex(parameter, "\\.com/\\?(.+)").getMatch(0);
                if (ID == null) ID = new Regex(parameter, "\\.com/.*?/(.*?)/").getMatch(0);
                if (ID != null) {
                    DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                    decryptedLinks.add(link);
                    return decryptedLinks;
                }
            } else {
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(red);
                    if (con.isContentDisposition()) {
                        String ID = new Regex(red, "//.*?/.*?/(.*?)/").getMatch(0);
                        if (ID != null) {
                            DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                            decryptedLinks.add(link);
                            return decryptedLinks;
                        }
                    }
                    br.followConnection();
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        if (br.containsHTML(">This page cannot be found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        Thread.sleep(500);
        String reqlink = br.getRegex("(This is a shared Folder)").getMatch(0);
        if (reqlink == null) {
            String ID = new Regex(parameter, "\\.com/\\?(.+)").getMatch(0);
            if (ID == null) ID = new Regex(parameter, "\\.com/.*?/(.*?)/").getMatch(0);
            if (ID != null) {
                DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + ID);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            return null;
        }
        String ID = new Regex(parameter, "\\.com/\\?([a-zA-Z0-9]+)").getMatch(0);
        if (ID == null) ID = new Regex(parameter, "\\.com/.*?/([a-zA-Z0-9]*?)/").getMatch(0);
        if (ID == null) {
            ID = br.getRegex("var afI= '(.*?)'").getMatch(0);
        }
        if (ID != null) {
            br.getPage("http://www.mediafire.com/api/folder/get_info.php?r=nuul&recursive=yes&folder_key=" + ID + "&response_format=json&version=2");
            if (br.containsHTML("\"message\":\"Unknown or invalid FolderKey\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String fileCounter = br.getRegex("file_count\":\"(\\d+)").getMatch(0);
            fpName = br.getRegex("name\":\"([^\"]+)").getMatch(0);
            HashSet<String> avoidDuplicates = new HashSet<String>();
            /* folder */
            // br.getPage("http://www.mediafire.com/api/folder/get_content.php?r=nuul&content_type=folders&order_by=name&order_direction=asc&version=2.6&folder_key="
            // + ID + "&response_format=json");
            int loop = 1;
            while (true) {
                if (loop > 1) {
                    br.getPage("http://www.mediafire.com/api/folder/get_content.php?r=null&content_type=files&order_by=name&order_direction=asc&chunk=" + loop + "&version=2.6&folder_key=" + ID + "&response_format=json");
                } else {
                    br.getPage("http://www.mediafire.com/api/folder/get_content.php?r=null&content_type=files&order_by=name&order_direction=asc&version=2.6&folder_key=" + ID + "&response_format=json");
                }
                final String links[][] = br.getRegex("quickkey\":\"(.*?)\",\"filename\":\"(.*?)\".*?\"size\":\"(\\d+)").getMatches();
                boolean freshAdded = false;
                for (String[] element : links) {
                    if (!element[2].equalsIgnoreCase("0") && avoidDuplicates.add(element[0]) == true) {
                        DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + element[0]);
                        link.setName(unescape(element[1]));
                        link.setDownloadSize(Long.parseLong(element[2]));
                        link.setProperty("origin", "decrypter");
                        link.setAvailable(true);
                        decryptedLinks.add(link);
                        freshAdded = true;

                    }
                }
                if (freshAdded == false) {
                    break;
                } else {
                    loop++;
                }
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private boolean getUserLogin() throws Exception {
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("mediafire.com");
        Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        if (aa == null) {
            final String username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + this.getHost() + " :");
            if (username == null) throw new DecrypterException(JDL.L("plugins.decrypter.mdfrfldr.nousername", "Username not entered!"));
            final String password = UserIO.getInstance().requestInputDialog("Enter password for " + this.getHost() + " :");
            if (password == null) throw new DecrypterException(JDL.L("plugins.decrypter.mdfrfldr.nopassword", "Password not entered!"));
            aa = new Account(username, password);
        }
        // Get a token which we can then use to get links out of (private)
        // folders
        // http://developers.mediafire.com/index.php/REST_API
        // br.getPage("https://www.mediafire.com/api/user/get_session_token.php?email="
        // + Encoding.urlEncode(aa.getUser()) + "&password=" +
        // Encoding.urlEncode(aa.getPass()) + "&application_id=1&signature=" +
        // JDHash.getSHA1(aa.getUser() + aa.getPass() + "application ID" +
        // "apikey") + "&version=1");
        try {
            ((MediafireCom) hosterPlugin).login(br, aa, true);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(hosterPlugin, aa);
        return true;
    }

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */

        final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
        if (plugin == null) throw new IllegalStateException("youtube plugin not found!");

        return jd.plugins.hoster.Youtube.unescape(s);
    }
}
