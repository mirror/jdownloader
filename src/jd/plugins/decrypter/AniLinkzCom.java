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
import java.util.List;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 8888 $", interfaceVersion = 2, names = { "anilinkz.com" }, urls = { "http://[\\w\\.]*?anilinkz\\.com/(?!get).+/.+" }, flags = { 0 })
public class AniLinkzCom extends PluginForDecrypt {

    private static final Pattern PATTERN_SUPPORTED_HOSTER         = Pattern.compile("(zshare\\.net|megavideo\\.com|youtube\\.com)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_UNSUPPORTED_HOSTER       = Pattern.compile("(veoh\\.com|google\\.com)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_FILE_EXTENSION = Pattern.compile("(\\.mp4|\\.flv|\\.fll)", Pattern.CASE_INSENSITIVE);

    public AniLinkzCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final Browser br2 = this.br.cloneBrowser();
        this.br.getHeaders().put("Referer", null);
        this.br.getPage(parameter);
        // set filepackage
        final String filepackage = this.br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        // get Mirrors
        final int mirrorCount = this.br.getRegex("Source \\d+").count();
        final List<String> unescape = new ArrayList<String>();
        String[] dllinks;
        String mirror = null;
        // get dllinks
        progress.setRange(mirrorCount);
        for (int i = 0; i <= mirrorCount; i++) {
            progress.increase(1);
            String escapeAll = this.br.getRegex("escapeall\\('(.*)'\\)\\)\\);").getMatch(0);
            if (escapeAll != null) {
                escapeAll = escapeAll.replaceAll("[A-Z~!@#\\$\\*\\{\\}\\[\\]\\-\\+\\.]?", "");
            } else {
                Plugin.logger.warning("Decrypter out of date for link: " + parameter);
                return null;
            }
            unescape.add(Encoding.htmlDecode(escapeAll));
            dllinks = new Regex(unescape.get(i), "(url|file)=(.*?)\"").getColumn(1);
            if ((dllinks == null) || (dllinks.length == 0)) {
                dllinks = new Regex(unescape.get(i), "src=\"(.*?)\"").getColumn(0);
            }
            if (dllinks.length > 0) {
                for (String dllink : dllinks) {
                    try {
                        if (dllink.contains("anilinkz.com/get")) {
                            br2.openGetConnection(dllink);
                            if (br2.getRedirectLocation() != null) {
                                dllink = "directhttp://" + br2.getRedirectLocation().toString();
                            } else {
                                break;
                            }
                        }
                        if (dllink.contains("embed.novamov.com")) {
                            br2.getPage(dllink);
                            dllink = br2.getRegex("flashvars.file=\"(.*?)\";").getMatch(0);
                            if (dllink == null) {
                                break;
                            }
                        }
                        if (dllink.contains("megavideo.com") && dllink.contains("v=")) {
                            final String id = dllink.substring(dllink.lastIndexOf("=") + 1);
                            dllink = "http://www.megavideo.com/v/" + id;
                            if (id == null) {
                                break;
                            }
                        }
                        if (dllink.contains("upload2.com")) {
                            br2.getPage(dllink);
                            dllink = br2.getRegex("video=(.*?)&rating").getMatch(0);
                            if (dllink == null) {
                                break;
                            }
                            dllink = "directhttp://" + dllink;
                            mirror = "upload2.com";
                        }
                        if (dllink.contains("youtube.com")) {
                            dllink = new Regex(dllink, "(http://[\\w\\.]*?youtube\\.com/v/\\w+)&").getMatch(0);
                            if (dllink != null) {
                                dllink = dllink.replace("v/", "watch?v=");
                            } else {
                                break;
                            }
                        }
                        if (dllink.contains("zshare.net")) {
                            br2.getPage(dllink);
                            dllink = br2.getRegex("\\s+<a href=\"(.*?)\".*Download Video").getMatch(0);
                            if (dllink == null) {
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        JDLogger.exception(e);
                        Plugin.logger.warning("Decrypter Exception for link: " + dllink);
                    }
                    if (mirror == null) {
                        mirror = new Regex(dllink, "http://.*?\\.(\\w+\\.\\w+)/.*?").getMatch(0);
                    }
                    if ((mirrorCount == 0) && (new Regex(mirror, AniLinkzCom.PATTERN_UNSUPPORTED_HOSTER).count() == 1)) {
                        Plugin.logger.warning(mirror + " is not supported yet! Link: " + parameter);
                        return null;
                    }
                    if ((new Regex(dllink, AniLinkzCom.PATTERN_SUPPORTED_FILE_EXTENSION).count() == 1) || (new Regex(dllink, AniLinkzCom.PATTERN_SUPPORTED_HOSTER).count() == 1)) {
                        String ext = dllink.substring(dllink.lastIndexOf("."));
                        if (ext.length() > 3) {
                            ext = ext.substring(0, 4);
                        }
                        final DownloadLink dl = this.createDownloadlink(dllink.trim());
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(filepackage.trim());
                        dl.setFilePackage(fp);
                        dl.setProperty("removeReferer", true);
                        if ((ext != null) && (new Regex(dllink, AniLinkzCom.PATTERN_SUPPORTED_HOSTER).count() == 0)) {
                            final String filename = filepackage + "_mirror_" + (i + 1) + "_" + mirror + ext;
                            dl.setFinalFileName(filename.trim());
                        }
                        mirror = null;
                        decryptedLinks.add(dl);
                    }
                }
            }
            if (mirrorCount > i) {
                this.br.getPage(parameter + (i + 2) + "/");
            }
        }
        if (decryptedLinks.size() == 0) {
            Plugin.logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }
}