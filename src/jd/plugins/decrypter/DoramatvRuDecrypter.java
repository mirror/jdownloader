/**
 * 
 */
package jd.plugins.decrypter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

/**
 * @author Steven Kuenzel
 * 
 */
@SuppressWarnings("deprecation")
@DecrypterPlugin(names = { "doramatv.ru" }, urls = { "http://(www\\.)?doramatv\\.ru/(.)*" }, flags = { 0 }, interfaceVersion = 2, revision = "")
public class DoramatvRuDecrypter extends PluginForDecrypt {

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForDecrypt#decryptIt(jd.plugins.CryptedLink, jd.controlling.ProgressController)
     */
    // @ whoever did this plugin: You don't need to handle the vkontakte links inside here. JD has direct support for those:
    // http://vk.com/video_ext.php?oid=-5703512&id=163133724&hash=542ece23ed62384b&
    // You can even configure the qualities in the vk.com plugin settings
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String link = parameter.getCryptedUrl();
        String seriesTitle = link.split("/")[link.split("/").length - 1];

        if (link.contains("series")) {
            // it is only a single episode

            decryptedLinks.addAll(findDownloadLinksForEpisode(link));
        } else {
            // it is a whole series

            try {
                br.getPage(link);

            } catch (IOException e) {
                e.printStackTrace(System.err);
            }

            int i = 0;
            while (true) {
                i++;
                Regex r = br.getRegex(seriesTitle + "/series" + i);
                if (r.count() > 0) {

                    decryptedLinks.addAll(findDownloadLinksForEpisode(link + "/series" + i));

                } else {
                    break;
                }
            }

        }

        return decryptedLinks;
    }

    private String title;
    private String extension = ".mp4";

    /**
     * Finds all available download links (-> resolutions) for a specific episode
     * 
     * @param url
     *            The URL to the episode
     * @return List of available DownloadLink
     * @throws Exception
     *             Any exception
     */
    private ArrayList<DownloadLink> findDownloadLinksForEpisode(String url) throws Exception {
        String[] urlParts = url.replace("http://", "").split("/");
        String resolution = "";

        if (urlParts.length == 3) {
            title = getTitle(urlParts[1] + "_episode_" + urlParts[2].replace("series", ""));
        }

        Browser browser2 = br.cloneBrowser();
        // load page
        browser2.getPage(url);

        HashMap<String, Boolean> linkMap = new HashMap<String, Boolean>();

        // Озвучка

        // // try to find any matches

        // define the standard source regex
        String regexString = "<input type=\"hidden\" name=\"embed_source\" class=\"embed_source\" value=\".{0,12}src=&quot;(.*?)&quot;.{0,200}id=\"embed_source\" />";

        // first try to find those video links which are respoken in Russian language
        Regex regexSound = browser2.getRegex("Озвучка.{0,1000}" + regexString);

        String[][] regexMatchesSound = regexSound.getMatches();

        for (int i = 0; i < regexSound.count(); i++) {
            String match = regexMatchesSound[i][0];
            // decode the link into "ordinary" HTML
            match = Encoding.htmlDecode(match);

            // check the real host of the video
            if (match.contains("vk.com")) {
                // vk.com

                linkMap.put(match, true);
            }
        }

        // now try to find those video links which do only have Russian subtitles
        Regex regex = browser2.getRegex(regexString);

        String[][] regexMatches = regex.getMatches();

        for (int i = 0; i < regex.count(); i++) {
            String match = regexMatches[i][0];

            // decode the link into "ordinary" HTML
            match = Encoding.htmlDecode(match);

            // check the real host of the video
            if (match.contains("vk.com")) {
                // vk.com

                if (!linkMap.containsKey(match)) linkMap.put(match, false);
            }

        }

        // nothing was found
        if (linkMap.size() < 1) return null;

        // sort the map containing the links
        linkMap = sortByValue(linkMap);

        ArrayList<DownloadLink> finalLinks = new ArrayList<DownloadLink>();

        /**
         * Determines whether a video in the given resolution was found: {720p, 480p, 360p, 240p}
         */
        boolean[] foundResolution = new boolean[] { false, false, false, false };
        Browser browser3;
        for (String match : linkMap.keySet()) {
            browser3 = br.cloneBrowser();
            browser3.getPage(match);

            Regex regex720 = foundResolution[0] ? null : browser3.getRegex("\"url720\":\"(.*?)\"");
            Regex regex480 = foundResolution[1] ? null : browser3.getRegex("\"url480\":\"(.*?)\"");
            Regex regex360 = foundResolution[2] ? null : browser3.getRegex("\"url360\":\"(.*?)\"");
            Regex regex240 = foundResolution[3] ? null : browser3.getRegex("\"url240\":\"(.*?)\"");

            DownloadLink result;

            if (!foundResolution[0] && regex720.count() > 0) {
                resolution = "720";

                result = findDownloadLink(regex720.getMatches(), resolution, linkMap.get(match));

                if (result != null) {
                    finalLinks.add(result);

                    foundResolution[0] = true;
                }

            }
            if (!foundResolution[1] && regex480.count() > 0) {
                resolution = "480";

                result = findDownloadLink(regex480.getMatches(), resolution, linkMap.get(match));

                if (result != null) {
                    finalLinks.add(result);

                    foundResolution[1] = true;
                }
            }
            if (!foundResolution[2] && regex360.count() > 0) {
                resolution = "360";

                result = findDownloadLink(regex360.getMatches(), resolution, linkMap.get(match));

                if (result != null) {
                    finalLinks.add(result);

                    foundResolution[2] = true;
                }
            }
            if (!foundResolution[3] && regex240.count() > 0) {
                resolution = "240";

                result = findDownloadLink(regex240.getMatches(), resolution, linkMap.get(match));

                if (result != null) {
                    finalLinks.add(result);

                    foundResolution[3] = true;
                }
            }

            if (foundResolution[0] & foundResolution[1] & foundResolution[2] & foundResolution[3]) break;
        }

        // create a new FilePackage for the episode
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title.replace("_", " ").trim());
        fp.addLinks(finalLinks);

        return finalLinks;
    }

    /**
     * Finds the download link for a specific episode in a specific resolution
     * 
     * @param regexMatches
     *            The results of the regular expression checking the containing web page
     * @param resolution
     *            The resolution of the video file (e.g. "480" or "720")
     * @return A valid DownloadLink with adapted FinalFileName
     */
    private DownloadLink findDownloadLink(String[][] regexMatches, String resolution, boolean russianSound) {

        List<String> finalList = new ArrayList<String>();

        for (int i = 0; i < Array.getLength(regexMatches); i++) {
            String finalMatch = regexMatches[i][0];

            finalMatch = finalMatch.replace("\\", "");

            finalList.add(finalMatch);
        }

        // no download links were found
        if (finalList.size() < 1) return null;

        DownloadLink downloadLink = createDownloadlink(finalList.get(0));

        // set the file name and suffix to denote whether Russian sound (rusSound) or only subtitles (rusText) are available
        downloadLink.setFinalFileName(title + resolution + (russianSound ? "_rusSound" : "_rusText") + extension);

        // set available true
        downloadLink.setAvailable(true);

        // try to get the file size
        try {
            Browser br2 = br.cloneBrowser();

            if (!br2.openGetConnection(downloadLink.getDownloadURL()).getContentType().contains("html")) {
                downloadLink.setDownloadSize(br2.getHttpConnection().getLongContentLength());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return downloadLink;
    }

    /**
     * Converts the title String into a more appropriate one (trivial approach)
     * 
     * @param title
     *            The title to convert
     * @return The converted title
     */
    private String getTitle(String title) {
        String result = "";

        String[] titleSplit = title.split("_");

        for (String s : titleSplit) {
            if (s.length() < 1) continue;

            result += s.substring(0, 1).toUpperCase() + s.substring(1) + "_";
        }

        return result;
    }

    /**
     * Sorts the given Map<String, Boolean> by its entries values: boolean -> true first
     * 
     * @param map
     *            The map to sort
     * @return The sorted map
     */
    private HashMap<String, Boolean> sortByValue(HashMap<String, Boolean> map) {
        List<Map.Entry<String, Boolean>> list = new LinkedList<Map.Entry<String, Boolean>>(map.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Boolean>>() {

            public int compare(Map.Entry<String, Boolean> m1, Map.Entry<String, Boolean> m2) {
                if (m1.getValue() & !m2.getValue()) {
                    return -1;
                } else if (!m1.getValue() & m2.getValue()) {
                    return 1;
                } else {
                    return 0;
                }
            }

        });

        HashMap<String, Boolean> result = new LinkedHashMap<String, Boolean>();
        for (Map.Entry<String, Boolean> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
