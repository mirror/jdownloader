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

package jd.gui.userio.dialog;

import javax.swing.JComponent;
import javax.swing.JTextPane;

import jd.utils.JDTheme;

public class HtmlDialog extends AbstractDialog {

    private static final long serialVersionUID = 5106956546862704641L;

    private String message;

    public HtmlDialog(int flag, String title, String message) {
        super(flag, title, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
        this.message = message;
        init();
    }

    @Override
    public JComponent contentInit() {
        JTextPane htmlArea = new JTextPane();
        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");
        htmlArea.setText(message);
        htmlArea.setOpaque(false);
        htmlArea.requestFocusInWindow();
        /**
         * TODO
         */
//        htmlArea.addHyperlinkListener(JLink.getHyperlinkListener());

        return htmlArea;
    }

}
