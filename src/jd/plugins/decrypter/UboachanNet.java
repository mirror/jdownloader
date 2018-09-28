package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uboachan.net" }, urls = { "https?://(www\\.)?uboachan\\.net/.+(/res/\\d+\\.html|/\\d+\\.html|/index\\.html)" })
public class UboachanNet extends antiDDoSForDecrypt {
    public UboachanNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final boolean singlePage;
        if (parameter.getCryptedUrl().matches("(?i).+/res/\\d+\\.html$")) {
            getPage(br, parameter.getCryptedUrl());
            singlePage = true;
        } else if (parameter.getCryptedUrl().matches("(?i).+/\\d+\\.html$")) {
            getPage(br, parameter.getCryptedUrl().replaceAll("(?i)/\\d+\\.html$", "/index.html"));
            singlePage = false;
        } else {
            getPage(br, parameter.getCryptedUrl());
            singlePage = false;
        }
        final String title = br.getRegex("<header>\\s*<h\\d+>\\s*(.*?)</h").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        final Set<String> pages = new HashSet<String>();
        while (!isAbort()) {
            final String files[][] = br.getRegex("<div class=\"file\"[^>]*>(.*?)</div").getMatches();
            if (files != null) {
                for (String file[] : files) {
                    final String href = new Regex(file[0], "href\\s*=\\s*\"([^\"]*/src/\\d+(-\\d+)?\\.[^\"]*)\"").getMatch(0);
                    if (href == null) {
                        if (file[0].matches("(?i).+post-image\\s*deleted.+")) {
                            continue;
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                    final String postfilename = new Regex(file[0], "\"postfilename\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
                    final String size = new Regex(file[0], ">\\s*\\(\\s*([0-9\\.]+\\s*[KGTBM]{1,2})").getMatch(0);
                    final DownloadLink downloadLink = createDownloadlink("directhttp://" + br.getURL(href).toString());
                    final String uploadID = new Regex(href, ".+/(.*?)\\.[^/]*$").getMatch(0);
                    if (size != null) {
                        downloadLink.setDownloadSize(SizeFormatter.getSize(size));
                    }
                    if (postfilename != null) {
                        downloadLink.setFinalFileName(uploadID + "_" + postfilename);
                    }
                    downloadLink.setAvailable(true);
                    if (!singlePage) {
                        downloadLink.setContainerUrl(br.getURL());
                    }
                    fp.add(downloadLink);
                    distribute(downloadLink);
                    ret.add(downloadLink);
                }
            }
            if (!singlePage) {
                final Form next = br.getFormBySubmitvalue("Next");
                if (next != null && next.getAction() != null && pages.add(next.getAction())) {
                    submitForm(next);
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return ret;
    }
}
