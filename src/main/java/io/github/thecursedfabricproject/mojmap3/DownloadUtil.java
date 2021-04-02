package io.github.thecursedfabricproject.mojmap3;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class DownloadUtil {
    private DownloadUtil() { }

    public static InputStream get(URL url, String name) {
        try {
            return new FileInputStream(name);
        } catch (FileNotFoundException e1) {
            try {
                ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(name);
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                readableByteChannel.close();
                fileOutputStream.close();
                return new FileInputStream(name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
    }
}
