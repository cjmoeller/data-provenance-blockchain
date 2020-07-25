package de.uol.provenancechain.workflow;

import net.corda.core.serialization.CordaSerializable;

/**
 * Implements the Data Quality Metric according to the Workflow model.
 */
@CordaSerializable
public class DataQualityMetric {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    private String name;
    private String[] parameters;
    private String unit;

    public DataQualityMetric(String name, String[] parameters, String unit) {
        this.name = name;
        this.parameters = parameters;
        this.unit = unit;
    }
}
