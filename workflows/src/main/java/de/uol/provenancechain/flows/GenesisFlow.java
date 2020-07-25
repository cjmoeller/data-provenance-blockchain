package de.uol.provenancechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import de.uol.provenancechain.contracts.WorkflowContract;
import de.uol.provenancechain.states.GenesisState;
import de.uol.provenancechain.workflow.WorkflowStep;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Initiator Flow for creating a new Workflow (creating a 'genesis block').
 * This will be executed by a client when a new Workflow-Chain is created in the blockchain data provenance system.
 */
@InitiatingFlow
@StartableByRPC
public class GenesisFlow extends FlowLogic<SignedTransaction> {

    private final List<WorkflowStep> initalSteps;
    private List<AbstractParty> validators;
    private List<AbstractParty> permissionedUsers;

    /**
     * Constructor
     * @param validators List of validators for the workflow
     * @param permissionedUsers List of nodes with the permission to participate in the workflow
     * @param dataspaceLocation  (URN)-Location of the initial data set in the dataspace
     * @param initialSteps List of workflow steps that were carried out by the data owner before publishing (e.g. anonymization).
     */
    public GenesisFlow(List<AbstractParty> validators, List<AbstractParty> permissionedUsers, String dataspaceLocation, List<WorkflowStep> initialSteps) {
        this.validators = validators;
        this.dataspaceLocation = dataspaceLocation;
        this.initalSteps = initialSteps;
        this.permissionedUsers = permissionedUsers;
    }

    private String dataspaceLocation;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();


    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        //We are the data owner if we create the Genesis Block.
        GenesisState outputState = new GenesisState(getOurIdentity(), validators, permissionedUsers, UUID.randomUUID(), dataspaceLocation, initalSteps);

        // Only the data owner needs to sign this transaction:
        List<AbstractParty> involvedParties = new ArrayList<>();
        involvedParties.add(getOurIdentity());
        List<PublicKey> involvedKeys = new ArrayList<>();
        involvedParties.forEach(p -> involvedKeys.add(p.getOwningKey()));

        Command command = new Command<>(new WorkflowContract.Commands.SelectValidator(), involvedKeys);

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, WorkflowContract.ID)
                .addCommand(command);

        // Only Signing the transaction by the dataOwner.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        List<FlowSession> sessions = new ArrayList<>();
        for (AbstractParty validator : validators) {
            if (!getOurIdentity().getOwningKey().equals(validator.getOwningKey()))
                sessions.add(initiateFlow(validator));
        }

        // We finalise the transaction and then send it to the counterparties.
        SignedTransaction finalisedTx = subFlow(new FinalityFlow(signedTx, sessions));


        //Notifying every validator and permissioned User:
        for (AbstractParty user : permissionedUsers)
            subFlow(new GenesisNotificationFlow(finalisedTx, user));


        return finalisedTx;

    }

}