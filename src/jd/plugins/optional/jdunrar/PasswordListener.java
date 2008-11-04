package jd.plugins.optional.jdunrar;

import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.utils.DynByteBuffer;
import jd.utils.Executer;
import jd.utils.ProcessListener;

public class PasswordListener extends ProcessListener {

    private String password;

    private int lastLinePosition = 0;

    public PasswordListener(String pass) {
        this.password = pass;
    }

    @Override
    public void onBufferChanged(Executer exec, DynByteBuffer buffer, int latestNum) {

        String lastLine = new String(buffer.getLast(buffer.position() - lastLinePosition));
        if (new Regex(lastLine, Pattern.compile(".*?password.{0,200}: $", Pattern.CASE_INSENSITIVE)).matches()) {

            exec.writetoOutputStream(this.password);
        }
        // else if (new Regex(buffer.toString(),
        // ".*?current.*?password.*?ll ").matches()) {
        // debugmsg(buffer.toString());
        // exec.writetoOutputStream("A");
        // } else if (interruptafter > 0 && origin ==
        // exec.getInputStreamBuffer()) {
        // if (origin.position() >= interruptafter) {
        // exec.interrupt();
        // }
        // }
    }

    @Override
    public void onProcess(Executer exec, String latestLine, DynByteBuffer sb) {
        this.lastLinePosition = sb.position();
    }

}
