package jd.plugins.hoster;

import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;

@HostPlugin(revision = "$Revision: 18471 $", interfaceVersion = 3, names = { "put.io" }, urls = { "https?://put\\.io/file/\\d+" }, flags = { 2 })
public class PutIO extends PluginForHost {

    public PutIO(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://put.io");
    }

    @Override
    public String getAGBLink() {
        return "https://put.io/tos";
    }

    private String getFileID(DownloadLink parameter) {
        return new Regex(parameter.getDownloadURL(), "/file/(\\d+)").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) { return requestFileInformation_intern(parameter, aa); }
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) { return false; }
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        br.getPage("https://api.put.io/v2/account/info?oauth_token=" + account.getPass());
        Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), Map.class);
        if (response == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!"OK".equals(response.get("status"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "JDownloader support not enabled for this account", PluginException.VALUE_ID_PREMIUM_DISABLE); }
        ai.setStatus("JDownloader support is enabled for this account");
        Map<String, Object> info = (Map<String, Object>) response.get("info");
        if (response == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "JDownloader support not enabled for this account", PluginException.VALUE_ID_PREMIUM_DISABLE); }
        account.setUser((String) info.get("username"));
        account.setValid(true);
        return ai;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private String getExtension(Map<String, Object> fileStatus) {
        if ("video/quicktime".equals(fileStatus.get("content_type"))) { return ".mov"; }
        return "";
    }

    private AvailableStatus requestFileInformation_intern(DownloadLink link, Account account) throws Exception {
        String fileID = getFileID(link);
        br.getPage("https://api.put.io/v2/files/" + fileID + "?oauth_token=" + account.getPass());
        Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), Map.class);
        if (response == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if ("invalid_grant".equals(response.get("error"))) { return AvailableStatus.UNCHECKABLE; }
        Map<String, Object> fileStatus = (Map<String, Object>) response.get("file");
        if (link.getFinalFileName() == null) {
            link.setFinalFileName((String) fileStatus.get("name") + getExtension(fileStatus));
        }
        long size = -1;
        Object ret = fileStatus.get("size");
        if (ret != null) {
            if (ret instanceof Long) {
                size = (Long) ret;
            } else if (ret instanceof Integer) {
                size = ((Integer) ret).longValue();
            }
        }
        if (size >= 0) {
            link.setVerifiedFileSize(size);
        }
        if (size < 0 || link.getFinalFileName() == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;

    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        AvailableStatus status = requestFileInformation_intern(link, account);
        if (AvailableStatus.UNCHECKABLE.equals(status)) throw new PluginException(LinkStatus.ERROR_RETRY);
        String fileID = getFileID(link);
        br.setFollowRedirects(false);
        br.getPage("https://api.put.io/v2/files/" + fileID + "/download?oauth_token=" + account.getPass());
        String url = br.getRedirectLocation();
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }
}
