package nz.ac.auckland.lablet.mailer;

import android.content.Context;
import android.support.annotation.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The class provides access to files in the external files directory.
 * This is where server configuration files are stored in the public
 * release.
 */
class FileHelper {

    /**
     * Gets an {@link InputStream} for a file in the external files directory.
     * @param context the context from which to read the external files directory
     * @param fileName the name of the file
     * @return the input stream
     */
    @NonNull
    static InputStream getInputStream(@NonNull Context context, @NonNull String fileName)
        throws IOException {
        return new FileInputStream(new File(context.getExternalFilesDir(null), fileName));
    }
}
