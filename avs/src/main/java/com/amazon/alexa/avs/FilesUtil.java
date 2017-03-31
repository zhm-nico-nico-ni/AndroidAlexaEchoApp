package com.amazon.alexa.avs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Created by ggec on 2017/3/30.
 */

public class FilesUtil {
    public static long copyReplaceExisting(InputStream var0, String var1) throws IOException {
        Path path = Paths.get(var1);
        return Files.copy(var0, path, StandardCopyOption.REPLACE_EXISTING);
    }
}
