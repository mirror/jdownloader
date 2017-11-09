package jd.plugins.decrypter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision: 34675 $", interfaceVersion = 2, names = { "exposure.co" }, urls = { "https?://(?<!www)[a-z0-9]+\\.exposure.co/[a-z0-9\\-_]+" })
public class ExposureCo extends PluginForDecrypt {
    public ExposureCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        final DecimalFormat df = new DecimalFormat("000");
        int index = 1;
        final Set<String> dups = new HashSet<String>();
        String photosVariable = br.getRegex("photos\\s*=\\s*(\\[?\\s*\\{.*?)\\s*</script").getMatch(0);
        if (photosVariable == null) {
            photosVariable = br.getRegex("photos\\s*:\\s*(\\[?\\s*\\{.*?)\\s*</script").getMatch(0);
        }
        final String assetURLs[] = new Regex(photosVariable, "asset_url\"\\s*:\\s*\"(https?://.*?)(\"|\\?)").getColumn(0);
        if (assetURLs != null) {
            for (final String assetURL : assetURLs) {
                if (dups.add(assetURL)) {
                    final DownloadLink link = createDownloadlink("directhttp://" + assetURL);
                    final String extension = getFileNameExtensionFromURL(assetURL, ".jpg");
                    link.setFinalFileName("image_" + df.format(index++) + extension);
                    ret.add(link);
                }
            }
        }
        String metaImages[] = br.getRegex("property=\"og:image\"\\s*content=(?:'|\")(https?://.*?)(\"|\\?|')").getColumn(0);
        if (metaImages == null || metaImages.length == 0) {
            metaImages = br.getRegex("<meta\\s*content=(?:\"|')(https?://.*?)(\"|\\?|')").getColumn(0);
        }
        if (metaImages != null) {
            for (final String metaImage : metaImages) {
                if (dups.add(metaImage)) {
                    final DownloadLink link = createDownloadlink("directhttp://" + metaImage);
                    final String extension = getFileNameExtensionFromURL(metaImage, ".jpg");
                    link.setFinalFileName("image_" + df.format(index++) + extension);
                    ret.add(link);
                }
            }
        }
        if (title != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(ret);
        }
        return ret;
    }
}
