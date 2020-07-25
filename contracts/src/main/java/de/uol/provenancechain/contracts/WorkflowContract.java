package de.uol.provenancechain.contracts;

import de.uol.provenancechain.states.GenesisState;
import de.uol.provenancechain.states.WorkflowState;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * Smart contract for the Workflow Steps.
 */
public class WorkflowContract implements Contract {
    // This is used to identify the contract when building a transaction.
    public static final String ID = "de.uol.provenancechain.contracts.WorkflowContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        Command<CommandData> transactionCommand = tx.getCommand(0);

        //Checks for adding a Workflow Step to an existing Workflow
        if (transactionCommand.getValue().getClass() == Commands.AddWorkflowStep.class) {
            ContractState genesisRef = tx.getReferenceStates().get(0);
            WorkflowState state = (WorkflowState) tx.getOutput(0);
            GenesisState gState = (GenesisState) genesisRef;
            List<PublicKey> signers = new ArrayList<>(transactionCommand.getSigners());
            List<PublicKey> validatorKeys = gState.getValidators().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
            //Remove the data owner signature, we only check the validators (unless data owner is validator, then he of course validates his own transcation).
            if (!validatorKeys.contains(gState.getDataOwner().getOwningKey()))
                signers.remove(gState.getDataOwner().getOwningKey());
            //Remove the issuer signature, we only check the validators (unless signer is validator, then he of course validates his own transcation).
            if (!validatorKeys.contains(state.getIssuer().getOwningKey()))
                signers.remove(state.getIssuer().getOwningKey());

            //Contract Requirements:
            requireThat(require -> {
                require.using("All signers must be validators", validatorKeys.containsAll(signers));
                require.using("More than 50% of the validators must accept the transaction. Signers:" + signers.toString() + "; Validators:" + validatorKeys.toString(), (Float.valueOf(signers.size()) / Float.valueOf(validatorKeys.size())) > 0.5f);


                return null;
            });

        //Checks for creating a new Workflow.
        } else {
            GenesisState gState = (GenesisState) tx.getOutput(0);
            List<PublicKey> validatorKeys = gState.getValidators().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
            boolean validatorsVerified = checkKeyValidity(validatorKeys);
            List<PublicKey> userKeys = gState.getPermissionedUsers().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
            boolean usersVerified = checkKeyValidity(userKeys);

            //Contract Requirements
            requireThat(require -> {
                require.using("Validator identities must be verified identities.", validatorsVerified);
                require.using("User identities must be verified identities.", usersVerified);
                return null;
            });
        }
    }

    /**
     * Checks a list of keys for their validity, by asking the DSSP if there is a valid certificate.
     * @param keys the keys to check
     * @return true if all keys are valid, false otherwise.
     */
    private boolean checkKeyValidity(List<PublicKey> keys) {
        List<byte[]> encoded = new ArrayList<>();
        for(PublicKey key : keys){
            encoded.add(key.getEncoded());
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ContractKeyCheckerService service = retrofit.create(ContractKeyCheckerService.class);
        for (PublicKey key : keys) {
            try {
                if (service.checkKey( Base64.getEncoder().encodeToString(key.getEncoded())).execute().body() == Boolean.FALSE)
                    return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        /**
         * Command for adding a new Workflow step to an existing workflow.
         */
        class AddWorkflowStep implements Commands {
        }

        /**
         * Command for Issuing a new Workflow (and selecting the validators)
         */
        class SelectValidator implements Commands {
        }
    }
}