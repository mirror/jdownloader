//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
//along with this program.  If not, see <http://gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class YouTubeCom extends PluginForDecrypt {

    // TODO: Neue Plugins im StreamingShare-Addon eintragen!
    static private String host = "youtube.com";
    static private final Pattern patternswfArgs = Pattern.compile("(.*?swfArgs.*)", Pattern.CASE_INSENSITIVE);
    private static final String PLAYER = "get_video";
    private static final String T = "\"t\"";
    private static final String VIDEO_ID = "video_id";
    private Pattern StreamingShareLink = Pattern.compile("\\< streamingshare=\"youtube\\.com\" name=\"(.*?)\" dlurl=\"(.*?)\" brurl=\"(.*?)\" convertto=\"(.*?)\" comment=\"(.*?)\" \\>", Pattern.CASE_INSENSITIVE);

    static public final Pattern YT_FILENAME = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);

    public YouTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String clean(String s) {
        s = s.replaceAll("\"", "");
        s = s.replaceAll("YouTube -", "");
        s = s.replaceAll("YouTube", "");
        s = s.trim();
        return s;
    }

    private void login(Account account) throws IOException {
        if (account.isEnabled()) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            br.clearCookies("youtube.com");
            br.getPage("http://www.youtube.com/signup?next=/index");
            Form login = br.getFormbyName("loginForm");
            login.put("username", account.getUser());
            login.put("password", account.getPass());
            br.submitForm(login);
            if (!br.getRegex("<title>YouTube - Mein YouTube: " + account.getUser() + "</title>").matches()) {
                logger.severe("Account invalid");
                account.setEnabled(false);
            }
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        Vector<ConversionMode> possibleconverts = new Vector<ConversionMode>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies("youtube.com");
        ArrayList<Account> accounts = JDUtilities.getAccountsForHost("youtube.com");
        if (accounts != null && accounts.size() != 0) login(accounts.get(0));

        try {
            if (StreamingShareLink.matcher(parameter).matches()) {
                // StreamingShareLink

                String[] info = new Regex(parameter, StreamingShareLink).getMatches()[0];

                for (String debug : info) {
                    logger.info(debug);
                }
                DownloadLink thislink = createDownloadlink(info[1]);
                thislink.setBrowserUrl(info[2]);
                thislink.setFinalFileName(info[0]);
                thislink.setSourcePluginComment("Convert to " + (ConversionMode.valueOf(info[3])).GetText());
                thislink.setProperty("convertto", info[3]);

                decryptedLinks.add(thislink);
                return decryptedLinks;
            }

            br.getPage(parameter);
            Form f = br.getForms()[2];
            if (f != null && f.action == null) {
                br.submitForm(f);
            } else {
                if (br.getURL().contains("verify_age?")) { throw new DecrypterException(DecrypterException.ACCOUNT); }
            }
            String video_id = "";
            String t = "";
            String match = br.getRegex(patternswfArgs).getMatch(0);
            if (match == null) { return null; }

            /* DownloadUrl holen */
            String[] lineSub = match.split(",|:");
            for (int i = 0; i < lineSub.length; i++) {
                String s = lineSub[i];
                if (s.indexOf(VIDEO_ID) > -1) {
                    video_id = clean(lineSub[i + 1]);
                }
                if (s.indexOf(T) > -1) {
                    t = clean(lineSub[i + 1]);
                }
            }
            String link = "http://" + host + "/" + PLAYER + "?" + VIDEO_ID + "=" + video_id + "&" + "t=" + t;
            String name = Encoding.htmlDecode(br.getRegex(YT_FILENAME).getMatch(0).trim());
            /* Konvertierungsmöglichkeiten adden */
            boolean gotHD = false;

            if (br.openGetConnection(link + "&fmt=18").getResponseCode() == 200) {
                br.getHttpConnection().disconnect();
                possibleconverts.add(ConversionMode.VIDEOMP4);
            }
            if (br.openGetConnection(link + "&fmt=22").getResponseCode() == 200) {
                br.getHttpConnection().disconnect();
                gotHD = true;
                possibleconverts.add(ConversionMode.VIDEOMP4);
            }
            possibleconverts.add(ConversionMode.VIDEOFLV);
            if (br.openGetConnection(link + "&fmt=13").getResponseCode() == 200) {
                br.getHttpConnection().disconnect();
                possibleconverts.add(ConversionMode.VIDEO3GP);
            }
            possibleconverts.add(ConversionMode.AUDIOMP3);
            possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);

            ConversionMode convertTo = ConvertDialog.DisplayDialog(possibleconverts.toArray(), name);
            if (convertTo != null) {
                if (convertTo == ConvertDialog.ConversionMode.VIDEOMP4) {
                    link += "&fmt=18";
                } else if (convertTo == ConvertDialog.ConversionMode.VIDEO3GP) {
                    link += "&fmt=13";
                }
                DownloadLink thislink = createDownloadlink(link);
                thislink.setBrowserUrl(parameter);
                thislink.setFinalFileName(name + ".tmp");
                thislink.setSourcePluginComment("Convert to " + convertTo.GetText());
                thislink.setProperty("convertto", convertTo.name());
                decryptedLinks.add(thislink);
                if (gotHD && convertTo == ConvertDialog.ConversionMode.VIDEOMP4) {
                    thislink = createDownloadlink(link.replaceAll("&fmt=18", "&fmt=22"));
                    thislink.setBrowserUrl(parameter);
                    thislink.setFinalFileName(name + "(HD)" + ".tmp");
                    thislink.setSourcePluginComment("Convert to " + convertTo.GetText());
                    thislink.setProperty("convertto", convertTo.name());
                    decryptedLinks.add(thislink);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}