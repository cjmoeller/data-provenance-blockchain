package de.uol.provenancechain.contracts;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Interface for the DSSP-API to check a key, if it is signed by an MCP key. This would typically done by querying for
 * the certificate. For the sake of simplicity, this is already done at the DSSP-side and the result is returned as a
 * boolean.
 */
public interface ContractKeyCheckerService {

    /**
     * Checks a key for validity.
     *
     * @param key the key to check
     * @return true if the key is valid.
     */
    @POST("checkKey")
    public Call<Boolean> checkKey(@Body String key);
}
