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

package jd.gui.swing.jdgui.menu.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.CNL2;
import jd.controlling.ClipboardHandler;
import jd.controlling.DistributeData;
import jd.controlling.IOEQ;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.parser.html.HTMLParser;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AddUrlAction extends ToolBarAction {

    private static final long serialVersionUID = -7185006215784212976L;

    public AddUrlAction() {
        super(_GUI._.action_addurl(), "action.addurl", "url");
    }

    @Override
    public void onAction(ActionEvent e) {
        IOEQ.add(new Runnable() {

            public void run() {
                StringBuilder def = new StringBuilder();
                try {
                    String newText = ClipboardHandler.getClipboard().getCurrentClipboardLinks();
                    String[] links = HTMLParser.getHttpLinks(newText, null);
                    ArrayList<String> pws = HTMLParser.findPasswords(newText);
                    for (String l : links)
                        def.append(l).append("\r\n");
                    for (String pw : pws) {
                        def.append("password: ").append(pw).append("\r\n");
                    }
                } catch (Exception e2) {
                }
                String link = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_LARGE, _GUI._.gui_dialog_addurl_title(), _GUI._.gui_dialog_addurl_message(), def.toString(), NewTheme.I().getIcon("linkgrabber", 32), _GUI._.gui_dialog_addurl_okoption_parse(), null);
                if (link == null || link.length() == 0) return;
                if (CNL2.checkText(link)) return;
                DistributeData tmp = new DistributeData(link, false);
                tmp.setDisableDeepEmergencyScan(false);
                tmp.start();
            }

        }, true);

    }

    @Override
    public void initDefaults() {
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_addurl_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_addurl_accelerator();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_addurl_tooltip();
    }

}