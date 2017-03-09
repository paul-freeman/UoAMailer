package nz.ac.auckland.lablet.mailer;

import android.content.Context;
import android.support.annotation.NonNull;
import java.io.IOException;
import java.io.InputStream;

/**
 * The class provides access to files in the assets files directory.
 * This is where server configuration files are stored in the private
 * University of Auckland release.
 */
class FileHelper {

    /**
     * Gets an {@link InputStream} for a file in assets.
     * @param context the context from which to read the assets
     * @param fileName the name of the file
     * @return the input stream, or null
     */
    @NonNull
    static InputStream getInputStream(@NonNull Context context, @NonNull String fileName)
        throws IOException {
        return context.getAssets().open(fileName);
    }
}
