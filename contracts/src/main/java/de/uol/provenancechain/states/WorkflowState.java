package de.uol.provenancechain.states;

import de.uol.provenancechain.contracts.WorkflowContract;
import de.uol.provenancechain.workflow.WorkflowStep;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.ConstructorForDeserialization;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This represents a single workflow step in the processing chain of steps. A GenesisState must exist before a workflow
 * step can be added to the BC.
 */
@BelongsToContract(WorkflowContract.class)
public class WorkflowState implements ContractState {
    public AbstractParty getIssuer() {
        return issuer;
    }

    private final AbstractParty issuer;
    private List<AbstractParty> participants;

    /**
     * @param participants the participants (permissioned users)
     */
    public void setParticipants(List<AbstractParty> participants) {
        this.participants = participants;
    }

    /**
     * @return The unique id of the workflow.
     */
    public UUID getWorkflowID() {
        return workflowID;
    }


    /**
     * @param workflowID The unique ID of the workflow.
     */
    public void setWorkflowID(UUID workflowID) {
        this.workflowID = workflowID;
    }

    /**
     * @return The workflow step.
     */
    public WorkflowStep getStep() {
        return step;
    }

    /**
     * @param step The workflow step.
     */
    public void setStep(WorkflowStep step) {
        this.step = step;
    }

    private UUID workflowID;
    private WorkflowStep step;

    /**
     * Appends a workflow step to an existing Workflow
     *
     * @param participants The participants of the Workflow
     * @param workflowID   The uuid of the existing Workflow
     * @param step         The workflow step.
     */
    @ConstructorForDeserialization
    public WorkflowState(List<AbstractParty> participants, UUID workflowID, WorkflowStep step, AbstractParty issuer) {
        this.participants = participants;
        this.workflowID = workflowID;
        this.step = step;
        this.issuer = issuer;
    }


    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> abstractParties = new ArrayList();
        this.participants.forEach(party -> abstractParties.add(party));
        return abstractParties;
    }
}