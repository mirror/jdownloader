package jd;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JFrame;
import javax.swing.JLabel;

import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.border.IconBorder;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.jdesktop.swingx.painter.BusyPainter;

public class Tester {

    public static void main(String[] args) throws Exception {
        JLabel label1 = new JLabel("IconBorder Test");
        label1.setIcon(JDTheme.II("gui.images.premium", 16, 16));
        label1.setBorder(new IconBorder(JDTheme.II("gui.images.resume", 16, 16), JLabel.EAST));

        JXBusyLabel label2 = new JXBusyLabel();
        label2.setText("BusyLabel Test");
        label2.setBusy(true);

        JXBusyLabel label3 = new JXBusyLabel(new Dimension(100, 84));
        BusyPainter painter = new BusyPainter(new Rectangle2D.Float(0, 0, 13.500001f, 1), new RoundRectangle2D.Float(12.5f, 12.5f, 59.0f, 59.0f, 10, 10));
        painter.setTrailLength(5);
        painter.setPoints(31);
        painter.setFrame(1);
        label3.setPreferredSize(new Dimension(100, 84));
        label3.setIcon(new EmptyIcon(100, 84));
        label3.setBusyPainter(painter);
        label3.setBusy(true);

        JFrame frame = new JFrame();
        frame.setLayout(new MigLayout("ins 5, wrap 1", "[center]"));
        frame.add(label1);
        frame.add(label2);
        frame.add(label3);
        frame.pack();
        frame.setVisible(true);
    }

}