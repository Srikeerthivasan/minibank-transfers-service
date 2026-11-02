package edu.bits.minibank.transfers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

abstract class MySqlTestBase {
    // Keep Windows selector socket paths short for Java's HTTP test client.
    static {
        if (System.getProperty("os.name").startsWith("Windows")
                && System.getProperty("jdk.net.unixdomain.tmpdir") == null)
            System.setProperty("jdk.net.unixdomain.tmpdir", ".");
    }
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        String url = System.getenv("TEST_DB_URL");
        // Tests DELETE data. Require an explicitly selected, dedicated test schema.
        if (url == null || !url.matches("jdbc:mysql://[^/]+/transfers_test(?:\\?.*)?"))
            throw new IllegalStateException("Set TEST_DB_URL to a dedicated transfers_test MySQL database; see the setup guide");
        r.add("spring.datasource.url", () -> url);
        r.add("spring.datasource.username", () -> System.getenv().getOrDefault("TEST_DB_USER", "transfers_test"));
        r.add("spring.datasource.password", () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", "local-test-password"));
        r.add("bank.internal-token", () -> "local-demo-token");
    }
}
