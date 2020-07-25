package de.uol.provenancechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;

/**
 * Receiver Flow of the Genesis Notification
 */
@InitiatedBy(GenesisNotificationFlow.class)
public class GenesisNotificationObserver extends FlowLogic<Void> {

    private final FlowSession otherSession;

    public GenesisNotificationObserver(FlowSession otherSession) {
        this.otherSession = otherSession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        ReceiveTransactionFlow flow = new ReceiveTransactionFlow(otherSession, true, StatesToRecord.ALL_VISIBLE);
        subFlow(flow);
        return null;
    }
}