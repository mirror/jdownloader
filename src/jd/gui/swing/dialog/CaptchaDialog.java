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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.ShadowBorder;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

/**
 * This Dialog is used to display a Inputdialog for the captchas
 */
public class CaptchaDialog extends AbstractDialog<String> implements ActionListener, WindowListener, MouseListener, CaptchaDialogInterface {

    private static final long serialVersionUID = 1L;

    private ExtTextField      textField;

    private final File        imagefile;

    private final String      defaultValue;

    private final String      explain;

    public static void main(String[] args) {
        CaptchaDialog cp = new CaptchaDialog(0, "filesonic.com", new File("C:\\Users\\Thomas\\.jd_home\\captchas\\filesonic.com_29.09.2011_11.49.41.233.jpg"), null, "Enter both words...");
        LookAndFeelController.getInstance().setUIManager();
        try {
            Dialog.getInstance().showDialog(cp);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

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

    @Override
    public JComponent layoutDialogContent() {
        setMinimumSize(new Dimension(300, 250));
        final JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]"));

        ImageIcon imageIcon = null;

        if (this.imagefile != null && this.imagefile.exists()) {
            // imageIcon = new ImageIcon(this.imagefile.getAbsolutePath());
            BufferedImage img;
            try {
                img = IconIO.getImage(imagefile.toURI().toURL());

                imageIcon = new ImageIcon(IconIO.colorRangeToTransparency(img, new Color(0xEEEEEE), new Color(0xFFFFFF)));
            } catch (MalformedURLException e) {
                e.printStackTrace();
                imageIcon = NewTheme.I().getIcon("ocr", 0);
            }
        } else {
            imageIcon = NewTheme.I().getIcon("ocr", 0);
        }

        final int size = SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.PARAM_CAPTCHA_SIZE, 100);
        if (size != 100) {
            imageIcon = new ImageIcon(imageIcon.getImage().getScaledInstance((int) (imageIcon.getIconWidth() * size / 100.0f), (int) (imageIcon.getIconHeight() * size / 100.0f), Image.SCALE_SMOOTH));
        }

        this.textField = new ExtTextField() {

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void onChanged() {
                cancel();
            }

        };
        textField.setClearHelpTextOnFocus(false);
        textField.setHelpText(explain);
        this.textField.setText(this.defaultValue);

        JLabel lbl;
        panel.add(lbl = new JLabel(imageIcon), "alignx center");
        lbl.setBorder(new ShadowBorder(2, lbl.getForeground()));
        // lbl.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
        // lbl.getForeground()));
        panel.add(this.textField);
        this.textField.requestFocusInWindow();
        this.textField.selectAll();
        // panel.add(new JLabel("HJ"));
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

    public String getCaptchaCode() {
        return textField.getText();
    }

}