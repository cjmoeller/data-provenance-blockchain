package de.uol.provenancechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.node.StatesToRecord;

/**
 * Flow for receiving WorkflowStep Notifications.
 */
@InitiatedBy(WorkflowStepNotificationFlow.class)
public class WorkflowStepNotificationObserver extends FlowLogic<Void> {

    private final FlowSession otherSession;

    public WorkflowStepNotificationObserver(FlowSession otherSession) {
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