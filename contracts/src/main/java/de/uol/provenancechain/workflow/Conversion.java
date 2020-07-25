package de.uol.provenancechain.workflow;

import net.corda.core.serialization.CordaSerializable;

/**
 * Implements the Conversion WorkflowStep according to the Workflow model.
 */
@CordaSerializable
public class Conversion extends WorkflowStep {
    public String getFromFormat() {
        return fromFormat;
    }

    public void setFromFormat(String fromFormat) {
        this.fromFormat = fromFormat;
    }

    public String getToFormat() {
        return toFormat;
    }

    public void setToFormat(String toFormat) {
        this.toFormat = toFormat;
    }

    private String fromFormat;
    private String toFormat;

    public Conversion(String name, String description, String hash, String fromFormat, String toFormat) {
        super(name, description, hash);
        this.fromFormat = fromFormat;
        this.toFormat = toFormat;
    }
}
