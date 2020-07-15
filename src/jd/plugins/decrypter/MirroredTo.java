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
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.AbortException;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mirrored.to" }, urls = { "https?://(?:www\\.)?((mirrorcreator\\.com|mirrored\\.to)/(files/|download\\.php\\?uid=)|mir\\.cr/)[0-9A-Z]{8}|https?://(?:www\\.)?mirrored\\.to/multilinks/[a-z0-9]+" })
public class MirroredTo extends PluginForDecrypt {
    private String                  userAgent      = null;
    private ArrayList<DownloadLink> decryptedLinks = null;
    private FilePackage             fp             = null;
    private String                  uid            = null;
    private CryptedLink             param          = null;

    public MirroredTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        try {
            this.param = param;
            br = new Browser();
            uid = new Regex(param.toString(), "([A-Z0-9]{8})$").getMatch(0);
            decryptedLinks = new ArrayList<DownloadLink>();
            if (userAgent == null) {
                userAgent = UserAgents.stringUserAgent();
            }
            br.getHeaders().put("User-Agent", userAgent);
            if (param.toString().contains("/multilinks/")) {
                br.getPage(param.toString());
                final String[] urls = br.getRegex("(https?://[^<>\"]+/files/[^<>\"]+|https?://mir\\.cr/[A-Za-z0-9]+)").getColumn(0);
                if (urls == null || urls.length == 0) {
                    return null;
                }
                for (final String url : urls) {
                    decryptedLinks.add(this.createDownloadlink(url));
                }
                /* These URLs will go back into this decrypter! */
                return decryptedLinks;
            }
            final String parameter = "https://www.mirrored.to/download.php?uid=" + uid;
            param.setCryptedUrl(parameter);
            br.setFollowRedirects(true);
            br.getPage(parameter);
            br.setFollowRedirects(false);
            // set packagename
            // because mirror creator is single file uploader. we want a single packagename for all these uploads vs one for each part!
            String fpName = br.getRegex("<title>\\s*([^<>\"]*?)\\s*\\-\\s*Mirrored\\.to").getMatch(0);
            final String filesize = br.getRegex(">\\s*File\\s+size\\s*:\\s*<span>\\s*([^<>\"]+)\\s*<").getMatch(0);
            if (fpName != null) {
                // here we will strip extensions!
                String ext;
                do {
                    ext = getFileNameExtensionFromString(fpName);
                    if (ext != null) {
                        fpName = fpName.replaceFirst(Pattern.quote(ext) + "$", "");
                    }
                } while (ext != null);
            }
            fp = fpName != null ? FilePackage.getInstance() : null;
            if (fp != null) {
                fp.setName(fpName);
                fp.setProperty("AllOW_MERGE", true);
            }
            // more steps y0! 20170602
            {
                final Form continueForm = getContinueForm();
                if (continueForm != null) {
                    br.submitForm(continueForm);
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
                logger.info("The following link should be offline: " + param.toString());
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
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
            logger.info("Task Complete! : " + parameter);
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
                /* 2020-01-21: Good for e.g. offline services or GEO-blocked such as Zippyshare. */
                if (filesize != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
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
            if (dllink == null || dllink.equals("")) {
                // Continue away, randomised pages can cause failures.
                logger.warning("Possible plugin error: " + param.toString());
                logger.warning("Continuing...");
                continue;
            }
            final DownloadLink fina = createDownloadlink(dllink);
            if (fp != null) {
                fp.add(fina);
            }
            distribute(fina);
            decryptedLinks.add(fina);
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