package com.example.music.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class LocalFileController {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileController.class);
    private static final MediaType SVG_MEDIA_TYPE = MediaType.parseMediaType("image/svg+xml");
    private static final MediaType MP3_MEDIA_TYPE = MediaType.parseMediaType("audio/mpeg");
    private static final MediaType WAV_MEDIA_TYPE = MediaType.parseMediaType("audio/wav");
    private static final byte[] FALLBACK_AUDIO = createFallbackAudio();

    @Value("${local.file.storage-path}")
    private String storagePath;

    @GetMapping("/user01/{fileName:.+\\..+}")
    public ResponseEntity<byte[]> getLegacyMusic(@PathVariable String fileName, HttpServletRequest request) {
        return getAudioResponse("song/" + fileName, request);
    }

    @GetMapping("/user01/singer/img/{fileName:.+}")
    public ResponseEntity<byte[]> getLegacySingerImage(@PathVariable String fileName) {
        return getImageResponse("img/singerPic/" + fileName, "Singer", "No image", "#355c7d", "#6c5b7b");
    }

    @GetMapping("/user01/songlist/{fileName:.+}")
    public ResponseEntity<byte[]> getLegacySongListImage(@PathVariable String fileName) {
        return getImageResponse("img/songListPic/" + fileName, "Playlist", "No cover", "#4568dc", "#b06ab3");
    }

    @GetMapping("/user01/singer/song/{fileName:.+}")
    public ResponseEntity<byte[]> getLegacySongImage(@PathVariable String fileName) {
        return getImageResponse("img/songPic/" + fileName, "Song", "No cover", "#134e5e", "#71b280");
    }

    @GetMapping("/img/avatorImages/{fileName:.+}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String fileName) {
        return getImageResponse("img/avatorImages/" + fileName, "User", "No avatar", "#1f4037", "#99f2c8");
    }

    @GetMapping("/img/singerPic/{fileName:.+}")
    public ResponseEntity<byte[]> getSingerImage(@PathVariable String fileName) {
        return getImageResponse("img/singerPic/" + fileName, "Singer", "No image", "#355c7d", "#6c5b7b");
    }

    @GetMapping("/img/songPic/{fileName:.+}")
    public ResponseEntity<byte[]> getSongImage(@PathVariable String fileName) {
        return getImageResponse("img/songPic/" + fileName, "Song", "No cover", "#134e5e", "#71b280");
    }

    @GetMapping({"/img/songListPic/{fileName:.+}", "/img/songlist/{fileName:.+}"})
    public ResponseEntity<byte[]> getSongListImage(@PathVariable String fileName) {
        return getImageResponse("img/songListPic/" + fileName, "Playlist", "No cover", "#4568dc", "#b06ab3");
    }

    @GetMapping("/img/swiper/{fileName:.+}")
    public ResponseEntity<byte[]> getBannerImage(@PathVariable String fileName) {
        return getImageResponse("img/swiper/" + fileName, "Banner", "No image", "#0f2027", "#2c5364");
    }

    @GetMapping("/song/{fileName:.+\\..+}")
    public ResponseEntity<byte[]> getSong(@PathVariable String fileName, HttpServletRequest request) {
        return getAudioResponse("song/" + fileName, request);
    }

    private ResponseEntity<byte[]> getImageResponse(String relativePath, String title, String subtitle, String startColor, String endColor) {
        Path filePath = resolvePath(relativePath);
        if (filePath != null && Files.exists(filePath)) {
            return buildFileResponse(filePath, determineMediaType(filePath, MediaType.IMAGE_JPEG));
        }
        return buildByteResponse(buildSvgPlaceholder(title, subtitle, startColor, endColor), SVG_MEDIA_TYPE);
    }

    private ResponseEntity<byte[]> getAudioResponse(String relativePath, HttpServletRequest request) {
        Path filePath = resolvePath(relativePath);
        if (filePath != null && Files.exists(filePath)) {
            return buildAudioResponse(filePath, determineMediaType(filePath, MP3_MEDIA_TYPE), request);
        }
        return buildByteResponse(FALLBACK_AUDIO, WAV_MEDIA_TYPE);
    }

    private Path resolvePath(String relativePath) {
        Path basePath = Paths.get(storagePath).toAbsolutePath().normalize();
        Path resolvedPath = basePath.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(basePath)) {
            logger.warn("Rejected suspicious path: {}", relativePath);
            return null;
        }
        return resolvedPath;
    }

    private ResponseEntity<byte[]> buildFileResponse(Path filePath, MediaType mediaType) {
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            return buildByteResponse(bytes, mediaType);
        } catch (IOException e) {
            logger.error("Failed to read media file: {}", filePath, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<byte[]> buildAudioResponse(Path filePath, MediaType mediaType, HttpServletRequest request) {
        try {
            long fileSize = Files.size(filePath);
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);

            if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(mediaType);
                headers.setContentLength(fileSize);
                headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
                return new ResponseEntity<>(Files.readAllBytes(filePath), headers, HttpStatus.OK);
            }

            long start = 0;
            long end = fileSize - 1;
            String[] ranges = rangeHeader.substring("bytes=".length()).split("-", 2);

            if (!ranges[0].isEmpty()) {
                start = Long.parseLong(ranges[0]);
            } else if (ranges.length > 1 && !ranges[1].isEmpty()) {
                long suffixLength = Long.parseLong(ranges[1]);
                start = Math.max(fileSize - suffixLength, 0);
            }

            if (ranges.length > 1 && !ranges[1].isEmpty() && !rangeHeader.endsWith("-")) {
                end = Long.parseLong(ranges[1]);
            }

            if (start > end || start >= fileSize) {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
                headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
                return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
            }

            end = Math.min(end, fileSize - 1);
            int contentLength = (int) (end - start + 1);
            byte[] bytes = new byte[contentLength];

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "r")) {
                randomAccessFile.seek(start);
                randomAccessFile.readFully(bytes);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(contentLength);
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            return new ResponseEntity<>(bytes, headers, HttpStatus.PARTIAL_CONTENT);
        } catch (IOException | NumberFormatException e) {
            logger.error("Failed to stream audio file: {}", filePath, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<byte[]> buildByteResponse(byte[] bytes, MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private MediaType determineMediaType(Path filePath, MediaType defaultType) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (fileName.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (fileName.endsWith(".svg")) {
            return SVG_MEDIA_TYPE;
        }
        if (fileName.endsWith(".mp3")) {
            return MP3_MEDIA_TYPE;
        }
        if (fileName.endsWith(".wav")) {
            return WAV_MEDIA_TYPE;
        }
        if (fileName.endsWith(".ogg")) {
            return MediaType.parseMediaType("audio/ogg");
        }
        if (fileName.endsWith(".aac")) {
            return MediaType.parseMediaType("audio/aac");
        }
        if (fileName.endsWith(".flac")) {
            return MediaType.parseMediaType("audio/flac");
        }
        if (fileName.endsWith(".m4a")) {
            return MediaType.parseMediaType("audio/mp4");
        }
        return defaultType;
    }

    private byte[] buildSvgPlaceholder(String title, String subtitle, String startColor, String endColor) {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"640\" height=\"640\" viewBox=\"0 0 640 640\">"
                + "<defs><linearGradient id=\"bg\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">"
                + "<stop offset=\"0%\" stop-color=\"" + startColor + "\"/>"
                + "<stop offset=\"100%\" stop-color=\"" + endColor + "\"/>"
                + "</linearGradient></defs>"
                + "<rect width=\"640\" height=\"640\" rx=\"40\" fill=\"url(#bg)\"/>"
                + "<circle cx=\"320\" cy=\"240\" r=\"96\" fill=\"rgba(255,255,255,0.16)\"/>"
                + "<text x=\"320\" y=\"370\" text-anchor=\"middle\" font-size=\"54\" font-family=\"Arial, sans-serif\" fill=\"#ffffff\">" + title + "</text>"
                + "<text x=\"320\" y=\"430\" text-anchor=\"middle\" font-size=\"28\" font-family=\"Arial, sans-serif\" fill=\"rgba(255,255,255,0.85)\">" + subtitle + "</text>"
                + "</svg>";
        return svg.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] createFallbackAudio() {
        int sampleRate = 22050;
        int seconds = 2;
        int totalSamples = sampleRate * seconds;
        int bytesPerSample = 2;
        int dataSize = totalSamples * bytesPerSample;

        try (ByteArrayOutputStream output = new ByteArrayOutputStream(44 + dataSize)) {
            writeAscii(output, "RIFF");
            writeLittleEndianInt(output, 36 + dataSize);
            writeAscii(output, "WAVE");
            writeAscii(output, "fmt ");
            writeLittleEndianInt(output, 16);
            writeLittleEndianShort(output, (short) 1);
            writeLittleEndianShort(output, (short) 1);
            writeLittleEndianInt(output, sampleRate);
            writeLittleEndianInt(output, sampleRate * bytesPerSample);
            writeLittleEndianShort(output, (short) bytesPerSample);
            writeLittleEndianShort(output, (short) 16);
            writeAscii(output, "data");
            writeLittleEndianInt(output, dataSize);

            for (int i = 0; i < totalSamples; i++) {
                double angle = (2 * Math.PI * 440 * i) / sampleRate;
                double envelope = 1.0 - ((double) i / totalSamples);
                short sample = (short) (Math.sin(angle) * 10000 * envelope);
                writeLittleEndianShort(output, sample);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build fallback audio", e);
        }
    }

    private static void writeAscii(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeLittleEndianInt(ByteArrayOutputStream output, int value) throws IOException {
        output.write(value & 0xFF);
        output.write((value >> 8) & 0xFF);
        output.write((value >> 16) & 0xFF);
        output.write((value >> 24) & 0xFF);
    }

    private static void writeLittleEndianShort(ByteArrayOutputStream output, short value) throws IOException {
        output.write(value & 0xFF);
        output.write((value >> 8) & 0xFF);
    }
}
