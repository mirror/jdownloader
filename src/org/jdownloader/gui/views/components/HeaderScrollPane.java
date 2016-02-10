package org.jdownloader.gui.views.components;

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.JTableHeader;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.BasicGui;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
import org.jdownloader.updatev2.gui.LAFOptions;
import org.jdownloader.updatev2.gui.LookAndFeelExtension;

public class HeaderScrollPane extends JScrollPane {

    private LookAndFeelExtension lafExtension;

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

        lafExtension = LAFOptions.getInstance().getExtension();

        JScrollBar sb;
        setVerticalScrollBar(sb = new JScrollBar() {
            {
                putClientProperty("JScrollBar.fastWheelScrolling", Boolean.TRUE);
            }

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
        this.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, new JTableHeader());
        LAFOptions.getInstance().getExtension().customizeHeaderScrollPane(this);

    }

    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
    }

    protected void paintBorder(Graphics g) {
        lafExtension.customizePaintHeaderScrollPaneBorder(this, g);

        super.paintBorder(g);

    }

    @Override
    public void setColumnHeader(JViewport columnHeader) {
        super.setColumnHeader(columnHeader);

    }

    // @Override
    // public void setColumnHeaderView(Component view) {
    // super.setColumnHeaderView(view);
    // System.out.println(getColumnHeader().getHeight());
    // }

    protected int getPrefHeaderHeight() {
        return getColumnHeader() == null ? 0 : getColumnHeader().getPreferredSize().height;
    }

    public int getHeaderHeight() {
        return getColumnHeader() == null ? 0 : getColumnHeader().getHeight();
    }

}
