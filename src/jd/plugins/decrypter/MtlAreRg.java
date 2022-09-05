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

import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metalarea.org" }, urls = { "https?://(?:www\\.)?metalarea\\.org/forum/index\\.php\\?showtopic=([0-9]+)" })
public class MtlAreRg extends PluginForDecrypt {
    /* must be static so all plugins share same lock */
    private static Object LOCK = new Object();

    public MtlAreRg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setCookiesExclusive(false);
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(400);
        getUserLogin(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<td width='99%' style='word-wrap:break-word;'>\\s*<div>\\s*<img[^>]*/>\\s*\\&nbsp;\\s*<b>\\s*(.*?)\\s*</div>\\s*</td>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
            if (fpName != null) {
                fpName = fpName.replace(" - Metal Area - Extreme Music Portal", "");
            }
        }
        if (fpName != null) {
            fpName = fpName.replaceAll("(</b>(\\s*,)?)", "");
        }
        // Filter links in hide(s)
        final String htmls[] = br.getRegex("<\\!\\-\\-HideBegin\\-\\->(.*?)<\\!\\-\\-HideEnd\\-\\->").getColumn(0);
        if (htmls == null || htmls.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String pagepiece : htmls) {
            String[] links = HTMLParser.getHttpLinks(pagepiece, "");
            if (links != null && links.length != 0) {
                for (String link : links) {
                    ret.add(createDownloadlink(link));
                }
            }
        }
        if (fpName != null) {
            fpName = Encoding.htmlDecode(fpName).trim();
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(ret);
        }
        return ret;
    }

    private void getUserLogin(final String url) throws IOException, DecrypterException, PluginException, DecrypterRetryException, InterruptedException {
        synchronized (LOCK) {
            final String logincookie = this.getPluginConfig().getStringProperty("masession_id");
            if (logincookie != null) {
                /* Re- use existing session */
                br.setCookie(this.getHost(), "masession_id", logincookie);
                getPageWithRateLimitHandling(br, url);
                checkErrors(br);
                if (this.isLoggedIN(br)) {
                    logger.info("Cookie login successful");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    this.br.clearCookies(br.getHost());
                }
            }
            logger.info("Full login required");
            String username = this.getPluginConfig().getStringProperty("user");
            String password = this.getPluginConfig().getStringProperty("pass");
            if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
                username = UserIO.getInstance().requestInputDialog("Enter Loginname for metalarea.org :");
                if (StringUtils.isEmpty(username)) {
                    logger.info("Invalid/no username provided");
                    throw new AccountRequiredException();
                }
                password = UserIO.getInstance().requestInputDialog("Enter password for metalarea.org :");
                if (StringUtils.isEmpty(password)) {
                    logger.info("Invalid/no password provided");
                    throw new AccountRequiredException();
                }
            }
            performFullLogin(br, username, password);
            getPageWithRateLimitHandling(br, url);
        }
    }

    private void performFullLogin(final Browser br, final String username, final String password) throws IOException, PluginException, DecrypterRetryException {
        br.getPage("https://" + this.getHost() + "/forum/index.php?act=Login");
        checkErrors(br);
        br.postPage("/forum/index.php?act=Login&CODE=01", "UserName=" + Encoding.urlEncode(username) + "&PassWord=" + Encoding.urlEncode(password) + "&CookieDate=1");
        checkErrors(br);
        final String logincookie = br.getCookie(br.getHost(), "masession_id", Cookies.NOTDELETEDPATTERN);
        if (logincookie != null && this.isLoggedIN(br)) {
            logger.info("Full login successful");
            this.getPluginConfig().setProperty("user", username);
            this.getPluginConfig().setProperty("pass", password);
            this.getPluginConfig().setProperty("masession_id", password);
            this.getPluginConfig().save();
        } else {
            /* Clear logindata so that we can ask user again the next time */
            logger.info("Full login failed");
            this.getPluginConfig().removeProperty("user");
            this.getPluginConfig().removeProperty("pass");
            this.getPluginConfig().removeProperty("masession_id");
            this.getPluginConfig().save();
            throw new AccountRequiredException();
        }
    }

    private void getPageWithRateLimitHandling(final Browser br, final String url) throws IOException, InterruptedException, PluginException, DecrypterRetryException {
        int counter = 0;
        do {
            counter++;
            br.getPage(url);
            if (looksLikeRateLimited(br)) {
                logger.info("Trying to get around rate-limit attempt: " + counter);
                br.clearCookies(br.getHost());
                this.performFullLogin(br, this.getPluginConfig().getStringProperty("user"), this.getPluginConfig().getStringProperty("pass"));
                Thread.sleep(1000l);
                continue;
            } else {
                if (counter > 1) {
                    logger.info("Successfully avoided rate-limit in run " + counter);
                }
                break;
            }
        } while (counter < 5 || !this.isAbort());
    }

    private void checkErrors(final Browser br) throws PluginException, DecrypterRetryException {
        if (looksLikeRateLimited(br)) {
            throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private boolean looksLikeRateLimited(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 400) {
            return true;
        } else {
            return false;
        }
    }

    boolean isLoggedIN(final Browser br) {
        /* Check if user control-panel is visible. */
        return br.containsHTML("id=\"userlinks\"");
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* Allow only one instance to avoid hitting rate-limits even faster */
        return 1;
    }
}