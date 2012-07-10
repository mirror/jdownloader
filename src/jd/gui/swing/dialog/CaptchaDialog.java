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
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import jd.Launcher;
import jd.controlling.captcha.CaptchaResult;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;

/**
 * This Dialog is used to display a Inputdialog for the captchas
 */
public class CaptchaDialog extends AbstractCaptchaDialog implements ActionListener, WindowListener, MouseListener {

    private ExtTextField textField;

    public static void main(String[] args) {
        AbstractCaptchaDialog cp;
        try {
            Application.setApplication(".jd_home");
            Launcher.statics();
            // getGifImages(new
            // File("C:/Users/Thomas/.BuildServ/applications/beta/sources/JDownloader/src/org/jdownloader/extensions/webinterface/webinterface/themes/main/images/core/load.gif").toURI().toURL())
            cp = new CaptchaDialog(Dialog.LOGIC_COUNTDOWN, DialogType.HOSTER, DomainInfo.getInstance("wupload.com"), ImageIO.read(new File("C:\\Users\\Thomas\\.jd_home\\captchas\\rusfolder.ru_10.07.2012_11.36.41.860.png")), null, "Enter both words...");

            LookAndFeelController.getInstance().setUIManager();

            Dialog.getInstance().showDialog(cp);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CaptchaDialog(final int flag, DialogType type, final DomainInfo DomainInfo, final Image image, final CaptchaResult defaultValue, final String explain) {
        this(flag, type, DomainInfo, new Image[] { image }, defaultValue, explain);
    }

    public CaptchaDialog(int flag, DialogType type, DomainInfo domainInfo, Image[] images, CaptchaResult defaultValue, String explain) {
        super(flag | Dialog.STYLE_HIDE_ICON, _GUI._.gui_captchaWindow_askForInput(domainInfo.getTld()), type, domainInfo, explain, defaultValue, images);

    }

    @Override
    protected void packed() {

        this.textField.requestFocusInWindow();
        textField.addFocusListener(new FocusListener() {

            public void focusLost(FocusEvent e) {
                cancel();
            }

            public void focusGained(FocusEvent e) {

            }
        });

    }

    @Override
    public CaptchaType getCaptchaType() {
        return CaptchaType.TEXT;
    }

    @Override
    public CaptchaResult getCaptchaResult() {

        return new CaptchaResult(textField.getText());

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
        textField.setHelpText(getHelpText());
        if (getDefaultValue() != null) this.textField.setText(getDefaultValue().getCaptchaText());
        this.textField.requestFocusInWindow();
        this.textField.selectAll();
        // panel.add(new JLabel("HJ dsf"));

        return textField;
    }

}