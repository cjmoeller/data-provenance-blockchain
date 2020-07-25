package de.uol.dummydssp;

import de.uol.dummydssp.model.DataSetLocation;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Implements the REST Controller for the DSSP.
 */
@RestController
public class DSSPController {
    private List<DataSetLocation> dataSets;
    private List<byte[][]> validCerts;

    /**
     * Constructor.
     */
    public DSSPController() {
        dataSets = new ArrayList<>();
        validCerts = new ArrayList<>();
    }

    /**
     * Returns the Location of a data set.
     * @param urn URN of the data set
     * @return Path to the dataset
     */
    @GetMapping("/data")
    public DataSetLocation getLocation(@RequestParam(value = "urn") String urn) {
        Optional<DataSetLocation> result = dataSets.stream().filter(x -> x.getUrn().equals(urn)).findFirst();
        if (!result.isPresent())
            throw new ResponseStatusException(NOT_FOUND, "Unable to find resource");
        else
            return result.get();
    }

    /**
     * Searches for a data set which name includes a specific keyword.
     * @param keyword the search keyword
     * @return Path to the dataset
     */
    @GetMapping("/search")
    public DataSetLocation searchLocation(@RequestParam(value = "s") String keyword) {
        Optional<DataSetLocation> result = dataSets.stream().filter(x -> x.getName().toLowerCase().contains(keyword.toLowerCase())).findFirst();
        if (!result.isPresent())
            throw new ResponseStatusException(NOT_FOUND, "Unable to find resource");
        else
            return result.get();
    }

    /**
     * Checks a key for validity. The key is encoded as bytes in a base64 String.
     * @param bytes the key
     * @return true if the key is a registered cross-signed key, which was registered with a certificate.
     */
    @PostMapping("/checkKey")
    public Boolean checkKey(@RequestBody String bytes) {

        bytes = bytes.replace("\"", "");
        byte[] arr = Base64.getDecoder().decode(bytes);
        for (byte[][] cert : validCerts) {
            if (Arrays.equals(cert[0], arr))
                return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Adds a workflow-related Dataset reference to the DSSP, which can then be queried by this API
     * @param location
     */
    @PostMapping("/adddata")
    public void addLocation(@RequestBody DataSetLocation location) {
        dataSets.add(location);
    }

    /**
     * Adds a certificate with a cross-signed key at the DSSP.
     * @param data data[0] must be the public key, data[1] must be the signature
     */
    @PostMapping("/addcert")
    public void addCertificate(@RequestBody byte[][] data) {

        byte[] signature = data[1]; //Do certificate validation here...
        validCerts.add(data);
    }

}