package dev.hephaestus.proximity.plugins.util;

import com.github.yuchi.semver.Range;
import com.github.yuchi.semver.Version;
import dev.hephaestus.proximity.Proximity;
import dev.hephaestus.proximity.util.Result;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Artifact {
    private static final Map<String, Artifact> ARTIFACTS = new HashMap<>();

    private final String id, repository, group, artifact, metadataUrl;
    private final Range version;

    private Artifact(String id, String repository, String group, String artifact, Range version) {
        this.id = id;
        this.repository = repository;
        this.group = group;
        this.artifact = artifact;
        this.version = version;

        this.metadataUrl = String.format("%s/%s/%s/maven-metadata.xml",
                repository,
                group.replace('.', '/'),
                artifact
        );
    }

    public Range getVersion() {
        return version;
    }

    public static Artifact create(String repository, String group, String artifact, String version) {
        String id = group + ":" + artifact + ":" + version;

        if (repository.endsWith("/")) {
            repository = repository.substring(0, repository.length() - 1);
        }

        if (!ARTIFACTS.containsKey(id)) {
            ARTIFACTS.put(id, new Artifact(id, repository, group, artifact, new Range(version)));
        }

        return ARTIFACTS.get(id);
    }

    public String getMetadataUrl() {
        return this.metadataUrl;
    }

    private Result<Element> loadMetadata() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.getMetadataUrl()))
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                Proximity.LOG.error("Could not fetch plugin {}: [{}]", this, response.statusCode());
            }

            Optional<String> contentType = response.headers().firstValue("Content-Type");

            if (contentType.isEmpty()) {
                Proximity.LOG.error("Plugin {} is missing Content-Type", this);
            }

            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(response.body());

            return Result.of(document.getDocumentElement());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private Result<String> getLatestMatchingVersion(Element root) {
        Version version = null;
        NodeList versioningTags = root.getElementsByTagName("versions");

        for (int j = 0; j < versioningTags.getLength(); ++j) {
            Node n = versioningTags.item(j);

            if (n instanceof Element versioningTag) {
                NodeList versionTags = versioningTag.getElementsByTagName("version");

                for (int k = 0; k < versionTags.getLength(); ++k) {
                    n = versionTags.item(k);

                    if (n instanceof Element e) {
                        Version v = new Version(e.getTextContent());

                        if ((version == null || v.compareTo(version) > 0) && this.getVersion().test(v)) {
                            version = v;
                        }
                    }
                }
            }
        }

        return version == null
                ? Result.error("No matching versions found for plugin '%s'", this)
                : Result.of(version.toString());
    }

    private Result<URI> createUrl(String version) {
        return Result.of(URI.create(String.format("%s/%s/%s/%s/%s-%s.jar",
                this.repository,
                this.group.replace('.', '/'),
                this.artifact,
                version,
                this.artifact,
                version
        )));
    }

    public Result<URI> getLatestMatchingVersionLocation() {
        return this.loadMetadata().then(this::getLatestMatchingVersion).then(this::createUrl);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
