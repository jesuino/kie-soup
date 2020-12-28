package org.dashbuilder.dataprovider.prometheus.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

public class HttpClient {

    private static HttpClient INSTANCE;

    private HttpClient() {
        // do nothing
    }

    public String doGet(String url) {
        return doGet(url, null, null);
    }

    public String doGet(String url, String username, String password) {
        try {
            URLConnection connection = new URL(url).openConnection();
            if (username != null) {
                addAuth(connection, username, password);
            }
            InputStreamReader in = new InputStreamReader(connection.getInputStream());
            try (BufferedReader br = new BufferedReader(in)) {
                return br.readLine();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error performing HTTP Request.", e);
        }
    }

    private void addAuth(URLConnection connection, String username, String password) {
        String userpass = username + ":" + password;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
        connection.setRequestProperty("Authorization", basicAuth);
    }

    public static HttpClient get() {
        if (INSTANCE == null) {
            INSTANCE = new HttpClient();
        }
        return INSTANCE;
    }

}
