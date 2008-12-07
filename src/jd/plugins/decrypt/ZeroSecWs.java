package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class ZeroSecWs extends PluginForDecrypt {

    public ZeroSecWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Vector<String> passwords;
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String directComment = new Regex(param.toString(), "http://[\\w\\.]*?zerosec\\.ws/.+/.+/#comments|/.+/#comments|/.+/.*?#(comment-\\d+)").getMatch(0);

        if (directComment != null) {
            String comment = br.getRegex(Pattern.compile("<div class=\"even.*?\" id=\"" + directComment + "\"><a name=\"comment-\\d+\"></a>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            passwords = HTMLParser.findPasswords(comment);
            String[] links = new Regex(comment, "rel=\"nofollow\">(.*?)</a>", Pattern.CASE_INSENSITIVE).getColumn(0);
            for (String link : links) {
                if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link)) {
                    DownloadLink dLink = createDownloadlink(link);
                    dLink.addSourcePluginPasswords(passwords);
                    decryptedLinks.add(dLink);
                }
            }
        } else {
            Vector<String> pages = new Vector<String>();
            pages.add(param.toString());
            String comment_pages_tag = br.getRegex(Pattern.compile("<!-- Comment page numbers -->(.*?)<!-- End comment page numbers -->", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (comment_pages_tag != null) {
                String comment_pages[] = new Regex(comment_pages_tag, "<a class=\".*?-comment-page\" href=\"(.*?)\"").getColumn(0);
                for (String page : comment_pages) {
                    pages.add(page);
                }
            }
            for (String page : pages) {
                br.getPage(page);
                String comments[] = br.getRegex(Pattern.compile("<div class=\"even.*?\" id=\"comment-\\d+\".*?>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getColumn(0);
                for (String comment : comments) {
                    passwords = HTMLParser.findPasswords(comment);
                    String[] links = new Regex(comment, "rel=\"nofollow\">(.*?)</a>", Pattern.CASE_INSENSITIVE).getColumn(0);
                    for (String link : links) {
                        if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link)) {
                            DownloadLink dLink = createDownloadlink(link);
                            dLink.addSourcePluginPasswords(passwords);
                            decryptedLinks.add(dLink);
                        }
                    }
                }
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
