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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import jd.controlling.JDLogger;
import jd.gui.swing.components.linkbutton.JLink;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class HelpDialog extends AbstractDialog {

    private static final long serialVersionUID = 5106956546862704641L;

    private String message;

    private String helpMessage;

    private String url;

    public HelpDialog(int flag, String title, String message, String helpMessage, String url) {
        super(flag, title, JDTheme.II("gui.images.config.tip", 32, 32), null, null);
        this.message = message;
        this.helpMessage = helpMessage;
        this.url = url;
        init();
    }

    @Override
    protected void addButtons(JPanel buttonBar) {
        JButton help = new JButton(helpMessage == null ? JDL.L("gui.btn_help", "Help") : helpMessage);
        help.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    JLink.openURL(url);
                } catch (Exception ex) {
                    JDLogger.exception(ex);
                }
                setReturnValue(false);
                dispose();
            }

        });
        buttonBar.add(help, "alignx right,tag help,sizegroup confirms");
    }

    @Override
    public JComponent contentInit() {
        JTextPane htmlArea = new JTextPane();
        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");
        htmlArea.setText(message);
        htmlArea.setOpaque(false);
        htmlArea.putClientProperty("Synthetica.opaque", Boolean.FALSE);

        return htmlArea;
    }

}
