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
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class DropItem extends Dialog{

    public DropItem(Shell parent) {
        super(parent, SWT.NO_TRIM | SWT.ON_TOP );
    }

    private int closeState = 1;
    private final static  int height = 60;
    private final static int width = 50;
    private final static int closex = width - 15;
    private final static int closey = 2;
    private final static int[] shape = new int[]{0, height, 0, height, 0, 6, 1, 5, 1, 4, 4, 1, 5, 1, 6, 0, width - 7, 0, width - 6, 1, width - 5, 1, width - 2, 4, width - 2, 5, width - 1, 6, width, height, width, height};
    private final static int[] closeShape = new int[]{closex, closey, closex + 2, closey, closex + 4, closey + 2, closex + 5, closey + 2, closex + 7, closey, closex + 9, closey, closex + 9, closey + 2, closex + 7, closey + 4, closex + 7, closey + 5, closex + 9, closey + 7, closex + 9, closey + 9, closex + 7, closey + 9, closex + 5, closey + 7, closex + 4, closey + 7, closex + 2, closey + 9, closex, closey + 9, closex, closey + 7, closex + 2, closey + 5, closex + 2, closey + 4, closex, closey + 2};

    void drawClose(Display display, GC gc, int closeImageState) {

        switch (closeImageState) {
            case 1 : {
                Color fill = display.getSystemColor(SWT.COLOR_WHITE);
                gc.setBackground(fill);
                gc.fillPolygon(closeShape);
                gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
                gc.drawPolygon(closeShape);
                break;
            }
            case 2 : {
                Color fill = new Color(display, new RGB(252, 160, 160));
                gc.setBackground(fill);
                gc.fillPolygon(closeShape);
                gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
                gc.drawPolygon(closeShape);
                break;
            }
            case 3 : {
                Color fill = display.getSystemColor(SWT.COLOR_RED);
                gc.setBackground(fill);
                gc.fillPolygon(closeShape);
                gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
                gc.drawPolygon(closeShape);
                break;
            }
        }
    }
    private void drawBackground(Display display, GC gc) {
        final Color[] colors = new Color[]{display.getSystemColor(SWT.COLOR_GRAY), display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_GRAY), display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_LIST_SELECTION)};
        int[] percents = new int[]{40, 25, 25, 25, 0};
        int x = 0;
        int y = 0;
        int pos = percents[percents.length - 1] * height / 100;
        gc.fillRectangle(x, y, width, pos);

        Color lastColor = colors[colors.length - 1];
        for (int i = percents.length - 1; i >= 0; i--) {
            gc.setForeground(lastColor);
            lastColor = colors[i];
            gc.setBackground(lastColor);
            int gradientHeight = percents[i] * height / 100;
            gc.fillGradientRectangle(x, y + pos, width, gradientHeight, true);
            pos += gradientHeight;
        }
    }
    private void createContents(final Shell shell) {
        final Display display = shell.getDisplay();
        ClassLoader cl = shell.getClass().getClassLoader();
        final Image image = new Image(display, cl.getResourceAsStream("img/swt/paste.png"));


        final Rectangle bounds = image.getBounds();
        final PaintListener close = new PaintListener() {
            public void paintControl(PaintEvent event) {
                GC gc = event.gc;
                drawClose(display, gc, closeState);

            }

        };
        Listener l = new Listener() {
            Point origin;
            boolean closebt = false;
            Rectangle rect = new Rectangle(closex, closey, 9, 9);
            public void handleEvent(Event e) {
                switch (e.type) {
                    case SWT.MouseDown : {
                        Point pt = new Point(e.x, e.y);
                        if ((e.button == 1) && rect.contains(pt)) {
                            closeState = 3;
                            e.gc = new GC(shell);
                            close.paintControl(new PaintEvent(e));
                            closebt = true;
                        } else if (e.button == 1) {
                            origin = pt;
                        }
                        break;
                    }
                    case SWT.MouseUp : {
                        origin = null;
                        Point pt = new Point(e.x, e.y);
                        if (closebt && rect.contains(pt))
                            shell.dispose();
                        else
                            closebt = false;
                        break;
                    }
                    case SWT.MouseMove : {
                        if (origin != null) {
                            Point p = display.map(shell, null, e.x, e.y);
                            shell.setLocation(p.x - origin.x, p.y - origin.y);
                        } else {
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
            Point size = shell.getSize();
            public void paintControl(PaintEvent event) {
                GC gc = event.gc;
                drawBackground(display, gc);
                gc.drawImage(image, 0, 0, bounds.width, bounds.height, -5, 5, size.x + 10, size.y - 5);
            }

        });
        shell.addPaintListener(close);
    }
    
    public void open() {
        
        Shell shell = new Shell(getParent(), getStyle());
        Region region = new Region();
       
        region.add(shape);
        // region.subtract(new Rectangle(0, 15, 38, 63));

        shell.setRegion(region);
        shell.setSize(region.getBounds().width, region.getBounds().height);
        createContents(shell);
       // shell.setLocation(parent.getBounds().x + (parent.getSize().x - shell.getSize().x) / 2, parent.getBounds().y + (parent.getSize().y - shell.getSize().y) / 2);
        shell.open();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
            region.dispose();
        }
    }
    
}