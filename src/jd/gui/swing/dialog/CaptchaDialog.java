//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JComponent;

import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;

/**
 * This Dialog is used to display a Inputdialog for the captchas
 */
public class CaptchaDialog extends AbstractCaptchaDialog implements ActionListener, MouseListener {

    private ExtTextField textField;
    private String       suggest;

    public CaptchaDialog(final int flag, DialogType type, final DomainInfo DomainInfo, final Image image, final String explain) {
        this(flag, type, DomainInfo, new Image[] { image }, explain);
    }

    public CaptchaDialog(int flag, DialogType type, DomainInfo domainInfo, Image[] images, String explain) {
        super(flag | Dialog.STYLE_HIDE_ICON, _GUI._.gui_captchaWindow_askForInput(domainInfo.getTld()), type, domainInfo, explain, images);

    }

    @Override
    protected void packed() {

        textField.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent e) {
                cancel();
            }

            public void focusGained(FocusEvent e) {

            }
        });
        getDialog().addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowLostFocus(WindowEvent windowevent) {
            }

            @Override
            public void windowGainedFocus(WindowEvent windowevent) {
                System.out.println("textfield.request");
                textField.requestFocusInWindow();
            }
        });

    }

    @Override
    protected JComponent createInputComponent() {

        this.textField = new ExtTextField() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void onChanged() {
                cancel();
            }

        };

        textField.setClearHelpTextOnFocus(false);
        textField.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
                System.out.println(e);
            }

            @Override
            public void focusGained(FocusEvent e) {
                System.out.println(e);
                textField.selectAll();
            }
        });
        textField.setHelpText(getHelpText());
        if (suggest != null) textField.setText(suggest);
        // this.textField.requestFocusInWindow();
        // this.textField.selectAll();

        // panel.add(new JLabel("HJ dsf"));

        return textField;
    }

    protected void initFocus(final JComponent focus) {

    }

    public String getResult() {
        return textField == null ? null : textField.getText();
    }

    public void suggest(String value) {
        suggest = value;
        if (textField != null && StringUtils.isEmpty(textField.getText())) {
            boolean hasFocus = textField.hasFocus();
            textField.setText(value);
            if (hasFocus) textField.selectAll();
        }
    }

}