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

package jd.gui.swing.dialog;

import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import jd.gui.UserIO;
import jd.gui.swing.components.linkbutton.JLink;
import jd.nutils.JDFlags;
import jd.nutils.encoding.Encoding;

public class ConfirmDialog extends AbstractDialog {

    private static final long serialVersionUID = -7647771640756844691L;

    private JTextPane textField;
    private String message;

    public ConfirmDialog(int flag, String title, String message, ImageIcon icon, String okOption, String cancelOption) {
        super(flag, title, icon, okOption, cancelOption);
        this.message = message;
        init();
    }

    public JComponent contentInit() {
        textField = new JTextPane();
        if (JDFlags.hasAllFlags(this.flag, UserIO.STYLE_HTML)) {
            textField.setContentType("text/html");
            textField.addHyperlinkListener(JLink.getHyperlinkListener());
        } else {
            textField.setContentType("text");
            textField.setMaximumSize(new Dimension(450, 600));
        }

        textField.setText(this.message);
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);

        if (JDFlags.hasAllFlags(this.flag, UserIO.STYLE_LARGE)) {
            JScrollPane sp = new JScrollPane(textField);
            sp.setMaximumSize(new Dimension(450, 600));
            return sp;
        } else {
            return textField;
        }
    }

    @Override
    public String toString() {
        return Encoding.filterString(super.toString() + this.message).replaceAll("\\d", "");
    }

}
