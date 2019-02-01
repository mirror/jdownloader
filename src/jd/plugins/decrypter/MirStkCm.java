//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "uploadmagnet.com", "mirr.re" }, urls = { "https?://(?:www\\.)?(?:multi\\.hotshare\\.biz|uploadmagnet\\.com|pdownload\\.net|zlinx\\.me|filesuploader\\.com|multiupload\\.biz|multimirrorupload\\.com|multifilemirror\\.com)/([a-zA-Z0-9]{1,2}_)?([a-zA-Z0-9]{12})", "https?://(?:www\\.)?(?:mirr\\.re)/d/([a-zA-Z0-9]+)" })
public class MirStkCm extends antiDDoSForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        if ("uploadmagnet.com".equals(getHost())) {
            return new String[] { "multi.hotshare.biz", "uploadmagnet.com", "pdownload.net", "zlinx.me", "filesuploader.com", "multiupload.biz", "multimirrorupload.com", "multifilemirror.com", "mirr.re" };
        }
        return null;
    }

    /*
     * DEV NOTES: (mirrorshack) - provider has issues at times, and doesn't unhash stored data values before exporting them into redirects.
     * I've noticed this with mediafire links for example http://mirrorstack.com/mf_dbfzhyf2hnxm will at times return
     * http://www.mediafire.com/?HASH(0x15053b48), you can then reload a couple times and it will work in jd.. provider problem not plugin.
     * Other example links I've used seem to work fine. - Please keep code generic as possible.
     *
     * Don't use package name as these type of link protection services export a list of hoster urls of a single file. When one imports many
     * links (parts), JD loads many instances of the decrypter and each url/parameter/instance gets a separate packagename and that sucks.
     * It's best to use linkgrabbers default auto packagename sorting.
     */
    // 16/12/2012
    // mirrorstack.com = up, multiple pages deep, requiring custom r_counter, link offline errorhandling
    // uploading.to = down/sudoparked = 173.192.223.71-static.reverse.softlayer.com
    // copyload.com = down/sudoparked = 208.43.167.115-static.reverse.softlayer.com
    // onmirror.com = up, finallink are redirects on first singleLink page
    // multiupload.biz = up, multiple pages deep, with waits on last page
    // mirrorhive.com = up, finallink are redirects on first singleLink page
    // 05/07/2013
    // filesuploader.com = up, finallink are redirects on first singleLink page
    // 09/09/2013
    // pdownload.net = up, finallinks are redriects on first singleLink page
    // version 0.6
    // Tags: Multi file upload, mirror, mirrorstack, GeneralMultiuploadDecrypter
    // Single link format eg. http://sitedomain/xx_uid. xx = hoster abbreviation
    private final String regexSingleLink = "(https?://[^/]+/(?:en/)?[a-zA-Z0-9]{1,2}_(?:[a-zA-Z0-9]{12}|[a-zA-ZA-Z0-9]{16}))";
    // Normal link format eg. http://sitedomain/uid
    private final String regexNormalLink = "(https?://[^/]+/[a-zA-Z0-9]{12})";

    public MirStkCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br = new Browser();
        // Easier to set redirects on and off than to define every provider. It also creates less maintenance if provider changes things up.
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 500 });
        getPage(parameter);
        if (br.containsHTML(">(File )?Not Found</|>404 ERROR<") || br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            logger.warning("Invalid URL, either removed or never existed :" + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fileName = br.getRegex("File:</b>\\s*(.*?)\\s*(\\(|<)").getMatch(0);
        final FilePackage fp;
        if (fileName != null) {
            fp = FilePackage.getInstance();
            fp.setName(fileName);
        } else {
            fp = null;
        }
        br.setFollowRedirects(false);
        String[] singleLinks = null;
        // Add a single link parameter to String[]
        if (parameter.matches(regexSingleLink)) {
            singleLinks = new Regex(parameter, "(.+)").getColumn(0);
        }
        // Normal links, find all singleLinks
        else {
            singleLinks = br.getRegex("<a\\s+[^>]*href\\s*=\\s*('|\")" + regexSingleLink + "\\1").getColumn(1);
            if (singleLinks == null || singleLinks.length == 0) {
                singleLinks = br.getRegex(regexSingleLink).getColumn(0);
            }
            if (singleLinks == null || singleLinks.length == 0) {
                singleLinks = br.getRegex("btn-large[^<>]+href='([^']+)'.*?Click to view link").getColumn(0);
            }
        }
        if ((singleLinks == null || singleLinks.length == 0) && parameter.contains("pdownload.net/") && br.containsHTML("</b> \\| \\(")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (singleLinks == null || singleLinks.length == 0) {
            logger.warning("Couldn't find singleLinks... :" + parameter);
            return null;
        }
        // make sites with long waits return back into the script making it multi-threaded, otherwise singleLinks * results = long time.
        if (singleLinks.length > 1 && parameter.matches(".+(multiupload\\.biz)/.+")) {
            for (String singleLink : singleLinks) {
                final DownloadLink link = createDownloadlink(singleLink);
                if (fp != null) {
                    fp.add(link);
                }
                decryptedLinks.add(link);
            }
        } else {
            // Process links found. Each provider has a slightly different requirement and outcome
            for (String singleLink : singleLinks) {
                String finallink = null;
                if (!singleLink.matches(regexSingleLink)) {
                    finallink = singleLink;
                }
                if (isAbort()) {
                    break;
                }
                final Browser brc = br.cloneBrowser();
                if (finallink == null) {
                    // if parameter == singlelink, no need for another page get
                    if (!parameter.matches(regexSingleLink)) {
                        getPage(brc, singleLink);
                    }
                    finallink = brc.getRedirectLocation();
                    if (finallink == null) {
                        String referer = null;
                        String add_char = "";
                        Integer wait = 0;
                        if (parameter.matches(".+(multiupload\\.biz)/.+")) {
                            add_char = "?";
                            referer = new Regex(br.getURL(), "(https?://[^/]+)/").getMatch(0) + "/r_counter";
                            wait = 10;
                        } else {
                            referer = new Regex(br.getURL(), "(https?://[^/]+)/").getMatch(0) + "/r_counter";
                        }
                        brc.getHeaders().put("Referer", referer);
                        sleep(wait * 1000, param);
                        getPage(brc, singleLink + add_char);
                        finallink = brc.getRedirectLocation();
                        if (StringUtils.isEmpty(finallink)) {
                            finallink = brc.getRegex("name\\s*=\\s*\"shturl\"[^>]*value\\s*=\\s*\"(https?://[^>]*?)\"").getMatch(0);
                            if (StringUtils.isEmpty(finallink)) {
                                final Form button = brc.getFormBySubmitvalue("Download");
                                if (button != null) {
                                    final Browser br2 = brc.cloneBrowser();
                                    br2.submitForm(button);
                                    finallink = br2.getRedirectLocation();
                                }
                                if (StringUtils.isEmpty(finallink)) {
                                    // fail over
                                    final String[] links = HTMLParser.getHttpLinks(brc.toString(), "");
                                    for (final String link : links) {
                                        if (!Browser.getHost(link).contains(Browser.getHost(brc.getURL()))) {
                                            final DownloadLink dl = createDownloadlink(link);
                                            if (fp != null) {
                                                fp.add(dl);
                                            }
                                            decryptedLinks.add(dl);
                                            distribute(dl);
                                        }
                                    }
                                    continue;
                                }
                            }
                        }
                    }
                }
                if (!Browser.getHost(finallink).contains(Browser.getHost(brc.getURL()))) {
                    final DownloadLink dl = createDownloadlink(finallink);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    decryptedLinks.add(dl);
                    distribute(dl);
                }
            }
        }
        return decryptedLinks;
    }

    /**
     * just some code for mirrorstack.com which might be useful if mirrorstack or other sites remove singleLinks from source!
     *
     * @param parameter
     * @param singleLinks
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private void notused(String parameter, String[] singleLinks) throws Exception {
        // all links still found on main page
        singleLinks = br.getRegex("<a href='" + regexSingleLink + "'").getColumn(0);
        if (singleLinks == null || singleLinks.length == 0) {
            singleLinks = br.getRegex(regexSingleLink).getColumn(0);
        }
        if (singleLinks == null || singleLinks.length == 0) {
            // final fail over, not really required but anyway here we go.
            String fstat = br.getRegex("(http://mirrorstack\\.com/update_fstat/\\d+/)").getMatch(0);
            if (fstat != null) {
                String referer = br.getURL();
                Browser br2 = br.cloneBrowser();
                br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br2.getPage(fstat + Math.random() * 10000);
                br.getHeaders().put("X-Requested-With", null);
                br.getHeaders().put("Referer", referer);
                singleLinks = br.getRegex(regexSingleLink).getColumn(0);
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Unknown_MirrorStack;
    }
}