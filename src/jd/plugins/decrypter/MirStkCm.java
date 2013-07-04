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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mirrorstack.com" }, urls = { "https?://(www\\.)?(filesuploader\\.com|multishared\\.com|onmirror\\.com|multiupload\\.biz|lastbox\\.net|mirrorhive\\.com|mirrorstack\\.com)/([a-z0-9]{12}|[a-z0-9]{1,2}_[a-z0-9]{12})" }, flags = { 0 })
public class MirStkCm extends PluginForDecrypt {

    /*
     * TODO many sites are using this type of script. Rename this plugin into general/template type plugin naming scheme (find the name of
     * the script and rename). Do this after next major update, when we can delete plugins again.
     */

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
    // mirrorstack.com = up, multiple pages deep, requiring custom r_counter
    // uploading.to = down/sudoparked =
    // 173.192.223.71-static.reverse.softlayer.com
    // copyload.com = down/sudoparked =
    // 208.43.167.115-static.reverse.softlayer.com
    // multishared.com = up, custom fields for singleLinks && finallinks
    // onmirror.com = up, finallink are redirects on first singleLink page
    // multiupload.biz = up, multiple pages deep, with waits on last page
    // lastbox.net = up, finallink are redirects on first singleLink page
    // mirrorhive.com = up, finallink are redirects on first singleLink page
    
    //05/07/2013
    // filesuploader.com = up, finallink are redirects on first singleLink page
    
    // version 0.6

    // Single link format eg. http://sitedomain/xx_uid. xx = hoster abbreviation
    private static final String regexSingleLink = "(https?://[^/]+/[a-z0-9]{2}_[a-z0-9]{12})";
    // Normal link format eg. http://sitedomain/uid
    private static final String regexNormalLink = "(https?://[^/]+/[a-z0-9]{12})";

    public MirStkCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // Easier to set redirects on and off than to define every provider. It
        // also creates less maintenance if provider changes things up.
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">(File )?Not Found</")) {
            logger.warning("Invalid URL, either removed or never existed :" + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        String[] singleLinks = null;
        // multishared have normal hoster links outside SingleLink'.
        if (parameter.matches(".+multishared.com/[a-z0-9]{12}")) {
            singleLinks = br.getRegex("id=\\'stat\\d+_\\d+\\'><a href=\\'(http[^\\']+)").getColumn(0);
        }
        // Add a single link parameter to String[]
        else if (parameter.matches(regexSingleLink)) {
            singleLinks = new Regex(parameter, "(.+)").getColumn(0);
        }
        // Normal links, find all singleLinks
        else if (parameter.matches(regexNormalLink) || parameter.matches(".*mirrorstack\\.com/[a-z]_[a-z0-9]{12}")) {
            singleLinks = br.getRegex("<a href=\\'" + regexSingleLink + "\\'").getColumn(0);
            if (singleLinks == null || singleLinks.length == 0) {
                singleLinks = br.getRegex(regexSingleLink).getColumn(0);
            }
        }
        if (singleLinks == null || singleLinks.length == 0) {
            logger.warning("Couldn't find singleLinks... :" + parameter);
            return null;
        }
        // make sites with long waits return back into the script making it
        // multi-threaded, otherwise singleLinks * results = long time.
        if (singleLinks.length > 1 && parameter.matches(".+(multiupload\\.biz)/.+")) {
            for (String singleLink : singleLinks) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        } else {
            // Process links found. Each provider has a slightly different
            // requirement and outcome
            for (String singleLink : singleLinks) {
                String finallink = null;
                if (!singleLink.matches(regexSingleLink)) {
                    finallink = singleLink;
                }
                final Browser brc = br.cloneBrowser();
                if (finallink == null) {
                    brc.getPage(singleLink);
                    if (parameter.matches(".+(multishared\\.com)/.+")) {
                        br.getPage(singleLink);
                        brc.getHeaders().put("Referer", "http://multishared.com/r_counter");
                        Thread.sleep(5 * 1000);
                        brc.getPage(singleLink);
                        finallink = brc.getRegex("http://multishared\\.com/r_ads\\'>[\t\n\r ]+<frame src=\"(http[^<>\"]*?)\"").getMatch(0);
                    } else {
                        finallink = brc.getRedirectLocation();
                        if (finallink == null) {
                            String referer = null;
                            String add_char = "";
                            Integer wait = 0;
                            if (parameter.matches(".+(mirrorstack\\.com)/.+")) {
                                add_char = "?";
                                referer = new Regex(br.getURL(), "(https?://[^/]+)/").getMatch(0) + "/" + add_char + "q=r_counter";
                            } else if (parameter.matches(".+(multiupload\\.biz)/.+")) {
                                add_char = "?";
                                referer = new Regex(br.getURL(), "(https?://[^/]+)/").getMatch(0).replace("http://", "https://") + "/r_counter";
                                wait = 20;
                            } else {
                                referer = new Regex(br.getURL(), "(https?://[^/]+)/").getMatch(0) + "/r_counter";
                            }
                            brc.getHeaders().put("Referer", referer);
                            Thread.sleep(wait * 1000);
                            brc.getPage(singleLink + add_char);
                            finallink = brc.getRedirectLocation();
                        }
                        if (finallink == null) {
                            logger.warning("WARNING: Couldn't find finallink. Please report this issue to JD Developement team. : " + parameter);
                            logger.warning("Continuing...");
                            continue;
                        }
                    }
                }
                final DownloadLink link = createDownloadlink(finallink);
                decryptedLinks.add(link);
                try {
                    distribute(link);
                } catch (final Throwable e) {
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
     * @throws IOException
     */
    @SuppressWarnings("unused")
    private void notused(String parameter, String[] singleLinks) throws IOException {
        // all links still found on main page
        singleLinks = br.getRegex("<a href=\\'" + regexSingleLink + "\\'").getColumn(0);
        if (singleLinks == null || singleLinks.length == 0) {
            singleLinks = br.getRegex(regexSingleLink).getColumn(0);
        }
        if (singleLinks == null || singleLinks.length == 0) {
            // final fail over, not really required but anyway here we go.
            String fstat = br.getRegex("(http://mirrorstack\\.com/update_fstat/\\d+/)").getMatch(0);
            if (fstat != null) {
                String referer = br.getURL();
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getPage(fstat + Math.random() * 10000);
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

}