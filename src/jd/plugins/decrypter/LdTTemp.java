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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lof.cc" }, urls = { "http://[\\w\\.]*?(lof\\.cc|92\\.241\\.168\\.5)/[!a-zA-Z0-9_]+" }, flags = { 0 })
public class LdTTemp extends PluginForDecrypt {

    public static final Object   LOCK                = new Object();
    private static final Pattern PATTERN_WAITTIME    = Pattern.compile("<p>Du musst noch (\\d+) Sekunden warten bis du", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_CAPTCHA     = Pattern.compile("(api\\.recaptcha\\.net|Das war leider Falsch|das Falsche Captcha eingegeben)", Pattern.CASE_INSENSITIVE);
    private static long          LATEST_BLOCK_DETECT = 0;
    private static long          LATEST_RECONNECT    = 0;

    private synchronized static boolean limitsReached(final Browser br, final int waittime) throws IOException {
        int ret = -100;
        if (br == null) {
            ret = UserIO.RETURN_OK;
        } else {
            if (System.currentTimeMillis() - LATEST_BLOCK_DETECT < 60000) { return true; }
            if (System.currentTimeMillis() - LATEST_RECONNECT < 15000) { return false; }
            UserIO.setCountdownTime(waittime);
            ret = UserIO.getInstance().requestConfirmDialog(0, "Wartezeit", "Ein Limit wurde erreicht!\r\nSie können entweder einen \"Reconnect\" durchführen oder die Wartezeit abbrechen.\r\nOder einfach nichts tun und die Wartezeit ablaufen lassen.\r\n", null, "Reconnect", "Abbrechen");
        }
        if (ret != -100) {
            if (UserIO.isOK(ret)) {
                try {
                    jd.controlling.reconnect.ipcheck.IPController.getInstance().invalidate();
                } catch (final Throwable e) {
                    /* not in 9580 stable */
                }
                if (Reconnecter.waitForNewIP(15000, false)) {
                    LATEST_RECONNECT = System.currentTimeMillis();
                    return true;
                }
            } else {
                LATEST_BLOCK_DETECT = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    public LdTTemp(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        synchronized (LOCK) {
            final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            final String parameter = param.toString();
            br.getPage(parameter);
            int i;
            for (i = 0; i < 5; i++) {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, param);
                rc.setCode(c);
                if (br.getRegex(PATTERN_WAITTIME).matches()) {
                    final int waittime = Integer.valueOf(br.getRegex(PATTERN_WAITTIME).getMatch(0));
                    if (waittime > 50 && limitsReached(br, waittime)) {

                        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
                        br.clearCookies(br.getHost());
                    } else {
                        sleep(waittime * 1001l, param);
                    }
                    br.getPage(parameter);
                    i = 0;
                    continue;
                } else if (br.getRegex(PATTERN_CAPTCHA).matches()) {
                    continue;
                }
                break;
            }
            if (br.getRegex(PATTERN_CAPTCHA).matches()) { throw new DecrypterException(DecrypterException.CAPTCHA); }
            if (br.getRegex(PATTERN_WAITTIME).matches()) { return null; }
            final Form allLinks = br.getForm(0);
            String links[] = allLinks.getRegex("<a href=\"(http.*?)\" target=\"_blank\" onclick=").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("First LdTTemp regex failed, trying the second one...");
                links = HTMLParser.getHttpLinks(br.toString(), "");
            }
            if (links.length == 0) { return null; }
            for (final String finallink : links) {
                if (!finallink.contains("iload.to") && !finallink.contains("lof.cc") && !finallink.endsWith(".gif") && !finallink.endsWith(".swf")) {
                    decryptedLinks.add(createDownloadlink(finallink));
                }
            }
            return decryptedLinks;
        }
    }
}
