package de.uol.provenancechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import de.uol.provenancechain.contracts.WorkflowContract;
import de.uol.provenancechain.states.GenesisState;
import de.uol.provenancechain.states.WorkflowState;
import de.uol.provenancechain.workflow.WorkflowStep;
import net.corda.core.contracts.ReferencedStateAndRef;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.utilities.ProgressTracker;

import net.corda.core.contracts.Command;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This flow is used to add a new WorkflowStep to an existing Workflow.
 */
@InitiatingFlow
@StartableByRPC
public class WorkflowStepFlow extends FlowLogic<SignedTransaction> {
    private final WorkflowStep step;
    private final UUID uuid;


    public WorkflowStepFlow(WorkflowStep step, UUID uuid) {
        this.step = step;
        this.uuid = uuid;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        long time = System.currentTimeMillis();
        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        List<StateAndRef<GenesisState>> states = getServiceHub().getVaultService().queryBy(GenesisState.class).getStates();

        //Find the GenesisBlock for this workflow
        StateAndRef<GenesisState> stateRef = null;
        GenesisState genesisState = null;
        for (StateAndRef<GenesisState> s : states) {
            if (s.getState().getData().getUuid().compareTo(uuid) == 0) {
                stateRef = s;
                genesisState = s.getState().getData();
            }
        }

        if (genesisState == null)
            throw new FlowException("Invalid Workflow ID.");
        ReferencedStateAndRef<GenesisState> referenceState = new ReferencedStateAndRef<>(stateRef);
        //Get the validators we need to ask to issue this transaction:
        List<AbstractParty> validators = genesisState.getValidators();
        validators = validators.stream().filter(p -> !p.getOwningKey().equals(getOurIdentity().getOwningKey())).collect(Collectors.toList()); //remove this node, if it is validator

        // Creating a session with every validator:
        List<FlowSession> sessions = new ArrayList<>();
        for (AbstractParty validator : validators)
            sessions.add(initiateFlow(validator));
        if (!getOurIdentity().equals(genesisState.getDataOwner().getOwningKey()))
            sessions.add(initiateFlow(genesisState.getDataOwner()));

        //Check who wants to sign the transaction:
        List<FlowSession> acceptingNodeSessions = new ArrayList<>();
        for (FlowSession session : sessions) {
            UntrustworthyData<Boolean> packet2 = session.sendAndReceive(Boolean.class, step);
            Boolean isAccepting = packet2.unwrap(data -> data);
            if (isAccepting)
                acceptingNodeSessions.add(session);
        }

        //Configuring the keys
        List<AbstractParty> involvedParties = new ArrayList<>();

        involvedParties.addAll(acceptingNodeSessions.stream().map(FlowSession::getCounterparty).collect(Collectors.toList()));

        if (!involvedParties.contains(getOurIdentity()))
            involvedParties.add(getOurIdentity());

        List<PublicKey> involvedKeys = new ArrayList<>();
        involvedParties.forEach(p -> involvedKeys.add(p.getOwningKey()));

        // Create the transaction components.
        WorkflowState outputState = new WorkflowState(involvedParties, uuid, step, getOurIdentity());
        Command command = new Command<>(new WorkflowContract.Commands.AddWorkflowStep(), involvedKeys);

        //Create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addReferenceState(referenceState) //this is our GenesisState
                .addOutputState(outputState, WorkflowContract.ID)
                .addCommand(command);

        // Signing the transaction (only by validators and this node).
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
        FlowSession selfSession = null;
        for (FlowSession session : sessions) {
            if (session.getCounterparty().getOwningKey().equals(getOurIdentity().getOwningKey()))
                selfSession = session;
        }
        sessions.remove(selfSession);

        SignedTransaction fullyStx = subFlow(new CollectSignaturesFlow(signedTx, sessions));

        //now adding all the sessions for the permissioned nodes
        // We finalise the transaction and then send it to the counterparties.
        SignedTransaction finalisedTx = subFlow(new FinalityFlow(fullyStx, sessions));

        //Notifying the rest of the participants about the transaction
        List<AbstractParty> allRelevantParties = new ArrayList<>();
        allRelevantParties.addAll(genesisState.getPermissionedUsers());
        allRelevantParties.add(genesisState.getDataOwner());
        validators.removeAll(involvedParties);
        allRelevantParties.addAll(validators);
        for (AbstractParty user : allRelevantParties)
            subFlow(new GenesisNotificationFlow(finalisedTx, user));
        return finalisedTx;
    }
}