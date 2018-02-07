package jd.plugins.decrypter;

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision: 37334 $", interfaceVersion = 2, names = { "bentbox.co" }, urls = { "https?://(?:www\\.)?bentbox\\.co/box(_view)?\\?[a-zA-Z0-9]+" })
public class BentBoxCo extends PluginForDecrypt {
    public BentBoxCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        setBrowserExclusive();
        final Account account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            try {
                jd.plugins.hoster.BentBoxCo.login(br, account);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.setError(AccountError.INVALID, -1, null);
                    return ret;
                }
                logger.log(e);
                return null;
            }
        } else {
            return ret;
        }
        final String boxID = new Regex(parameter.getCryptedUrl(), "box(?:_view)?\\?(.+)").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(parameter.getCryptedUrl());
        final String title = br.getRegex("\"og:title\"\\s*content=\"(.*?)\"").getMatch(0);
        final String author = br.getRegex("meta\\s*name=\"author\"\\s*content=\"\\s*(.*?)\\s*\"").getMatch(0);
        final PostRequest boxView = new PostRequest(br.getURL("/load_box_view.php"));
        boxView.setPostDataString("boxId=" + boxID);
        boxView.getHeaders().put("Origin", "http://bentbox.co");
        boxView.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        boxView.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
        br.getPage(boxView);
        final DecimalFormat df = new DecimalFormat("000");
        int index = 1;
        final String downloadFiles[][] = br.getRegex("data-fileurl=\"(https?://.*?)\"\\s*data-filename=\"(.*?)\"[^<>]*onclick='downloadFile").getMatches();
        for (final String downloadFile[] : downloadFiles) {
            // TODO: links do expire! add support for dedicated plugin
            final DownloadLink downloadLink = createDownloadlink("directhttp://" + br.getURL("/downloadFile_fromBox.php?path=" + Encoding.urlEncode(downloadFile[0])).toString());
            downloadLink.setAvailable(true);
            String fileName = downloadFile[1];
            if (!StringUtils.endsWithCaseInsensitive(fileName, ".mp4") && !StringUtils.endsWithCaseInsensitive(fileName, ".jpg")) {
                final String urlExtension = getFileNameExtensionFromURL(downloadFile[0]);
                if (urlExtension != null) {
                    fileName = fileName + urlExtension;
                }
            }
            downloadLink.setFinalFileName(df.format(index++) + "_download_" + fileName);
            ret.add(downloadLink);
        }
        final String normalFiles[][] = br.getRegex("href=\"(https?://.*?)\"\\s*class=\"swipebox\"\\s*title=\"(.*?)\"").getMatches();
        for (final String normalFile[] : normalFiles) {
            // TODO: links do expire! add support for dedicated plugin
            final DownloadLink downloadLink = createDownloadlink("directhttp://" + normalFile[0]);
            downloadLink.setAvailable(true);
            String fileName = normalFile[1];
            if (!StringUtils.endsWithCaseInsensitive(fileName, ".mp4") && !StringUtils.endsWithCaseInsensitive(fileName, ".jpg")) {
                final String urlExtension = getFileNameExtensionFromURL(normalFile[0]);
                if (urlExtension != null) {
                    fileName = fileName + urlExtension;
                }
            }
            downloadLink.setFinalFileName(df.format(index++) + "_web_" + fileName);
            ret.add(downloadLink);
        }
        final String transcodedFiles[][] = br.getRegex("data-transcodedvideourl=\"(https?://.*?)\".*?<span style.*?>(.*?)</").getMatches();
        for (final String transcodedFile[] : transcodedFiles) {
            // TODO: links do expire! add support for dedicated plugin
            final DownloadLink downloadLink = createDownloadlink("directhttp://" + transcodedFile[0]);
            downloadLink.setAvailable(true);
            String fileName = transcodedFile[1];
            if (!StringUtils.endsWithCaseInsensitive(fileName, ".mp4") && !StringUtils.endsWithCaseInsensitive(fileName, ".jpg")) {
                final String urlExtension = getFileNameExtensionFromURL(transcodedFile[0]);
                if (urlExtension != null) {
                    fileName = fileName + urlExtension;
                }
            }
            downloadLink.setFinalFileName(df.format(index++) + "_transcoded_" + fileName);
            ret.add(downloadLink);
        }
        if (title != null || author != null) {
            final FilePackage fp = FilePackage.getInstance();
            if (title != null && author != null) {
                fp.setName(author + "-" + Encoding.htmlDecode(title));
            } else if (title != null) {
                fp.setName(Encoding.htmlDecode(title));
            } else {
                fp.setName(author);
            }
            fp.addLinks(ret);
        }
        return ret;
    }
}
