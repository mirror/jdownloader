package jd.unrar;
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


import java.io.IOException;

public abstract class AxeWorker extends Thread
{
    protected AxeEventListener ael = null;
    protected ProgressEventListener pel = null;
    protected boolean bStopped;
    protected long lJobSize;
    protected ProgressEvent pe;
    protected long lCurrent;

    public void setEventListener (AxeEventListener ael)
    {
        this.ael = ael;
    }

    public AxeEventListener getEventListener()
    {
        return ael;
    }

    protected void dispatchEvent (AxeEvent ae)
    {
        if (ael != null)
            ael.handleEvent (ae);
    }

    public void setProgressEventListener (ProgressEventListener pel)
    {
        this.pel = pel;
    }

    public ProgressEventListener getProgressEventListener()
    {
        return pel;
    }

    protected void dispatchProgressEvent (ProgressEvent pe)
    {
        if (pel != null)
            pel.handleEvent (pe);
    }

    protected void initProgress()
    {
        lCurrent = 0;
        pe = new ProgressEvent (this, 0, lJobSize);
    }

    protected void dispatchProgress ()
    {
        pe.setCurrent (lCurrent);
        dispatchProgressEvent (pe);
    }

    protected void dispatchProgress (long l)
    {
        lCurrent = l;
        dispatchProgress();
    }

    protected void dispatchIncrementalProgress (long l)
    {
        lCurrent += l;
        dispatchProgress();
    }

    public void freeze()
    {
        bStopped = true;
//      interrupt();
        System.out.println ("Freezed!");
    }

    protected abstract void computeJobSize() throws IOException;
}
