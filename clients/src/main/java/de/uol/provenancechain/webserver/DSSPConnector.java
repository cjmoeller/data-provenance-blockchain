package de.uol.provenancechain.webserver;

import de.uol.dummydssp.model.DataSetLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Connector for the Data Space Support Platform (DSSP)
 */
@Component
public class DSSPConnector {
    RestTemplate template;

    @Autowired
    public DSSPConnector(RestTemplateBuilder builder) {
        this.template = builder.build();
    }


    /**
     * Returns a DataSetLocation from an Universal Resource Name (URN)
     *
     * @param urn the urn
     * @return the DataSetLocation
     */
    public DataSetLocation getDataSetFromURN(String urn) {
        Map<String, String> vars = new HashMap<>();
        vars.put("urn", urn);
        DataSetLocation ds = template.getForObject(
                "http://localhost:8080/data?urn={urn}", DataSetLocation.class, vars);
        return ds;
    }

    /**
     * Returns the first result for a DataSetLocation with a matching name.
     *
     * @param keyword the search keyword
     * @return the DataSetLocation
     */
    public DataSetLocation searchDataSet(String keyword) {
        Map<String, String> vars = new HashMap<>();
        vars.put("s", keyword);
        DataSetLocation ds = template.getForObject(
                "http://localhost:8080/search?s={s}", DataSetLocation.class, vars);
        return ds;
    }

    /**
     * Adds a DataSetLocation to the DSSP.
     *
     * @param newDataset the new DataSetLocation.
     */
    public void addDataSet(DataSetLocation newDataset) {
        template.postForObject("http://localhost:8080/adddata", newDataset, Void.class);
    }

    /**
     * Registers a certificate at the DSSP (Cross-signed certificates for verifying the node identities with MCP keys).
     *
     * @param cert the certificate
     */
    public void addCert(X509Certificate cert) {
        byte[][] data = new byte[2][];
        data[0] = cert.getPublicKey().getEncoded();
        data[1] = cert.getSignature();
        template.postForObject("http://localhost:8080/addcert", data, Void.class);

    }


}
