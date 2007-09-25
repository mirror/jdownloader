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
    private static int shapex = 33;
    private static int shapey = 2;


    static void drawClose(Display display, Point size, GC gc, int closeImageState, Color closeBorder) {

        int x=shapex;
        int y=shapey;
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
    private static void drawBackground(Display display, GC gc, int[] shape) {
        final Color[] colors = new Color[]{display.getSystemColor(SWT.COLOR_GRAY), display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_GRAY), display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_LIST_SELECTION)};
        int[] percents = new int[]{25,25,25,25,20};
        int height=69;
        int x=0;
        int y = -25;
        int width=48;
        int pos = percents[percents.length - 1] * height / 100;
        gc.fillRectangle(x, y, width, pos);
        
        Color lastColor = colors[colors.length-1];
        for (int i = percents.length-1; i >= 0; i--) {
        gc.setForeground(lastColor);
        lastColor = colors[i];
        gc.setBackground(lastColor);
        int gradientHeight = percents[i] * height / 100;
        gc.fillGradientRectangle(x, y+pos, width, gradientHeight, true);
        pos += gradientHeight;
        }
    }
    public static void main(String[] args) {
        final Display display = new Display();
        final Shell shell = new Shell(display,  SWT.NO_TRIM | SWT.ON_TOP);
        ClassLoader cl = shell.getClass().getClassLoader();
        final Image image = new Image(display, cl.getResourceAsStream("img/swt/paste.png"));
        Region region = new Region();
        final int[] shape = new int[] {0,69,0,69,0,6,1,5,1,4,4,1,5,1,6,0,41,0,42,1,43,1,46,4,46,5,47,6,48,69,48,69};
        region.add(shape);
        //region.subtract(new Rectangle(0, 15, 38, 63));
 
        shell.setRegion(region);
        shell.setSize(region.getBounds().width, region.getBounds().height);
        final Point size = shell.getSize();
        final Rectangle bounds = image.getBounds();
        final PaintListener close = new PaintListener() {
            public void paintControl(PaintEvent event) {
                GC gc = event.gc;
                drawClose(display, size, gc, closeState, display.getSystemColor(SWT.COLOR_BLACK));

            }

        };
        Listener l = new Listener() {
            Point origin;

            Rectangle rect = new Rectangle(shapex, shapey, 9, 9);
            public void handleEvent(Event e) {
                switch (e.type) {
                    case SWT.MouseDown:
                    {
                        Point pt = new Point(e.x, e.y);
                      if (rect.contains(pt) && e.button == 1) {
                          shell.dispose();
                      } else if (e.button == 1) {
                          origin = pt;
                      }
                      break;
                    }
                    case SWT.MouseUp:
                    {
                      origin = null;
                      break;
                    }
                    case SWT.MouseMove:
                    {
                      if (origin != null) {
                        Point p = display.map(shell, null, e.x, e.y);
                        shell.setLocation(p.x - origin.x, p.y - origin.y);
                      }
                      else
                      {
                          Point pt = new Point(e.x, e.y);
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
                      break;
                    }
                    }
 
            }

        };
        shell.addListener(SWT.MouseDown, l);
        shell.addListener(SWT.MouseUp, l);
        shell.addListener(SWT.MouseMove, l);
        shell.addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent event) {
                GC gc = event.gc;
                drawBackground(display, gc, shape);
                gc.drawImage(image, 0, 0, bounds.width, bounds.height, -5, 10, size.x+10, size.y-10 );
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