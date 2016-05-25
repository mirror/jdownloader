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

import jd.PluginWrapper;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vmall.com" }, urls = { "https?://(?:www\\.)?dl\\.(?:dbank|vmall)\\.com/[a-z0-9]+" }, flags = { 0 })
public class VmallComFolder extends PluginForDecrypt {

    public VmallComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String parameter = null;
    private String passCode  = null;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        parameter = param.toString().replace("dbank.com/", "vmall.com/");
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("vmall.com/linknotexist.html") || br.getURL().contains("vmall.com/netdisk/search.html") || br.containsHTML("(>抱歉，此外链不存在。|1、你输入的地址错误；<br/>|2、外链中含非法内容；<br />|3、创建外链的文件还没有上传到服务器，请稍后再试。<br /><br />)")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.getURL().contains("authorize?")) {
            /* Account needed */
            logger.info("Account needed to add this url: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.getURL().contains("dl.vmall.com/m_forbidsave.html")) {
            // Link can only be accessed if a specified Referer is set
            final String linkID = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("Accept", "application/json, text/javascript, */*");
            br.postPage("http://dl.vmall.com/app/encry_resource.php", "action=getAccessWhite&id=" + linkID);
            final String whiteListAsText = br.getRegex("\"whitelist\":\\[(.*?)\\]").getMatch(0);
            if (whiteListAsText == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String[] whitelistedDomains = whiteListAsText.split(",");
            if (whitelistedDomains == null || whitelistedDomains.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final int pickRandom = new Random().nextInt(whitelistedDomains.length - 1);
            String randomWhitelistedDomain = whitelistedDomains[pickRandom].replace("\"", "");
            if (!randomWhitelistedDomain.startsWith("http://")) {
                randomWhitelistedDomain = "http://" + randomWhitelistedDomain;
            }
            br.getHeaders().put("Referer", randomWhitelistedDomain);
            br.getPage(parameter);
        }
        /* Password protected links */
        if (br.getURL().contains("/m_accessPassword.html")) {
            String id = new Regex(br.getURL(), "id=(\\w+)$").getMatch(0);
            id = id == null ? parameter.substring(parameter.lastIndexOf("/") + 1) : id;

            for (int i = 0; i < 3; i++) {
                passCode = Plugin.getUserInput(null, param);
                br.postPage("http://dl.vmall.com/app/encry_resource.php", "id=" + id + "&context=%7B%22pwd%22%3A%22" + passCode + "%22%7D&action=verify");
                if (br.getRegex("\"retcode\":\"0000\"").matches()) {
                    break;
                }
            }
            if (!br.getRegex("\"retcode\":\"0000\"").matches()) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            br.getPage(parameter);
        }

        String fpName = null;

        String json = br.getRegex("var globallinkdata = (\\{[^<]+\\});").getMatch(0);
        if (json == null) {
            json = br.getRegex("var globallinkdata = (\\{.*?\\});").getMatch(0);
        }
        if (json == null) {
            return null;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(json);
        entries = (LinkedHashMap<String, Object>) entries.get("data");
        entries = (LinkedHashMap<String, Object>) entries.get("resource");
        fpName = (String) entries.get("title");
        if (fpName == null) {
            fpName = parameter;
        }
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("files");
        for (final Object o : ressourcelist) {
            final LinkedHashMap<String, Object> finfomap = (LinkedHashMap<String, Object>) o;
            final String type = (String) finfomap.get("type");
            if (type.equals("File")) {
                decryptedLinks.add(crawlFile(o));
            } else {
                final ArrayList<Object> ressourcelist_subfolder = (ArrayList) finfomap.get("childList");
                for (final Object filesub : ressourcelist_subfolder) {
                    decryptedLinks.add(crawlFile(filesub));
                }
            }
        }
        String links = new Regex(json, "\"files\":\\[(.*?)\\]\\}").getMatch(0);
        if (links == null) {
            links = new Regex(json, "\"files\":\\[(.*?)\\],").getMatch(0);
        }

        if (links == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private DownloadLink crawlFile(final Object o) {
        final LinkedHashMap<String, Object> finfomap = (LinkedHashMap<String, Object>) o;
        final String filename = (String) finfomap.get("name");
        final long fid = getLongValue(finfomap.get("id"));
        final DownloadLink dl = createDownloadlink("http://vmalldecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
        final long filesize = getLongValue(finfomap.get("size"));
        if (passCode != null) {
            dl.setProperty("password", passCode);
        }
        dl.setProperty("mainlink", parameter);
        dl.setProperty("id", fid);
        dl.setContentUrl(parameter);
        dl.setDownloadSize(filesize);
        dl.setName(filename);
        dl.setAvailable(true);
        return dl;
    }

    private long getLongValue(final Object o) {
        long lo = -1;
        if (o instanceof Long) {
            lo = ((Long) o).longValue();
        } else {
            lo = ((Integer) o).intValue();
        }
        return lo;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}