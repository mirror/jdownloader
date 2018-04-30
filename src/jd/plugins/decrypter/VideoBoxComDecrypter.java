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
import java.util.LinkedHashMap;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videobox.com" }, urls = { "https?://(?:www\\.)?videobox\\.com/(?:movie\\-details\\?contentId=|.*?flashPage/)\\d+" })
public class VideoBoxComDecrypter extends PluginForDecrypt {
    public VideoBoxComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String[] qualities = { "DVD", "H264_640", "HIGH", "H264_IPOD" };
        if (!getUserLogin()) {
            logger.info("Cannot decrypt without logindata: " + parameter);
            return decryptedLinks;
        }
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("videobox.com");
        final Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        final String encodedUsername = Encoding.urlEncode(aa.getUser());
        final String sessionID = br.getCookie("https://videobox.com/", "JSESSIONID");
        final String videoID = new Regex(parameter, "(\\d+)$").getMatch(0);
        br.getPage("https://www." + this.getHost() + "/content/details/generate/" + videoID + "/content-column.json?x-user-name=" + encodedUsername + "&x-session-key=" + sessionID + "&callback=metai.buildMovieDetails");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.getRegex("\\((.+)\\);$").getMatch(0));
        ArrayList<Object> ressourcelist;
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "details/{0}");
        final String fpName = (String) entries.get("name");
        if (StringUtils.isEmpty(fpName)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /* 2018-04-30: Official download possible? / They have "Download" accounts and "Streaming" accounts! */
        final boolean canDownload = ((Boolean) entries.get("canDownload")).booleanValue();
        if (canDownload) {
            /* Download via official download URLs */
            br.getPage("/content/download/options/" + videoID + ".json?x-user-name=" + encodedUsername + "&x-session-key=" + sessionID + "&callback=metai.buildDownloadLinks");
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.getRegex("\\((.+)\\);$").getMatch(0));
            ressourcelist = (ArrayList<Object>) entries.get("content");
            for (final Object resO : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) resO;
                final String directLink = (String) entries.get("url");
                final String downloadSize = (String) entries.get("size");
                final String quality = (String) entries.get("res");
                if (StringUtils.isEmpty(directLink) || StringUtils.isEmpty(downloadSize) || StringUtils.isEmpty(quality)) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("http://videoboxdecrypted.com/decryptedscene/" + System.currentTimeMillis() + new Random().nextInt(10000));
                final String finalfilename = fpName + "_" + quality + getFileNameExtensionFromString(directLink, "");
                dl.setAvailable(true);
                dl.setDownloadSize(SizeFormatter.getSize(downloadSize));
                dl.setFinalFileName(finalfilename);
                dl.setProperty("originalurl", parameter);
                dl.setProperty("sceneid", videoID);
                dl.setProperty("directlink", directLink);
                dl.setProperty("quality", quality);
                dl.setProperty("plainfilesize", downloadSize);
                dl.setProperty("finalname", finalfilename);
                decryptedLinks.add(dl);
            }
        } else {
            ressourcelist = (ArrayList<Object>) entries.get("scenes");
            if (ressourcelist != null && ressourcelist.size() > 0) {
                int currentSceneNumber = 1;
                for (final Object sceneO : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) sceneO;
                    final String sceneName = (String) entries.get("name");
                    final String sceneID = (String) entries.get("id");
                    if (sceneName == null || sceneID == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    br.getPage("/content/download/options/" + sceneID + ".json?x-user-name=" + encodedUsername + "&x-session-key=" + sessionID + "&callback=metai.buildDownloadLinks");
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.getRegex("\\((.+)\\);$").getMatch(0));
                    ArrayList<Object> resolutions = (ArrayList<Object>) entries.get("content");
                    for (final Object resO : resolutions) {
                        entries = (LinkedHashMap<String, Object>) resO;
                        final String directLink = (String) entries.get("url");
                        final String downloadSize = (String) entries.get("size");
                        final String quality = (String) entries.get("res");
                        if (StringUtils.isEmpty(directLink) || StringUtils.isEmpty(downloadSize) || StringUtils.isEmpty(quality)) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        final DownloadLink dl = createDownloadlink("http://videoboxdecrypted.com/decryptedscene/" + System.currentTimeMillis() + new Random().nextInt(10000));
                        final String finalfilename = fpName + "_" + quality + getFileNameExtensionFromString(directLink, "");
                        dl.setAvailable(true);
                        dl.setDownloadSize(SizeFormatter.getSize(downloadSize));
                        dl.setFinalFileName(finalfilename);
                        dl.setProperty("originalurl", parameter);
                        dl.setProperty("sceneid", videoID);
                        dl.setProperty("directlink", directLink);
                        dl.setProperty("quality", quality);
                        dl.setProperty("plainfilesize", downloadSize);
                        dl.setProperty("finalname", finalfilename);
                        decryptedLinks.add(dl);
                    }
                    currentSceneNumber++;
                }
            } else {
                /* Download stream */
                br.getPage("/content/download/url/" + videoID + ".json?x-user-name=" + encodedUsername + "&x-session-key=&callback=metai.loadHtml5Video");
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.getRegex("\\((.+)\\);$").getMatch(0));
                String urlHD = (String) entries.get("urlHD");
                if (StringUtils.isEmpty(urlHD)) {
                    return null;
                }
                if (urlHD.startsWith("//")) {
                    urlHD = "https:" + urlHD;
                }
                final String quality = "720p";
                final DownloadLink dl = createDownloadlink("http://videoboxdecrypted.com/decryptedscene/" + System.currentTimeMillis() + new Random().nextInt(10000));
                final String finalfilename = fpName + "_" + quality + ".mp4";
                dl.setAvailable(true);
                dl.setFinalFileName(finalfilename);
                dl.setProperty("originalurl", parameter);
                dl.setProperty("sceneid", videoID);
                dl.setProperty("directlink", urlHD);
                dl.setProperty("quality", quality);
                dl.setProperty("finalname", finalfilename);
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private boolean getUserLogin() throws Exception {
        final PluginForHost hosterPlugin = JDUtilities.getPluginForHost("videobox.com");
        Account aa = AccountController.getInstance().getValidAccount(hosterPlugin);
        if (aa == null) {
            String username = UserIO.getInstance().requestInputDialog("Enter Loginname for " + this.getHost() + " :");
            if (username == null) {
                return false;
            }
            String password = UserIO.getInstance().requestInputDialog("Enter password for " + this.getHost() + " :");
            if (password == null) {
                return false;
            }
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
