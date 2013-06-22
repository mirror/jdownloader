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

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 000000 $", interfaceVersion = 2, names = { "beeg.com" }, urls = { "http://(www\\.)?beeg\\.com" }, flags = { 0 })
public class BeegCom extends PluginForDecrypt {
    // , "http://(www\\.)?beeg\\.com/tag/\\w+(.)\\?w+", "http://(www\\.)?beeg\\.com/section/long-videos/",
    // "http://(www\\.)?beeg\\.com/search?q=\\w+"
    public BeegCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setCookie("beeg.com", "qchange", "h");
        String page = br.getPage(parameter);
        // System.out.println(">joe(page): " + page);

        // Offline1
        if (br.containsHTML(">Error 404 \\- Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Offline2
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Link offline (invalid link): " + parameter);
            return decryptedLinks;
        }

        String filename = br.getRegex("var tumbid(\\s+)=\\[.*?\\]").getMatch(0);
        System.out.println(">joe(tumbid_BR): " + filename);
        String str = "";
        // Getting var tumbid =[numbers, ...]
        Pattern p = Pattern.compile("var tumbid(\\s+)=\\[.*?\\]");
        Matcher m = p.matcher(page);
        // System.out.println("joe(pattern):");
        if (m.find()) {
            str = m.group();
            System.out.println(">joe(tumbid_PAT):" + m.group());
        }
        // System.out.print("\n");

        // Getting [numbers,...]
        p = Pattern.compile("\\[.*?\\]");
        m = p.matcher(str);
        m.reset();
        System.out.print("joe(pattern2):");
        if (m.find()) {
            str = m.group();
            System.out.println(m.group() + "_");
        }
        System.out.print("\n");

        // Getting numbers,...
        str = str.substring(1, str.length() - 1);
        String[] numPages = str.split(",");
        // for (String numPage : numPages)
        // System.out.print(numPage + "_");

        // System.out.println(">joe(url): " + br.getURL());

        int i = 0;
        String pageVideo = "";
        String[] linkPages = new String[numPages.length];
        for (String numPage : numPages) {
            // pageVideo = getVideoPage(numPage);
            // System.out.print(pageVideo + "_");
            pageVideo = "http://beeg.com/" + numPage;
            linkPages[i] = pageVideo;
            i++;
            System.out.println(pageVideo);
        }

        String videoLink = "";
        for (String linkPage : linkPages) {
            videoLink = getVideoPage(linkPage);
            System.out.println(videoLink);
            decryptedLinks.add(createDownloadlink(videoLink));
        }
        return decryptedLinks;
    }

    public String getVideoPage(String videoPage) throws IOException {
        String str = "";
        str = br.getPage(videoPage);

        Pattern p = Pattern.compile("http://.*?\\.mp4");
        Matcher m = p.matcher(str);
        if (m.find()) {
            str = m.group();
        }
        // http://video.mystreamservice.com/480p/3028102.mp4

        return str;
    }

    /*
     * public DownloadLink getDownloadLink(String page) { try { String newPage = new Browser().getPage(page); newPage } catch (IOException
     * e) { e.printStackTrace(); } return createDownloadlink(""); }
     */
    /*
     * private String decryptUrl(final String fun, final String value) {
     * 
     * Object result = new Object(); final ScriptEngineManager manager = new ScriptEngineManager(); final ScriptEngine engine =
     * manager.getEngineByName("javascript"); final Invocable inv = (Invocable) engine;
     * 
     * try { engine.eval(fun); result = inv.invokeFunction("decrypt", value); } catch (final Throwable e) { return null; } return result !=
     * null ? result.toString() : null; }
     */

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
