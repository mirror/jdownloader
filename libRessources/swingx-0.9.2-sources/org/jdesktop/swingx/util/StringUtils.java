/*
 * StringUtils.java
 *
 * Created on November 12, 2006, 9:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author joshy
 */
public abstract class StringUtils {
    
    
    public static String[] regexSearch(String source, String pattern) {
        Pattern pat = Pattern.compile(pattern,Pattern.DOTALL);
        Matcher matcher = pat.matcher(source);
        matcher.find();
        String[] list = new String[matcher.groupCount()+1];
        for(int i=0; i<=matcher.groupCount(); i++) {
            list[i] = matcher.group(i);
        }
        return list;
    }
    
}
