//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.swing.ImageIcon;

import jd.gui.swing.components.JDLabelContainer;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;

public class HostPluginWrapper extends PluginWrapper implements JDLabelContainer {
    private static final ArrayList<HostPluginWrapper> HOST_WRAPPER = new ArrayList<HostPluginWrapper>();

    private static final ReentrantReadWriteLock       lock         = new ReentrantReadWriteLock();
    public static final ReadLock                      readLock     = lock.readLock();
    public static final WriteLock                     writeLock    = lock.writeLock();

    public static ArrayList<HostPluginWrapper> getHostWrapper() {
        throw new WTFException();
    }

    public static boolean hasPlugin(final String data) {
        throw new WTFException();
    }

    public HostPluginWrapper(final String host, final String classNamePrefix, final String className, final String patternSupported, final int flags, final String revision) {
        super(host, classNamePrefix, className, patternSupported, flags, revision);
        throw new WTFException();
    }

    public HostPluginWrapper(final String host, final String simpleName, final String pattern, final int flags, final String revision) {
        this(host, "jd.plugins.hoster.", simpleName, pattern, flags, revision);
        throw new WTFException();
    }

    @Override
    public PluginForHost getPlugin() {
        throw new WTFException();
    }

    @Override
    public PluginForHost getNewPluginInstance() {
        throw new WTFException();
    }

    public boolean isPremiumEnabled() {
        throw new WTFException();
    }

    @Override
    public String toString() {
        throw new WTFException();
    }

    public ImageIcon getIcon() {
        throw new WTFException();
    }

    public String getLabel() {
        throw new WTFException();
    }

    @Override
    public boolean equals(Object obj) {
        throw new WTFException();
    }

    @Override
    public int hashCode() {
        throw new WTFException();
    }

}
