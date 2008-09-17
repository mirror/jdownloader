package jd.utils;

public interface ProcessListener {
    /**
     * 
     * @param exec  Source Object
     * @param latest  Die zuletzte gelesene zeile. \b chars werden als new line char angesehen
     * @param buffer  Der complette BUffer  (exec.getInputStringBuffer()| exec.getErrorStringBuffer())
     */
 abstract public void onProcess(Executer exec, String latestLine,StringBuffer buffer);

    public abstract void onBufferChanged(Executer exec, StringBuffer buffer);
}
