/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.utils;

import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.exceptions.AuthenticationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.TypeLiteral;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.codenvy.im.utils.Version.compare;
import static com.codenvy.im.utils.Version.valueOf;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class Commons {

    /** Simplifies the way to combine paths. Takes care about normalization. */
    public static String combinePaths(String apiEndpoint, String path) {
        if (apiEndpoint.endsWith("/")) {
            if (path.startsWith("/")) {
                return apiEndpoint + path.substring(1);
            } else {
                return apiEndpoint + path;
            }
        } else {
            if (path.startsWith("/")) {
                return apiEndpoint + path;
            } else {
                return apiEndpoint + "/" + path;
            }
        }
    }

    /** Copies input stream to output stream, and stop copying if current thread was interrupted. */
    public static void copyInterruptable(InputStream in, OutputStream out) throws CopyStreamInterruptedException, IOException {
        byte[] buffer = new byte[8196];
        int length;
        while ((length = in.read(buffer)) != -1) {
            if (currentThread().isInterrupted()) {
                throw new CopyStreamInterruptedException("The copying was interrupted.");
            }

            out.write(buffer, 0, length);
        }
    }

    /** Adds query parameter to url. */
    public static String addQueryParam(String path, String key, String value) {
        return path + (path.contains("?") ? "&" : "?") + key + "=" + value;
    }

    /** Translates JSON to the list of DTO objects. */
    public static <DTO> List<DTO> createListDtoFromJson(String json, Class<DTO> dtoInterface) throws IOException {
        return JsonUtils.createListDtoFromJson(json, dtoInterface);
    }

    /** Translates JSON to the list of DTO objects. */
    public static <DTO> DTO createDtoFromJson(String json, Class<DTO> dtoInterface) throws IOException {
        return JsonUtils.createDtoFromJson(json, dtoInterface);
    }

    /** Translates JSON to object. */
    public static <T> T fromJson(String json, Class<T> clazz) throws JsonParseException {
        return JsonUtils.fromJson(json, clazz, null);
    }

    /** Translates JSON to object. */
    public static Map asMap(String json) throws JsonParseException {
        return JsonUtils.asMap(json);
    }

    /** Translates JSON to object. */
    public static <T> T fromJson(String json, Class<T> clazz, Type type) throws JsonParseException {
        return JsonUtils.fromJson(json, clazz, type);
    }

    /** Translates object to JSON without null fields and with order defined by @JsonPropertyOrder annotation above the class. */
    public static String toJson(Object obj) throws JsonProcessingException {
        return JsonUtils.jsonWriter.writeValueAsString(obj);
    }

    /** Translates object to JSON with properties sorted alphabetically and aligned by colons. */
    public static String toJsonWithSortedAndAlignedProperties(Map<String, String> map) throws JsonProcessingException {
        return JsonUtils.toJsonWithSortedAndAlignedProperties(map);
    }

    /** @return the version of the artifact out of path */
    public static String extractVersion(Path pathToBinaries) {
        return pathToBinaries.getParent().getFileName().toString();
    }

    /** @return the artifact name out of path */
    public static String extractArtifactName(Path pathToBinaries) {
        return pathToBinaries.getParent().getParent().getFileName().toString();
    }

    /**
     * @return the last version of the artifact in the directory
     * @throws com.codenvy.im.exceptions.ArtifactNotFoundException
     *         if artifact is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    public static String getLatestVersion(String artifact, Path dir) throws IOException {
        Version latestVersion = null;

        if (!exists(dir)) {
            throw ArtifactNotFoundException.from(artifact);
        }

        Iterator<Path> pathIterator = Files.newDirectoryStream(dir).iterator();
        while (pathIterator.hasNext()) {
            try {
                Path next = pathIterator.next();
                if (Files.isDirectory(next)) {
                    Version version = valueOf(next.getFileName().toString());
                    if (latestVersion == null || compare(version, latestVersion) > 0) {
                        latestVersion = version;
                    }
                }
            } catch (IllegalArgumentException e) {
                // maybe it isn't a version directory
            }
        }

        if (latestVersion == null) {
            throw ArtifactNotFoundException.from(artifact);
        }

        return latestVersion.toString();
    }

    /** Extract server url from url with path */
    public static String extractServerUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getProtocol() + "://" + url.getHost();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Returns correct exception depending on initial type of exception. */
    public static IOException getProperException(IOException e, Artifact artifact) {
        if (e instanceof HttpException) {
            switch (((HttpException)e).getStatus()) {
                case 404:
                    return new ArtifactNotFoundException(artifact);

                case 302:
                    return new AuthenticationException();
            }
        }

        return e;
    }

    public static IOException getProperException(IOException e) {
        if (e instanceof HttpException) {
            switch (((HttpException)e).getStatus()) {
                case 403:
                    return new AuthenticationException();
            }
        }

        return e;
    }

    /** Calculates md5 sum */
    public static String calculateMD5Sum(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            try (InputStream in = newInputStream(file)) {
                int read;
                byte[] buffer = new byte[8192];
                while ((read = in.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }

            byte[] digest = md.digest();

            // convert the bytes to hex format
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }


    /** Set of artifacts to keep them in the specific order. */
    public static class ArtifactsSet extends TreeSet<Artifact> {
        public ArtifactsSet(Collection<Artifact> s) {
            super(new Comparator<Artifact>() {
                @Override
                public int compare(Artifact o1, Artifact o2) {
                    return o2.getPriority() - o1.getPriority();
                }
            });

            addAll(s);
        }
    }

    /**
     * @return "[artifact]-[delimeter]-[version]-binary.tar.gz"
     */
    public static String getBinaryFileName(Artifact artifact, Version version, @Nullable String artifactAndVersionDelimeter) {
        if (artifactAndVersionDelimeter == null) {
            return format("%s-%s-binary.tar.gz", artifact.getName(), version.toString());
        } else {
            return format("%s-%s-%s-binary.tar.gz", artifact.getName(), artifactAndVersionDelimeter, version.toString());
        }

    }

    private static class JsonUtils {
        private JsonUtils() {
        }

        /** include only non null fields into json; write pretty printed json. */
        private static final ObjectWriter jsonWriter =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).writerWithDefaultPrettyPrinter();

        /** Translates JSON to the list of DTO objects. */
        public static <DTO> List<DTO> createListDtoFromJson(String json, Class<DTO> dtoInterface) throws IOException {
            return DtoFactory.getInstance().createListDtoFromJson(json, dtoInterface);
        }

        /** Translates JSON to the list of DTO objects. */
        public static <DTO> DTO createDtoFromJson(String json, Class<DTO> dtoInterface) throws IOException {
            return DtoFactory.getInstance().createDtoFromJson(json, dtoInterface);
        }

        /** Translates JSON to object. */
        public static Map asMap(String json) throws JsonParseException {
            return JsonHelper.fromJson(json, Map.class, new TypeLiteral<Map<String, String>>() {
            }.getType());
        }

        /** Translates JSON to object. */
        private static <T> T fromJson(String json, Class<T> clazz, Type type) throws JsonParseException {
            return JsonHelper.fromJson(json, clazz, type);
        }

        /** Translates map to JSON with entries sorted alphabetically and aligned by colons. */
        private static String toJsonWithSortedAndAlignedProperties(Map<String, String> originMap) throws JsonProcessingException {
            Map<String, String> map = new TreeMap<>(originMap);  // sort map

            removeNullValues(map);

            if (map.size() < 2) {
                return toJson(map);
            }

            final List<Integer> indentations = getIndentationsForEntryAlignment(map);

            PrettyPrinter prettyPrinter = new DefaultPrettyPrinter() {
                private int index;

                @Override
                public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
                    jg.writeRaw(getIndentationOfLength(indentations.get(index++)) + " : ");
                }

                @Override public DefaultPrettyPrinter createInstance() {
                    return this;
                }

                private String getIndentationOfLength(Integer length) {
                    return new String(new char[length]).replace("\0", " ");
                }
            };

            ObjectWriter writer = new ObjectMapper().writer().with(prettyPrinter);
            return writer.writeValueAsString(map);
        }

        private static List<Integer> getIndentationsForEntryAlignment(Map<String, String> map) {
            int longestKeyName = getLongestKeyName(map);
            List<Integer> indentations = new ArrayList<>(map.size());
            for (String key: map.keySet()) {
                int indentation = longestKeyName - key.length();
                indentations.add(indentation);
            }

            return indentations;
        }

        private static int getLongestKeyName(Map<String, String> map) {
            List<String> keys = new ArrayList(map.keySet());
            Collections.sort(keys, new Comparator<String>() {
                @Override public int compare(String s1, String s2) {
                    return s2.length() - s1.length();  // order from the longest key to the shortest
                }
            });

            return keys.get(0).length();
        }

        private static void removeNullValues(Map<String, String> map) {
            Map<String, String> temp = new HashMap<>(map);
            for (Map.Entry<String, String> entry: temp.entrySet()) {
                if (entry.getValue() == null) {
                    map.remove(entry.getKey());
                }
            }
        }
    }
}
