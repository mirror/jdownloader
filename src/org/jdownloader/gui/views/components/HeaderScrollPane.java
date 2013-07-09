package org.jdownloader.gui.views.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import jd.gui.swing.laf.LAFOptions;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.BasicGui;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;

public class HeaderScrollPane extends JScrollPane {
    private Color headerColor;
    private Color headerlineColor;

    public static void main(String[] args) {
        LookAndFeelController.getInstance().setUIManager();
        new BasicGui("TEst") {

            @Override
            protected void requestExit() {
            }

            @Override
            protected void layoutPanel() {

                getFrame().add(new LinkGrabberPanel());
            }
        };
    }

    public HeaderScrollPane(JComponent sidebar) {
        super(sidebar);
        Color c = LAFOptions.createColor(LookAndFeelController.getInstance().getLAFOptions().getColorForPanelHeader());

        headerColor = c;
        // setBorder(new JTextField().getBorder());
        headerlineColor = LAFOptions.createColor(LookAndFeelController.getInstance().getLAFOptions().getColorForPanelHeaderLine());

        setVerticalScrollBar(new JScrollBar() {
            public void setBounds(Rectangle rec) {

                // workaround for synthetica rounded borders. without this
                // workaround, synthetica themes would calculate a wrong y
                // coordinate for the vertical scrollbar
                if (getColumnHeader() != null) {
                    int newY = getColumnHeader().getHeight() + HeaderScrollPane.this.getBorder().getBorderInsets(HeaderScrollPane.this).top;
                    int newHeight = rec.height + (rec.y - newY);
                    rec.y = newY;
                    rec.height = newHeight;
                    super.setBounds(rec);
                } else {
                    super.setBounds(rec);
                }

            }
        });
        this.getVerticalScrollBar().setBlockIncrement(15);
    }

    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
    }

    protected void paintBorder(Graphics g) {
        if (getColumnHeader() != null) {
            g.setColor(headerColor);
            int in = getBorder().getBorderInsets(this).top;
            g.fillRect(1, 1, getWidth() - 2, getHeaderHeight() + in - 1);
            g.setColor(headerlineColor);
            g.drawLine(1, getHeaderHeight() + in - 1, getWidth() - 2, getHeaderHeight() + in - 1);
        }

        super.paintBorder(g);

    }

    protected int getHeaderHeight() {
        return getColumnHeader().getHeight();
    }

}
