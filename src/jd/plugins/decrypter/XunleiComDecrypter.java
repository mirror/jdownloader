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
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xunlei.com" }, urls = { "http://(www\\.)?(kuai\\.xunlei\\.com/(d/([a-zA-Z]{1,2}\\-)?[a-zA-Z0-9]+|download\\?[^\"\\'<>]+|s/[\\w\\-]+)|f\\.xunlei\\.com/\\d+/file/[a-z0-9\\-]+)" }, flags = { 0 })
public class XunleiComDecrypter extends PluginForDecrypt {

    public XunleiComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.setReadTimeout(3 * 60 * 1000);
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().contains("kuai.xunlei.com/invalid")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("http://verify")) {
            logger.info("xunlei.com decrypter: found captcha...");
            final String fid = new Regex(parameter, "([A-Z]+)$").getMatch(0);
            for (int i = 0; i <= 3; i++) {
                final String captchaLink = br.getRegex("\"(http://verify\\d+\\.xunlei\\.com/image\\?t=[^<>\"]*?)\"").getMatch(0);
                if (captchaLink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final String code = getCaptchaCode(captchaLink, param);
                br.getPage("http://kuai.xunlei.com/webfilemail_interface?v_code=" + code + "&shortkey=" + fid + "&ref=&action=check_verify");
                if (!br.containsHTML("http://verify")) break;
            }
            if (br.containsHTML("http://verify")) throw new DecrypterException(DecrypterException.CAPTCHA);
            logger.info("Captcha passed!");
        }
        checks(parameter, br.getURL());
        // hoster download links
        if (parameter.matches("http://(www\\.)?(kuai\\.xunlei\\.com/(d/([a-zA-Z]{1,2}\\-)?[a-zA-Z0-9]+|download\\?[^\"\\'<>]+)|f\\.xunlei\\.com/\\d+/file/[a-z0-9\\-]+)")) {
            parseDownload(decryptedLinks, parameter, parameter);
        }
        // folders with spanning page, + subpage support
        if (parameter.matches("http://(www\\.)?kuai\\.xunlei\\.com/s/[\\w\\-]+")) {
            String uid = new Regex(parameter, "/s/(.+)").getMatch(0);
            String[] Pages = br.getRegex("<div id=\\'page_bar(\\d+)\\' class=\"page_co\"").getColumn(0);
            parsePage(decryptedLinks, parameter);
            if (Pages != null && Pages.length != 0) {
                for (String page : Pages)
                    if (!page.equals("1")) {
                        br.getPage("http://kuai.xunlei.com/s/" + uid + "?p_index=" + page);
                        checks(parameter, br.getURL());
                        parsePage(decryptedLinks, parameter);
                    }
            }
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String parameter) throws IOException, Exception {
        String[] links = br.getRegex("href=\"(https?://kuai.xunlei.com/download\\?[^\"\\'<>]+)").getColumn(0);
        if (links == null || links.length == 0) return;
        HashSet<String> filter = new HashSet<String>();
        for (String dl : links) {
            if (filter.add(dl) == false) continue;
            parseDownload(ret, parameter, dl);
        }
    }

    private void parseDownload(final ArrayList<DownloadLink> ret, final String parameter, final String dlparm) throws Exception {
        if (!br.getURL().matches(dlparm)) {
            br.getPage(dlparm);
        }
        checks(parameter, br.getURL());
        // think this old and out dated.
        String fpName = br.getRegex("<span style=\"color:gray\">下载 (.*?) 分享的文件</span>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("file_name=\"([^\"]+)").getMatch(0);
            if (fpName == null) {
                // for f.xunlei
                fpName = br.getRegex("<h3 id=\"c_counts\">[\r\n\t ]+(.+?)</h3>").getMatch(0);
                if (fpName == null) {
                    fpName = new Regex(parameter, "([A-Z0-9]+)$").getMatch(0);
                }
            }
        }
        // video links?
        if (br.getURL().matches("https?://f\\.xunlei\\.com/\\d+/file/[a-z0-9\\-]+.+")) {
            // not supported yet..
            return;
        } else {
            final String[] links = br.getRegex("\"(http://(dl\\d+\\.[a-z]+\\d+\\.sendfile\\.vip\\.xunlei\\.com|([1-2]?[0-9]{1,2}\\.){3}[1-2]?[0-9]{1,2}):\\d+/[^<>\"]+)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return;
            }
            HashSet<String> fLinks = new HashSet<String>();
            for (final String aLink : links) {
                if (aLink.trim().matches("https?://(192\\.168\\.[^/]+|127\\.0\\.0\\.1|10\\.[^/]+):\\d+/.+")) {
                    logger.info("Invalid URLs found (LAN or localhost subnets).");
                    continue;
                }
                if (fLinks.add(aLink) == false) continue;
                final DownloadLink dl = createDownloadlink(aLink);
                dl.setProperty("origin", parameter);
                dl.setAvailable(true);
                ret.add(dl);
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(ret);
        }
    }

    private void checks(String parameter, String CurrentURL) throws Exception {
        // offline & incorrect urls
        if (br.containsHTML("(>对不起，该文件夹还未开放共享哦|抱歉，您下载的链接已失效)")) {
            logger.warning("Xunlei Decrypter: Invalid URL" + parameter);
            return;
        }
        // Captchas seems to trigger after reaching some GET threshold.
        if (br.containsHTML("http://verify\\d+.xunlei.com/image\\?t=MMA&s=\\d+")) {
            for (int i = 0; i <= 5; i++) {
                String captchaIMG = br.getRegex("(http://verify\\d+.xunlei.com/image\\?t=MMA&s=\\d+)").getMatch(0);
                // SITE HAS CRAP FORM STRUCTURE, they don't close </form>'s
                // find the form we store in String and do this manually.
                String captchaForm = br.getRegex("(<form action=\"/webfilemail_interface\">.*</dl>[\r\n\t ]+<form>)").getMatch(0);
                if (captchaForm == null || captchaIMG == null) {
                    logger.warning("Xunlei Decrypter: couldn't find the captcha form or captchaIMG, Please report this issue to the JDownloader Development Team." + parameter);
                    return;
                }
                // captcha form values
                String shortkey = new Regex(captchaForm, "value=(\\'|\")([^\\'\"]+)(\\'|\") name=\"shortkey\"").getMatch(1);
                String submit = new Regex(captchaForm, "value=(\\'|\")([^\\'\"]+)(\\'|\") name=\"Submit\"").getMatch(1);
                // in browser this is null
                String ref = new Regex(captchaForm, "value=(\\'|\")([^\\'\"]+)(\\'|\") name=\"ref\"").getMatch(1);
                String action = new Regex(captchaForm, "value=(\\'|\")([^\\'\"]+)(\\'|\") name=\"action\"").getMatch(1);
                // throw error before prompting users for captcha solution
                // don't check shortkey, often its null
                if (submit == null || action == null) {
                    logger.warning("Xunlei Decrypter: couldn't find the captcha form values, Please report this issue to the JDownloader Development Team." + parameter);
                    return;
                }
                String captchaCode = getCaptchaCode(captchaIMG, null);
                br.getPage("http://kuai.xunlei.com/webfilemail_interface?v_code=" + Encoding.urlEncode(captchaCode) + "&shortkey=" + Encoding.urlEncode(shortkey) + "&ref=&action=" + Encoding.urlEncode(action) + "&Submit=" + Encoding.urlEncode(submit));
                br.getPage(CurrentURL);
                if (br.containsHTML("http://verify\\d+.xunlei.com/image\\?t=MMA&s=\\d+"))
                    continue;
                else {
                    break;
                }
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}