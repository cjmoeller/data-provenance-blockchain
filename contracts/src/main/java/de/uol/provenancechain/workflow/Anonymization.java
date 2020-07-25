package de.uol.provenancechain.workflow;

import net.corda.core.serialization.CordaSerializable;

/**
 * Implements the Anonymization WorkflowStep according to the Workflow model.
 */
@CordaSerializable
public class Anonymization extends WorkflowStep {
    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    private String algorithm;
    private String parameters;

    public Anonymization(String name, String description, String hash, String algorithm, String parameters) {
        super(name, description, hash);
        this.algorithm = algorithm;
        this.parameters = parameters;
    }
}
