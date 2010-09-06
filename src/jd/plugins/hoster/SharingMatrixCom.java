//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.lang.reflect.Field;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharingmatrix.com" }, urls = { "SHARINGMATRIXGONE:NEVERUSETHISREGEX" }, flags = { 2 })
public class SharingMatrixCom extends PluginForHost implements ControlListener {

    boolean added = false;
    private static final Object LOCK = new Object();
    private DownloadLink link = null;

    public SharingMatrixCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.filesonic.com/en/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.filesonic.com/en/contact-us";
    }

    public void login(Account account) throws Exception {
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        handleFree(downloadLink);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        synchronized (LOCK) {
            if (!getPluginConfig().getBooleanProperty("acctransfered2", false)) {
                PluginForHost newplugin = JDUtilities.getPluginForHost("filesonic.com");
                if (newplugin != null) {
                    ArrayList<Account> olds = new ArrayList<Account>(AccountController.getInstance().getAllAccounts("sharingmatrix.com"));
                    for (Account old : olds) {
                        AccountController.getInstance().addAccount(newplugin, old);
                        AccountController.getInstance().removeAccount(this, old);
                    }
                    getPluginConfig().setProperty("acctransfered2", true);
                    getPluginConfig().save();
                }
            }
        }
        if (!added) {
            JDController.getInstance().addControlListener(this);
            added = true;
        }
        link = downloadLink;
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public String getSessionInfo() {
        return "Restart required to convert SharingMatrix to FileSonic";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public void controlEvent(ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_SYSTEM_EXIT) {
            if (link != null) {
                try {
                    /* replace sharingmatrix link with filesonic link */
                    String id = new Regex(link.getDownloadURL(), "file/(\\d+)").getMatch(0);
                    link.setUrlDownload("http://www.filesonic.com/en/file/" + id);
                    /* replace host sharingmatrix.com with filesonic.com */
                    Field privateStringField = DownloadLink.class.getDeclaredField("host");
                    privateStringField.setAccessible(true);
                    privateStringField.set(link, "filesonic.com");
                } catch (Throwable e) {
                    logger.info("Could not replace SharingMatrix to FileSonic!");
                }

            }
        }

    }
}