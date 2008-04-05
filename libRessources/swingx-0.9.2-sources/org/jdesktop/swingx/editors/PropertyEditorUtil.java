/*
 * PropertyEditorUtil.java
 *
 * Created on August 16, 2006, 7:09 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.editors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author joshy
 */
public class PropertyEditorUtil {
    //the text could be in many different formats. All of the supported formats are as follows:
    //(where x and y are doubles of some form)
    //[x,y]
    //[x y]
    //x,y]
    //[x,y
    //[ x , y ] or any other arbitrary whitespace
    // x , y ] or any other arbitrary whitespace
    //[ x , y  or any other arbitrary whitespace
    //x,y
    // x , y (or any other arbitrary whitespace)
    //x y
    // (empty space)
    //null
    //[]
    //[ ]
    //any other value throws an IllegalArgumentException
    public static Object createValueFromString(String text, int count, Class objectClass, Class paramClass) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        // strip all extra whitespace
        text = text.replaceAll("[\\[|,| |\\]]"," ");
        text = text.replaceAll("\\s+"," ");
        text = text.trim();
//        u.p("text = " + text);
        if (text == null || text.equals("") || text.equals("null")) {
            return null;
        }
        // split by spaces
        String[] strs = text.split(" ");
//        u.p("split:");
//        u.p(strs);
//        u.p("len = " + strs.length);
        if(strs.length != count) {
            return null;
        }
        Object[] params = new Object[count];
        Class[] paramClasses = new Class[count];
        for(int i=0; i<strs.length; i++) {
            if(paramClass == int.class) {
                params[i] = Integer.valueOf(strs[i]);
                paramClasses[i] = paramClass;
            }
            if(paramClass == double.class) {
                params[i] = Double.valueOf(strs[i]);
                paramClasses[i] = paramClass;
            }
        }
//        u.p("parms = ");
//        u.p(params);
        Constructor con = objectClass.getConstructor(paramClasses);
        return con.newInstance(params);
    }
    
}
