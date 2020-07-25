package de.uol.provenancechain.states;

import de.uol.provenancechain.contracts.WorkflowContract;
import de.uol.provenancechain.workflow.WorkflowStep;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the 'Genesis Block' of a Workflow. This includes information on the data owner, the validators and the
 * permissioned users. Anytime a new Workflow Step is added to an existing Workflow, the GenesisState is set as a
 * reference state for the transaction, to check if the user was permissioned and to send the transaction to the specified
 * validators. In our implementation we assumed that a single workflow only can have a single data owner and that
 * permissioned users are fixed for the workflow. This could of course be implemented differently in other scenarios.
 */
@BelongsToContract(WorkflowContract.class)
public class GenesisState implements ContractState {

    /**
     * @return A list of users with access to the workflow.
     */
    public List<AbstractParty> getPermissionedUsers() {
        return permissionedUsers;
    }

    /**
     * Returns a list of initial Workflow steps, that have been executed by the data owner before the data was published to the
     * dataspace (e.g. anonymization).
     *
     * @return the list of workflow steps.
     */
    public List<WorkflowStep> getInitialSteps() {
        return initialSteps;
    }

    /**
     * Sets a list of initial Workflow steps, that have been executed by the data owner before the data was published to the
     * dataspace (e.g. anonymization).
     *
     * @param initialSteps the initial workflow steps.
     */
    public void setInitialSteps(List<WorkflowStep> initialSteps) {
        this.initialSteps = initialSteps;
    }

    private final List<AbstractParty> permissionedUsers;
    private AbstractParty dataOwner;

    private List<AbstractParty> validators;

    /**
     * @return the location of the dataset in the dataspace
     */
    public String getDataspaceLocation() {
        return dataspaceLocation;
    }

    /**
     * @param dataspaceLocation the location of the dataset in the dataspace
     */
    public void setDataspaceLocation(String dataspaceLocation) {
        this.dataspaceLocation = dataspaceLocation;
    }

    private String dataspaceLocation;

    /**
     * @return the data owner of the workflow
     */
    public AbstractParty getDataOwner() {
        return dataOwner;
    }

    /**
     * @param dataOwner the data owner of the workflow
     */
    public void setDataOwner(AbstractParty dataOwner) {
        this.dataOwner = dataOwner;
    }

    /**
     * @return the list of validators
     */
    public List<AbstractParty> getValidators() {
        return validators;
    }

    /**
     * @param validators the list of validators
     */
    public void setValidators(List<AbstractParty> validators) {
        this.validators = validators;
    }

    /**
     * @return A unique ID of the workflow
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @param uuid The unique ID of the workflow
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    private UUID uuid;

    private List<WorkflowStep> initialSteps;

    /**
     * Creates a Genesis State for a new Workflow Process.
     *
     * @param dataOwner         the data owner
     * @param validators        the validators
     * @param uuid              the uuid for the workflow
     * @param dataspaceLocation location where to find the data in the dataspace
     * @param initialSteps      Initial steps that have been conducted before publishing the data set
     */
    public GenesisState(AbstractParty dataOwner, List<AbstractParty> validators, List<AbstractParty> permissionedUsers, UUID uuid, String dataspaceLocation, List<WorkflowStep> initialSteps) {
        this.dataOwner = dataOwner;
        this.validators = validators;
        this.uuid = uuid;
        this.dataspaceLocation = dataspaceLocation;
        this.initialSteps = initialSteps;
        this.permissionedUsers = permissionedUsers;
    }


    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> parties = new ArrayList<>();
        parties.addAll(validators);
        parties.add(dataOwner);
        //parties.addAll(this.permissionedUsers);
        return parties;
    }
}
