package org.jdownloader.gui.mainmenu.action;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.menu.actions.sendlogs.LogAction;

import org.appwork.swing.MigPanel;
import org.appwork.utils.logging2.sendlogs.LogFolder;
import org.appwork.utils.logging2.sendlogs.SendLogDialog;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class LogSendAction extends CustomizableAppAction {
    public LogSendAction() {
        setName(_GUI.T.LogAction());
        setIconKey(IconKey.ICON_LOG);
        setTooltipText(_GUI.T.LogAction_tooltip());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new LogAction() {
            @Override
            protected SendLogDialog getSendLogDialog(List<LogFolder> folders) {
                return new SendLogDialog(folders) {
                    @Override
                    protected MigPanel createBottomPanel() {
                        final MigPanel ret = new MigPanel("ins 0", "[][]20[grow,fill][]", "[]");
                        try {
                            final JLink help = new JLink(_GUI.T.action_help(), new AbstractIcon(IconKey.ICON_DIALOG_HELP, 16), new URL("https://support.jdownloader.org/Knowledgebase/Article/View/how-to-create-and-upload-session-logs"));
                            final JLink privacy = new JLink(_GUI.T.jd_gui_swing_components_AboutDialog_privacy(), new AbstractIcon(IconKey.ICON_ABOUT, 16), new URL("https://my.jdownloader.org/legal/privacy.html#jdownloader"));
                            ret.add(help, "gapleft 0");
                            ret.add(privacy);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        return ret;
                    }
                };
            }
        }.actionPerformed(e);
    }
}
