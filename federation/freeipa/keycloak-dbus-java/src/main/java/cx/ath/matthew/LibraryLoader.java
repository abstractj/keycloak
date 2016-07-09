package cx.ath.matthew;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class LibraryLoader {

    private static final Logger LOGGER = Logger.getLogger(LibraryLoader.class.getSimpleName());

    private static final String[] PATHS = {"/usr/lib/", "/usr/lib64/", "/usr/local/lib/", "/opt/local/lib/"};
    private static final String LIBRARY_NAME = "libunix_dbus_java";
    private static final String VERSION = "0.0.8";
    private static boolean loadSucceeded;

    public static void load() {
        for (String path : PATHS) {
            try {
                System.load(String.format("%s/%s.so.%s",path, LIBRARY_NAME, VERSION));
                loadSucceeded = true;
                break;
            } catch (UnsatisfiedLinkError e) {
                loadSucceeded = false;
            }

        }

        if (!loadSucceeded) LOGGER.log(Level.WARNING, "libunix_dbus_java not found\n" +
                "Please, make sure you have the package libunix-dbus-java installed.");
    }
}
