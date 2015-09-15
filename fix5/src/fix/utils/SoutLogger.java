package fix.utils;



import org.apache.log4j.Logger;

import java.io.PrintStream;

public class SoutLogger {
    private static final Logger logger = Logger.getLogger(SoutLogger.class);

    public static void tieSystemOutAndErrToLog() {
        System.setErr(createLoggingProxy(System.err));
    }

    public static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                realPrintStream.print(string);
                logger.error(string);
            }
        };
    }

}
