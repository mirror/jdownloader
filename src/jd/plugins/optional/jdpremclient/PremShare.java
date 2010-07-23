package jd.plugins.optional.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class PremShare extends PluginForHost {

    private boolean proxyused = false;
    // private static ImageIcon Icon = new
    // ImageIcon(JDImage.getImage("logo/logo_16_16"));
    private String infostring = null;
    private PluginForHost plugin = null;
    private static final String server = "";

    public void setReplacedPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    public PremShare(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        infostring = "JDPremium @ " + wrapper.getHost();
        // if (((PremShareHost) wrapper).getReplacedPlugin() != null) plugin =
        // (PluginForHost) ((PremShareHost)
        // wrapper).getReplacedPlugin().getNewPluginInstance();
    }

    public synchronized int getFreeConnections() {
        if (plugin != null) return plugin.getFreeConnections();
        return super.getFreeConnections();
    }

    public int getMaxConnections() {
        if (plugin != null) return plugin.getMaxConnections();
        return super.getMaxConnections();
    }

    public String getVersion() {
        if (plugin == null) return getVersion("$Revision: 86 $");
        return plugin.getVersion();
    }

    public void actionPerformed(ActionEvent e) {
        if (plugin != null) {
            plugin.actionPerformed(e);
        } else {
            super.actionPerformed(e);
        }
    }

    public String getHost() {
        if (plugin == null) return "jdownloader.org";
        return plugin.getHost();
    }

    public boolean isAGBChecked() {
        if (plugin == null) return true;
        return plugin.isAGBChecked();
    }

    public void setAGBChecked(boolean value) {
        if (plugin == null) return;
        plugin.setAGBChecked(value);
    }

    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.jdownloader.org";
        return plugin.getBuyPremiumUrl();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://www.jdownloader.org";
        return plugin.getAGBLink();
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        if (handleJDPremServ(link)) return;
        proxyused = false;
        plugin.clean();
        plugin.handleFree(link);
    }

    public void clean() {
        super.clean();
        if (plugin != null) plugin.clean();
    }

    private boolean handleJDPremServ(DownloadLink link) throws Exception {
        Account acc = JDPremium.getAccount();
        if (acc == null) return false;
        proxyused = true;
        requestFileInformation(link);
        br = new Browser();
        br.setDebug(true);
        Form form = new Form();
        br.getPage(server);
        /* add and force download */
        form.setAction("/?force=" + link.getDownloadURL());
        form.setMethod(MethodType.POST);
        form.put("username", Encoding.urlEncode(acc.getUser()));
        form.put("password", Encoding.urlEncode(acc.getPass()));

        /* first request,with force */
        String status = br.submitForm(form);
        int refresh = 10 * 1000;
        while (true) {
            if (status.contains("OK: 100")) {
                break;
            }
            if (status == null || status.length() == 0 || status.contains("ERROR")) {
                logger.info(status);
                return false;
            }
            if (status.contains("OK: 0")) {
                refresh = 10 * 1000;
                link.getLinkStatus().setStatusText("Queued");
            } else if (status.contains("OK: 1")) {
                /* show speed and ETA */
                String ints[] = new Regex(status, "(\\d+)/(\\d+)/(\\d+)").getRow(0);
                Long size = Long.parseLong(ints[1]);
                Long current = Long.parseLong(ints[0]);
                Long speed = Long.parseLong(ints[2]);
                String sp = Formatter.formatReadable(speed) + "/s";
                if (speed != 0 && size > 0) {
                    Long left = (size - current) / speed;
                    link.getLinkStatus().setStatusText("Downloading " + Formatter.formatSeconds(left) + " " + sp);
                } else {
                    link.getLinkStatus().setStatusText("Downloading " + sp);
                }
                refresh = 5 * 1000;
            }
            Thread.sleep(refresh);
            status = br.submitForm(form);
        }
        /* download now */
        form.setAction("/?download=1&force=" + link.getDownloadURL());
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, form, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            logger.info(br.toString());
            return false;
        }
        return dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (plugin == null) return AvailableStatus.UNCHECKABLE;
        return plugin.requestFileInformation(parameter);
    }

    @Override
    public void reset() {
        if (plugin != null) plugin.reset();
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (plugin == null) {
            AccountInfo ac = new AccountInfo();
            account.setValid(true);
            return ac;
        } else
            return plugin.fetchAccountInfo(account);
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        if (handleJDPremServ(downloadLink)) return;
        proxyused = false;
        plugin.clean();
        plugin.handlePremium(downloadLink, account);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (plugin != null) plugin.resetDownloadlink(link);
    }

    public String getSessionInfo() {
        if (proxyused || plugin == null) return infostring;
        return plugin.getSessionInfo();
    }

    public void correctDownloadLink(DownloadLink link) throws Exception {
        if (plugin != null) plugin.correctDownloadLink(link);
    }

    public int getMaxSimultanFreeDownloadNum() {
        if (plugin == null) return super.getMaxSimultanFreeDownloadNum();
        return plugin.getMaxSimultanFreeDownloadNum();
    }

    public int getMaxSimultanPremiumDownloadNum() {
        if (plugin == null) return super.getMaxSimultanPremiumDownloadNum();
        return plugin.getMaxSimultanPremiumDownloadNum();
    }

    public int getMaxSimultanDownload(final Account account) {
        if (plugin != null) return plugin.getMaxSimultanDownload(account);
        return 0;
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (plugin == null) return false;
        return plugin.checkLinks(urls);
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        if (proxyused || plugin == null) return "";
        return plugin.getFileInformationString(downloadLink);
    }

    public ArrayList<jd.gui.swing.jdgui.menu.MenuAction> createMenuitems() {
        if (plugin == null) return super.createMenuitems();
        return plugin.createMenuitems();
    }

    public ArrayList<Account> getPremiumAccounts() {
        if (plugin != null) return plugin.getPremiumAccounts();
        return super.getPremiumAccounts();
    }

    public boolean hasAccountSupport() {
        if (plugin != null) return plugin.hasAccountSupport();
        return false;
    }

    public String getCustomFavIconURL() {
        if (proxyused) return "http://www.jdownloader.org/";
        return null;
    }

}
