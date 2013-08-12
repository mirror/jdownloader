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

import javax.swing.JComponent;

import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.gui.translate._GUI;

/**
 * This Dialog is used to display a Inputdialog for the captchas
 */
public class ClickCaptchaDialog extends AbstractCaptchaDialog {

    private Point resultPoint = null;

    public ClickCaptchaDialog(final int flag, DialogType type, final DomainInfo DomainInfo, final Image image, final String explain) {
        this(flag, type, DomainInfo, new Image[] { image }, explain);
    }

    public ClickCaptchaDialog(int flag, DialogType type, DomainInfo domainInfo, Image[] images, String explain) {
        super(flag | Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_OK, _GUI._.gui_captchaWindow_askForInput(domainInfo.getTld()), type, domainInfo, explain, images);

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

    public ClickedPoint getResult() {
        if (resultPoint == null) return null;
        return new ClickedPoint(resultPoint.x, resultPoint.y);
    }

}