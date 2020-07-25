package de.uol.provenancechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import de.uol.provenancechain.states.GenesisState;
import de.uol.provenancechain.states.WorkflowState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.VaultService;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.SignatureException;
import java.util.List;
import java.util.UUID;

/**
 * Responder Flow to the GenesisFlow. Only accepts the new Workflow.
 */
@InitiatedBy(GenesisFlow.class)
public class GenesisFlowResponder extends FlowLogic<Void> {
    private final FlowSession otherPartySession;

    /**
     * Constructor
     * @param otherPartySession originator of the genesis block
     */
    public GenesisFlowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        subFlow(new ReceiveFinalityFlow(otherPartySession, null, StatesToRecord.ALL_VISIBLE));
        return null;


    }
}

