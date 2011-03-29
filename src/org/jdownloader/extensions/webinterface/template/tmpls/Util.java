/*
 *      HTML.Template:  A module for using HTML Templates with java
 *
 *      Copyright (c) 2002 Philip S Tellis (philip.tellis@iname.com)
 *
 *      This module is free software; you can redistribute it
 *      and/or modify it under the terms of either:
 *
 *      a) the GNU General Public License as published by the Free
 *      Software Foundation; either version 1, or (at your option)
 *      any later version, or
 *
 *      b) the "Artistic License" which comes with this module.
 *
 *      This program is distributed in the hope that it will be
 *      useful, but WITHOUT ANY WARRANTY; without even the implied
 *      warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *      PURPOSE.  See either the GNU General Public License or the
 *      Artistic License for more details.
 *
 *      You should have received a copy of the Artistic License
 *      with this module, in the file ARTISTIC.  If not, I'll be
 *      glad to provide one.
 *
 *      You should have received a copy of the GNU General Public
 *      License along with this program; if not, write to the Free
 *      Software Foundation, Inc., 59 Temple Place, Suite 330,
 *      Boston, MA 02111-1307 USA
 */

package org.jdownloader.extensions.webinterface.template.tmpls;

public class Util {
    public static boolean debug = false;

    public static void debug_print(final Object o) {
        Util.debug_print(o.toString());
    }

    public static void debug_print(final String msg) {
        if (!debug) { return; }

        System.err.println(msg);
    }

    public static String escapeHTML(final String element) {
        String s = element;
//      String s = new String(element); // don't change the original
        // bobbel: a string object is immutable, so no one can change it ;)
        final String[] metas = { "&", "<", ">", "\"" };
        final String[] repls = { "&amp;", "&lt;", "&gt;", "&quot;" };
        for (int i = 0; i < metas.length; i++) {
            int pos = 0;
            do {
                pos = s.indexOf(metas[i], pos);
                if (pos < 0) {
                    break;
                }
                s = s.substring(0, pos) + repls[i] + s.substring(pos + 1);
                pos++;
            } while (pos >= 0);
        }
        return s;
    }

    public static String escapeQuote(final String element) {
        String s = element;
//      String s = new String(element); // don't change the original
        // bobbel: a string object is immutable, so no one can change it ;)
        final String[] metas = { "\"", "'" };
        final String[] repls = { "\\\"", "\\'" };
        for (int i = 0; i < metas.length; i++) {
            int pos = 0;
            do {
                pos = s.indexOf(metas[i], pos);
                if (pos < 0) {
                    break;
                }
                s = s.substring(0, pos) + repls[i] + s.substring(pos + 1);
                pos++;
            } while (pos >= 0);
        }
        return s;
    }

    public static String escapeURL(final String url) {
        final StringBuilder s = new StringBuilder();
        final String no_escape = "./-_";

        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (!Character.isLetterOrDigit(c) && no_escape.indexOf(c) < 0) {
                String h = Integer.toHexString(c);
                s.append("%");
                if (h.length() < 2) {
                    s.append("0");
                }
                s.append(h);
            } else {
                s.append(c);
            }
        }
        return s.toString();
    }

    // method is quite useless, isn't it?
//    public static boolean isNameChar(char c) {
//        return true;
//    }

    public static boolean isNameChar(final String s) {
        final String alt_valid = "./+-_";

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isLetterOrDigit(s.charAt(i)) && alt_valid.indexOf(s.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }
}
