//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetConfigPanel;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.config.Order;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.HostPlugin;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "premiumize.me" }, urls = { "https?://(?:[a-z0-9\\.\\-]+)?premiumize\\.me/file\\?id=([A-Za-z0-9\\-_]+)" })
public class PremiumizeMe extends ZeveraCore {
    protected static MultiHosterManagement mhm = new MultiHosterManagement("premiumize.me");

    public PremiumizeMe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/premium");
    }

    @Override
    public String getClientID() {
        return getClientIDExt();
    }

    public static String getClientIDExt() {
        return "616325511";
    }

    @Override
    public int getDownloadModeMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        /* 2019-02-19: premiumize.me/free */
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public static interface PremiumizeMeConfigInterface extends UsenetAccountConfigInterface {
        /** 2024-04-26: Removed pairing login setting */
        public class Translation {
            // public String getEnablePairingLogin_label() {
            // return "Enable pairing login?\r\nOnce enabled, you won't be able to use Usenet with Premiumize in JD anymore!!";
            // }
            public String getEnableBoosterPointsUnlimitedTrafficWorkaround_label() {
                return "Enable booster points unlimited traffic workaround for this account? \r\nThis is only for owners of booster-points! \r\nMore information: premiumize.me/booster";
            }
        }

        public static final PremiumizeMeConfigInterface.Translation TRANSLATION = new Translation();
        // @DefaultBooleanValue(false)
        // @Order(20)
        // boolean isEnablePairingLogin();
        //
        // void setEnablePairingLogin(boolean b);

        @AboutConfig
        @DefaultBooleanValue(false)
        @Order(30)
        boolean isEnableBoosterPointsUnlimitedTrafficWorkaround();

        void setEnableBoosterPointsUnlimitedTrafficWorkaround(boolean b);
    };

    @Override
    protected PluginConfigPanelNG createConfigPanel() {
        return new UsenetConfigPanel() {
            private static final long serialVersionUID = 1L;
            // @Override
            // protected boolean showKeyHandler(KeyHandler<?> keyHandler) {
            // return "enablepairinglogin".equals(keyHandler.getKey()) ||
            // "enableboosterpointsunlimitedtrafficworkaround".equals(keyHandler.getKey());
            // }
            //
            // @Override
            // protected boolean useCustomUI(KeyHandler<?> keyHandler) {
            // return !"enablepairinglogin".equals(keyHandler.getKey()) &&
            // !"enableboosterpointsunlimitedtrafficworkaround".equals(keyHandler.getKey());
            // }

            @Override
            protected boolean showKeyHandler(KeyHandler<?> keyHandler) {
                return "enableboosterpointsunlimitedtrafficworkaround".equals(keyHandler.getKey());
            }

            @Override
            protected boolean useCustomUI(KeyHandler<?> keyHandler) {
                return !"enableboosterpointsunlimitedtrafficworkaround".equals(keyHandler.getKey());
            }

            @Override
            protected void initAccountConfig(PluginForHost plgh, Account acc, Class<? extends AccountConfigInterface> cf) {
                super.initAccountConfig(plgh, acc, cf);
                extend(this, getHost(), getAvailableUsenetServer(), getAccountJsonConfig(acc));
            }
        };
    }

    @Override
    public PremiumizeMeConfigInterface getAccountJsonConfig(final Account acc) {
        return (PremiumizeMeConfigInterface) super.getAccountJsonConfig(acc);
    }

    @Override
    public boolean supportsUsenet(final Account account) {
        /**
         * 2019-12-18: At the moment usenet client support is only working with apikey login, not yet in pairing login mode. </br>
         * Premiumize is working on making this possible in pairing mode as well.
         */
        if (usePairingLogin(account)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean usePairingLogin(final Account account) {
        /**
         * 2021-01-29: Hardcoded-disabled this because API changes would be required to make Usenet work when logged in via this method.
         * Also some users enabled this by mistake and then failed to login (WTF)
         */
        // if (account == null) {
        // return false;
        // } else if (false && this.getAccountJsonConfig(account).isEnablePairingLogin()) {
        // return true;
        // } else {
        // return false;
        // }
        return false;
    }

    @Override
    public boolean isBoosterPointsUnlimitedTrafficWorkaroundActive(final Account account) {
        if (this.getAccountJsonConfig(account).isEnableBoosterPointsUnlimitedTrafficWorkaround()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("usenet.premiumize.me", false, 119));
        ret.addAll(UsenetServer.createServerList("usenet.premiumize.me", true, 563));
        return ret;
    }

    @Override
    protected MultiHosterManagement getMultiHosterManagement() {
        return mhm;
    }
}