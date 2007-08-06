package jd.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

public class Utilities {
    private static ResourceBundle resourceBundle = null;
    private static Locale locale = null;
    /**
     * Alle verfügbaren Bilder werden hier gespeichert
     */
    private static HashMap<String, ImageIcon> images = new HashMap<String, ImageIcon>();
    
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
    public static ImageIcon getImage(String imageName){
        return images.get(imageName);
    }
    public static void addImage(String iconName, ImageIcon icon){
        images.put(iconName, icon);
    }
    
    private static class FilterJAR implements FileFilter{
        
        public boolean accept(File f) {
            if(f.getName().endsWith(".jar"))
                return true;
            else
                return false;
        }
    }
}
