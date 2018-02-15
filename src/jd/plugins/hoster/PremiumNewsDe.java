package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.net.usenet.InvalidAuthException;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision: 37041 $", interfaceVersion = 3, names = { "premiumnews.de" }, urls = { "" })
public class PremiumNewsDe extends UseNet {
    public PremiumNewsDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.premiumnews.de/preise/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.premiumnews.de/allgemeine-geschaeftsbedingungen/";
    }

    public static interface PremiumNewsDeConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        try {
            verifyUseNetLogins(account);
            final AccountInfo ai = new AccountInfo();
            ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
            account.setMaxSimultanDownloads(20);// no way to check account type (silber(8)/gold(20)/platin(20+))
            account.setConcurrentUsePossible(true);
            account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 2 * 60 * 60 * 1000l);
            return ai;
        } catch (InvalidAuthException e) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.premium-news.de", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("news.premium-news.de", true, 563, 443));
        ret.addAll(UsenetServer.createServerList("news.premium-news.net", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("news.premium-news.net", true, 563, 443));
        ret.addAll(UsenetServer.createServerList("news1.premium-news.de", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("news1.premium-news.de", true, 563, 443));
        ret.addAll(UsenetServer.createServerList("news2.premium-news.de", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("news2.premium-news.de", true, 563, 443));
        return ret;
    }
}
