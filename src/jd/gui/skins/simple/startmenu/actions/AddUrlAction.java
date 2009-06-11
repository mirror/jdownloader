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

package jd.gui.skins.simple.startmenu.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;

import jd.controlling.DistributeData;
import jd.gui.UserIO;
import jd.parser.html.HTMLParser;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class AddUrlAction extends StartAction {

    private static final long serialVersionUID = -7185006215784212976L;

    public AddUrlAction() {
        super("action.addurl", "gui.images.url");
    }

    public void actionPerformed(ActionEvent e) {
        AddUrlAction.addUrlDialog();
    }

    public static void addUrlDialog() {
        String def = "";
        try {
            String newText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            String[] links = HTMLParser.getHttpLinks(newText, null);
            for (String l : links)
                def += l + "\r\n";
        } catch (Exception e2) {

        }

        String link = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_LARGE, JDLocale.L("gui.dialog.addurl.title", "Add URL(s)"), JDLocale.L("gui.dialog.addurl.message", "Add a URL(s). JDownloader will load and parse them for further links."), def, JDTheme.II("gui.images.taskpanes.linkgrabber", 32, 32), JDLocale.L("gui.dialog.addurl.okoption_parse", "Parse URL(s)"), null);
        if (link == null || link.length() == 0) return;
        new DistributeData(link, false).start();
    }
}
