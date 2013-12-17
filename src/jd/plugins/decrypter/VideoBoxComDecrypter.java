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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videobox.com" }, urls = { "http://(www\\.)?videobox\\.com/(movie\\-details\\?contentId=|flashPage/)\\d+" }, flags = { 0 })
public class VideoBoxComDecrypter extends PluginForDecrypt {

    public VideoBoxComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String[] qualities = { "DVD", "H264_640", "HIGH", "H264_IPOD" };
        if (!getUserLogin()) {
            logger.info("Cannot decrypt without logindata: " + parameter);
            return decryptedLinks;
        }
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("videobox.com");
        final Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        final String encodedUsername = Encoding.urlEncode(aa.getUser());
        br.getPage(parameter);
        final String sessionID = br.getCookie("http://videobox.com/", "JSESSIONID");
        br.getPage("http://www.videobox.com/content/details/generate/" + new Regex(parameter, "(\\d+)$").getMatch(0) + "/content-column.json?x-user-name=" + encodedUsername + "&x-session-key=" + sessionID + "&callback=metai.buildMovieDetails");
        final String fpName = getJson("name", br.toString());
        if (fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (parameter.contains("videobox.com/flashPage/")) {
            final String linkID = new Regex(parameter, "(\\d+)$").getMatch(0);
            br.getPage("http://www.videobox.com/content/download/options/" + linkID + ".json?x-user-name=" + encodedUsername + "&x-session-key=" + sessionID + "&callback=metai.buildDownloadLinks");
            for (final String quality : qualities) {
                final String qualityInfo = br.getRegex("(\"res\" : \"" + quality + "\".*?)\\}").getMatch(0);
                if (qualityInfo != null) {
                    String directLink = getJson("url", qualityInfo);
                    final String downloadSize = getJson("size", qualityInfo);
                    if (directLink == null || downloadSize == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    directLink = directLink.replace("\\", "");
                    final DownloadLink dl = createDownloadlink("http://videoboxdecrypted.com/decryptedscene/" + System.currentTimeMillis() + new Random().nextInt(10000));
                    final String finalfilename = fpName + "_" + quality + directLink.substring(directLink.lastIndexOf("."));
                    dl.setAvailable(true);
                    dl.setDownloadSize(SizeFormatter.getSize(downloadSize));
                    dl.setFinalFileName(finalfilename);
                    dl.setProperty("sceneid", linkID);
                    dl.setProperty("directlink", directLink);
                    dl.setProperty("quality", quality);
                    dl.setProperty("plainfilesize", downloadSize);
                    dl.setProperty("finalname", finalfilename);
                    decryptedLinks.add(dl);
                }
            }
        } else {
            final String sceneText = br.getRegex("\"scenes\" : \\[(.*?)\\],[\t\n\r ]+\"image_0\"").getMatch(0);
            if (sceneText == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String[] scenes = new Regex(sceneText, "(\"id\" : \\d+,.*?\"premiumName\" : null)").getColumn(0);
            if (scenes == null || scenes.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            int currentSceneNumber = 1;
            for (final String scene : scenes) {
                final String sceneName = getJson("name", scene);
                final String sceneID = getJson("id", scene);
                if (sceneName == null || sceneID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                br.getPage("http://www.videobox.com/content/download/options/" + sceneID + ".json?x-user-name=" + encodedUsername + "&x-session-key=" + sessionID + "&callback=metai.buildDownloadLinks");
                for (final String quality : qualities) {
                    final String qualityInfo = br.getRegex("(\"res\" : \"" + quality + "\".*?)\\}").getMatch(0);
                    if (qualityInfo != null) {
                        String directLink = getJson("url", qualityInfo);
                        final String downloadSize = getJson("size", qualityInfo);
                        if (directLink == null || downloadSize == null) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        directLink = directLink.replace("\\", "");
                        final DownloadLink dl = createDownloadlink("http://videoboxdecrypted.com/decryptedscene/" + System.currentTimeMillis() + new Random().nextInt(10000));
                        final String finalfilename = fpName + "_scene_" + currentSceneNumber + "_" + quality + directLink.substring(directLink.lastIndexOf("."));
                        dl.setAvailable(true);
                        dl.setDownloadSize(SizeFormatter.getSize(downloadSize));
                        dl.setFinalFileName(finalfilename);
                        dl.setProperty("sceneid", sceneID);
                        dl.setProperty("directlink", directLink);
                        dl.setProperty("quality", quality);
                        dl.setProperty("plainfilesize", downloadSize);
                        dl.setProperty("finalname", finalfilename);
                        decryptedLinks.add(dl);
                    }
                }
                currentSceneNumber++;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\"([ ]+)?:([ ]+)?(\\d+)").getMatch(2);
        if (result == null) result = new Regex(source, "\"" + parameter + "\"([ ]+)?:([ ]+)?\"([^<>\"]*?)\"").getMatch(2);
        return result;
    }

    private boolean getUserLogin() throws Exception {
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("videobox.com");
        Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        if (aa == null) {
            String username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + this.getHost() + " :");
            if (username == null) return false;
            String password = UserIO.getInstance().requestInputDialog("Enter password for " + this.getHost() + " :");
            if (password == null) return false;
            aa = new Account(username, password);
        }
        try {
            ((jd.plugins.hoster.VideoBoxCom) hosterPlugin).login(aa, false, this.br);
        } catch (final PluginException e) {

            aa.setValid(false);
            return false;
        }
        // Account is valid, let's just add it
        AccountController.getInstance().addAccount(hosterPlugin, aa);
        return true;
    }
}
