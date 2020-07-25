package de.uol.provenancechain.workflow;

import net.corda.core.serialization.CordaSerializable;

import java.util.List;

/**
 * Implements the Data Transformation WorkflowStep according to the Workflow model.
 */
@CordaSerializable
public class DataTransformation extends WorkflowStep {
    private List<DataTransformation> fusionWith;

    public List<DataTransformation> getFusionWith() {
        return fusionWith;
    }

    public void setFusionWith(List<DataTransformation> fusionWith) {
        this.fusionWith = fusionWith;
    }

    public String getProcedure() {
        return procedure;
    }

    public void setProcedure(String procedure) {
        this.procedure = procedure;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    private String procedure;
    private String arguments;

    public DataTransformation(String name, String description, String procedure, String arguments, String hash, List<DataTransformation> fusionWith) {
        super(name, description, hash);
        this.procedure = procedure;
        this.arguments = arguments;
        this.fusionWith = fusionWith;
    }
}
