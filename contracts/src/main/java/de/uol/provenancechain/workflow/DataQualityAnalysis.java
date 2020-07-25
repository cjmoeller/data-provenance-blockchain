package de.uol.provenancechain.workflow;

import net.corda.core.serialization.CordaSerializable;

/**
 * Implements the Dat Quality Analysis WorkflowStep according to the Workflow model.
 */
@CordaSerializable
public class DataQualityAnalysis extends WorkflowStep {
    public String getAnalyzedAttributes() {
        return analyzedAttributes;
    }

    public void setAnalyzedAttributes(String analyzedAttributes) {
        this.analyzedAttributes = analyzedAttributes;
    }

    public Double getResult() {
        return result;
    }

    public void setResult(Double result) {
        this.result = result;
    }

    public DataQualityMetric getMetric() {
        return metric;
    }

    public void setMetric(DataQualityMetric metric) {
        this.metric = metric;
    }

    private String analyzedAttributes;
    private Double result;
    private DataQualityMetric metric;

    public DataQualityAnalysis(String name, String description, String hash, String analyzedAttributes, Double result, DataQualityMetric metric) {
        super(name, description, hash);
        this.analyzedAttributes = analyzedAttributes;
        this.result = result;
        this.metric = metric;
    }
}
