package dasniko.testcontainers.keycloak;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak";
    private static final String KEYCLOAK_VERSION = "8.0.1";

    private static final int KEYCLOAK_PORT_HTTP = 8080;
    private static final int KEYCLOAK_PORT_HTTPS = 8443;

    private static final String KEYCLOAK_ADMIN_USER = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "admin";
    private static final String KEYCLOAK_AUTH_PATH = "/auth";

    private String adminUsername = KEYCLOAK_ADMIN_USER;
    private String adminPassword = KEYCLOAK_ADMIN_PASSWORD;
    private boolean useHttps = false;

    private String importFile;

    public KeycloakContainer() {
        this(KEYCLOAK_IMAGE + ":" + KEYCLOAK_VERSION);
    }

    /**
     * Create a KeycloakContainer by passing the full docker image name
     *
     * @param dockerImageName Full docker image name, e.g. quay.io/keycloak/keycloak:8.0.1
     */
    public KeycloakContainer(String dockerImageName) {
        super(dockerImageName);
        addExposedPort(KEYCLOAK_PORT_HTTP);
        setWaitStrategy(Wait
            .forHttp(KEYCLOAK_AUTH_PATH)
            .withStartupTimeout(Duration.ofMinutes(2))
        );
        withLogConsumer(new Slf4jLogConsumer(logger()));
    }

    @Override
    protected void configure() {
        withCommand("-c standalone.xml"); // don't start infinispan cluster

        withEnv("KEYCLOAK_USER", adminUsername);
        withEnv("KEYCLOAK_PASSWORD", adminPassword);

        Stream.of("crt", "key").forEach(e -> {
            String sslFileInContainer = "/etc/x509/https/tls." + e;
            withCopyFileToContainer(MountableFile.forClasspathResource("tls." + e), sslFileInContainer);
        });

        if (importFile != null) {
            String importFileInContainer = "/tmp/" + importFile;
            withCopyFileToContainer(MountableFile.forClasspathResource(importFile), importFileInContainer);
            withEnv("KEYCLOAK_IMPORT", importFileInContainer);
        }
    }

    public KeycloakContainer withRealmImportFile(String importFile) {
        this.importFile = importFile;
        return self();
    }

    public KeycloakContainer withAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
        return self();
    }

    public KeycloakContainer withAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
        return self();
    }

    public KeycloakContainer withHttps(boolean useHttps) {
        this.useHttps = useHttps;
        return self();
    }

    public String getAuthServerUrl() {
        return String.format("http%s://%s:%s%s", useHttps ? "s" : "", getContainerIpAddress(),
            useHttps ? getMappedPort(KEYCLOAK_PORT_HTTPS) : getMappedPort(KEYCLOAK_PORT_HTTP), KEYCLOAK_AUTH_PATH);
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    protected String getKeycloakVersion() {
        return KEYCLOAK_VERSION;
    }

}
