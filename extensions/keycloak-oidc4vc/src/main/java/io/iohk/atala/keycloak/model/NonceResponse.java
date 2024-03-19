package io.iohk.atala.keycloak.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class NonceResponse {
    private String nonce;
    private int nonceExpiresIn;

    public NonceResponse() {
        // for reflection
    }

    public static NonceResponse fromResponse(CloseableHttpResponse response) throws RuntimeException {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            try (response) {
                HttpEntity entity = response.getEntity();
                String jsonString = EntityUtils.toString(entity);
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(jsonString, NonceResponse.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("The response status from the agent was unsuccessful: " + statusCode);
        }
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public int getNonceExpiresIn() {
        return nonceExpiresIn;
    }

    public void setNonceExpiresIn(int nonceExpiresIn) {
        this.nonceExpiresIn = nonceExpiresIn;
    }
}