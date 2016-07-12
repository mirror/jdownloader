package org.jdownloader.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;

import org.appwork.swing.MigPanel;

public class ColumnButton extends MigPanel {
    private class InternalButton extends JButton {
        private Icon icon;

        public InternalButton(Icon icon) {
            super("");
            this.icon = icon;

        }

        @Override
        public void setContentAreaFilled(boolean b) {
            super.setContentAreaFilled(b);
        }

        @Override
        public void setBackground(Color bg) {

            super.setBackground(bg);
        }

        @Override
        public void setBorderPainted(boolean b) {
            super.setBorderPainted(b);
        }

        public Dimension getPreferredSize() {

            return new Dimension(icon.getIconWidth() + 2, icon.getIconHeight() + 2);
        };

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            ColumnButton.this.dispatchEvent(e);
            super.processMouseMotionEvent(e);
        }

        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);

            icon.paintIcon(this, g, (getWidth() - icon.getIconWidth()) / 2, (getHeight() - icon.getIconHeight()) / 2);

        };

    }

    private InternalButton internButton;

    public ColumnButton(Icon icon) {
        super("ins 1", "[]", "[]");
        internButton = new InternalButton(icon);
        setOpaque(false);
        add(internButton);

    }

    public void addActionListener(ActionListener actionListener) {
        internButton.addActionListener(actionListener);
    }

}
