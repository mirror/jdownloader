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

package jd.plugins.optional.extraction.hjsplit.jaxe;

import java.io.IOException;

public abstract class AxeWorker extends Thread {
    protected AxeEventListener ael = null;
    protected boolean bStopped;
    protected long lCurrent;
    protected long lJobSize;
    protected ProgressEvent pe;
    protected ProgressEventListener pel = null;

    protected abstract void computeJobSize() throws IOException;

    protected void dispatchEvent(AxeEvent ae) {
        if (ael != null) {
            ael.handleEvent(ae);
        }
    }

    protected void dispatchIncrementalProgress(long l) {
        lCurrent += l;
        dispatchProgress();
    }

    protected void dispatchProgress() {
        pe.setCurrent(lCurrent);
        dispatchProgressEvent(pe);
    }

    protected void dispatchProgress(long l) {
        lCurrent = l;
        dispatchProgress();
    }

    protected void dispatchProgressEvent(ProgressEvent pe) {
        if (pel != null) {
            pel.handleEvent(pe);
        }
    }

    public void freeze() {
        bStopped = true;
        // interrupt();
        System.out.println("Freezed!");
    }

    public AxeEventListener getEventListener() {
        return ael;
    }

    public ProgressEventListener getProgressEventListener() {
        return pel;
    }

    protected void initProgress() {
        lCurrent = 0;
        pe = new ProgressEvent(this, 0, lJobSize);
    }

    public void setEventListener(AxeEventListener ael) {
        this.ael = ael;
    }

    public void setProgressEventListener(ProgressEventListener pel) {
        this.pel = pel;
    }
}