package org.superbiz.moviefun.albums;

import org.apache.tika.Tika;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.superbiz.moviefun.blobstore.Blob;
import org.superbiz.moviefun.blobstore.BlobStore;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static java.lang.ClassLoader.getSystemResource;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.nio.file.Files.readAllBytes;

@Controller
@RequestMapping("/albums")
public class AlbumsController {

    private final AlbumsBean albumsBean;
    private final BlobStore blobStore;

    public AlbumsController(AlbumsBean albumsBean, BlobStore blobStore) {
        this.albumsBean = albumsBean;
        this.blobStore = blobStore;
    }


    @GetMapping
    public String index(Map<String, Object> model) {
        model.put("albums", albumsBean.getAlbums());
        return "albums";
    }

    @GetMapping("/{albumId}")
    public String details(@PathVariable long albumId, Map<String, Object> model) {
        model.put("album", albumsBean.find(albumId));
        return "albumDetails";
    }

    @PostMapping("/{albumId}/cover")
    public String uploadCover(@PathVariable long albumId, @RequestParam("file") MultipartFile uploadedFile) throws IOException {
        saveUploadToBlobStore(uploadedFile, albumId);

        return format("redirect:/albums/%d", albumId);
    }

    @GetMapping("/{albumId}/cover")
    public HttpEntity<byte[]> getCover(@PathVariable long albumId) throws IOException, URISyntaxException {
        Optional<Blob> coverBlob = blobStore.get(makeBlobName(albumId));
        byte[] imageBytes;
        String contentType;

        if (coverBlob.isPresent()) {
            imageBytes = readAllBytesFromStream(coverBlob.get().inputStream);
            contentType = coverBlob.get().contentType;
        } else {
            imageBytes = readDefaultImageBytes();
            contentType = MediaType.IMAGE_JPEG_VALUE;
        }

        HttpHeaders headers = createImageHttpHeaders(contentType, imageBytes);

        return new HttpEntity<>(imageBytes, headers);
    }

    private byte[] readAllBytesFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int numRead;
        byte[] buffer = new byte[1024];
        while ((numRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, numRead);
        }
        return baos.toByteArray();
    }

    private byte[] readDefaultImageBytes() throws IOException {
        return readAllBytesFromStream(getSystemResourceAsStream("default-cover.jpg"));
    }


    private void saveUploadToBlobStore(@RequestParam("file") MultipartFile uploadedFile, long albumId) throws IOException {
        String blobName = makeBlobName(albumId);
        System.out.println("Making blob: " + blobName + ", " + uploadedFile.getContentType());
        Blob blob = new Blob(blobName, uploadedFile.getInputStream(), uploadedFile.getContentType());
        blobStore.put(blob);
    }

    private String makeBlobName(long albumId) {
        return format("covers-%d", albumId);
    }

    private HttpHeaders createImageHttpHeaders(String contentType, byte[] imageBytes) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(imageBytes.length);
        return headers;
    }
}
