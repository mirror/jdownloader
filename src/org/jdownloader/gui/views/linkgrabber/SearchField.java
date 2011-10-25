package org.jdownloader.gui.views.linkgrabber;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.BorderFactory;

import org.appwork.swing.components.ExtTextField;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class SearchField extends ExtTextField {
    private static final int SIZE = 20;
    private Image            img;

    public SearchField() {
        super();
        img = NewTheme.I().getImage("search", SIZE);
        setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(0, 22, 0, 0)));
        setHelpText(_GUI._.SearchField_SearchField_helptext());
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.drawImage(img, 3, 3, 3 + SIZE, 3 + SIZE, 0, 0, SIZE, SIZE, null);
        g2.setComposite(comp);
    }
}
