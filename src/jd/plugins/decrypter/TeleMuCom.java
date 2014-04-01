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
//
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "telechargementmu.com" }, urls = { "http://(www\\.)?telechargementmu\\.com/.*\\.html|http://feedproxy\\.google\\.com/~r/telechargementmu/.*\\.html" }, flags = { 0 })
public class TeleMuCom extends PluginForDecrypt {

    public TeleMuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.startsWith("http://feedproxy.google.com")) {
            br.getPage(parameter);
            parameter = br.getRedirectLocation();
        }

        br.setFollowRedirects(false);
        // The site needs authentication to works (It seems that the browser
        // cookie does not work, then I add it here)
        br.setCookie(parameter, "dle_user_id", "9756");
        br.setCookie(parameter, "dle_password", "23d45b337ff85d0a326a79082f7c6f50");
        br.setCookie(parameter, "dle_hash", "935743d380448d8e8c1fe36a6078bacd");
        // Cookie for the forum
        br.setCookie(parameter, "SMFCookie46", "a%3A4%3A%7Bi%3A0%3Bs%3A4%3A%224574%22%3Bi%3A1%3Bs%3A40%3A%2233661b384a7c5362c6c15412ae13771babd8de9e%22%3Bi%3A2%3Bi%3A1585034187%3Bi%3A3%3Bi%3A0%3B%7D");

        br.getPage(parameter);
        String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        }

        // On this site, when an accent is set, the result is as extended ASCII
        // character which is bad in the name of the package
        fpName = Encoding.htmlDecode(fpName);
        fpName = RemoveCharacter(fpName);

        if (parameter.contains("forum")) {
            // New web site (Forum)
            if (GiveThankYou(br)) br.getPage(parameter);
            decryptedLinks = GetLinks(br);
        } else {
            // Old web site
            int iNbImage = 0;
            int iLinkImage = 0;
            String[] TabTemp = br.getRegex("<img[^>]+src\\s*=\\s*[\\'\"](http://[^\\'\"]+)\\.jpg[\\'\"][^>]*>").getColumn(0);
            String[] TabImageJpg = new String[TabTemp.length];
            if (TabTemp != null) {
                for (String strImageLink : TabTemp) {
                    if (VerifyImage(strImageLink)) {
                        TabImageJpg[iLinkImage] = strImageLink + ".jpg";
                        iLinkImage++;
                    }
                }
            }
            iNbImage += iLinkImage;

            iLinkImage = 0;
            TabTemp = br.getRegex("<img[^>]+src\\s*=\\s*[\\'\"](http://[^\\'\"]+)\\.jpeg[\\'\"][^>]*>").getColumn(0);
            String[] TabImageJpeg = new String[TabTemp.length];
            if (TabTemp != null) {
                for (String strImageLink : TabTemp) {
                    if (VerifyImage(strImageLink)) {
                        TabImageJpeg[iLinkImage] = strImageLink + ".jpeg";
                        iLinkImage++;
                    }
                }
            }
            iNbImage += iLinkImage;

            iLinkImage = 0;
            TabTemp = br.getRegex("<img[^>]+src\\s*=\\s*[\\'\"](http://[^\\'\"]+)\\.png[\\'\"][^>]*>").getColumn(0);
            String[] TabImagePng = new String[TabTemp.length];
            if (TabTemp != null) {
                for (String strImageLink : TabTemp) {
                    if (VerifyImage(strImageLink)) {
                        TabImagePng[iLinkImage] = strImageLink + ".png";
                        iLinkImage++;
                    }
                }
            }
            iNbImage += iLinkImage;

            String[] TabImage = new String[iNbImage];
            iLinkImage = 0;
            for (String strImageLink : TabImageJpg) {
                if (strImageLink != null) {
                    TabImage[iLinkImage] = strImageLink;
                    iLinkImage++;
                }
            }
            for (String strImageLink : TabImageJpeg) {
                if (strImageLink != null) {
                    TabImage[iLinkImage] = strImageLink;
                    iLinkImage++;
                }
            }
            for (String strImageLink : TabImagePng) {
                if (strImageLink != null) {
                    TabImage[iLinkImage] = strImageLink;
                    iLinkImage++;
                }
            }

            String[] TabPassword = br.getRegex("</a>[ \t]+([A-Z0-9][^ ]*)[ \t]+<a href").getColumn(0);

            // Creation of the array of link that is supported by all plug-in
            final String[] links = br.getRegex("href=\"(.*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            String[] linksCrypted = br.getRegex("\"(http://telechargementmu\\.com/engine/go\\.php\\?url=.*?)\"").getColumn(0);

            // Added links
            for (String directlink : links) {
                decryptedLinks.add(createDownloadlink(directlink));
            }

            // Added crypted links
            for (String redirectlink : linksCrypted) {
                br.getPage(redirectlink);
                String finallink = br.getRedirectLocation();
                if (finallink != null) {
                    if (finallink.toLowerCase().contains("revivelink")) {
                        finallink = finallink + "###" + fpName.trim();
                    }
                    decryptedLinks.add(createDownloadlink(finallink));
                }
            }

            if (TabImage != null) {
                for (String strImageLink : TabImage) {
                    if (strImageLink != null) decryptedLinks.add(createDownloadlink(strImageLink, false));
                }
            }

            for (int i = decryptedLinks.size() - 1; i >= 0; i--) {
                if (decryptedLinks.get(i) == null) {
                    decryptedLinks.remove(i);
                }
            }
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        return createDownloadlink(link, true);
    }

    protected DownloadLink createDownloadlink(String link, Boolean bVerify) {
        if (!link.startsWith("http")) return null;
        link = link.replace("http://www.", "http://");
        if (bVerify && link.startsWith("http://telechargementmu")) return null;

        return super.createDownloadlink(link);
    }

    /**
     * Allows to remove some character to have a nice name
     * 
     * @param strName
     *            The name of the package
     * @return the name of the package normalized.
     */
    private String RemoveCharacter(String strName) {
        String strRemover = "( - )|( · )";
        String[] strTemp = strName.split(strRemover);
        if (strTemp.length >= 2) {
            strName = strTemp[0].trim() + " - " + strTemp[1].trim();
        }

        strName = strName.replace("", "-");
        strName = strName.replace("", "'");
        strName = strName.replace(":", ",");
        strName = strName.replace("/", "&");
        strName = strName.replace("\\", "&");
        strName = strName.replace("’", "'");

        strName = strName.replace("VA - ", "");
        strName = strName.replace("Various Artists - ", "");

        // Remove the exact words mp3, flac, lossless and ape
        strName = ReplacePattern(strName, "(?<!\\p{L})mp3(?!\\p{L})|(?<!\\p{L})ape(?!\\p{L})|(?<!\\p{L})flac(?!\\p{L})|(?<!\\p{L})lossless(?!\\p{L})");

        // Remove the box set information
        strName = ReplacePattern(strName, "- [0-9]+ *CD Box Set");
        strName = ReplacePattern(strName, "Box Set");

        // Remove the number of CD
        strName = ReplacePattern(strName, "[0-9]+ *CD");

        // Replace Vol ou Vol. with 0
        strName = ReplacePattern(strName, "Vol(\\. |\\.| )", "0");

        // Replace the [ ] with nothing because the string in this bracket has been removed
        strName = ReplacePattern(strName, "\\[ *\\]");

        // Replace the ( ) with nothing because the string in this parenthesis has been removed
        strName = ReplacePattern(strName, "^\\[.*\\] ");

        // Replace the ( ) with nothing because the string in this parenthesis has been removed
        strName = ReplacePattern(strName, "\\( *\\)");

        // Replace several space characters in one
        strName = ReplacePattern(strName, "\\s{2,}", " ");

        // Remove the encoding information
        strName = ReplacePattern(strName, "[0-9]+ Kbps itunes");
        strName = ReplacePattern(strName, "[0-9]+ Kbps *&");
        strName = ReplacePattern(strName, "[0-9]+ Kbps\\.");
        strName = ReplacePattern(strName, "[0-9]+ Kbps");
        strName = ReplacePattern(strName, "[0-9]+Kbps");
        strName = ReplacePattern(strName, "320 *&");
        strName = ReplacePattern(strName, "& 320");
        strName = ReplacePattern(strName, "\\s{2,}");
        strName = ReplacePattern(strName, "\\s{2,}");
        strName = ReplacePattern(strName, "\\s{2,}");
        strName = ReplacePattern(strName, "\\s{2,}");

        strName = strName.trim();
        if (strName.endsWith("320")) strName = strName.substring(0, strName.length() - 3);

        // Replace brackets with parenthesis
        strName = strName.replace("[", "(");
        strName = strName.replace("]", ")");

        // Add a space between two parenthesis
        strName = strName.replace(")(", ") (");

        return strName;
    }

    /**
     * Allows to replace text using a regular expression pattern
     * 
     * @param strText
     *            Text to replace
     * @param strPattern
     *            Pattern to use for replacing
     * @return Text replaced
     */
    private String ReplacePattern(String strText, String strPattern) {
        return ReplacePattern(strText, strPattern, "");
    }

    /**
     * Allows to replace text using a regular expression pattern
     * 
     * @param strText
     *            Text to replace
     * @param strPattern
     *            Pattern to use for replacing
     * @param strReplace
     *            Text use to replace
     * @return Text replaced
     */
    private String ReplacePattern(String strText, String strPattern, String strReplace) {
        Pattern pattern = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(strText);
        return matcher.replaceAll(strReplace);
    }

    /**
     * Allows to know if the picture should be downloaded or not
     * 
     * @param strImageLink
     *            The link of the picture
     * @return True if the picture must be downloaded, false otherwise.
     */
    private boolean VerifyImage(String strImageLink) {
        if (strImageLink.contains("img4.hostingpics.net/pics/")) return false;
        if (strImageLink.endsWith("WRI_-")) return false;
        if (strImageLink.endsWith("3A1nr")) return false;
        if (strImageLink.endsWith("bouton-videos")) return false;
        if (strImageLink.endsWith("www.journaldupirate.com/jdp")) return false;
        return true;
    }

    /**
     * Give a thank you and the web page to see the links
     * 
     * @param br
     *            The browser that is connected to the page
     * @return True if a Thank has been given, false otherwise.
     */
    private boolean GiveThankYou(Browser br) {
        String strWebPage = br.toString();

        boolean bGiveThankYou = false;
        String strThankYouURL = "";
        int iPositionStart = strWebPage.toLowerCase().indexOf("<li class=\"thank_you_button\">");
        int iPositionEnd = -1;
        if (iPositionStart != -1) {
            iPositionEnd = strWebPage.toLowerCase().indexOf("</li>", iPositionStart);
            if (iPositionStart != -1 && iPositionEnd != -1) {
                String strTemp = strWebPage.substring(iPositionStart, iPositionEnd);
                iPositionStart = strTemp.toLowerCase().indexOf("href=\"");
                iPositionEnd = strTemp.toLowerCase().indexOf("\"", iPositionStart + 6);
                if (iPositionStart != -1 && iPositionEnd != -1) strThankYouURL = strTemp.substring(iPositionStart + 6, iPositionEnd);
            }
        }

        try {
            if (strThankYouURL != "") {
                br.getPage(strThankYouURL);
                bGiveThankYou = true;
            }
        } catch (Exception ex) {
        }
        return bGiveThankYou;
    }

    /**
     * Get all the links of the page
     * 
     * @param br
     *            The browser that is connected to the page
     * @return An array of links that must be added.
     */
    private ArrayList<DownloadLink> GetLinks(Browser br) {
        ArrayList<DownloadLink> alReturn = new ArrayList<DownloadLink>();
        String strWebPage = br.toString();

        if (strWebPage != "") {
            // Get text part with all links
            String strText = strWebPage;
            int iPositionStart = strText.toLowerCase().indexOf("<h5 id=\"subject_");
            int iPositionEnd = strText.toLowerCase().indexOf("<div class=\"moderatorbar\">", iPositionStart);
            if (iPositionStart != -1 && iPositionEnd != -1) strText = strWebPage.substring(iPositionStart, iPositionEnd);

            iPositionStart = strText.toLowerCase().indexOf("class=\"inner\"");

            // Get images links
            iPositionEnd = -1;
            if (iPositionStart != -1) iPositionEnd = strText.indexOf("img src", iPositionStart);

            if (iPositionEnd != -1) {
                String strTemp = strText.substring(iPositionEnd, strText.length());
                String[] strLinks = strTemp.split("img src=\"");
                for (String strLine : strLinks) {
                    iPositionEnd = strLine.indexOf("\"");
                    if (iPositionEnd != -1) alReturn.add(createDownloadlink(strLine.substring(0, iPositionEnd), false));
                }
            }

            // Get download links
            iPositionEnd = -1;
            if (iPositionStart != -1) iPositionEnd = strText.indexOf("href", iPositionStart);

            if (iPositionEnd != -1) {
                String strTemp = strText.substring(iPositionEnd, strText.length());
                String[] strLinks = strTemp.split("href=\"");
                for (String strLine : strLinks) {
                    iPositionEnd = strLine.indexOf("\"");
                    if (iPositionEnd != -1) alReturn.add(createDownloadlink(strLine.substring(0, iPositionEnd), false));
                }
            }
        }
        return alReturn;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}