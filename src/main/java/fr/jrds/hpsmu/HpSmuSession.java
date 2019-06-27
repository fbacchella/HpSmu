package fr.jrds.hpsmu;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.slf4j.event.Level;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import jrds.Util;
import jrds.probe.HttpClientStarter;
import jrds.probe.HttpSession;
import jrds.starter.XmlProvider;

public class HpSmuSession extends HttpSession {

    private String login;
    private String password;
    private String token;

    @Override
    public boolean startConnection() {
        token = null;
        return true;
    }

    @Override
    public void stopConnection() {
        token = null;
    }

    @Override
    public long setUptime() {
        return 0;
    }

    @Override
    public boolean makeSession(HttpRequestBase request) {
        URI uri;
        URI newUri;
        uri = request.getURI();
        try {
            newUri = new URI("https", uri.getUserInfo(), uri.getHost(), 443, uri.getPath(), uri.getQuery(), uri.getFragment());
            request.setURI(newUri);
        } catch (URISyntaxException e) {
            log(Level.ERROR, e, "can't rewrite URL %s: %s", uri, e.getMessage());
            return false;
        }
        if (token == null) {
            try {
                log(Level.DEBUG, "Authentication needed for %s", newUri);
                MessageDigest md5 = Util.getmd5();
                md5.reset();
                byte[] digest = md5.digest(String.format("%s_%s", login, password).getBytes());
                md5.reset();
                URL url = new URL(newUri.getScheme(), newUri.getHost(), newUri.getPort(), "/api/login/" + DatatypeConverter.printHexBinary(digest).toLowerCase());
                HttpClientStarter httpstarter = getLevel().find(HttpClientStarter.class);
                HttpClient cnx = httpstarter.getHttpClient();
                HttpGet hg = new HttpGet(url.toURI());
                HttpResponse response = cnx.execute(hg);

                if(response.getStatusLine().getStatusCode() != 200) {
                    log(Level.ERROR, "Connection to %s fail with %s", url, response.getStatusLine().getReasonPhrase());
                    EntityUtils.consumeQuietly(response.getEntity());
                    return false;
                }
                HttpEntity entity = response.getEntity();
                if(entity == null) {
                    log(Level.ERROR, "Not response body to %s", url);
                    return false;
                }
                XmlProvider xmlstarter = getLevel().find(XmlProvider.class);
                if(xmlstarter == null) {
                    log(Level.ERROR, "XML Provider not found");
                    return false;
                }

                Document d = xmlstarter.getDocument(entity.getContent());
                try {
                    String responseTtype = xmlstarter.getNode(d, "/RESPONSE/OBJECT/PROPERTY[@name='response-type']").getTextContent();
                    if ( ! "success".equals(responseTtype)) {
                        log(Level.ERROR, "authentication failed");
                        return false;
                    }
                    token = xmlstarter.getNode(d, "/RESPONSE/OBJECT/PROPERTY[@name='response']").getTextContent();
                } catch (DOMException | XPathExpressionException e) {
                    log(Level.ERROR, e, "invalid XML operation: %s", e.getMessage());
                }

            } catch (MalformedURLException|URISyntaxException e) {
                log(Level.ERROR, e, "can't get login URL: %s", e.getMessage());
                return false;
            } catch (ClientProtocolException e) {
                log(Level.ERROR, e, "HTTP error during authentication: %s", e.getMessage());
                return false;
            } catch (IOException e) {
                log(Level.ERROR, e, "IO error during authentication: %s", e.getMessage());
                return false;
            }
        }

        request.addHeader("sessionKey", token);
        request.addHeader("dataType", "ipa");
        return true;

    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the login
     */
    public String getLogin() {
        return login;
    }

    /**
     * @param login the login to set
     */
    public void setLogin(String login) {
        this.login = login;
    }

}
