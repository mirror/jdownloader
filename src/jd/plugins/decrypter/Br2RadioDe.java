package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "br.de" }, urls = { "https?://(www\\.)?br\\.de/radio/bayern2/[^<>\"]+\\d+\\.html" }) public class Br2RadioDe extends PluginForDecrypt {

    public Br2RadioDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        br.getPage(parameter.getCryptedUrl());
        final String dataURL = br.getRegex("dataURL:'(/radio/bayern2/.*xml)'").getMatch(0);
        if (dataURL != null) {
            final ArrayList<DownloadLink> audio = new ArrayList<DownloadLink>();
            final Browser br2 = br.cloneBrowser();
            br2.getPage(dataURL);
            final String assets[][] = br2.getRegex("<asset type=\"(STANDARD|MOBILEAAC|STANDARDAAC|PREMIUM|PREMIUMAAC)\">.*?<downloadurl>(https?://.*?)</downloadurl>.*?<size>(\\d+)</size>").getMatches();
            final String title = br2.getRegex("<title>(.*?)</title>").getMatch(0);
            for (final String asset[] : assets) {
                final DownloadLink link = createDownloadlink(asset[1]);
                link.setAvailable(true);
                link.setVerifiedFileSize(Long.parseLong(asset[2]));
                if (title != null) {
                    link.setFinalFileName(encodeUnicode(title) + "_" + asset[0] + Plugin.getFileNameExtensionFromURL(asset[1]));
                }
                audio.add(link);
            }
            final String images2[] = br.getRegex("class=\"(lead_picture|play)\">\\s*(</span>)?\\s*?<img.*? src=\"(/radio/bayern2/[^<>\"']*?\\.(jpg|png|gif|jpeg)\\?version=[^<>\"']*?)\"").getColumn(2);
            if (images2 != null) {
                for (final String image : images2) {
                    final URL url = br.getURL(image);
                    final DownloadLink link = createDownloadlink(url.toString());
                    link.setAvailable(true);
                    link.setName(Plugin.getFileNameFromURL(url));
                    audio.add(link);
                }
            }
            if (title != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(encodeUnicode(title));
                fp.addLinks(audio);
            }
            return audio;
        } else {
            final ArrayList<DownloadLink> rest = new ArrayList<DownloadLink>();
            final String prev = br.getRegex("class=\"df_prev\".*?href=\"(/radio/bayern2/[^<>\"']*?100\\.html)\"").getMatch(0);
            final String next = br.getRegex("class=\"df_next\".*?href=\"(/radio/bayern2/[^<>\"']*?100\\.html)\"").getMatch(0);
            final String additionals[] = br.getRegex("href=\"(/radio/bayern2/[^<>\"']*?\\d+\\.html)\" class=\"link_(audio|download)").getColumn(0);
            final String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (additionals != null && additionals.length > 0) {
                final HashSet<String> followUps = new HashSet<String>(Arrays.asList(additionals));
                followUps.remove(prev);
                followUps.remove(next);
                for (final String additional : additionals) {
                    final DownloadLink link = createDownloadlink(br.getURL(additional).toString());
                    rest.add(link);
                }
            }
            final String images[] = br.getRegex("class=\"picturebox\">\\s*<div class=\"pb_content\">\\s*<a.*? href=\"(/radio/bayern2/[^<>\"']*?\\.(jpg|png|gif|jpeg)\\?version=[^<>\"']*?)\"").getColumn(0);
            if (images != null) {
                for (final String image : images) {
                    final URL url = br.getURL(image);
                    final DownloadLink link = createDownloadlink(url.toString());
                    link.setAvailable(true);
                    link.setName(Plugin.getFileNameFromURL(url));
                    rest.add(link);
                }
            }
            final String images2[] = br.getRegex("class=\"(lead_picture|play)\">\\s*(</span>)?\\s*?<img.*? src=\"(/radio/bayern2/[^<>\"']*?\\.(jpg|png|gif|jpeg)\\?version=[^<>\"']*?)\"").getColumn(2);
            if (images2 != null) {
                for (final String image : images2) {
                    final URL url = br.getURL(image);
                    final DownloadLink link = createDownloadlink(url.toString());
                    link.setAvailable(true);
                    link.setName(Plugin.getFileNameFromURL(url));
                    rest.add(link);
                }
            }
            final Form[] forms = br.getForms();
            if (forms != null) {
                for (final Form form : forms) {
                    if (form.containsHTML("download_button")) {
                        final URL url = br.getURL(form.getAction());
                        final DownloadLink link = createDownloadlink(url.toString());
                        link.setAvailable(true);
                        link.setName(Plugin.getFileNameFromURL(url));
                        rest.add(link);
                    }
                }
            }
            if (rest.size() > 0 && title != null && parameter.getCryptedUrl().endsWith("100.html")) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(encodeUnicode(title));
                fp.addLinks(rest);
                fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
            }
            return rest;
        }
    }

}
