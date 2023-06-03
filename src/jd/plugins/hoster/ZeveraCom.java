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

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.gui.swing.components.linkbutton.JLink;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.HostPlugin;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zevera.com" }, urls = { "https?://(?:[a-z0-9\\.\\-]+)?zevera\\.com/file\\?id=([A-Za-z0-9\\-_]+)" })
public class ZeveraCom extends ZeveraCore {
    protected static MultiHosterManagement mhm = new MultiHosterManagement("zevera.com");

    public ZeveraCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/premium");
    }

    @Override
    public String getClientID() {
        return getClientIDExt();
    }

    public static String getClientIDExt() {
        return "306575304";
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
        return 0;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new ZeveraAccountFactory(callback);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public boolean supportsUsenet(final Account account) {
        return false;
    }

    @Override
    public boolean supportsFreeAccountDownloadMode(final Account acc) {
        return false;
    }

    @Override
    public boolean usePairingLogin(final Account account) {
        /** 2019-08-05: Pairing login is not supported by this service! */
        return false;
    }

    public static class ZeveraAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final String      APIKEYHELP       = "Enter your API Key";
        private final JLabel      apikeyLabel;

        private String getPassword() {
            if (this.pass == null) {
                return null;
            } else if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
                return null;
            } else {
                return new String(this.pass.getPassword());
            }
        }

        public boolean updateAccount(Account input, Account output) {
            boolean changed = false;
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                changed = true;
            }
            if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                changed = true;
            }
            return changed;
        }

        private final ExtPasswordField pass;
        private static String          EMPTYPW = "                 ";

        public ZeveraAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to find your API Key:"));
            add(new JLink("https://www.zevera.com/account"));
            add(apikeyLabel = new JLabel("API Key:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(APIKEYHELP);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                // name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            final String password = getPassword();
            if (isAPIKEY(password)) {
                apikeyLabel.setForeground(Color.BLACK);
                return true;
            } else {
                apikeyLabel.setForeground(Color.RED);
                return false;
            }
        }

        @Override
        public Account getAccount() {
            return new Account(null, getPassword());
        }
    }

    @Override
    protected MultiHosterManagement getMultiHosterManagement() {
        return mhm;
    }
}