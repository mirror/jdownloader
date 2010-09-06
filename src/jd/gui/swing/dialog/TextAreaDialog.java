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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.UserIO;
import jd.gui.swing.components.JDTextArea;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;

public class TextAreaDialog extends AbstractDialog<String> {

    private static final long serialVersionUID = 5129590048597691591L;

    private final String      message;

    private final String      def;

    private JDTextArea        txtArea;

    public TextAreaDialog(final String title, final String message, final String def) {
        super(UserIO.NO_COUNTDOWN, title, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
        this.message = message;
        this.def = def;

    }

    @Override
    protected String createReturnValue() {
        return this.txtArea.getText();
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel panel = new JPanel(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[]5[]"));
        panel.add(new JLabel(this.message));
        panel.add(this.txtArea = new JDTextArea(this.def), "h 100!");
        return panel;
    }

}
