package jd.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

public class Utilities {
    private static ResourceBundle resourceBundle = null;
    private static Locale locale = null;
    /**
     * Alle verfügbaren Bilder werden hier gespeichert
     */
    private static HashMap<String, Image> images = new HashMap<String, Image>();

    public static FilterJAR filterJar = new FilterJAR();

    /**
     * Genau wie add, aber mit den Standardwerten iPadX,iPadY=0
     *
     * @param cont Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp Die Komponente, die hinzugefuegt werden soll
     * @param x X-Position innerhalb des GriBagLayouts
     * @param y Y-Position innerhalb des GriBagLayouts
     * @param width Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets Abstände der Komponente
     * @param fill Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y,
            int width, int height, int weightX, int weightY, Insets insets, int fill, int anchor) {
        addToGridBag(cont,comp,x,y,width,height,weightX,weightY,insets,0,0,fill,anchor);
    }
    /**
     * Diese Klasse fuegt eine Komponente einem Container hinzu
     *
     * @param cont Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp Die Komponente, die hinzugefuegt werden soll
     * @param x X-Position innerhalb des GriBagLayouts
     * @param y Y-Position innerhalb des GriBagLayouts
     * @param width Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets Abständer der Komponente
     * @param iPadX Leerraum zwischen einer GridBagZelle und deren Inhalt (X-Richtung)
     * @param iPadY Leerraum zwischen einer GridBagZelle und deren Inhalt (Y-Richtung)
     * @param fill Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp,
            int x,       int y,
            int width,   int height,
            int weightX, int weightY,
            Insets insets,
            int iPadX,   int iPadY,
            int fill,    int anchor) {
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = x;
        cons.gridy = y;
        cons.gridwidth = width;
        cons.gridheight = height;
        cons.weightx = weightX;
        cons.weighty = weightY;
        cons.fill = fill;
        cons.anchor = anchor;
        if (insets != null)
            cons.insets = insets;
        cons.ipadx = iPadX;
        cons.ipady = iPadY;
        cont.add(comp, cons);
    }
    /**
     * Liefert einen Punkt zurück, mit dem eine Komponente auf eine andere zentriert werden kann
     *
     * @param parent Die Komponente, an der ausgerichtet wird
     * @param child Die Komponente die ausgerichtet werden soll
     * @return Ein Punkt, mit dem diese Komponente mit der setLocation Methode zentriert dargestellt werden kann
     */
    public static Point getCenterOfComponent(Component parent, Component child){
        Point center;
        if (parent == null || !parent.isShowing()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width  = screenSize.width;
            int height = screenSize.height;
            center = new Point(width/2, height/2);
        }
        else{
            center = parent.getLocationOnScreen();
            center.x += parent.getWidth()/2;
            center.y += parent.getHeight()/2;
        }

        //Dann Auszurichtende Komponente in die Berechnung einfließen lassen
        center.x -= child.getWidth()/2;
        center.y -= child.getHeight()/2;
        return center;
    }
    /**
     * Liefert eine Zeichenkette aus dem aktuellen ResourceBundle zurück
     *
     * @param key Identifier der gewünschten Zeichenkette
     * @return Die gewünschte Zeichnenkette
     */
    public static String getResourceString(String key){
        if(resourceBundle== null){
            if(locale == null){
                locale = Locale.getDefault();
            }
            resourceBundle = ResourceBundle.getBundle("LanguagePack", locale);
        }
        return resourceBundle.getString(key);
    }
    /**
     * Liefert aus der Map der geladenen Bilder ein Element zurück
     *
     * @param imageName Name des Bildes das zurückgeliefert werden soll
     * @return Das gewünschte Bild oder null, falls es nicht gefunden werden kann
     */
    public static Image getImage(String imageName){
        return images.get(imageName);
    }
    /**
     * Fügt ein Bild zur Map hinzu
     *
     * @param imageName Name des Bildes, daß hinzugefügt werden soll
     * @param image Das hinzuzufügende Bild
     */
    public static void addImage(String imageName, Image image){
        images.put(imageName, image);
    }
    /**
     * Als FileFilter akzeptiert diese Klasse alle .jar Dateien
     *
     * @author astaldo
     */
    private static class FilterJAR implements FileFilter{
        public boolean accept(File f) {
            if(f.getName().endsWith(".jar"))
                return true;
            else
                return false;
        }
    }
}
