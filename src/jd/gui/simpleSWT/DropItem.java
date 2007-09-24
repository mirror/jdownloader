package jd.gui.simpleSWT;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class DropItem {
    private static int closeState = 1;
    static int[] createCircle(int xOffset, int yOffset, int radius) {
        int[] circlePoints = new int[10 * radius];
        for (int loopIndex = 0; loopIndex < 2 * radius + 1; loopIndex++) {
            int xCurrent = loopIndex - radius;
            int yCurrent = (int) Math.sqrt(radius * radius - xCurrent * xCurrent);
            int doubleLoopIndex = 2 * loopIndex;

            circlePoints[doubleLoopIndex] = xCurrent + xOffset;
            circlePoints[doubleLoopIndex + 1] = yCurrent + yOffset;
            circlePoints[10 * radius - doubleLoopIndex - 2] = xCurrent + xOffset;
            circlePoints[10 * radius - doubleLoopIndex - 1] = -yCurrent + yOffset;
        }

        return circlePoints;
    }
    static void drawClose(Display display, Point size, GC gc, int closeImageState, Color closeBorder) {

        int x = size.x - 30;
        int y = 15;

        switch (closeImageState) {
            case 1 : {
                int[] shape = new int[]{x, y, x + 2, y, x + 4, y + 2, x + 5, y + 2, x + 7, y, x + 9, y, x + 9, y + 2, x + 7, y + 4, x + 7, y + 5, x + 9, y + 7, x + 9, y + 9, x + 7, y + 9, x + 5, y + 7, x + 4, y + 7, x + 2, y + 9, x, y + 9, x, y + 7, x + 2, y + 5, x + 2, y + 4, x, y + 2};
                Color fill = display.getSystemColor(SWT.COLOR_WHITE);
                gc.setBackground(fill);
                gc.fillPolygon(shape);
                gc.setForeground(closeBorder);
                gc.drawPolygon(shape);
                break;
            }
            case 2 : {
                int[] shape = new int[]{x, y, x + 2, y, x + 4, y + 2, x + 5, y + 2, x + 7, y, x + 9, y, x + 9, y + 2, x + 7, y + 4, x + 7, y + 5, x + 9, y + 7, x + 9, y + 9, x + 7, y + 9, x + 5, y + 7, x + 4, y + 7, x + 2, y + 9, x, y + 9, x, y + 7, x + 2, y + 5, x + 2, y + 4, x, y + 2};
                Color fill = new Color(display, new RGB(252, 160, 160));
                gc.setBackground(fill);
                gc.fillPolygon(shape);
                gc.setForeground(closeBorder);
                gc.drawPolygon(shape);
                break;
            }
        }
    }

    public static void main(String[] args) {
        final Display display = new Display();
        final Shell shell = new Shell(display, SWT.NO_TRIM);
        ClassLoader cl = shell.getClass().getClassLoader();
        final Image image = new Image(display, cl.getResourceAsStream("img/swt/paste.png"));
        Region region = new Region();
        region.add(createCircle(48, 48, 48));
        shell.setRegion(region);
        shell.setSize(region.getBounds().width, region.getBounds().height);
        shell.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
        final Point size = shell.getSize();
        final Rectangle bounds = image.getBounds();
        final PaintListener close = new PaintListener() {
            public void paintControl(PaintEvent event) {
                GC gc = event.gc;

                gc.drawImage(image, 0, 0, bounds.width, bounds.height, 10, 10, size.x - 20, size.y - 20);
                drawClose(display, size, gc, closeState, display.getSystemColor(SWT.COLOR_BLACK));

            }

        };
        shell.addListener(SWT.MouseMove, new Listener() {

            public void handleEvent(Event e) {
                Point pt = new Point(e.x, e.y);
                Rectangle rect = new Rectangle(shell.getSize().x - 30, 15, 9, 9);

                if (rect.contains(pt)) {
                    if (closeState > 1)
                        return;
                    closeState = 2;
                    e.gc = new GC(shell);
                    close.paintControl(new PaintEvent(e));
                } else if (closeState != 1) {
                    closeState = 1;
                    e.gc = new GC(shell);
                    close.paintControl(new PaintEvent(e));
                }
            }

        });

        shell.addListener(SWT.MouseDown, new Listener() {

            public void handleEvent(Event e) {
                System.out.println(e);
                Point pt = new Point(e.x, e.y);
                Rectangle rect = new Rectangle(shell.getSize().x - 30, 15, 9, 9);

                if (rect.contains(pt) && e.button == 1) {
                    shell.dispose();
                } else if (e.button == 1) {
                    closeState = 1;
                    e.gc = new GC(shell);
                    close.paintControl(new PaintEvent(e));
                }
            }

        });
        shell.addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent event) {
                GC gc = event.gc;
                gc.drawImage(image, 0, 0, bounds.width, bounds.height, 10, 10, size.x - 20, size.y - 20);
            }

        });
        shell.addPaintListener(close);

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        region.dispose();
        display.dispose();
    }
}