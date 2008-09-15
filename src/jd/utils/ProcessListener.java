package jd.utils;

public interface ProcessListener {
 abstract public void onProcess(Executer exec, String err,String out,String latest);
}
