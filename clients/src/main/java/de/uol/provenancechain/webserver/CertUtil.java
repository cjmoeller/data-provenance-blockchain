package de.uol.provenancechain.webserver;

import org.bouncycastle.x509.X509V3CertificateGenerator;

import javax.security.auth.x500.X500Principal;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Random;

/**
 * Utility class for creating certificates.
 */
public class CertUtil {

    /**
     * Helper method for reading a PrivateKey from a KeyStore object.
     *
     * @param keystore
     * @param alias
     * @param password
     * @return
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     */
    private static PrivateKey getKey(final KeyStore keystore,
                                     final String alias, final String password) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
        final PrivateKey key = (PrivateKey) keystore.getKey(alias, password.toCharArray());
        return key;
    }

    /**
     * Creates a certificate.
     *
     * @param publicKey  PublicKey of the Certificate
     * @param privateKey Private Key for signing
     * @param name       Name of the Node
     * @return
     * @throws Exception
     */
    private static X509Certificate createCertificate(PublicKey publicKey, PrivateKey privateKey, String name) throws Exception {
        X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
        certGenerator.setSerialNumber(BigInteger.valueOf(Math.abs(new Random()
                .nextLong())));
        //Insert dummy data:
        certGenerator.setIssuerDN(new X500Principal("CN=Test CA"));
        certGenerator.setSubjectDN(new X500Principal("DC= Test Node" + name));
        certGenerator.setIssuerDN(new X500Principal("C=DE, ST=Lower Saxony, L=Oldenburg, O=DSSP, OU=Cross Signature, CN=CA/Email=ca@trustme.dom")); // Set issuer!
        certGenerator.setNotBefore(Calendar.getInstance().getTime());
        certGenerator.setNotAfter(Calendar.getInstance().getTime());

        certGenerator.setPublicKey(publicKey);

        certGenerator.setSignatureAlgorithm("SHA256WITHECDSA");
        X509Certificate certificate = (X509Certificate) certGenerator.generate(
                privateKey, "BC");
        return certificate;
    }

    /**
     * Creates a certificate and signs it with an Maritime Connectivity Platform key.
     *
     * @param publicKey the public key.
     * @param name      the name of the node
     * @return A X509Certificate for the publicKey signed with an MCP key.
     */
    public static X509Certificate createCertificate(PublicKey publicKey, String name) {
        KeyStore ks = null;
        PrivateKey privKey = null;
        try {
            ks = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        try {
            char[] pwdArray = "password".toCharArray();
            ks.load(new FileInputStream("clients/src/main/resources/keys/Keystore_user.jks"), pwdArray);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        try {
            privKey = getKey(ks, "user", "password");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        X509Certificate cert = null;
        try {
            cert = createCertificate(publicKey, privKey, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cert;
    }
}
