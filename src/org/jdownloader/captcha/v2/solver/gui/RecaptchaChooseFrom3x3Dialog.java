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

package org.jdownloader.captcha.v2.solver.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.HashSet;

import javax.swing.JComponent;

import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;

import jd.gui.swing.dialog.AbstractImageCaptchaDialog;
import jd.gui.swing.dialog.DialogType;

/**
 * This Dialog is used to display a Inputdialog for the captchas
 */
public class RecaptchaChooseFrom3x3Dialog extends AbstractImageCaptchaDialog {

    private HashSet<Integer> selected;

    public RecaptchaChooseFrom3x3Dialog(final int flag, DialogType type, final DomainInfo DomainInfo, final Image image, final String explain) {
        this(flag, type, DomainInfo, new Image[] { image }, explain);
    }

    public RecaptchaChooseFrom3x3Dialog(int flag, DialogType type, DomainInfo domainInfo, Image[] images, String explain) {
        super(flag | Dialog.STYLE_HIDE_ICON, _GUI._.gui_captchaWindow_askForInput(domainInfo.getTld()), type, domainInfo, explain, images);

    }

    protected void paintIconComponent(Graphics g, int width, int height, int xOffset, int yOffset, BufferedImage scaled) {
        if (bounds == null) {
            return;
        }
        int columnWidth = bounds.width / 3;
        int rowHeight = bounds.height / 3;
        for (int yslot = 0; yslot < 3; yslot++) {
            for (int xslot = 0; xslot < 3; xslot++) {
                int x = (1 + xslot) * columnWidth;
                int y = (1 + yslot) * rowHeight;
                int num = xslot + yslot * 3;
                // Color color = Color.RED;
                if (selected.contains(num)) {
                    CheckBoxIcon.TRUE.paintIcon(iconPanel, g, xOffset + x - CheckBoxIcon.TRUE.getIconWidth() - 5, yOffset + y - CheckBoxIcon.TRUE.getIconHeight() - 5);
                    g.setColor(Color.GREEN);
                    ((Graphics2D) g).setStroke(new BasicStroke(2));
                    g.drawRect(bounds.x + xslot * columnWidth + 1, bounds.y + yslot * rowHeight + 1, columnWidth - 2, rowHeight - 2);
                } else {
                    CheckBoxIcon.FALSE.paintIcon(iconPanel, g, xOffset + x - CheckBoxIcon.TRUE.getIconWidth() - 5, yOffset + y - CheckBoxIcon.TRUE.getIconHeight() - 5);
                }
            }
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        JComponent ret = super.layoutDialogContent();
        this.selected = new HashSet<Integer>();
        iconPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        iconPanel.setToolTipText(getHelpText());
        iconPanel.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {

                int x = (e.getX() - bounds.x) / (bounds.width / 3);
                int y = (e.getY() - bounds.y) / (bounds.height / 3);
                int num = x + y * 3;
                System.out.println("pressed " + num);
                if (!selected.remove(num)) {
                    selected.add(num);

                }
                iconPanel.repaint();

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

    public String getResult() {
        StringBuilder sb = new StringBuilder();

        for (Integer s : selected) {
            if (sb.length() > 0) {
                sb.append(",");

            }
            sb.append(s + 1);
        }
        return sb.toString();
    }

}