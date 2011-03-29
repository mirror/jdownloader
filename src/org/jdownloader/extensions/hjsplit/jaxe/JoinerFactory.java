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

import jd.controlling.JDLogger;

public class JoinerFactory {
    public static JAxeJoiner getJoiner(File FileName) {
        return JoinerFactory.getJoiner(FileName, FileName.getParentFile());
    }

    public static JAxeJoiner getJoiner(File FileName, File DestDir) {

        if (DestDir == null) { return JoinerFactory.getJoiner(FileName); }
        String sFileName = FileName.getAbsolutePath();
        String sDestDir = DestDir.getAbsolutePath();
        if (SplitFileFilter.isSplitFile(sFileName) || SplitFileFilter.isZippedSplitFile(sFileName)) {
            JDLogger.getLogger().info("Normal split found");
            return new JAxeJoiner(sFileName, sDestDir);
        }
        if (UnixSplitFileFilter.isSplitFile(sFileName)) {
            JDLogger.getLogger().info("Unix split found");
            return new UnixSplitJoiner(sFileName, sDestDir);
        }

        return null;
    }
}
