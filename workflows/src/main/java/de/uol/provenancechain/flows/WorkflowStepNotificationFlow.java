package de.uol.provenancechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.SignedTransaction;

/**
 * Flow for notifying the non-validator parties of a new WorkflowStep
 */
@InitiatingFlow
public class WorkflowStepNotificationFlow extends FlowLogic<Void> {

    private final AbstractParty counterparty;
    private final SignedTransaction stx;

    public WorkflowStepNotificationFlow(SignedTransaction stx, AbstractParty counterparty) {
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
