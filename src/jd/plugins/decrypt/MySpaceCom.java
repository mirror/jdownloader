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

package jd.plugins.decrypt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class MySpaceCom extends PluginForDecrypt {
    private static final String HOST = "myspace.com";
    private static final String CODER = "ToKaM";
    // private static final Pattern PATTERN_SUPPORTED = Pattern.compile(
    // "http://[\\w\\.]*?myspace\\.(com|de)/index\\.cfm\\?fuseaction=user\\.viewprofile&friendid=(\\d+)"
    // , Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED = Pattern.compile("http://[\\w\\.]*?myspace\\.(com|de)/.+", Pattern.CASE_INSENSITIVE);

    // private static final String ANY_ATTRIBUTE = "[^\"]+";
    private static final String ANY_URL = "[\\w~\\.\\!\\*'\\(\\);:@&=\\+$,/\\?%#\\[\\]-]*?";

    // private static final String DOWNLOAD_INFORMATIONS =
    // "<song bsid=\"\\d+\" title=\"("
    // +ANY_ATTRIBUTE+")\" songid=\"\\d+\" plays=\"\\d+\" comments=\""
    // +ANY_ATTRIBUTE+"\" rate=\""+ANY_ATTRIBUTE+"\" downloadable=\"("+ANY_URL+
    // ")\" imagename=\""
    // +ANY_URL+"\" imagedesc=\""+ANY_ATTRIBUTE+"\" filename=\""
    // +ANY_ATTRIBUTE+"\" url=\""
    // +ANY_URL+"\" durl=\"("+ANY_URL+")\" token=\""+ANY_ATTRIBUTE
    // +"\" curl=\""+ANY_URL+"\"/>";

    // Regex für XML Attribute
    private static final String TITEL = "title=\"([^\"]+)\"";
    private static final String DURLS = "durl=\"(" + ANY_URL + ")\"";
    private static final String TITEL_MUSICPLAYLIST_US = "<annotation>(.+?)</annotation>";
    private static final String DURLS_MUSICPLAYLIST_US = "<originallocation>(" + ANY_URL + ")</originallocation>";
    private static final String TITEL_MP3_ASSEST = "name=\"([^\"]+)\"";
    private static final String DURLS_MP3_ASSEST = "path=\"([^\"]+)\"";

    // Pattern.compile("<title>$\\s+?^\\s+?.+?www.myspace.com/(.+)$",Pattern.
    // MULTILINE);
    // private static final String MYSPACEURL =
    // "<td><div align=\"left\">&nbsp;&nbsp;<span class=\"searchMonkey-displayURL\">http://www.myspace.com/.+?</span>&nbsp;&nbsp;</div></td>"
    // ;

    // Benötigt das Flag Pattern.MULTILINE
    private static final String NICK_NAME = "<title>$\\s+?^\\s+?.+?www.myspace.com/(.+)$";

    private static final String FLASH_PLAYER_MINI = "\"http://lads\\.myspace\\.com/mini/mini.swf\\?b=.+?\"";

    private static final String FLASH_PLAYER_MUSIC = "\"http://lads\\.myspace\\.com/music/musicplayer.swf\\?n=.+?\"";

    private static final String FLASH_PLAYER_MUSICPLAYLIST_US = "http://www\\.musicplaylist\\.us/loadplaylist\\.php\\?playlist=\\d+";

    private static final String FLASH_PLAYER_MP3_ASSET_COM = "(?s)http://www\\.mp3asset\\.com/swf/mp3/myflashfetish-mp3-player\\.swf.+?<param name=\"flashvars\" value=\"myid=\\d+&path=\\d+/\\d+/\\d+&";

    private static final Pattern PATTERN_PAGE_INFOS = Pattern.compile("(" + NICK_NAME + "|" + FLASH_PLAYER_MINI + "|" + FLASH_PLAYER_MUSIC + "|" + FLASH_PLAYER_MUSICPLAYLIST_US + "|" + FLASH_PLAYER_MP3_ASSET_COM + ")", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    // Weiterverarbeitung der Matches
    private static final String FLASH_PLAYER_MP3_ASSET_COM_UID = "<param name=\"flashvars\" value=\"myid=(\\d+)&path=(\\d+/\\d+/\\d+)&";

    // Keys für die Config
    /**
     * Configuarations-Property: Gibt an ob alle Downloads in
     * downloads/myspace.com/ gespeichert werden sollen
     */
    private static final String ENABLE_SUBFOLDERS1 = "MYSPACE_ENABLE_SUBFOLDERS1";
    /**
     * Configuarations-Property: Gibt an ob für die mp3s eines Artisten ein
     * zusätzlicher Ordner erstellt werden soll
     */
    private static final String ENABLE_SUBFOLDERS2 = "MYSPACE_ENABLE_SUBFOLDERS2";
    /**
     * Configuarations-Property: Gibt an ob für Dateinamen
     * "[myspaceusername]-filename" und das genannte prefix hinzugefügt wird
     */
    private static final String ENABLE_DL_NAME_MODIFICATION = "MYSPACE_ENABLE_DL_NAME_MODIFICATION";

    public MySpaceCom(String cfgName) {
        super(cfgName);
        setConfigElements();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        // Ermittle nickname und playerurl
        br.getPage(cryptedLink.getCryptedUrl());
        String[][] matches = br.getRegex(PATTERN_PAGE_INFOS).getMatches();
        String nick = "";
        String playerUrl = null;
        for (int i = 0; i < matches.length && i < 2; i++) {
            nick = i == 0 ? matches[i][1] : nick;
            playerUrl = i == 1 ? matches[i][0] : playerUrl;
        }
        String[] titel = null;
        String[] dUrls = null;
        // HandlePlayers
        if (playerUrl == null) {
            // TODO err msg player nicht gefunden!
            logger.severe("player nicht gefunden!");
            return null;
        } else if (new Regex(playerUrl, FLASH_PLAYER_MINI, Pattern.CASE_INSENSITIVE).matches()) {
            String strUserId = new Regex(playerUrl, "\\&o=(.+?)(\\&|\")", Pattern.CASE_INSENSITIVE).getMatch(0);
            String[][] data = parseXmlStandartPlayer(Encoding.Base64Decode(strUserId));
            titel = data[0];
            dUrls = data[1];
        } else if (new Regex(playerUrl, FLASH_PLAYER_MUSIC, Pattern.CASE_INSENSITIVE).matches()) {
            String strUserId = new Regex(playerUrl, "\\&d=(.+?)(\\&|\")", Pattern.CASE_INSENSITIVE).getMatch(0);
            String[][] data = parseXmlStandartPlayer(new Regex(Encoding.Base64Decode(strUserId), "(\\d+)?\\^", Pattern.CASE_INSENSITIVE).getMatch(0));
            titel = data[0];
            dUrls = data[1];
        } else if (new Regex(playerUrl, FLASH_PLAYER_MUSICPLAYLIST_US, Pattern.CASE_INSENSITIVE).matches()) {
            String[][] data = parseXmlMusicPlayListUs(playerUrl);
            titel = data[0];
            dUrls = data[1];
        } else if (new Regex(playerUrl, FLASH_PLAYER_MP3_ASSET_COM, Pattern.CASE_INSENSITIVE).matches()) {
            Regex reg = new Regex(playerUrl, FLASH_PLAYER_MP3_ASSET_COM_UID, Pattern.CASE_INSENSITIVE);
            String userId = reg.getColumn(0).length > 0 ? reg.getColumn(0)[0] : "";
            String path = reg.getColumn(1).length > 0 ? reg.getColumn(1)[0] : "";
            String[][] data = parseXmlMusicPlayerMp3Asset(path, userId);
            titel = data[0];
            dUrls = data[1];
        } else {
            // TODO err msg player nicht erkannt!
            logger.severe("player nicht gefunden!");
            return null;
        }

        if (titel.length != dUrls.length) logger.warning("Fehler Anzahl-Titel und Anzahl-Downloads stimmen nicht überein!");
        // Erstelle DownloadLinks
        FilePackage filePackage = new FilePackage();
        filePackage.setName("myspace.com");
        for (int i = 0; i < titel.length; i++) {
            String einTitel = titel[i];
            String link = dUrls[i];
            DownloadLink dl_link = createDownloadlink("myspace://" + link);

            if (getPluginConfig().getBooleanProperty(ENABLE_SUBFOLDERS1)) {
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false) == false) dl_link.addSubdirectory("myspace.com");
            }
            if (getPluginConfig().getBooleanProperty(ENABLE_SUBFOLDERS2)) {
                dl_link.addSubdirectory(nick);
            }
            if (getPluginConfig().getBooleanProperty(ENABLE_DL_NAME_MODIFICATION)) {
                dl_link.setStaticFileName(nick + "_" + einTitel + ".mp3");
                dl_link.setName(nick + "_" + einTitel + ".mp3");
            } else {
                dl_link.setStaticFileName(einTitel + ".mp3");
                dl_link.setName(einTitel + ".mp3");
            }
            dl_link.setName(einTitel);
            dl_link.setFilePackage(filePackage);
            dl_link.setBrowserUrl(cryptedLink.toString());
            decryptedLinks.add(dl_link);
        }
        return decryptedLinks;
    }

    private String[][] parseXmlStandartPlayer(String userId) throws IOException {
        String page = br.getPage("http://www.myspace.com//services/media/musicplayerxml.ashx?b=" + userId);
        String[][] ret = new String[2][];
        // Titel
        ret[0] = new Regex(page, TITEL, Pattern.CASE_INSENSITIVE).getColumn(0);
        // Durls
        ret[1] = new Regex(page, DURLS, Pattern.CASE_INSENSITIVE).getColumn(0);
        return ret;
    }

    private String[][] parseXmlMusicPlayListUs(String url) throws IOException {
        br.setFollowRedirects(true);
        String page = br.getPage(url);
        String[][] ret = new String[2][];
        // Titel
        ret[0] = new Regex(page, TITEL_MUSICPLAYLIST_US, Pattern.CASE_INSENSITIVE).getColumn(0);
        // Durls
        ret[1] = new Regex(page, DURLS_MUSICPLAYLIST_US, Pattern.CASE_INSENSITIVE).getColumn(0);
        return ret;
    }

    private String[][] parseXmlMusicPlayerMp3Asset(String path, String userid) throws IOException {
        String[][] ret = new String[2][];
        String page = br.getPage("http://www.mp3asset.com/xml/" + path + "/" + userid + ".xml");
        // Titel
        ret[0] = new Regex(page, TITEL_MP3_ASSEST, Pattern.CASE_INSENSITIVE).getColumn(0);
        // Durls
        ret[1] = new Regex(page, DURLS_MP3_ASSEST, Pattern.CASE_INSENSITIVE).getColumn(0);
        return ret;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_SUBFOLDERS1, JDLocale.L("plugins.decrypt.myspacecom0", "Load all downloads into \"myspace.com/\"")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_SUBFOLDERS2, JDLocale.L("plugins.decrypt.myspacecom1", "Create a subfolder for each artist")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_DL_NAME_MODIFICATION, JDLocale.L("plugins.decrypt.myspacecom2", "Put the myspace username as prefix to the filename of all downloads")).setDefaultValue(true));
    }
}
