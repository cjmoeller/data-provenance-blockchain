package de.uol.provenancechain.processing;

import de.uol.provenancechain.workflow.WorkflowStep;

import java.nio.file.Path;
import java.util.List;

/**
 * This interface represents a data processing flow.
 */
public interface DataProcessingFlow {
    /**
     * @return Returns a list of WorkflowSteps that were executed in the workflow during doProcessing.
     */
    List<WorkflowStep> getWorkflowSteps();

    /**
     * The actual processing of the workflow.
     */
    void doProcessing();

    /**
     * @return Returns a Path to the finally processed data set.
     */
    Path getResultLocation();
}
