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
import java.util.Set;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.RandomUserAgent;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.jdownloader.controlling.PasswordUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rlslog.net" }, urls = { "http://(www\\.)?rlslog\\.net/.+/(.+/)?#comments" }) 
public class Rlslg extends PluginForDecrypt {

    public Rlslg(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static String ua = RandomUserAgent.generate();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("#comments", "");
        br.getHeaders().put("User-Agent", ua);
        br.getPage(parameter);
        String directComment = new Regex(param.toString(), "http://[\\w\\.]*?rlslog\\.net/.+/.+/#comments|/.+/#comments|/.+/.*?#(comment\\-\\d+)").getMatch(0);
        if (directComment != null) {
            String comment = br.getRegex(Pattern.compile("<li class=.*? id=.*?" + directComment + ".*?>(.*?)</li>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            final Set<String> pws = PasswordUtils.getPasswords(comment);
            String[] links = new Regex(comment, "rel=\"nofollow\">(.*?)</a>", Pattern.CASE_INSENSITIVE).getColumn(0);
            if (links != null) {
                for (String link : links) {
                    if (!new Regex(link, this.getSupportedLinks()).matches()) {
                        DownloadLink dLink = createDownloadlink(link);
                        if (pws != null && pws.size() > 0) {
                            dLink.setSourcePluginPasswordList(new ArrayList<String>(pws));
                        }
                        decryptedLinks.add(dLink);
                    }
                }
            }
        } else {
            ArrayList<String> pages = new ArrayList<String>();
            pages.add(param.toString());
            String comment_pages[] = br.getRegex("class=\\'page\\-numbers\\' href=\\'(http://(www\\.)?rlslog\\.net/.*?)\\'").getColumn(0);
            if (comment_pages != null && comment_pages.length != 0) {
                for (String page : comment_pages) {
                    if (!pages.contains(page)) {
                        pages.add(page);
                    }
                }
            }
            progress.setRange(pages.size());
            for (String page : pages) {
                // Don't enter first page as it is already entered
                if (!page.equals(param.toString())) {
                    br.getPage(page.replace("#comments", ""));
                }
                String comments[] = br.getRegex(Pattern.compile("<div class=('|\")commenttext('|\")>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getColumn(2);
                for (String comment : comments) {
                    final Set<String> pws = PasswordUtils.getPasswords(comment);
                    String[] links = new Regex(comment, "rel=\"nofollow\">(.*?)</a>", Pattern.CASE_INSENSITIVE).getColumn(0);
                    if (links != null && links.length != 0) {
                        for (String link : links) {
                            if (!new Regex(link, this.getSupportedLinks()).matches()) {
                                DownloadLink dLink = createDownloadlink(link);
                                if (pws != null && pws.size() > 0) {
                                    dLink.setSourcePluginPasswordList(new ArrayList<String>(pws));
                                }
                                decryptedLinks.add(dLink);
                            }
                        }
                    }
                }
                progress.increase(1);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}