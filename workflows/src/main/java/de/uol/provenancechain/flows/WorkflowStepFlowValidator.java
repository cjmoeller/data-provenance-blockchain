package de.uol.provenancechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import de.uol.provenancechain.workflow.WorkflowStep;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;

import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.UntrustworthyData;
import org.jetbrains.annotations.NotNull;

/**
 * This flow is representing the validator logic.
 */
@InitiatedBy(WorkflowStepFlow.class)
public class WorkflowStepFlowValidator extends FlowLogic<SignedTransaction> {
    private final FlowSession otherPartySession;

    /**
     * Constructor.
     *
     * @param otherPartySession session with the issuer.
     */
    public WorkflowStepFlowValidator(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        UntrustworthyData<WorkflowStep> proposedStep = otherPartySession.receive(WorkflowStep.class); //receive the proposal
        otherPartySession.send(this.checkTranscation(proposedStep.unwrap(data -> data)));

        SignTransactionFlow signTransactionFlow = new SignTransactionFlow(otherPartySession) { //Check the proposed transaction
            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

                if (false) {//Validation Logic goes here
                    throw new FlowException("Illegal Transaction encountered!");
                }
            }

        };
        SecureHash txId = subFlow(signTransactionFlow).getId();
        SignedTransaction finalisedTx = subFlow(new ReceiveFinalityFlow(otherPartySession, txId)); //Finalize the signed Transaction
        return finalisedTx;
    }

    /**
     * Pre-check of the proposed transaction.
     * @param step the proposed workflow step.
     * @return true, if the transaction is fine, false otherwise.
     */
    private Boolean checkTranscation(WorkflowStep step) {
        //dummy logic:
        double decision = Math.random();
        if (decision > 0f)
            return Boolean.TRUE;
        return Boolean.FALSE;
    }
}

