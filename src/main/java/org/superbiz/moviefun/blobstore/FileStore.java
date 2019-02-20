package org.superbiz.moviefun.blobstore;

import java.io.*;
import java.nio.file.Files;
import java.util.Optional;

public class FileStore implements BlobStore {
    private final static String BLOBSTORE_ROOT = "blobs/";

    @Override
    public void put(Blob blob) throws IOException {
        File blobDataFile = blobDataFile(blob.name);
        File metaDataFile = metaDataFile(blob.name);
        initialiseFile(blobDataFile);
        initialiseFile(metaDataFile);

        try (FileOutputStream outputStream = new FileOutputStream(blobDataFile)) {
            copyBlobStreamToOutputStream(blob, outputStream);
        }
        try (FileOutputStream outputStream = new FileOutputStream(metaDataFile)) {
            outputStream.write(blob.contentType.getBytes());
        }
    }

    private void copyBlobStreamToOutputStream(Blob blob, FileOutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = blob.inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
    }

    private static void initialiseFile(File file) throws IOException {
        file.delete();
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    private static File blobDataFile(String name) {
        return new File(BLOBSTORE_ROOT + name);
    }

    private static File metaDataFile(String name) {
        return new File(BLOBSTORE_ROOT + name + ".metadata");
    }

    @Override
    public Optional<Blob> get(String name) throws IOException {
        File blobFile = blobDataFile(name);
        if (blobFile.exists()) {
            InputStream inputStream = new FileInputStream(blobDataFile(name));
            String contentType = new String(Files.readAllBytes(metaDataFile(name).toPath()));
            return Optional.of(new Blob(name, inputStream, contentType));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void deleteAll() {
        // ...
    }
}
