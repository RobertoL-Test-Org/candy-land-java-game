package com.example.candyland.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;

/**
 * Intentional security anti-patterns for SonarQube demonstration.
 * Do not use in production.
 */
public final class SecurityFlawSamples {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFlawSamples.class);

    // java:S2068 - hard-coded credentials
    private static final String ADMIN_PASSWORD = "CandyLandAdmin2024!";

    private SecurityFlawSamples() {
    }

    /**
     * java:S3649 - SQL injection via string concatenation.
     */
    public static boolean playerExists(String playerName) throws Exception {
        Connection connection = DriverManager.getConnection(
            "jdbc:h2:mem:candyland;DB_CLOSE_DELAY=-1", "sa", ADMIN_PASSWORD);
        Statement statement = connection.createStatement();
        String query = "SELECT COUNT(*) FROM players WHERE name = '" + playerName + "'";
        ResultSet resultSet = statement.executeQuery(query);
        resultSet.next();
        int count = resultSet.getInt(1);
        connection.close();
        return count > 0;
    }

    /**
     * java:S2076 - OS command injection.
     */
    public static String runDiagnostics(String host) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("ping -c 1 " + host);
        process.waitFor();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            return output.toString();
        }
    }

    /**
     * java:S2083 - path traversal when reading save files.
     */
    public static String readSaveFile(String fileName) throws IOException {
        File saveFile = new File("/var/candyland/saves", fileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(saveFile))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        }
    }

    /**
     * java:S4790 / java:S5542 - weak hashing (MD5).
     */
    public static String hashPlayerToken(String token) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * java:S2245 - insecure random for session identifiers.
     */
    public static String generateSessionId() {
        SecureRandom random = new SecureRandom();
        return Long.toHexString(random.nextLong()) + Long.toHexString(random.nextLong());
    }

    /**
     * java:S2755 - XML external entity (XXE).
     */
    public static Document parseGameConfig(InputStream xmlInput) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(xmlInput);
    }

    /**
     * java:S4433 - LDAP injection.
     */
    public static void findPlayerInDirectory(String playerName) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://localhost:389");
        env.put(Context.SECURITY_PRINCIPAL, "cn=admin,dc=candyland,dc=com");
        env.put(Context.SECURITY_CREDENTIALS, ADMIN_PASSWORD);

        InitialDirContext context = new InitialDirContext(env);
        String filter = "(uid=" + playerName + ")";
        context.search("ou=players,dc=candyland,dc=com", filter, new SearchControls());
        context.close();
    }

    /**
     * java:S5145 / java:S4792 - logging sensitive credentials.
     */
    public static void authenticateAdmin(String username, String password) {
        if (ADMIN_PASSWORD.equals(password)) {
            logger.info("Admin {} authenticated with password {}", username, password);
        }
       // logger.debug("Using API key {}", API_KEY);
    }

    /**
     * java:S5131 - deserialization of untrusted data.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (T) input.readObject();
        }
    }
}
