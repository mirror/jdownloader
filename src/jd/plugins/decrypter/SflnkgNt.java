//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

//Similar to SafeUrlMe (safeurl.me)
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {}, flags = {})
public class SflnkgNt extends abstractSafeLinking {

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "3download.safelinking.net", "7pz.com", "a7layom.ta4a.info", "adsva.org", "blacklistlink.com", "bidasse.com", "chigontech.com", "cdn.muvdog.com", "darkme.net", "anonlink.ml", "deni1743-safelink.tk", "cryptit.so", "animetal-tux.com", "downloads.tinynhd.com", "download.tinymoviez.biz", "download.jegethy.com", "download.isogator.eu", "ceelanka.org", "dwnlinks.com", "earningsguide.tk", "filedp.com", "ekoice.info", "fastgo.cu.cc", "farrel82protectlink.tk", "ignore.jp", "hydelink.in", "hosmy.com", "i-share.ws", "i7q.com", "hidemylinks.net", "kenggames.tk", "jessica.suyalynx.com", "gol.li", "link.rolex1494.net", "link.spyluv.com", "link.hentailinkz.com", "linkler.us", "hdclub.eu", "link.yify.info", "links.aniapps.net", "links.bulkseries.com", "links.crv.nu", "links.index-paw.net", "links.quartx.tk", "links.sceper.ws", "links.loadby.us", "lulusafe.tk",
                "linkshield.org", "matthy.tk", "mtsafelinking.org", "moesubs.tk", "monkwarez.tk", "mylinkz.tk", "nexushd2urlprotector.tytung.com", "ninjasecure.cf", "mway.tk", "movierulez.us", "one.gopmwd.com", "nsad.xcarlos.safelinking.net", "oxyl.me", "pastefull.com", "links.rlsarc.tk", "r4dm.com", "protect.mmportal.info", "resumable-link.info", "rgf.me", "safe.linkninja.net", "safe.dlinks.info", "safe.linksjunk.com", "safelinking.net", "safe.tres20kbps.com", "safelinking.com", "safelinking.biz", "safelinker.ga", "safelinknsn.net", "safemylink.com", "safelink.strike-up.net", "pucuk.tk", "roricon.com", "savelinks.net", "safelinking.mobi", "safetater.com", "seber-neko.net", "seclinks.tk", "secureyourlinks.com", "securelinking.tk", "shorten.ardiansyah.me", "serv021.esy.es", "sflux.tk", "sf.anime4u.ir", "sl.0xxx.in", "shortadult.com", "somedomain.com", "shotimedl.tk", "sl.8x.biz",
                "sflnk.tk", "slinx.tk", "trollbridge.org", "sl.unspeakable.org", "switchitonagain.net", "thisdoesntexist.tk", "torlink.org", "vault.vhsclub.com", "url-shortener.info", "upload.morto.org", "url.4-update.com", "url.bajak.in", "vepeliculasonline.com", "verificar.tk", "t9c.de", "vrrr.in", "yls.re", "wpnteam.pl", "sflk.in" };
        // add new domains at the beginning of the array, not the END
    }

    /**
     * returns the annotation pattern array
     *
     * @return
     */
    public static String[] getAnnotationUrls() {
        String[] a = new String[getAnnotationNames().length];
        int i = 0;
        for (final String domain : getAnnotationNames()) {
            if (i == a.length) {
                // short link domain! www will redirect to standard page. no https (but we will correct via correctLinks call to
                // supportsHTTPS())
                a[i] = "https?://sflk\\.in/[a-zA-Z0-9]{10}";
            } else {
                // https is only supported on there safelinking.net domain, once again auto correct within correctLink call to supportsHTTPS
                a[i] = "https?://(?:www\\.)?" + Pattern.quote(domain) + "/(?:(?:p|d(?:/com)?)/[a-zA-Z0-9]{10}|" + regexBase58() + ")";
            }
            i++;
        }
        return a;
    }

    /**
     * Returns the annotations flags array
     *
     * @return
     */
    public static int[] getAnnotationFlags() {
        final int gl = getAnnotationNames().length;
        int[] a = new int[gl];
        for (int i = 0; i < gl; i++) {
            a[i] = 0;
        }
        return a;
    }

    public SflnkgNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = super.decryptIt(param, progress);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    @Override
    protected boolean supportsHTTPS() {
        if ("safelinking.net".equalsIgnoreCase(Browser.getHost(parameter))) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean enforcesHTTPS() {
        return false;
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected String regexLinkD() {
        return "https?://[^/]*" + regexSupportedDomains() + "/d(?:/com)?/[a-z0-9]+";
    }

    @Override
    protected String getShortHost() {
        return "sflk.in";
    }

}