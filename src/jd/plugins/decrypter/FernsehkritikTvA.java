//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fernsehkritik.tv" }, urls = { "http://(?:www\\.)?fernsehkritik\\.tv/folge\\-\\d+" })
public class FernsehkritikTvA extends PluginForDecrypt {

    private static final String GRAB_POSTECKE      = "GRAB_POSTECKE";
    private static final String CUSTOM_DATE        = "CUSTOM_DATE";
    private static final String CUSTOM_PACKAGENAME = "CUSTOM_PACKAGENAME";
    private static final String FASTLINKCHECK      = "FASTLINKCHECK";
    private boolean             FASTCHECKENABLED   = false;
    private SubConfiguration    CFG                = null;
    private PluginForHost       HOSTPLUGIN         = null;
    private String              DATE               = null;
    private String              EPISODENUMBER      = null;
    private short               episodenumber_short;

    public FernsehkritikTvA(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* Load host plugin */
        CFG = SubConfiguration.getConfig(this.getHost());
        HOSTPLUGIN = JDUtilities.getPluginForHost(this.getHost());
        FilePackage fp;
        final String parameter = param.toString();
        EPISODENUMBER = new Regex(parameter, "folge\\-(\\d+)").getMatch(0);
        episodenumber_short = Short.parseShort(EPISODENUMBER);
        if (CFG.getBooleanProperty(FASTLINKCHECK, false)) {
            FASTCHECKENABLED = true;
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(parameter);

        if (br.containsHTML(jd.plugins.hoster.FernsehkritikTv.HTML_MASSENGESCHMACK_OFFLINE)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        DATE = br.getRegex("vom ([^<>\"]+)</h3>").getMatch(0);
        if (DATE == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        DATE = Encoding.htmlDecode(DATE.trim());

        if (CFG.getBooleanProperty(GRAB_POSTECKE, false)) {
            /* Check for external links */
            String posteckelink = br.getRegex("<a href=\"([^<>\"]*?)\" class=\"btn btn\\-default\"><i class=\"fa fa\\-play\\-circle\"></i> Feedback <span").getMatch(0);
            if (posteckelink == null && episodenumber_short >= 139) {
                /* 139 or higher == new massengeschmack Postecke ("Massengeschmack Direkt") starting from 139 == 1 */
                final short massengeschmack_direct_episodenumber = (short) (episodenumber_short - 138);
                posteckelink = "http://massengeschmack.tv/play/direkt-" + massengeschmack_direct_episodenumber;
            }
            /* Check if we got a new massengeschmack link - simply add it. */
            if (posteckelink != null && posteckelink.matches("http://massengeschmack\\.tv/play/.+")) {
                decryptedLinks.add(createDownloadlink(posteckelink));
            } else if (posteckelink != null) {
                br.setFollowRedirects(false);
                /* External link - redirects to youtube or similar */
                if (!posteckelink.startsWith("http")) {
                    posteckelink = "http://fernsehkritik.tv" + posteckelink;
                }
                br.getPage(posteckelink);
                final String extern_link = br.getRedirectLocation();
                if (extern_link == null) {
                    return null;
                }
                decryptedLinks.add(createDownloadlink(extern_link));
                br.setFollowRedirects(true);
            }
        }

        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("massengeschmack.tv");
        Account account = null;
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                /* Premium needed to watch/download fernsehkritik.tv on massengeschmack.tv! */
                if (n.isEnabled() && n.isValid() && n.getType() == AccountType.PREMIUM) {
                    account = n;
                    break;
                }
            }
        }
        if (account != null) {
            /* Account available? Add URL as premium! */
            final DownloadLink dl = this.createDownloadlink("http://massengeschmack.tv/play/fktv" + EPISODENUMBER);
            /* Set date here as it isn't necessarily given via massengeschmack.tv website. */
            dl.setProperty("directdate", this.DATE);
            decryptedLinks.add(dl);
        } else {
            final ArrayList<DownloadLink> dllinks = getFktvParts(parameter, EPISODENUMBER);
            decryptedLinks.addAll(dllinks);
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            return null;
        }

        fp = FilePackage.getInstance();
        final String formattedpackagename = getFormattedPackagename(EPISODENUMBER, DATE);
        fp.setName(formattedpackagename);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private ArrayList<DownloadLink> getFktvParts(final String parameter, final String episode) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String playurl = parameter + "/play/";
        br.getPage(playurl);
        final DownloadLink dlLink = createDownloadlink("http://fernsehkritik.tv/jdownloaderfolgeneu" + System.currentTimeMillis() + new Random().nextInt(1000000));

        dlLink.setContentUrl(parameter);

        dlLink.setProperty("directdate", DATE);
        dlLink.setProperty("directepisodenumber", EPISODENUMBER);
        dlLink.setProperty("directtype", ".mp4");
        dlLink.setProperty("mainlink", playurl);
        final String formattedFilename = ((jd.plugins.hoster.FernsehkritikTv) HOSTPLUGIN).getFKTVFormattedFilename(dlLink);
        dlLink.setFinalFileName(formattedFilename);
        if (FASTCHECKENABLED) {
            dlLink.setAvailable(true);
        }
        decryptedLinks.add(dlLink);
        return decryptedLinks;
    }

    private final static String defaultCustomPackagename = "Fernsehkritik.tv Folge *episodenumber* vom *date*";
    private static final String inputDateformat          = "dd. MMMMM yyyy";
    private final static String defaultCustomDate        = "dd MMMMM yyyy";

    private String getFormattedPackagename(final String episodenumber, final String date) throws ParseException {
        String formattedpackagename = CFG.getStringProperty(CUSTOM_PACKAGENAME, defaultCustomPackagename);
        if (formattedpackagename == null || formattedpackagename.equals("")) {
            formattedpackagename = defaultCustomPackagename;
        }
        if (!formattedpackagename.contains("*episodenumber*")) {
            formattedpackagename = defaultCustomPackagename;
        }

        String formattedDate = null;
        if (date != null && formattedpackagename.contains("*date*")) {
            final String userDefinedDateFormat = CFG.getStringProperty(CUSTOM_DATE, defaultCustomDate);
            SimpleDateFormat formatter = new SimpleDateFormat(inputDateformat, new Locale("de", "DE"));
            Date dateStr = formatter.parse(date);

            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);

            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            if (formattedDate != null) {
                formattedpackagename = formattedpackagename.replace("*date*", formattedDate);
            } else {
                formattedpackagename = formattedpackagename.replace("*date*", "");
            }
        }
        if (formattedpackagename.contains("*episodenumber*")) {
            formattedpackagename = formattedpackagename.replace("*episodenumber*", episodenumber);
        }

        return formattedpackagename;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}