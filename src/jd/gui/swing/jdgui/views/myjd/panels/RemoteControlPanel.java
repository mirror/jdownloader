package jd.gui.swing.jdgui.views.myjd.panels;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import jd.gui.swing.jdgui.views.settings.components.SettingsButton;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.StorageException;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.statistics.StatsManager.CollectionName;

public class RemoteControlPanel extends AbstractConfigPanel {
    public RemoteControlPanel() {
        this.addHeader(getTitle(), getIcon());
        this.addDescription(_GUI.T.RemoteControlPanel_description());
        this.addHeader("Webinterface @ my.jdownloader.org", new AbstractIcon(IconKey.ICON_LOGO_MYJDOWNLOADER, 32));
        this.addDescription(_GUI.T.RemoteControlPanel_Webinterface());
        addButton("http://my.jdownloader.org?referer=JDownloader", IconKey.ICON_BOTTY_ROBOT_INFO, "<html>" + _GUI.T.RemoteControlPanel_website_open().replace("\r\n", "<br>") + "</html>");
        this.addHeader("Mobile Apps", new AbstractIcon(IconKey.ICON_MOBILE, 32));
        this.addDescription(_GUI.T.RemoteControlPanel_mobile_desc());
        addButton("https://play.google.com/store/apps/details?id=org.appwork.myjdandroid", IconKey.ICON_LOGO_ANDROID, "<html>" + _GUI.T.RemoteControlPanel_android_open().replace("\r\n", "<br>") + "</html>");
        addButton("https://itunes.apple.com/us/app/myjdownloader-remote/id1203271558?ls=1&mt=8", IconKey.ICON_LOGO_IOS, "<html>" + _GUI.T.RemoteControlPanel_ios_open2().replace("\r\n", "<br>") + "</html>");
        addButton("http://www.pixelvalley.de/?page_id=1649", IconKey.ICON_LOGO_WINDOWS, "<html>" + _GUI.T.RemoteControlPanel_file_recon_open().replace("\r\n", "<br>") + "</html>");
        this.addHeader("Browser Extensions", new AbstractIcon("url", 32));
        this.addDescription(_GUI.T.RemoteControlPanel_browser_extension_desc());
        addButton("https://chrome.google.com/webstore/detail/my-jdownloader/fbcohnmimjicjdomonkcbcpbpnhggkip", IconKey.ICON_LOGO_CHROME, "<html>" + _GUI.T.RemoteControlPanel_chrome_open().replace("\r\n", "<br>") + "</html>");
        addButton("https://my.jdownloader.org/apps/", IconKey.ICON_LOGO_FIREFOX, "<html>" + _GUI.T.RemoteControlPanel_firefox_open().replace("\r\n", "<br>") + "</html>");
        // addButton("https://addons.mozilla.org/de/firefox/addon/official-my-jdownloader-add/", IconKey.ICON_LOGO_FIREFOX, "<html>" +
        // _GUI.T.RemoteControlPanel_firefox_open().replace("\r\n", "<br>") + "</html>");
    }

    private void addButton(final String url, final String iconkey, final String text) {
        SettingsButton bt = new SettingsButton(new AppAction() {
            {
                setName(text);
                setIconKey(iconkey);
                setIconSizes(48);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    StatsManager.I().track("myjd/" + url, CollectionName.BASIC);
                    CrossSystem.openURLOrShowMessage("http://update3.jdownloader.org/jdserv/RedirectInterface/redirect?" + Encoding.urlEncode(url));
                } catch (StorageException e1) {
                    e1.printStackTrace();
                }
            }
        });
        bt.setHorizontalAlignment(SwingConstants.LEFT);
        add(bt, "gapleft 64,spanx,pushx,growx");
    }

    @Override
    public String getTitle() {
        return _GUI.T.RemoteControlPanel_title();
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_CLOUD_SYNC, 32);
    }

    @Override
    protected void onShow() {
        super.onShow();
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }
}
