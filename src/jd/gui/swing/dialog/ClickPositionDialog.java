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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.images.NewTheme;

public class ClickPositionDialog extends AbstractDialog<Point> implements ActionListener, MouseListener {

    private static final long serialVersionUID = 5540481255364141955L;

    private Point             result           = null;

    private final File        imagefile;

    private final String      explain;

    public ClickPositionDialog(final int flag, final File imagefile, final String title, final String explain) {
        super(flag | Dialog.STYLE_HIDE_ICON, title, null, null, null);
        this.imagefile = imagefile;
        this.explain = explain;
    }

    public void actionPerformed(final ActionEvent e) {
        this.mouseEntered(null);
        this.dispose();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.swing.dialog.AbstractDialog#getRetValue()
     */
    @Override
    protected Point createReturnValue() {
        return this.result;
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel panel = new JPanel(new MigLayout("ins 5,wrap 1", "[fill,grow]"));

        ImageIcon imageIcon = null;

        if (this.imagefile != null && this.imagefile.exists()) {
            imageIcon = new ImageIcon(this.imagefile.getAbsolutePath());
        } else {
            imageIcon = NewTheme.I().getIcon("ocr", 0);
        }

        final int size = SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.PARAM_CAPTCHA_SIZE, 100);
        if (size != 100) {
            imageIcon = new ImageIcon(imageIcon.getImage().getScaledInstance((int) (imageIcon.getIconWidth() * size / 100.0f), (int) (imageIcon.getIconHeight() * size / 100.0f), Image.SCALE_SMOOTH));
        }

        final JLabel captcha = new JLabel(imageIcon);
        captcha.addMouseListener(this);
        captcha.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        captcha.setToolTipText(this.explain);

        if (this.explain != null) {
            final JTextPane tf = new JTextPane();
            tf.setBorder(null);
            tf.setBackground(null);
            tf.setContentType("text/html");
            tf.setOpaque(false);
            tf.putClientProperty("Synthetica.opaque", Boolean.FALSE);
            tf.setText(this.explain);
            tf.setEditable(false);
            panel.add(tf, "");
        }
        panel.add(captcha, "w pref!, h pref!, alignx center");

        return panel;
    }

    public void mouseClicked(final MouseEvent e) {
    }

    public void mouseEntered(final MouseEvent e) {
        this.cancel();
    }

    public void mouseExited(final MouseEvent e) {
    }

    public void mousePressed(final MouseEvent e) {
    }

    public void mouseReleased(final MouseEvent e) {
        this.result = e.getPoint();
        final int size = SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.PARAM_CAPTCHA_SIZE, 100);
        if (size != 100) {
            this.result.setLocation(this.result.getX() / (size / 100.0f), this.result.getY() / (size / 100.0f));
        }
        this.dispose();
    }

}
