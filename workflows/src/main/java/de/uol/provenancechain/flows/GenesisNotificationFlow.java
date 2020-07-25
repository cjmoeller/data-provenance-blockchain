package de.uol.provenancechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.SignedTransaction;

/**
 * Notifies all permissioned parties about the new genesis block.
 */
@InitiatingFlow
public class GenesisNotificationFlow extends FlowLogic<Void> {

    private final AbstractParty counterparty;
    private final SignedTransaction stx;

    /**
     * Constructor.
     * @param stx The signed 'genesis' transaction.
     * @param counterparty The party who sent this notification.
     */
    public GenesisNotificationFlow(SignedTransaction stx, AbstractParty counterparty) {
        this.counterparty = counterparty;
        this.stx = stx;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        FlowSession session = initiateFlow(counterparty);
        subFlow(new SendTransactionFlow(session, stx));
        return null;
    }


}
