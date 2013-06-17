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

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.MalformedURLException;

import javax.swing.JComponent;

import jd.SecondLevelLaunch;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.gui.translate._GUI;

/**
 * This Dialog is used to display a Inputdialog for the captchas
 */
public class ClickCaptchaDialog extends AbstractCaptchaDialog implements ClickCaptchaDialogInterface {
    public static void main(String[] args) {
        AbstractCaptchaDialog cp;
        try {
            Application.setApplication(".jd_home");
            SecondLevelLaunch.statics();
            cp = new ClickCaptchaDialog(Dialog.LOGIC_COUNTDOWN, DialogType.HOSTER, DomainInfo.getInstance("wupload.com"), getGifImages(new File("C:/Users/Thomas/.BuildServ/applications/beta/sources/JDownloader/src/org/jdownloader/extensions/webinterface/webinterface/themes/main/images/core/load.gif").toURI().toURL()), "Enter both words...");

            LookAndFeelController.getInstance().setUIManager();

            Dialog.getInstance().showDialog(cp);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private Point resultPoint = null;

    public ClickCaptchaDialog(final int flag, DialogType type, final DomainInfo DomainInfo, final Image image, final String explain) {
        this(flag, type, DomainInfo, new Image[] { image }, explain);
    }

    public ClickCaptchaDialog(int flag, DialogType type, DomainInfo domainInfo, Image[] images, String explain) {
        super(flag | Dialog.STYLE_HIDE_ICON | Dialog.BUTTONS_HIDE_OK, _GUI._.gui_captchaWindow_askForInput(domainInfo.getTld()), type, domainInfo, explain, images);

    }

    @Override
    public JComponent layoutDialogContent() {
        JComponent ret = super.layoutDialogContent();

        iconPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        iconPanel.setToolTipText(getHelpText());
        iconPanel.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
                resultPoint = e.getPoint();

                resultPoint.x -= getOffset().x;
                resultPoint.y -= getOffset().y;
                resultPoint.x *= getScaleFaktor();
                resultPoint.y *= getScaleFaktor();
                setReturnmask(true);
                dispose();
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });
        return ret;
    }

    @Override
    protected JComponent createInputComponent() {
        ExtTextField ret = new ExtTextField();
        ret.setText(getHelpText());
        ret.setEditable(false);
        return ret;
    }

    @Override
    public ClickedPoint getResult() {
        if (resultPoint == null) return null;
        return new ClickedPoint(resultPoint.x, resultPoint.y);
    }

}