//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MightyScriptAdLinkFlyDefault extends MightyScriptAdLinkFly {
    public MightyScriptAdLinkFlyDefault(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "cut-urls.com" };
    }

    private static final String[]     domains                    = { "shorts-link.com", "easy4earn.com", "linkat4all.com", "shrtz.me", "ctkings.com", "linksad.net", "paylink.pro", "123link.pro", "donia2link.com", "cutpaid.com", "shortadz.org", "itiurl.co", "shortli.net", "cutearn.ca", "icutit.ca", "cut-one.com", "cll.press", "link-zero.com", "linktor.io", "cash4url.com", "cashat.net", "123short.com", "skip-url.me", "msms4.com", "empireshort.com", "loadurl.com", "cutt.us.com", "arabdollar.com", "shortenow.com", "best3link.com", "solo-link.com", "best5link.com", "lkky.co", "win4cut.com", "adlink.guru", "ibly.co", "urle.co", "mitly.us", "zlshorte.net", "igram.im", "gram.im", "adbilty.me", "linclik.com", "oke.io", "pnd.tl", "met.bz", "urlcloud.us", "clik.pw", "z2i.com", "fant1asy.com",
            /** pwrpa.cc domains */
            "pwrpa.cc",
            /** 2023-09-11: tmearn.com domains */
            "tmearn.com", "tmearn.net",
            /** 2022-07-13 */
            "linkshrink.ca",
            /** 2021-08-26: rapid-cut.com domains */
            "rapid-cut.com", "sougkniss.com",
            /** 2021-08-20 */
            "shrt10.com",
            /** 2021-07-30 */
            "webo.one",
            /** 2021-06-17 */
            "srts.me",
            /** 2021-04-22: Belongs to filehost "fastdrive.io" and is used to redirect to their final downloadurls. */
            "roda.site", "securitystickers.info", "fencepedia.com",
            /** 2021-03-29: tny.so -> URL-Shortener of doodstream.com [promoted 2021-03-27 via E-Mail campaign] */
            "tny.so",
            /** 2021-02-08: hoastie.com */
            "hoastie.com",
            /** 2021-01-19: viralhacks.net domains */
            "viralhacks.net",
            /** 2021-01-14: shortzzy.com domains */
            "shortzzy.com", "shortzzy.link", "janusnotes.com", "shortzzy.in", "l.shortzzy.in",
            /** 2020-04-20: shrinkhere.xyz domains */
            "shrinkhere.xyz",
            /** 2020-04-01: za.gl domains */
            "za.gl", "zee.gl",
            /** 2020-02-16: payskip.org domains */
            "payskip.org",
            /** 2020-02-10: adlinkcash.com domains */
            "adlinkcash.com",
            /** 2019-12-04: shortmony.com domains */
            "shortmony.com", "shortmony.me", "gamez-ar.com",
            /** 2019-11-06: skip4all.com */
            "skip4all.com",
            /** 2019-10-30: glory-link.com domains */
            "glory-link.com",
            /** 2020-01-07: cashurl.win domains */
            "cashurl.win", "todaynewspk.win",
            /** 2019-10-30: exe.io domains */
            "exe.io", "exe.app", "exep.app", "exee.app", "exee.io", "exey.io", "iddeas.xyz", "artiicle.xyz", "techbeast.xyz", "techofaqs.com", "caat.site", "2xs.io", "wealthh.xyz", "cu6.io", "mediumarticles.com", "bolssc.com", "mealip.com", "cut-urls.com", "exeo.app",
            /** 2020-01-21: encurta.net domains */
            "encurta.net", "enrt.eu",
            /** 2020-01-20: 7r6.com domains */
            "7r6.com",
            /** 2019-11-13: linkshorty */
            "linkshorty.com", "americansvsarabs.com", "mat3sports.com", "shortyup.com",
            /** 2019-11-13: ex-foary.com */
            "ex-foary.com",
            /** 2019-11-13: Zoom Link */
            "zoom-link.com", "empir33.com",
            /** 2019-08-24: shortzon.com domains */
            "shortzon.com", "infothon.com",
            /** 2019-08-24: ilinkshort.com domains */
            "ilinkshort.com", "loopdiet.com", "xpshort.com",
            /** 2019-07-24: 2click.us domains */
            "2click.us", "2ck.cc", "earticale.xyz",
            /** 2019-07-24: 2shorten.com domains */
            "2shorten.com", "nashr.online", "almrf.com",
            /** shrinkearn.com domains */
            "shrinkearn.com", "shortearn.eu",
            /** clk.sh domains */
            "clk.sh", "oko.sh", "iir.ai",
            /** bt4link.com domains */
            "bt4link.com", "btc4link.com", "ur4link.com",
            /** shorted4us.com domains */
            "shorted4us.com", "thefreech.com", "sewrn.com",
            /** asrar-alrb7.com domains */
            "asrar-alrb7.com", "savircity.com",
            /** cut1.ga domains */
            "cut1.ga", "shortit.ca",
            /** safelinku.com domains */
            "safelinku.com", "idsly.bid", "idsly.net", "updatetribun.org",
            /** cepmuzikindir.me domains - NOTE: safelinku.NET != safelinku.COM */
            "cepmuzikindir.me", "safelinku.net", "miui.space",
            /** short.pe domains */
            "short.pe", "chrt.pw",
            /** urlcero.us domains */
            "urlcero.us", "urlcero.com",
            /** eglink.info domains */
            "eglink.info", "egyptiangy.net", "egypt-mix.com",
            /** wicr.me domains */
            "wicr.me", "wi.cr",
            /** cutwin.com domains */
            "cutwin.com", "cutwin.us",
            /** linkawynet.xyz links */
            "linkawynet.xyz",
            /** shortawy.com links */
            "shortawy.com",
            /** up4cash.com links */
            "up4cash.com",
            /** 2022-08-09: ier.ai links */
            "ier.ai",
            /** best-cpm links */
            "best-cpm.com",
            /** link-yz links */
            "link-yz.com" };
    /** List of services for which waittime is skippable. */
    private static final List<String> domains_waittime_skippable = Arrays.asList(new String[] {});
    // /** List of services for which captcha is skippable or not required. */
    /** TODO: Find a way to automatically detect this edge-case */
    private static final List<String> domains_captcha_skippable  = Arrays.asList(new String[] { "safelinku.com", "idsly.bid", "idsly.net" });

    @Override
    protected String getContentURL(final CryptedLink param) {
        final String contenturlFromUpperClass = super.getContentURL(param);
        if (contenturlFromUpperClass.contains("up4cash.com/s/")) {
            return contenturlFromUpperClass.replace("s/", "");
        } else {
            /* Nothing to correct */
            return contenturlFromUpperClass;
        }
    }

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        /* [a-zA-Z0-9]{2,} will work for 99% but site shortit.ca is not compatible. */
        /* 2019-04-25: Added special pattern for direct-redirects of clicksfly.com domains('/entrar/...') */
        return new String[] { host + "/(?!full)[a-zA-Z0-9]{2,}/?" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = "https?://(?:www\\.)?" + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    @Override
    protected boolean supportsHost(final String host) {
        for (final String domain : domains) {
            if (domain.equals(host)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean waittimeIsSkippable(final String source_host) {
        if (StringUtils.isEmpty(source_host)) {
            /* WTF */
            return false;
        } else if (domains_waittime_skippable.contains(source_host)) {
            /* Waittime is skippable for this host */
            return true;
        } else {
            /* Waittime is NOT skippable for all other hosts */
            return false;
        }
    }

    @Override
    protected boolean captchaIsSkippable(final String source_host) {
        return source_host != null && domains_captcha_skippable.contains(source_host);
    }
}
