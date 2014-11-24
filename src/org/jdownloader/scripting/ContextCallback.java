package org.jdownloader.scripting;

import net.sourceforge.htmlunit.corejs.javascript.ErrorReporter;
import net.sourceforge.htmlunit.corejs.javascript.Evaluator;
import net.sourceforge.htmlunit.corejs.javascript.Script;

public interface ContextCallback {

    String onBeforeSourceCompiling(String source, Evaluator compiler, ErrorReporter compilationErrorReporter, String sourceName, int lineno, Object securityDomain);

    Script onAfterSourceCompiling(Script ret, String source, Evaluator compiler, ErrorReporter compilationErrorReporter, String sourceName, int lineno, Object securityDomain);

}
