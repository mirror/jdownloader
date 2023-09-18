//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.AbortException;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class MirroredTo extends PluginForDecrypt {
    private String                  userAgent      = null;
    private ArrayList<DownloadLink> decryptedLinks = null;
    private FilePackage             fp             = null;
    private String                  uid            = null;
    private CryptedLink             param          = null;

    public MirroredTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mirrored.to", "mirrorcreator.com", "mir.cr" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(";
            regex += "multilinks/[a-z0-9]+";
            regex += "|(files/|download\\.php\\?uid=)?[0-9A-Z]{8}";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        try {
            this.param = param;
            uid = new Regex(param.getCryptedUrl(), "([A-Z0-9]{8})$").getMatch(0);
            decryptedLinks = new ArrayList<DownloadLink>();
            if (userAgent == null) {
                userAgent = UserAgents.stringUserAgent();
            }
            br.getHeaders().put("User-Agent", userAgent);
            if (param.getCryptedUrl().contains("/multilinks/")) {
                br.setFollowRedirects(true);
                br.getPage(param.getCryptedUrl());
                final String[] urls = br.getRegex("(https?://[^<>\"]+/files/[^<>\"]+|https?://mir\\.cr/[A-Za-z0-9]+)").getColumn(0);
                if (urls == null || urls.length == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                for (final String url : urls) {
                    decryptedLinks.add(this.createDownloadlink(url));
                }
                /* These URLs will go back into this decrypter! */
                return decryptedLinks;
            }
            final String contenturl = "https://www.mirrored.to/download.php?uid=" + uid;
            br.setFollowRedirects(true);
            br.getPage(contenturl);
            br.setFollowRedirects(false);
            String filename = br.getRegex("<title>\\s*([^<>\"]*?)\\s*\\-\\s*Mirrored\\.to").getMatch(0);
            final String filesize = br.getRegex("<span>(\\d+(\\.\\d{1,2})? [MBTGK]+)</span><br>").getMatch(0);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename).trim();
                // here we will strip extensions!
                String ext;
                do {
                    ext = getFileNameExtensionFromString(filename);
                    if (ext != null) {
                        filename = filename.replaceFirst(Pattern.quote(ext) + "$", "");
                    }
                } while (ext != null);
            }
            fp = filename != null ? FilePackage.getInstance() : null;
            if (fp != null) {
                fp.setName(filename);
                fp.setAllowMerge(true);
            }
            // more steps y0! 20170602
            {
                final Form continueForm = getContinueForm();
                if (continueForm != null) {
                    br.submitForm(continueForm);
                }
            }
            {
                /* 2021-03-15: New step */
                final String continuelinkExtra = br.getRegex("\"([^\"]+\\&dl=0)").getMatch(0);
                if (continuelinkExtra != null) {
                    br.getPage(continuelinkExtra);
                }
            }
            {
                final String continuelink = br.getRegex("\"(/m(?:ir)?stats?\\.php\\?uid=" + uid + "[^\"]*)\"").getMatch(0);
                if (continuelink != null) {
                    br.getPage(continuelink);
                }
            }
            /* Error handling */
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(>Unfortunately, the link you have clicked is not available|>Error - Link disabled or is invalid|>Links Unavailable as the File Belongs to Suspended Account\\. <|>Links Unavailable|>Sorry, an error occured)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(">\\s*Sorry, no download links available")) {
                /* 2020-07-15 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            // lots of forms
            Form[] forms = br.getFormsByActionRegex("/downlink\\.php\\?uid=" + uid);
            if (forms == null || forms.length == 0) {
                forms = br.getFormsByActionRegex("/downlink/[A-Z0-9]+");
                if (forms == null || forms.length == 0) {
                    forms = br.getFormsByActionRegex("/out_url.*");
                }
            }
            if (forms != null && forms.length > 0) {
                logger.info("Found " + forms.length + " links");
                for (final Form form : forms) {
                    final Browser br2 = br.cloneBrowser();
                    br2.submitForm(form);
                    handleLink(br2, filesize);
                }
            } else {
                // older shit
                // they comment in fakes, so we will just try them all!
                String[] links = br.getRegex("(/getlink/[^/]+/[^/]+/[^<>\"/]*?=[a-z0-9]{25,32})\"").getColumn(0);
                if (links == null || links.length == 0) {
                    links = br.getRegex("(/[^<>\"/]*?=[a-z0-9]{25,32})\"").getColumn(0);
                }
                if (links == null || links.length == 0) {
                    links = br.getRegex("\"(/hosts/" + uid + "[^\"]+)\"").getColumn(0);
                }
                if (links.length > 0) {
                    for (String link : links) {
                        Browser br2 = br.cloneBrowser();
                        br2.getPage(link);
                        handleLink(br2, filesize);
                        if (this.isAbort()) {
                            break;
                        }
                    }
                } else {
                    /* 2020-07-15: (Sometimes) they do not "hide" their mirror-URLs behind a redirect. */
                    final String[] directURLs = br.getRegex("a href=\"(https?://[^<>\"]+)\" target=\"_blank\"").getColumn(0);
                    if (directURLs.length == 0) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    for (final String url : directURLs) {
                        decryptedLinks.add(this.createDownloadlink(url));
                    }
                }
            }
            logger.info("Task Complete! : " + contenturl);
        } catch (final AbortException a) {
            logger.info("User Aborted task!");
        }
        return decryptedLinks;
    }

    private Form getContinueForm() {
        final Form[] forms = br.getForms();
        for (final Form form : forms) {
            if (form.hasInputFieldByName("c_click")) {
                return form;
            } else if (form.hasInputFieldByName("c-click")) {
                return form;
            } else if (form.containsHTML("dl_form")) {
                /* 2020-05-04 */
                return form;
            }
        }
        return null;
    }

    private void handleLink(final Browser br, final String filesize) throws Exception {
        if (this.isAbort()) {
            logger.info("Decryption aborted...");
            throw new AbortException();
        }
        Browser br2 = br.cloneBrowser();
        // adware for some users!
        final String[] redirectLinks = br2.getRegex("(\"|')(?![^\"']*optic4u\\.info)(/[^/\r\n\t]+/" + uid + "/[^\"\r\n\t]+)\\1").getColumn(1);
        if (redirectLinks == null || redirectLinks.length == 0) {
            // not redirects but final download link in html.
            String finallink = br2.getRegex("<a href=(http[^ ]+)\\s*(?:TARGET\\s*=\\s*'_blank')?>Your").getMatch(0);
            if (finallink == null) {
                finallink = br2.getRegex("<div class=\"[^\"]*highlight[^\"]*\"\\s*>\\s*<a\\s*href\\s*=\\s*\"?(.*?)\"?\\s*(target|<)").getMatch(0);
            }
            if (finallink == null) {
                finallink = br2.getRegex("<META HTTP\\-EQUIV=\"Refresh\" CONTENT=\"\\d+; URL=https?://[^=]+=(http[^\"]+)\">").getMatch(0);
            }
            if (finallink != null) {
                logger.info("Creating download link for " + finallink);
                final DownloadLink dl = createDownloadlink(finallink);
                if (fp != null) {
                    fp.add(dl);
                    dl.setName(fp.getName());
                }
                if (filesize != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                setSpecialProperties(dl);
                distribute(dl);
                decryptedLinks.add(dl);
                return;
            }
            if ((redirectLinks == null || redirectLinks.length == 0) && finallink == null) {
                logger.warning("Scanning for redirectLinks && finallinks failed, possible change in html, continuing...");
                return;
            }
        }
        logger.info("Found " + redirectLinks.length + " " + this.getHost() + " links to decrypt...");
        for (String singlelink : redirectLinks) {
            singlelink = singlelink.replace("\"", "").replace(" ", "");
            br2 = br.cloneBrowser();
            String dllink = null;
            // Handling for links that need to be regexed or that need to be get by redirect
            if (singlelink.matches("/[^/]+/.*?" + uid + ".*?/.*?")) {
                br2.getPage(singlelink.trim());
                dllink = br2.getRedirectLocation();
                if (dllink == null) {
                    dllink = br2.getRegex("Please <a href=(\"|')?(http.*?)\\1 ").getMatch(1);
                    if (dllink == null) {
                        dllink = br2.getRegex("redirecturl\">(https?://[^<>]+)</div>").getMatch(0);
                    }
                }
            } else {
                // Handling for already regexed final-links
                dllink = singlelink;
            }
            if (StringUtils.isEmpty(dllink)) {
                // Continue away, randomised pages can cause failures.
                logger.warning("Possible plugin error: " + br.getURL());
                continue;
            }
            final DownloadLink dl = createDownloadlink(dllink);
            if (fp != null) {
                fp.add(dl);
            }
            setSpecialProperties(dl);
            distribute(dl);
            decryptedLinks.add(dl);
        }
    }

    private void setSpecialProperties(final DownloadLink link) {
        if (fp != null) {
            /*
             * Special: This service creates multiple mirror of files but filehost may change the original filename. Store the original name
             * as property so if users or other plugins want to, they can make use of that.
             */
            link.setProperty(this.getHost() + "_filename", fp.getName());
            link.setName(fp.getName());
        }
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-01-21: Avoid IP block! */
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}