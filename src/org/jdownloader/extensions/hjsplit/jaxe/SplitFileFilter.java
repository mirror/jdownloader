/*
 * Copyright (C) 2002 - 2005 Leonardo Ferracci
 *
 * This file is part of JAxe.
 *
 * JAxe is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * JAxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with JAxe; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.  Or, visit http://www.gnu.org/copyleft/gpl.html
 */

package org.jdownloader.extensions.hjsplit.jaxe;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class SplitFileFilter extends FileFilter {
    public static String getJoinedFileName(String s) {
        int i = s.lastIndexOf(".001");

        if (i == -1) {
            return s;
        } else {
            return s.substring(0, i);
        }
    }

    public static boolean isSplitFile(String s) {
        return SplitFileFilter.isSplitFile(s, true);
    }

    public static boolean isSplitFile(String s, boolean b) {
        int i;

        if (s.endsWith(".001")) {
            return true;
        } else if (b) {
            if ((i = s.indexOf(".001.")) > 0 && s.lastIndexOf('.') == i + 4) { return true; }
        }
        return false;
    }

    public static boolean isZippedSplitFile(String s) {
        return s.toLowerCase().endsWith(".001.zip");
    }

    private boolean bIESafe;

    public SplitFileFilter() {
        this(true);
    }

    public SplitFileFilter(boolean b) {
        bIESafe = true;
    }

    //@Override
    public boolean accept(File f) {
        return SplitFileFilter.isSplitFile(f.getName(), bIESafe) || f.isDirectory() || SplitFileFilter.isZippedSplitFile(f.getName());
    }

    //@Override
    public String getDescription() {
        return "Files split using JAxe/HJSplit";
    }
}
