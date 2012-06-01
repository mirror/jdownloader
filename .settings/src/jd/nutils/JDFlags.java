//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nutils;

import org.appwork.utils.BinaryLogic;

/**
 * TODO: Remove with next major update and change to
 * {@link org.appwork.utils.BinaryLogic}
 */
public class JDFlags {

    public static boolean hasAllFlags(int status, int... flags) {
        return BinaryLogic.containsAll(status, flags);
    }

    public static boolean hasNoFlags(int status, int... flags) {
        return BinaryLogic.containsNone(status, flags);
    }

    public static boolean hasSomeFlags(int status, int... flags) {
        return BinaryLogic.containsSome(status, flags);
    }

    /**
     * &-Operation returns only bits which are set in both integers
     * 
     * @param curState
     * @param filtermask
     * @return curState&filtermask
     */
    public static int filterFlags(int curState, int filtermask) {
        return curState & filtermask;
    }

}
