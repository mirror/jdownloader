package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;

public class PicButton extends JButton implements ActionListener, MouseListener {

    private int                      position;
    private KeyCaptchaCategoryDialog owner;
    private Image                    image;
    private BufferedImage            gray;
    private boolean                  mouseover = false;

    public PicButton(int i, KeyCaptchaCategoryDialog keyCaptchaCategoryDialog, Image image) {
        this.position = i;
        this.owner = keyCaptchaCategoryDialog;
        this.image = image;
        gray = ImageProvider.convertToGrayScale(IconIO.toBufferedImage(IconIO.getTransparent(image, 0.2f)));
        setSelected(i == 1);
        addActionListener(this);
        addMouseListener(this);
        updateIcon();
    }

    @Override
    public void setSelected(boolean b) {
        super.setSelected(b);
        updateIcon();
    }

    void updateIcon() {
        if (mouseover) {
            setIcon(new ImageIcon(image));
        } else {
            setIcon(new ImageIcon(gray));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        owner.updateIcons(this);

        owner.next();

    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseover = true;
        updateIcon();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseover = false;
        updateIcon();
    }

}
