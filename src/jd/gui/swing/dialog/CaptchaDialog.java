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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

/**
 * This Dialog is used to display a Inputdialog for the captchas
 */
public class CaptchaDialog extends AbstractDialog<String> implements ActionListener, KeyListener, WindowListener, MouseListener {

    private static final long serialVersionUID = 1L;

    private JTextField        textField;

    private final File        imagefile;

    private final String      defaultValue;

    private final String      explain;

    public CaptchaDialog(final int flag, final String host, final File imagefile, final String defaultValue, final String explain) {
        super(flag | Dialog.STYLE_HIDE_ICON, (host != null ? host + ": " : "") + _GUI._.gui_captchaWindow_askForInput(), null, null, null);

        this.imagefile = imagefile;
        this.defaultValue = defaultValue;
        this.explain = explain;
    }

    @Override
    protected void packed() {
        this.textField.requestFocusInWindow();
    }

    @Override
    protected String createReturnValue() {
        if (Dialog.isOK(this.getReturnmask())) return this.textField.getText();
        return null;
    }

    public void keyPressed(final KeyEvent e) {
        this.cancel();
    }

    public void keyReleased(final KeyEvent e) {
    }

    public void keyTyped(final KeyEvent e) {
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

        this.textField = new JTextField(10);
        this.textField.addKeyListener(this);
        this.textField.setText(this.defaultValue);

        if (this.explain != null) {
            JTextField tf;
            panel.add(tf = new JTextField(), "");
            tf.setBorder(null);
            tf.setBackground(null);
            tf.setOpaque(false);
            tf.setText(this.explain);
            tf.setEditable(false);
        }
        panel.add(new JLabel(imageIcon), "alignx center");
        panel.add(this.textField);
        this.textField.requestFocusInWindow();
        this.textField.selectAll();
        return panel;
    }

    public void mouseClicked(final MouseEvent e) {
        this.cancel();
    }

    public void mouseEntered(final MouseEvent e) {
    }

    public void mouseExited(final MouseEvent e) {
    }

    public void mousePressed(final MouseEvent e) {
        this.cancel();
    }

    public void mouseReleased(final MouseEvent e) {
        this.cancel();
    }

}