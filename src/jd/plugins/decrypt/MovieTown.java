package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class MovieTown extends PluginForDecrypt {

    public MovieTown(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.toString();
        this.setBrowserExclusive();
        br.getPage(url);
        String pw = br.getRegex("Passwort:<.*?<span class=.*?>(.*?)</span>").getMatch(0);

        String[] links = br.getRegex("Mirror.*?\\d+.*?<a href=\"(.*?)\"").getColumn(0);
        for (String link : links) {
            DownloadLink dl = this.createDownloadlink(link);
            if (pw != null && !pw.contains("keins")) dl.addSourcePluginPassword(pw.trim());
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    public String getVersion() {
        return getVersion("$Revision: 5927 $");
    }

}
