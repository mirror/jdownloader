package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.os.CrossSystem;

@DecrypterPlugin(revision = "$Revision: 26321 $", interfaceVersion = 3, names = { "nzbking.com" }, urls = { "https?://[\\w\\.]*nzbking.com/details(:|%3a)[0-9a-zA-Z]+" }, flags = { 0 })
public class NzbKingCom extends PluginForDecrypt {

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public NzbKingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        URLConnectionAdapter con = null;
        File nzbFile = null;
        try {
            br.setLoadLimit(Integer.MAX_VALUE);
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
            final Form form = br.getFormbyAction("/nzb/");
            final Request request = br.createFormRequest(form);
            request.getHeaders().put("Accept-Encoding", "identity");
            con = br.openRequestConnection(request);
            final String fileName;
            if (con.isContentDisposition()) {
                final String tmp = Plugin.getFileNameFromDispositionHeader(con);
                if (StringUtils.endsWithCaseInsensitive(tmp, ".nzb")) {
                    fileName = tmp;
                } else {
                    fileName = tmp + ".nzb";
                }
            } else {
                fileName = System.currentTimeMillis() + ".nzb";
            }
            final String contentType = con.getContentType();
            if (con.isOK()) {
                nzbFile = Application.getTempResource("container/" + CrossSystem.alleviatePathParts(fileName));
                IO.secureWrite(nzbFile, IO.readStream(-1, con.getInputStream()));
                final DownloadLink link = new DownloadLink(null, nzbFile.getName(), null, URLHelper.createURL(nzbFile.toURI().toURL().toExternalForm()).toExternalForm(), true);
                link.setContainerUrl(param.getCryptedUrl());
                ret.add(link);
            } else {
                br.followConnection();
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
            if (nzbFile != null) {
                if (ret.size() == 0) {
                    nzbFile.delete();
                } else {
                    nzbFile.deleteOnExit();
                }
            }
        }
        return ret;
    }
}
