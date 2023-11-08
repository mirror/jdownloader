//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.appwork.utils.DebugMode;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class WdfilesRu extends YetiShareCore {
    public WdfilesRu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null solvemedia reCaptchaV2<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "wdfiles.ru" });
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
        return YetiShareCore.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean requiresWWW() {
        /* 2020-03-03: Special */
        return false;
    }

    @Override
    protected void runPostRequestTask(final Browser ibr) throws Exception {
        final Form antibotform = this.getAntibotForm(ibr);
        if (antibotform != null) {
            /* Automatically handle simple anti bot challenge: Pick correct color */
            logger.info("Jumping into anti bot handling");
            final String errormessageOnFailure = "Antibot handling failed";
            // final String errormessageOnFailure = "Antibot auto handling failed";
            final boolean tryCaptchaAutoHandling = false;
            if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE || !tryCaptchaAutoHandling) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessageOnFailure);
            }
            final String imageb64 = ibr.getRegex("(?i)\"data:image/png;base64,([^\"]+)\"").getMatch(0);
            if (imageb64 == null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessageOnFailure);
            }
            int red = 0;
            int green = 0;
            int blue = 0;
            try {
                final byte[] imagebytes = org.appwork.utils.encoding.Base64.decode(imageb64);
                final ByteArrayInputStream bais = new ByteArrayInputStream(imagebytes);
                // InputStream is = new ByteArrayInputStream(bytes);
                final BufferedImage img = ImageIO.read(bais);
                int x = 1;
                int y = 1;
                int clr = img.getRGB(x, y);
                red = (clr & 0x00ff0000) >> 16;
                green = (clr & 0x0000ff00) >> 8;
                blue = clr & 0x000000ff;
            } catch (final Throwable ignore) {
                logger.log(ignore);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessageOnFailure);
            }
            logger.info("r = " + red + " g = " + green + " b = " + blue);
            final int threshold = 150;
            final String colortext;
            if (red > threshold && green < threshold && blue < threshold) {
                colortext = "red";
            } else if (red < threshold && green > threshold && blue < threshold) {
                colortext = "green";
            } else if (red < threshold && green < threshold && blue > threshold) {
                colortext = "blue";
            } else if (red > threshold && green > threshold && blue == 0) {
                colortext = "yellow";
            } else if (red == 0 && green == 0 && blue == 0) {
                colortext = "black";
            } else if (red == 128 && green == 128 && blue == 128) {
                colortext = "gray";
            } else {
                /* We do not know hot to call this colortext-wise */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessageOnFailure);
            }
            antibotform.put("color", colortext.toUpperCase(Locale.ENGLISH));
            ibr.submitForm(antibotform);
            final String jsredirect = ibr.getRegex("<script>document\\.location=\"([^\"]+)\";</script>").getMatch(0);
            if (jsredirect != null) {
                ibr.getPage(jsredirect);
            }
            if (getAntibotForm(ibr) != null) {
                /* Looks like we failed the challenge -> This should never happen! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessageOnFailure);
            }
        }
    }

    private Form getAntibotForm(final Browser br) {
        final Browser br2 = br.cloneBrowser();
        br2.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.getRequest().getHtmlCode()));
        final Form antibotform = br2.getFormByRegex("antibot");
        return antibotform;
    }
}