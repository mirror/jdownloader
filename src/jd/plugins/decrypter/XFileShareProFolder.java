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
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

@SuppressWarnings("deprecation")
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "XFileShareProFolder" }, urls = {
        "https?://(www\\.)?(subyshare\\.com|brupload\\.net|(exclusivefaile\\.com|exclusiveloader\\.com)|ex-load\\.com|hulkload\\.com|koofile\\.com|bestreams\\.net|powvideo\\.net|lunaticfiles\\.com|youwatch\\.org|streamratio\\.com|vshare\\.eu|up\\.media1fire\\.com|salefiles\\.com|ortofiles\\.com|restfile\\.ca|restfilee\\.com|storagely\\.com|free\\-uploading\\.com|rapidfileshare\\.net|fireget\\.com|ishareupload\\.com|gorillavid\\.in|mixshared\\.com|longfiles\\.com|novafile\\.com|orangefiles\\.me|qtyfiles\\.com|free\\-uploading\\.com|free\\-uploading\\.com|uppit\\.com|downloadani\\.me|movdivx\\.com|faststore\\.org|clicknupload\\.org|isra\\.cloud|(up\\-4\\.net|up\\-4ever\\.com))/(users/[a-z0-9_]+/[^\\?\r\n]+|folder/\\d+/[^\\?\r\n]+)|https?://(?:www\\.)?users(?:files|cloud)\\.com/go/[a-zA-Z0-9]{12}/?|https?://(www\\.)?(hotlink\\.cc|ex-load\\.com)/folder/[a-f0-9\\-]+|https?://(?:www\\.)?imgbaron\\.com/g/[A-Za-z0-9]+" })
public class XFileShareProFolder extends antiDDoSForDecrypt {
    // DONT FORGET TO MAINTAIN HERE ALSO!
    public String[] siteSupportedNames() {
        return new String[] { "up-4.net", "usersfiles.com", "subyshare.com", "brupload.net", "exclusivefaile.com", "exclusiveloader.com", "ex-load.com", "hulkload.com", "koofile.com", "powvideo.net", "lunaticfiles.com", "youwatch.org", "streamratio.com", "vshare.eu", "up.media1fire.com", "salefiles.com", "ortofiles.com", "restfile.ca", "restfilee.com", "storagely.com", "free-uploading.com", "rapidfileshare.net", "fireget.com", "ishareupload.com", "gorillavid.in", "mixshared.com", "longfiles.com", "novafile.com", "orangefiles.me", "qtyfiles.com", "free-uploading.com", "free-uploading.com", "uppit.com", "downloadani.me", "movdivx.com", "faststore.org", "hotlink.cc", "clicknupload.org", "isra.cloud", "imgbaron.com" };
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }

    // DEV NOTES
    // other: keep last /.+ for fpName. Not needed otherwise.
    // other: group sister sites or aliased domains together, for easy
    // maintenance.
    // TODO: remove old xfileshare folder plugins after next major update.
    private String                        host           = null;
    private String                        parameter      = null;
    private boolean                       fast_linkcheck = false;
    private final ArrayList<String>       dupe           = new ArrayList<String>();
    private final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    FilePackage                           fp             = null;

    /**
     * @author raztoki
     */
    public XFileShareProFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        dupe.clear();
        decryptedLinks.clear();
        postData = null;
        i = 1;
        parameter = param.toString();
        host = new Regex(parameter, "https?://(www\\.)?([^:/]+)").getMatch(1);
        if (host == null) {
            logger.warning("Failure finding HOST : " + parameter);
            return null;
        }
        br.setCookie("https://" + host, "lang", "english");
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.containsHTML("No such user exist")) {
            logger.warning("Incorrect URL or Invalid user : " + parameter);
            return decryptedLinks;
        }
        // name isn't needed, other than than text output for fpName.
        String fpName = new Regex(parameter, "(folder/\\d+/|f/[a-z0-9]+/|go/[a-z0-9]+/)[^/]+/(.+)").getMatch(1); // name
        if (fpName == null) {
            fpName = new Regex(parameter, "(folder/\\d+/|f/[a-z0-9]+/|go/[a-z0-9]+/)(.+)").getMatch(1); // id
            if (fpName == null) {
                fpName = new Regex(parameter, "users/[a-z0-9_]+/[^/]+/(.+)").getMatch(0); // name
                if (fpName == null) {
                    fpName = new Regex(parameter, "users/[a-z0-9_]+/(.+)").getMatch(0); // id
                    if (fpName == null) {
                        if (parameter.matches(".+users(?:files|cloud)\\.com/.+")) {
                            fpName = br.getRegex("<title>\\s*(.*?)\\s*folder\\s*</title>").getMatch(0);
                        } else if ("hotlink.cc".equals(host)) {
                            fpName = br.getRegex("<i class=\"glyphicon glyphicon-folder-open\"></i>\\s*(.*?)\\s*</span>").getMatch(0);
                        } else if ("ex-load.com".equals(host)) {
                            fpName = br.getRegex("Files in (.*?) folder</title>").getMatch(0);
                            if (fpName == null) {
                                fpName = br.getRegex("<h1.*?</i>\\s*(.*?)\\s*</h1>").getMatch(0);
                            }
                        }
                    }
                }
            }
            if (fpName == null) {
                /* 2019-02-08: E.g. for photo galleries (e.g. imgbaron.com) */
                fpName = br.getRegex("<H1>\\s*?(.*?)\\s*?</H1>").getMatch(0);
            }
        }
        if (fpName != null) {
            fpName = Encoding.htmlDecode(fpName);
            fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
        }
        dupe.add(parameter);
        // count value prevents continuous loop.
        int count = 0;
        do {
            count = decryptedLinks.size();
            parsePage();
        } while (decryptedLinks.size() > count && parseNextPage() && !this.isAbort());
        return decryptedLinks;
    }

    private void parsePage() throws PluginException {
        final String[] links = br.getRegex("href=(\"|')(https?://(?:www\\.)?" + Pattern.quote(host) + "/[a-z0-9]{12}(?:/.*?)?)\\1").getColumn(1);
        if (links != null && links.length > 0) {
            for (final String link : links) {
                final String linkid = new Regex(link, "https?://(?:www\\.)?[^/]+/([a-z0-9]{12})").getMatch(0);
                if (dupe.add(linkid)) {
                    /**
                     * TODO: Consider adding support for "fast linkcheck" --> Set links as available here - maybe only if filename is given
                     * inside URL (which is often the case). In general, files inside a folder should be online!
                     */
                    final DownloadLink dl = createDownloadlink(link);
                    /* Set ContentURL - VERY important for XFS (Mass-)Linkchecking! */
                    dl.setContentUrl(link);
                    if (fast_linkcheck) {
                        dl.setAvailable(true);
                    }
                    decryptedLinks.add(dl);
                    distribute(dl);
                }
            }
        }
        // these should only be shown when its a /user/ decrypt task
        final String folders[] = br.getRegex("folder.?\\.gif.*?<a href=\"(.+?" + Pattern.quote(host) + "[^\"]+users/[^\"]+)").getColumn(0);
        if (folders != null && folders.length > 0) {
            for (String folder : folders) {
                if (folder.matches(this.getSupportedLinks().pattern()) && dupe.add(folder) && !parameter.equals(folder)) {
                    final DownloadLink dlfolder = createDownloadlink(folder);
                    decryptedLinks.add(dlfolder);
                    distribute(dlfolder);
                }
            }
        }
    }

    private int    i        = 1;
    private String postData = null;

    private boolean parseNextPage() throws Exception {
        // not sure if this is the same for normal folders, but the following
        // picks up users/username/*, 2019-02-08: will also work for photo galleries ('host.tld/g/bla')
        /* Increment page */
        i++;
        String nextPage = br.getRegex("<div class=(\"|')paging\\1>[^\r\n]+<a href=('|\")([^']+\\&amp;page=\\d+|/go/[a-zA-Z0-9]{12}/\\d+/?)\\2>Next").getMatch(2);
        if (nextPage != null) {
            nextPage = HTMLEntities.unhtmlentities(nextPage);
            nextPage = Request.getLocation(nextPage, br.getRequest());
            getPage(nextPage);
            return true;
        }
        if (postData == null) {
            /* pagination ? */
            final String pagination = br.getRegex("setPagination\\('\\.files_paging',.*?\\);").getMatch(-1);
            if (pagination == null) {
                return false;
            }
            final String op = new Regex(pagination, "op:\\s*'(\\w+)'").getMatch(0);
            final String usr_login = new Regex(pagination, "usr_login:\\s*'(\\w+)'").getMatch(0);
            final String fld_id = new Regex(pagination, "fld_id:\\s*'(\\w+)'").getMatch(0);
            if (op == null || usr_login == null || fld_id == null) {
                return false;
            }
            postData = "op=" + Encoding.urlEncode(op) + "&load=files&page=%s&fld_id=" + Encoding.urlEncode(fld_id) + "&usr_login=" + Encoding.urlEncode(usr_login);
        }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("x-requested-with", "XMLHttpRequest");
        postPage(br.getURL(), String.format(postData, i));
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}