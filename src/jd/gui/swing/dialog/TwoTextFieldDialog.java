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

import jd.gui.swing.components.JDTextField;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;

public class TwoTextFieldDialog extends AbstractDialog<String[]> {

    private static final long serialVersionUID = -7426399217833694784L;

    private final String      messageOne;

    private final String      defOne;

    private final String      messageTwo;

    private final String      defTwo;

    private JDTextField       txtFieldOne;

    private JDTextField       txtFieldTwo;

    public TwoTextFieldDialog(final String title, final String messageOne, final String defOne, final String messageTwo, final String defTwo) {
        super(0, title, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
        this.messageOne = messageOne;
        this.defOne = defOne;
        this.messageTwo = messageTwo;
        this.defTwo = defTwo;

    }

    @Override
    protected String[] createReturnValue() {

        return new String[] { this.txtFieldOne.getText(), this.txtFieldTwo.getText() };
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel panel = new JPanel(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[]5[]10[]5[]"));
        panel.add(new JLabel(this.messageOne));
        panel.add(this.txtFieldOne = new JDTextField(this.defOne));
        panel.add(new JLabel(this.messageTwo));
        panel.add(this.txtFieldTwo = new JDTextField(this.defTwo));
        return panel;
    }

}
