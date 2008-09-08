//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RbaDe extends PluginForDecrypt {
    private static final String HOST = "r-b-a.de";
    private static final String CODER = "ToKaM";
    // http://www.r-b-a.de/index.php?ID=4101&BATTLE=27505&sid=
    // 6ecea8d124eb0cc28d4c101c7b848ffc
    private static final Pattern BATTLE_REL_PATH = Pattern.compile("(index\\.php\\?ID=4101&(amp;)?BATTLE=\\d+(&sid=\\w+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_BATTLE = Pattern.compile("http://[\\w\\.]*?r-b-a\\.de/" + BATTLE_REL_PATH, Pattern.CASE_INSENSITIVE);
    // http://www.r-b-a.de/index.php?ID=4100&direction=last&MEMBER=2931&sid=
    // 6ecea8d124eb0cc28d4c101c7b848ffc
    private static final Pattern PATTERN_SUPPORTED_USER = Pattern.compile("http://[\\w\\.]*?r-b-a\\.de/index\\.php\\?ID=4100(&direction=last)?&MEMBER=\\d+(&sid=\\w+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED = Pattern.compile(PATTERN_SUPPORTED_BATTLE.pattern() + "|" + PATTERN_SUPPORTED_USER.pattern(), Pattern.CASE_INSENSITIVE);

    private static final Pattern REGEX_DOWNLOADLINK = Pattern.compile("(download\\.php\\?FILE=(\\d+)-(\\d)\\.mp3&(amp;)?PATH=\\d)");

    // TODO
    // //Keys für die Config
    // /** Configuarations-Propperty: Gibt an ob alle Downloads in
    // downloads/RBA-BATTELS/ gespeichert werden sollen */
    // private static final String ENABLE_SUBFOLDERS1 =
    // "RBA_ENABLE_SUBFOLDERS1";
    // /** Configuarations-Propperty: Gibt an ob für die mp3s eines Artisten ein
    // zusätzlicher Ordner erstellt werden soll */
    // private static final String ENABLE_SUBFOLDERS2 =
    // "RBA_ENABLE_SUBFOLDERS2";
    // /** Configuarations-Propperty: Gibt an ob für Dateinamen
    // "[myspaceusername]-filename" und das genannte prefix hinzugefügt wird */
    // private static final String SUBFOLDER1_PATH = "RBA_SUBFOLDER1_PATH";
    // private void setConfigElements() {
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
    // getPluginConfig(), ENABLE_SUBFOLDERS1,
    // JDLocale.L("plugins.decrypt.RbaDe0",
    // "Alle Downloads in einem übergeordneten Verzeichnis speichern"
    // )).setDefaultValue(true));
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
    // getPluginConfig(), ENABLE_SUBFOLDERS2,
    // JDLocale.L("plugins.decrypt.RbaDe1",
    // "Für jedes Battel einen eigenen Ordner anlegen"
    // )).setDefaultValue(false));
    // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD,
    // getPluginConfig(), SUBFOLDER1_PATH, JDLocale.L("plugins.decrypt.RbaDe2",
    // "Name des übergeordneten Verzeichnises")).setDefaultValue(this.));
    //
    // }

    public RbaDe() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Browser br = new Browser();
        br.clearCookies(getHost());
        String page = br.getPage(cryptedLink.getCryptedUrl());
        if (new Regex(cryptedLink, PATTERN_SUPPORTED_BATTLE).matches()) {
            String links[] = new Regex(page, REGEX_DOWNLOADLINK).getColumn(0);
            for (String link : links) {
                decryptedLinks.add(createDownloadlink("http://www.r-b-a.de/" + link));
            }
            return decryptedLinks;
        } else if (new Regex(cryptedLink, PATTERN_SUPPORTED_USER).matches()) {
            String links[] = new Regex(page, BATTLE_REL_PATH).getColumn(0);
            for (String link : links) {
                DownloadLink dl_link = createDownloadlink("http://www.r-b-a.de/" + link);
                decryptedLinks.add(dl_link);
                logger.info(link);
            }
            return decryptedLinks;
        }

        return null;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PATTERN_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
