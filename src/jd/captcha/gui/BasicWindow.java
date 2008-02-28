package jd.captcha.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import jd.captcha.utils.UTILITIES;

/**
 * Die Klasse dient als Window Basis Klasse.
 * 
 * @author JD-Team
 */
public class BasicWindow extends JFrame {
    /**
     * 
     */
    private static final long serialVersionUID = 8474181150357563979L;

    /**
     * Gibt an ob beim Schließen des fensters das programm beendet werden sol
     */
    public boolean            exitSystem       = false;

    /**
     * Aktuelle X Position der Autopositionierung
     */
    private static int        screenPosX       = 0;

    /**
     * Aktuelle Y Position der Autopositionierung
     */
    private static int        screenPosY       = 0;

    /**
     * Owner. Owner der GUI
     */
    public Object             owner;

    /**
     * Erstellt ein neues GUI Fenster mit dem Oner owner
     * 
     * @param owner
     */
    public BasicWindow(Object owner) {
        this.owner = owner;
        initWindow();
    }

    /**
     * Erstellt ein einfaches neues GUI Fenster
     */
    public BasicWindow() {
        initWindow();
    }

    /**
     * Gibt die default GridbagConstants zurück
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     * @return Default GridBagConstraints
     */
    public GridBagConstraints getGBC(int x, int y, int width, int height) {

        GridBagConstraints gbc = UTILITIES.getGBC(x, y, width, height);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;

        return gbc;
    }

    /**
     * Initialisiert das Fenster und setzt den WindowClosing Adapter
     */
    private void initWindow() {
        final BasicWindow _this = this;
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                Window window = event.getWindow();
                _this.setVisible(true);
                window.setVisible(false);
                window.dispose();
                if (_this.exitSystem) {
                    System.exit(0);
                }
            }

        });

        resizeWindow(100);
        setLocationByScreenPercent(50, 50);
        setBackground(Color.LIGHT_GRAY);
    }

    /**
     * Gibt das Fenster wieder frei
     */
    public void destroy() {
        setVisible(false);
        dispose();
    }

    /**
     * Prozentuales (im bezug aufd en Screen) setzend er größe
     * 
     * @param percent
     *            in screenProzent
     */
    public void resizeWindow(int percent) {
        Dimension screenSize = getToolkit().getScreenSize();
        setSize((screenSize.width * percent) / 100, (screenSize.height * percent) / 100);
    }

    /**
     * Skaliert alle Komponenten und das fenster neu
     */
    public void refreshUI() {
        this.pack();
        this.repack();
    }

    public void pack() {
        try{
        super.pack();
        Dimension screenSize = getToolkit().getScreenSize();
        int newWidth = (int) Math.min(this.getSize().width, screenSize.getWidth());
        int newHeight = (int) Math.min(this.getSize().height, screenSize.getHeight());
        this.setSize(newWidth, newHeight);
        }catch(Exception e){
            
        }
    }

    /**
     * packt das fenster neu
     */
    public void repack() {
        final BasicWindow _this=this;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SwingUtilities.updateComponentTreeUI(_this);

            }
        });
       
    }

    /**
     * Prozentuales Positionsetzen des fensters (Mittelpunkt
     * 
     * @param width
     *            in screenprozent
     * @param height
     *            in ScreenProzent
     */
    public void setLocationByScreenPercent(int width, int height) {
        Dimension screenSize = getToolkit().getScreenSize();

        setLocation(((screenSize.width - getSize().width) * width) / 100, ((screenSize.height - getSize().height) * height) / 100);
    }
/**
 * Fügt relative Threadsafe  an x,y die Kompoente cmp ein
 * @param x
 * @param y
 * @param cmp
 */
    public void setComponent(final int x, final int y, final Component cmp) {
        if(cmp==null)return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                add(cmp, getGBC(x, y, 1, 1));

            }
        });
    }
    /**
     * Fügt relative Threadsafe  an x,y den text cmp ein
     * @param x
     * @param y
     * @param cmp
     */
    public void setText(final int x, final int y, final Object cmp) {
        if(cmp==null)return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                add(new JLabel(cmp.toString()), getGBC(x, y, 1, 1));

            }
        });
    }
    /**
     * Fügt relative Threadsafe  an x,y das Bild img ein
     * @param x
     * @param y
     * @param img 
     */
    public void setImage(final int x, final int y, final Image img) {
if(img==null)return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                add(new ImageComponent(img), getGBC(x, y, 1, 1));

            }
        });
    }

    /**
     * Zeigt ein Image in einem Neuen fenster an. Die fenster Positionieren sich
     * von Links oben nach rechts uten von selbst
     * 
     * @param file
     * @param title
     * @return Neues Fenster
     */

    public static BasicWindow showImage(File file, String title) {
        Dimension screenSize = new JFrame().getToolkit().getScreenSize();
        Image img = UTILITIES.loadImage(file);
        BasicWindow w = new BasicWindow();
        ImageComponent ic = new ImageComponent(img);

        w.setSize(ic.getImageWidth() + 10, ic.getImageHeight() + 20);
        w.setLocation(screenPosX, screenPosY);
        screenPosY += ic.getImageHeight() + 40;
        if (screenPosY >= screenSize.height) {
            screenPosX += ic.getImageWidth() + 160;
            screenPosY = 0;
        }
        w.setTitle(title);
        w.setLayout(new GridBagLayout());
        w.add(ic, UTILITIES.getGBC(0, 0, 1, 1));
        w.setVisible(true);
        w.pack();
        w.repack();

        return w;

    }

    /**
     * Zeigt ein Image in einem neuen fenster an.Die fenster Positionieren sich
     * von Links oben nach rechts uten von selbst
     * 
     * @param img
     * @return BasicWindow Das neue fenster
     */
    public static BasicWindow showImage(Image img) {

        return showImage(img, img.toString());
    }

    /**
     * Zeigt ein image in einem Neuen fenster an. Das fenster positioniert sich
     * im nächsten Freien bereich
     * 
     * @param img
     * @param title
     * @return BasicWindow das neue fenster
     */
    public static BasicWindow showImage(Image img, String title) {
        Dimension screenSize = new JFrame().getToolkit().getScreenSize();

        BasicWindow w = new BasicWindow();
        ImageComponent ic = new ImageComponent(img);

        w.setSize(ic.getImageWidth() + 10, ic.getImageHeight() + 20);
        w.repack();
        w.pack();
        w.setLocation(screenPosX, screenPosY);
        screenPosY += w.getSize().width + 30;
        if (screenPosY >= screenSize.height) {
            screenPosX += w.getSize().height + 160;
            screenPosY = 0;
        }
        w.setTitle(title);
        w.setLayout(new GridBagLayout());
        w.add(ic, UTILITIES.getGBC(0, 0, 1, 1));
        w.setVisible(true);

        w.repack();
        w.pack();

        return w;

    }

    /**
     * Zeigt ein Bild an und setzt es um width/Height zum letzten Bild auf den
     * Screen
     * 
     * @param img
     * @param width
     * @param height
     * @return Neues Fenster
     */
    public static BasicWindow showImage(Image img, int width, int height) {
        Dimension screenSize = new JFrame().getToolkit().getScreenSize();

        BasicWindow w = new BasicWindow();
        ImageComponent ic = new ImageComponent(img);

        w.setSize(width, height);

        w.setLocation(screenPosX, screenPosY);
        screenPosY += height + 30;
        if (screenPosY >= screenSize.height) {
            screenPosX += width + 40;
            screenPosY = 0;
        }

        w.setLayout(new GridBagLayout());
        w.add(ic, UTILITIES.getGBC(0, 0, 1, 1));
        w.setVisible(true);

        w.repack();
        w.pack();

        return w;

    }

    /**
     * @param title
     * @param width
     * @param height
     * @return neues Fenster
     */
    public static BasicWindow getWindow(String title, int width, int height) {
        Dimension screenSize = new JFrame().getToolkit().getScreenSize();

        BasicWindow w = new BasicWindow();

        w.setSize(width, height);

        w.setLocation(screenPosX, screenPosY);
        screenPosY += height + 30;
        if (screenPosY >= screenSize.height) {
            screenPosX += width + 40;
            screenPosY = 0;
        }

        w.setLayout(new GridBagLayout());
        w.setVisible(true);
        w.setTitle(title);
        w.repack();
        w.pack();

        return w;

    }
}