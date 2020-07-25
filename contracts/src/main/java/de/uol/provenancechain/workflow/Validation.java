package de.uol.provenancechain.workflow;

import net.corda.core.serialization.CordaSerializable;

/**
 * Implements the Validation WorkflowStep according to the Workflow model.
 */
@CordaSerializable
public class Validation extends WorkflowStep {
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    private String type;
    private String result;

    public Validation(String name, String description, String hash, String type, String result) {
        super(name, description, hash);
        this.type = type;
        this.result = result;
    }
}
