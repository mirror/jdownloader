package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ted.com" }, urls = { "http://(www\\.)?ted.com/talks/(lang/[a-zA-Z\\-]+/)?\\w+\\.html" }, flags = { 0 })
public class TedCom extends PluginForDecrypt {

    public TedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CHECKFAST_VIDEOS                   = "CHECKFAST_VIDEOS";
    private static final String CHECKFAST_MP3                      = "CHECKFAST_MP3";
    private static final String CHECKFAST_SUBTITLES                = "CHECKFAST_SUBTITLES";

    private static final String GRAB_VIDEO_BEST                    = "GRAB_VIDEO_BEST";
    private static final String GRAB_VIDEO_LOWRES                  = "GRAB_VIDEO_LOWRES";
    private static final String GRAB_VIDEO_STANDARDRES             = "GRAB_VIDEO_STANDARDRES";
    private static final String GRAB_VIDEO_HIGHRES                 = "GRAB_VIDEO_HIGHRES";

    private static final String GRAB_MP3                           = "GRAB_MP3";
    private static final String GRAB_ALL_AVAILABLE_SUBTITLES       = "GRAB_ALL_AVAILABLE_SUBTITLES";
    private static final String GRAB_SUBTITLE_ALBANIAN             = "GRAB_SUBTITLE_ALBANIAN";
    private static final String GRAB_SUBTITLE_ARABIC               = "GRAB_SUBTITLE_ARABIC";
    private static final String GRAB_SUBTITLE_ARMENIAN             = "GRAB_SUBTITLE_ARMENIAN";
    private static final String GRAB_SUBTITLE_AZERBAIJANI          = "GRAB_SUBTITLE_AZERBAIJANI";
    private static final String GRAB_SUBTITLE_BENGALI              = "GRAB_SUBTITLE_BENGALI";
    private static final String GRAB_SUBTITLE_BULGARIAN            = "GRAB_SUBTITLE_BULGARIAN";
    private static final String GRAB_SUBTITLE_CHINESE_SIMPLIFIED   = "GRAB_SUBTITLE_CHINESE_SIMPLIFIED";
    private static final String GRAB_SUBTITLE_CHINESE_TRADITIONAL  = "GRAB_SUBTITLE_CHINESE_TRADITIONAL";
    private static final String GRAB_SUBTITLE_CROATIAN             = "GRAB_SUBTITLE_CROATIAN";
    private static final String GRAB_SUBTITLE_CZECH                = "GRAB_SUBTITLE_CZECH";
    private static final String GRAB_SUBTITLE_DANISH               = "GRAB_SUBTITLE_DANISH";
    private static final String GRAB_SUBTITLE_DUTCH                = "GRAB_SUBTITLE_DUTCH";
    private static final String GRAB_SUBTITLE_ENGLISH              = "GRAB_SUBTITLE_ENGLISH";
    private static final String GRAB_SUBTITLE_ESTONIAN             = "GRAB_SUBTITLE_ESTONIAN";
    private static final String GRAB_SUBTITLE_FINNISH              = "GRAB_SUBTITLE_FINNISH";
    private static final String GRAB_SUBTITLE_FRENCH               = "GRAB_SUBTITLE_FRENCH";
    private static final String GRAB_SUBTITLE_GEORGIAN             = "GRAB_SUBTITLE_GEORGIAN";
    private static final String GRAB_SUBTITLE_GERMAN               = "GRAB_SUBTITLE_GERMAN";
    private static final String GRAB_SUBTITLE_GREEK                = "GRAB_SUBTITLE_GREEK";
    private static final String GRAB_SUBTITLE_HEBREW               = "GRAB_SUBTITLE_HEBREW";
    private static final String GRAB_SUBTITLE_HUNGARIAN            = "GRAB_SUBTITLE_HUNGARIAN";
    private static final String GRAB_SUBTITLE_INDONESIAN           = "GRAB_SUBTITLE_INDONESIAN";
    private static final String GRAB_SUBTITLE_ITALIAN              = "GRAB_SUBTITLE_ITALIAN";
    private static final String GRAB_SUBTITLE_JAPANESE             = "GRAB_SUBTITLE_JAPANESE";
    private static final String GRAB_SUBTITLE_KOREAN               = "GRAB_SUBTITLE_KOREAN";
    private static final String GRAB_SUBTITLE_KURDISH              = "GRAB_SUBTITLE_KURDISH";
    private static final String GRAB_SUBTITLE_LITHUANIAN           = "GRAB_SUBTITLE_LITHUANIAN";
    private static final String GRAB_SUBTITLE_MACEDONIAN           = "GRAB_SUBTITLE_MACEDONIAN";
    private static final String GRAB_SUBTITLE_MALAY                = "GRAB_SUBTITLE_MALAY";
    private static final String GRAB_SUBTITLE_NORWEGIAN_BOKMAL     = "GRAB_SUBTITLE_NORWEGIAN_BOKMAL";
    private static final String GRAB_SUBTITLE_PERSIAN              = "GRAB_SUBTITLE_PERSIAN";
    private static final String GRAB_SUBTITLE_POLISH               = "GRAB_SUBTITLE_POLISH";
    private static final String GRAB_SUBTITLE_PORTUGUESE           = "GRAB_SUBTITLE_PORTUGUESE";
    private static final String GRAB_SUBTITLE_PORTUGUESE_BRAZILIAN = "GRAB_SUBTITLE_PORTUGUESE_BRAZILIAN";
    private static final String GRAB_SUBTITLE_ROMANIAN             = "GRAB_SUBTITLE_ROMANIAN";
    private static final String GRAB_SUBTITLE_RUSSIAN              = "GRAB_SUBTITLE_RUSSIAN";
    private static final String GRAB_SUBTITLE_SERBIAN              = "GRAB_SUBTITLE_SERBIAN";
    private static final String GRAB_SUBTITLE_SLOVAK               = "GRAB_SUBTITLE_SLOVAK";
    private static final String GRAB_SUBTITLE_SLOVENIAN            = "GRAB_SUBTITLE_SLOVENIAN";
    private static final String GRAB_SUBTITLE_SPANISH              = "GRAB_SUBTITLE_SPANISH";
    private static final String GRAB_SUBTITLE_SWEDISH              = "GRAB_SUBTITLE_SWEDISH";
    private static final String GRAB_SUBTITLE_THAI                 = "GRAB_SUBTITLE_THAI";
    private static final String GRAB_SUBTITLE_TURKISH              = "GRAB_SUBTITLE_TURKISH";
    private static final String GRAB_SUBTITLE_UKRAINIAN            = "GRAB_SUBTITLE_UKRAINIAN";
    private static final String GRAB_SUBTITLE_VIETNAMESE           = "GRAB_SUBTITLE_VIETNAMESE";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.toString();
        br.getPage(url);
        /** Look for external links */
        String externalLink = br.getRegex("class=\"external\" href=\"(http://(www\\.)?youtube\\.com/[^<>\"]*?)\"").getMatch(0);
        if (externalLink != null) {
            decryptedLinks.add(createDownloadlink(externalLink));
            return decryptedLinks;
        }

        // This is needed later for the subtitle decrypter
        final String subtitleText = br.getRegex("<select name=\"languageCode\" id=\"languageCode\"><option value=\"\">Show transcript</option>(.*?)</select>").getMatch(0);
        final String tedID = br.getRegex("\"id\":(\\d+)").getMatch(0);

        /** Decrypt video */
        final SubConfiguration cfg = SubConfiguration.getConfig("ted.com");
        String talkInfo = br.getRegex(">var talkDetails = \\{(.*?)<div class=\"talk\\-wrapper\">").getMatch(0);
        talkInfo = Encoding.htmlDecode(talkInfo).replace("\\", "");
        if (talkInfo == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String title = br.getRegex("<span id=\"altHeadline\" >([^<>\"]*?)</span>").getMatch(0);
        String plainfilename = br.getRegex("\"http://download\\.ted\\.com/talks/([^<>\"]*?)\\.mp4([^<>\"]+)?\">download the video</a>").getMatch(0);
        if (plainfilename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        plainfilename = Encoding.htmlDecode(plainfilename.trim());
        if (title == null) title = plainfilename;
        title = Encoding.htmlDecode(title);

        /** Decrypt video */
        br.getPage("http://www.ted.com/download/links/slug/" + plainfilename + "/type/talks/ext/mp4");
        final String[][] videoQualities = { { "480p", "-480p", "high-resolution" }, { "regular", "", "standard-resolution" }, { "light", "-light", "low-resolution" } };
        final LinkedHashMap<String, String> foundVideoLinks = new LinkedHashMap();
        final ArrayList<String> selectedVideoQualities = new ArrayList<String>();
        for (final String[] videoQuality : videoQualities) {
            final String dataName = videoQuality[0];
            final String qualityValue = videoQuality[1];
            final String qualityKey = videoQuality[2];
            if (br.containsHTML("data\\-name=\"" + dataName + "\" value=\"" + qualityValue + "\"")) {
                final String finallink = "http://download.ted.com/talks/" + plainfilename + qualityValue + ".mp4?apikey=TEDDOWNLOAD";
                foundVideoLinks.put(qualityKey, finallink);
            }
        }
        // Collect selected qualities
        if (cfg.getBooleanProperty(GRAB_VIDEO_BEST, false)) {
            final ArrayList<String> list = new ArrayList<String>(foundVideoLinks.keySet());
            final String highestAvailableQualityValue = list.get(0);
            selectedVideoQualities.add(highestAvailableQualityValue);
        } else {
            boolean qLow = cfg.getBooleanProperty(GRAB_VIDEO_LOWRES, false);
            boolean qStandard = cfg.getBooleanProperty(GRAB_VIDEO_STANDARDRES, false);
            boolean qHigh = cfg.getBooleanProperty(GRAB_VIDEO_HIGHRES, false);
            if (!qLow && !qStandard && !qHigh) {
                qLow = true;
                qStandard = true;
                qHigh = true;
            }
            if (qLow) selectedVideoQualities.add("low-resolution");
            if (qStandard) selectedVideoQualities.add("standard-resolution");
            if (qHigh) selectedVideoQualities.add("high-resolution");
        }
        for (final String selectedVideoQuality : selectedVideoQualities) {
            final String finallink = foundVideoLinks.get(selectedVideoQuality);
            if (selectedVideoQuality != null) {
                final DownloadLink dl = createDownloadlink("decrypted://decryptedtedcom.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String finalName = title + "_video_" + selectedVideoQuality + ".mp4";
                dl.setFinalFileName(finalName);
                dl.setProperty("finalfilename", finalName);
                dl.setProperty("directlink", finallink);
                dl.setProperty("type", "video");
                dl.setProperty("selectedvideoquality", selectedVideoQuality);
                if (cfg.getBooleanProperty(CHECKFAST_VIDEOS, false)) dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }

        /** Decrypt mp3 */
        final String dlMP3 = br.getRegex("<dt><a href=\"(http://download\\.ted\\.com/talks/[^<>\"]*?)\">Download to desktop \\(MP3\\)<").getMatch(0);
        if (dlMP3 != null && cfg.getBooleanProperty(GRAB_MP3, false)) {
            final DownloadLink dl = createDownloadlink("decrypted://decryptedtedcom.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            final String finalName = title + "_mp3.mp3";
            dl.setFinalFileName(finalName);
            dl.setProperty("finalfilename", finalName);
            if (cfg.getBooleanProperty(CHECKFAST_MP3, false)) dl.setAvailable(true);
            dl.setProperty("directlink", dlMP3);
            dl.setProperty("type", "mp3");
            decryptedLinks.add(dl);
        }

        /** Decrypt subtitles */
        boolean notFinished = true;
        if (subtitleText != null && tedID != null && !notFinished) {
            final String[][] allSubtitleValues = { { "sq", "Albanian" }, { "ar", "Arabic" }, { "hy", "Armenian" }, { "az", "Azerbaijani" }, { "bn", "Bengali" }, { "bg", "Bulgarian" } };
            final ArrayList<String[]> selectedSubtitles = new ArrayList<String[]>();
            final String[] availableSubtitles = new Regex(subtitleText, "value=\"([a-z\\-]{2,5})\">").getColumn(0);
            final LinkedHashMap<String, String> foundSubtitles = new LinkedHashMap();
            for (final String currentSubtitle : availableSubtitles) {
                foundSubtitles.put(currentSubtitle, "http://www.ted.com/talks/subtitles/id/" + tedID + "/lang/" + currentSubtitle + "/format/srt");
            }
            if (cfg.getBooleanProperty(GRAB_ALL_AVAILABLE_SUBTITLES, false)) {
                for (final String[] subtitleValue : allSubtitleValues) {
                    selectedSubtitles.add(subtitleValue);
                }
            } else {
                if (cfg.getBooleanProperty(GRAB_SUBTITLE_ALBANIAN, false)) selectedSubtitles.add(new String[] { "sq", "Albanian" });
                if (cfg.getBooleanProperty(GRAB_SUBTITLE_ARABIC, false)) selectedSubtitles.add(new String[] { "ar", "Arabic" });
                if (cfg.getBooleanProperty(GRAB_SUBTITLE_ARMENIAN, false)) selectedSubtitles.add(new String[] { "hy", "Armenian" });
                if (cfg.getBooleanProperty(GRAB_SUBTITLE_AZERBAIJANI, false)) selectedSubtitles.add(new String[] { "az", "Azerbaijani" });
                if (cfg.getBooleanProperty(GRAB_SUBTITLE_BENGALI, false)) selectedSubtitles.add(new String[] { "bn", "Bengali" });
                if (cfg.getBooleanProperty(GRAB_SUBTITLE_BULGARIAN, false)) selectedSubtitles.add(new String[] { "bg", "Bulgarian" });
            }
            // Find available qualities and add them to the decrypted links
            for (final String[] selectedSubtitle : selectedSubtitles) {
                final String foundSubtitleDirectLink = foundSubtitles.get(selectedSubtitle[0]);
                if (foundSubtitleDirectLink != null) {
                    final String subtitleName = selectedSubtitle[1];
                    final DownloadLink dl = createDownloadlink("decrypted://decryptedtedcom.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                    final String finalName = title + "_subtitle_" + subtitleName + ".srt";
                    dl.setFinalFileName(finalName);
                    dl.setProperty("finalfilename", finalName);
                    dl.setProperty("directlink", foundSubtitleDirectLink);
                    dl.setProperty("type", "subtitle");
                    if (cfg.getBooleanProperty(CHECKFAST_SUBTITLES, false)) dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
            }

        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}