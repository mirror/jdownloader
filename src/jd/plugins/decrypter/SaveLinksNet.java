//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkpacking.net", "doridroz.com", "resumable-link.info", "kids-funclub.com", "noor7.me", "trollbridge.org", "mylinks.asia", "wicket99.com", "safelinks.fr", "t9c.de", "gamezine.info", "deni1743-safelink.tk", "moesubs.tk", "backdoor.ir", "uniquerelink.com", "lulusafe.tk", "doujinlinks.org", "matthy.tk", "hydelink.in", "safelinking.biz", "securelinking.tk", "urlshrtnr.com", "uniquerelink.com", "filedp.com", "keepyourlinkssafe.com", "7pz.com", "safemylink.net", "mway.tk", "url-shortener.info", "rgf.me", "720pm.com", "javaddiction.us", "hidemylinks.net", "r4dm.com", "xsharez.com", "savelinks.net" }, urls = { "http://(www\\.)?linkpacking\\.net/(p|d)/[a-z0-9]+", "http://(www\\.)?doridroz\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?resumable\\-link\\.info/(p|d)/[a-z0-9]+",
        "http://(www\\.)?kids\\-funclub\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?noor7\\.me/(p|d)/[a-z0-9]+", "http://(www\\.)?trollbridge\\.org/(p|d)/[a-z0-9]+", "http://(www\\.)?mylinks\\.asia/(p|d)/[a-z0-9]+", "http://(www\\.)?wicket99\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?safelinks\\.fr/(p|d)/[a-z0-9]+", "http://(www\\.)?t9c\\.de/(p|d)/[a-z0-9]+", "http://(www\\.)?gamezine\\.info/(p|d)/[a-z0-9]+", "http://(www\\.)?deni1743\\-safelink\\.tk/(p|d)/[a-z0-9]+", "http://(www\\.)?moesubs\\.tk/(p|d)/[a-z0-9]+", "http://(www\\.)?backdoor\\.ir/(p|d)/[a-z0-9]+", "http://(www\\.)?uniquerelink\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?lulusafe\\.tk/(p|d)/[a-z0-9]+", "http://(www\\.)?doujinlinks\\.org/(p|d)/[a-z0-9]+", "http://(www\\.)?matthy\\.tk/(p|d)/[a-z0-9]+", "http://(www\\.)?hydelink\\.in/(p|d)/[a-z0-9]+", "http://(www\\.)?safelinking\\.biz/(p|d)/[a-z0-9]+",
        "http://(www\\.)?securelinking\\.tk/(p|d)/[a-z0-9]+", "http://(www\\.)?urlshrtnr\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?uniquerelink\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?filedp\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?keepyourlinkssafe\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?7pz\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?safemylink\\.net/(p|d)/[a-z0-9]+", "http://(www\\.)?mway\\.tk/(p|d)/[a-z0-9]+", "http://(www\\.)?url\\-shortener\\.info/(p|d)/[a-z0-9]+", "http://(www\\.)?rgf\\.me/(p|d)/[a-z0-9]+", "http://(www\\.)?720pm\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?javaddiction\\.us/(p|d)/[a-z0-9]+", "https?://(www\\.)?hidemylinks\\.net/(p|d)/[a-z0-9]+", "https?://(www\\.)?r4dm\\.com/(p|d)/[a-z0-9]+", "https?://(www\\.)?xsharez\\.com/(p|d)/[a-z0-9]+", "http://(www\\.)?savelinks\\.net/(p|d)/[a-z0-9]+" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, })
public class SaveLinksNet extends SflnkgNt {

    // Use this as generic plugin until JD2 comes out as stable. Remove these type of and do it all within SflnkgNt class!
    // No need for individual classes for each of these sites, specially if they do not require differing steps.
    // This will reduce IO paging and increase over all performance. Also reduces updates

    // Notes:
    // - forward and reversed dns results tell you the real story. these are just aliased domains of safelinking.net
    // - See https://safelinking.net/ucp/domain for more info.
    // - DNS on 5.135.187.149 = safelinking.net
    // - the IP is OVH subnet.
    // - you can not switch up the uid between domains.
    // - whois indicates 'name server' services provided by cloudflare

    /*
     * Name: resumable-link.info Address: 5.135.187.149 (safelinking.net) Name: kids-funclub.com Address: 5.135.187.149 (safelinking.net) Name: noor7.me
     * Address: 5.135.187.149 (safelinking.net) Name: trollbridge.org Address: 5.135.187.149 (safelinking.net) Name: mylinks.asia Address: 5.135.187.149
     * (safelinking.net) Name: wicket99.com Address: 5.135.187.149 (safelinking.net) Name: safelinks.fr Address: 5.135.187.149 (safelinking.net) Name: t9c.de
     * Address: 5.135.187.149 (safelinking.net) Name: gamezine.info Address: 5.135.187.149 (safelinking.net) Name: deni1743-safelink.tk Address: 5.135.187.149
     * (safelinking.net) Name: moesubs.tk Address: 5.135.187.149 (safelinking.net) Name: backdoor.ir Address: 5.135.187.149 (safelinking.net) Name:
     * uniquerelink.com Address: 5.135.187.149 (safelinking.net) Name: lulusafe.tk Address: 5.135.187.149 (safelinking.net) Name: doujinlinks.org Address:
     * 5.135.187.149 (safelinking.net) Name: matthy.tk Address: 5.135.187.149 (safelinking.net) Name: hydelink.in Address: 5.135.187.149 (safelinking.net) Name:
     * safelinking.biz Address: 5.135.187.149 (safelinking.net) Name: securelinking.tk Address: 5.135.187.149 (safelinking.net) Name: urlshrtnr.com Address:
     * 5.135.187.149 (safelinking.net) Name: uniquerelink.com Address: 5.135.187.149 (safelinking.net) Name: filedp.com Address: 5.135.187.149 (safelinking.net)
     * Name: keepyourlinkssafe.com Address: 5.135.187.149 (safelinking.net) Name: 7pz.com Address: 5.135.187.149 (safelinking.net) Name: safemylink.net Address:
     * 5.135.187.149 (safelinking.net) Name: mway.tk Address: 5.135.187.149 (safelinking.net) Name: url-shortener.info Address: 5.135.187.149 (safelinking.net)
     * Name: rgf.me Address: 5.135.187.149 (safelinking.net) Name: 720pm.com Address: 5.135.187.149 (safelinking.net) Name: javaddiction.us Address:
     * 5.135.187.149 (safelinking.net) Name: hidemylinks.net Address: 5.135.187.149 (safelinking.net) Name: r4dm.com Address: 5.135.187.149 (safelinking.net)
     * Name: xsharez.com Address: 5.135.187.149 (safelinking.net) Name: savelinks.net Address: 5.135.187.149 (safelinking.net)
     * 
     * Non safelink.net alias domains (maybe they have other ips we don't know about)? Name: linkpacking.net Address: 184.170.132.98
     * (server12.hostwhitelabel.com) Name: keeplinks.me ** has its own plugin for some reason... cant be bothered to figure out why. Address: 192.196.156.84
     * (host.keeplinks.me) Name: doridroz.com Address: 94.23.47.27 (ns369610.ovh.net)
     */

    public SaveLinksNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final GeneralSafelinkingHandling gsh = new GeneralSafelinkingHandling(br, param, getHost());
        gsh.startUp();
        try {
            gsh.decrypt();
        } catch (final DecrypterException e) {
            final String errormessage = e.getMessage();
            if ("offline".equals(errormessage)) { return decryptedLinks; }
            throw e;
        }
        decryptedLinks = gsh.getDecryptedLinks();

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}