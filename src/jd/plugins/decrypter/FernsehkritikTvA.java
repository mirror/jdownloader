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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fernsehkritik.tv" }, urls = { "http://(www\\.)?fernsehkritik\\.tv/folge\\-\\d+" }, flags = { 0 })
public class FernsehkritikTvA extends PluginForDecrypt {

    private static final String DL_AS_MOV          = "DL_AS_MOV";
    private static final String DL_AS_MP4          = "DL_AS_MP4";
    private static final String DL_AS_FLV          = "DL_AS_FLV";
    private boolean             MOV                = true;
    private boolean             MP4                = true;
    private boolean             FLV                = true;
    private static final String GRAB_POSTECKE      = "GRAB_POSTECKE";
    private static final String CUSTOM_DATE        = "CUSTOM_DATE";
    private static final String CUSTOM_PACKAGENAME = "CUSTOM_PACKAGENAME";
    private static final String FASTLINKCHECK      = "FASTLINKCHECK";
    private boolean             FASTCHECKENABLED   = false;
    private SubConfiguration    CFG                = null;
    private PluginForHost       HOSTPLUGIN         = null;
    private String              DATE               = null;
    private String              EPISODENUMBER      = null;

    public FernsehkritikTvA(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        CFG = SubConfiguration.getConfig("fernsehkritik.tv");
        HOSTPLUGIN = JDUtilities.getPluginForHost("fernsehkritik.tv");
        FilePackage fp;
        final String parameter = param.toString();
        EPISODENUMBER = new Regex(parameter, "folge\\-(\\d+)").getMatch(0);
        if (CFG.getBooleanProperty(FASTLINKCHECK, false)) {
            FASTCHECKENABLED = true;
        }
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(parameter);

        DATE = br.getRegex("vom (\\d{1,2}\\. [A-Za-z]+ \\d{4})</h3>").getMatch(0);
        if (DATE == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        DATE = Encoding.htmlDecode(DATE.trim());

        if (CFG.getBooleanProperty(GRAB_POSTECKE, false)) {
            /* Check for external links */
            String posteckelink = br.getRegex("<a href=\"([^<>\"]*?)\" class=\"btn btn\\-default\"><i class=\"fa fa\\-play\\-circle\"></i> Feedback <span").getMatch(0);
            /* Check if we got a new massengeschmack link - simply add it. */
            if (posteckelink != null && posteckelink.matches("http://massengeschmack\\.tv/play/.+")) {
                decryptedLinks.add(createDownloadlink(posteckelink));
            } else if (posteckelink != null) {
                br.setFollowRedirects(false);
                /* External link - redirects to youtube/myvideo or similar */
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

        final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("fernsehkritik.tv");
        Account account = null;
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    account = n;
                    break;
                }
            }
        }
        if (account != null) {
            MOV = CFG.getBooleanProperty(DL_AS_MOV, true);
            MP4 = CFG.getBooleanProperty(DL_AS_MP4, true);
            FLV = CFG.getBooleanProperty(DL_AS_FLV, true);
            if (MOV) {
                final DownloadLink dlLink = createDownloadlink("http://couch.fernsehkritik.tv/dl/fernsehkritik" + EPISODENUMBER + ".mov");
                decryptedLinks.add(dlLink);
            }
            if (MP4) {
                final DownloadLink dlLink = createDownloadlink("http://couch.fernsehkritik.tv/dl/fernsehkritik" + EPISODENUMBER + ".mp4");
                decryptedLinks.add(dlLink);
            }
            if (FLV) {
                final DownloadLink dlLink = createDownloadlink("http://couch.fernsehkritik.tv/userbereich/archive#stream:" + EPISODENUMBER);
                decryptedLinks.add(dlLink);
            }
            if (!MOV && !MP4 && !FLV) {
                ArrayList<DownloadLink> dllinks = getParts(parameter, EPISODENUMBER);
                decryptedLinks.addAll(dllinks);
            }
        } else {
            ArrayList<DownloadLink> dllinks = getParts(parameter, EPISODENUMBER);
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

    @SuppressWarnings("deprecation")
    private ArrayList<DownloadLink> getParts(final String parameter, final String episode) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter + "/play/");
        final String finallink = br.getRegex("type=\"video/mp4\" src=\"(http://[^<>\"]*?\\.mp4)\"").getMatch(0);
        if (finallink == null) {
            return null;
        }
        final String formattedpackagename = getFormattedPackagename(EPISODENUMBER, DATE);
        final DownloadLink dlLink = createDownloadlink("http://fernsehkritik.tv/jdownloaderfolgeneu" + System.currentTimeMillis() + new Random().nextInt(1000000));
        try {
            dlLink.setContentUrl(parameter);
        } catch (final Throwable e) {
            /* Not available in old 0.9.581 Stable */
            dlLink.setBrowserUrl(parameter);
        }
        dlLink.setProperty("directdate", DATE);
        dlLink.setProperty("directepisodenumber", EPISODENUMBER);
        dlLink.setProperty("directtype", ".mp4");
        dlLink.setProperty("directlink", finallink);
        final String formattedFilename = ((jd.plugins.hoster.FernsehkritikTv) HOSTPLUGIN).getFKTVFormattedFilename(dlLink);
        dlLink.setFinalFileName(formattedFilename);
        if (FASTCHECKENABLED) {
            dlLink.setAvailable(true);
        }
        decryptedLinks.add(dlLink);
        return decryptedLinks;
    }

    private String fileExtension(final String arg) {
        String ext = arg.substring(arg.lastIndexOf("."));
        ext = ext == null ? ".flv" : ext;
        return ext;
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