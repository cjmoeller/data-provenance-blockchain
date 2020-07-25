package de.uol.provenancechain.webserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.uol.dummydssp.model.DataSetLocation;
import de.uol.provenancechain.flows.GenesisFlow;
import de.uol.provenancechain.flows.WorkflowStepFlow;
import de.uol.provenancechain.processing.FilterAndSelectionFlow;
import de.uol.provenancechain.processing.HeatMapFlow;
import de.uol.provenancechain.processing.VesselTrafficAnonymizationFlow;
import de.uol.provenancechain.states.GenesisState;
import de.uol.provenancechain.states.WorkflowState;
import de.uol.provenancechain.workflow.WorkflowStep;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Defines a very basic API for calling the single processing steps of the case study. The single processing steps would typically be implemented
 * on different data processing entities. However, for simplification we have one client that connects to each BC-Node separately.
 * This class is the main class for connecting data processing with data provenance information storage.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    /**
     * Connector to the Data Space Support Platform (DSSP).
     */
    private final DSSPConnector dssp;
    /**
     * Logger.
     */
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    /**
     * Constructor.
     *
     * @param dssp The dssp connector.
     */
    public Controller(DSSPConnector dssp) {
        this.dssp = dssp;
    }

    /**
     * API Method for registering the Node Keys with a cross-signed certificate at the DSSP.
     *
     * @return
     */
    @GetMapping(value = "/regKeys", produces = MediaType.APPLICATION_JSON_VALUE)
    private String registerKeys() {
        CordaRPCOps proxy = this.getNodeConnection(0);

        //Retrieve the keys from the BC-Network
        CordaX500Name x500NameA = CordaX500Name.parse("O=PartyA,L=London,C=GB");
        Party a = proxy.wellKnownPartyFromX500Name(x500NameA);
        CordaX500Name x500NameB = CordaX500Name.parse("O=PartyB,L=New York,C=US");
        Party b = proxy.wellKnownPartyFromX500Name(x500NameB);
        CordaX500Name x500NameC = CordaX500Name.parse("O=PartyC,L=Oldenburg,C=DE");
        Party c = proxy.wellKnownPartyFromX500Name(x500NameC);

        //Sign the public keys with a MCP key. This would typically be done on each Data Processing Entity separately.
        List<X509Certificate> certs = Arrays.asList(CertUtil.createCertificate(a.getOwningKey(), "A"),
                CertUtil.createCertificate(b.getOwningKey(), "B"), CertUtil.createCertificate(c.getOwningKey(), "C"));

        //Push the certificates to the DSSP to make them available to the whole network for validation.
        for (X509Certificate cert : certs) {
            this.dssp.addCert(cert);
        }
        return "[]";
    }

    /**
     * API Method for the first Processing step  (Organization A).
     *
     * @return json-encoded Data provenance information stored in the blockchain.
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws JsonProcessingException
     */
    @GetMapping(value = "/step1", produces = MediaType.APPLICATION_JSON_VALUE)
    private String processingOrgA() throws ExecutionException, InterruptedException, JsonProcessingException {
        CordaRPCOps proxy = this.getNodeConnection(0);
        VesselTrafficAnonymizationFlow workflow = new VesselTrafficAnonymizationFlow(Paths.get("clients/src/main/resources/data-1574672992387.csv"));
        workflow.doProcessing();

        CordaX500Name x500NameB = CordaX500Name.parse("O=PartyB,L=New York,C=US");
        Party b = proxy.wellKnownPartyFromX500Name(x500NameB);


        //Select validator(s):
        CordaX500Name x500NameC = CordaX500Name.parse("O=PartyC,L=Oldenburg,C=DE");
        Party c = proxy.wellKnownPartyFromX500Name(x500NameC);
        List<AbstractParty> validators = Arrays.asList(c);

        //Create a Genesis Block, Org A acts as the data owner, Org C as a validator and Org B has permission to access the information.
        FlowHandle handle = proxy.startFlowDynamic(GenesisFlow.class, validators, Arrays.asList(b), "urn:mrn:mcp:dataspace:dummy-id", workflow.getWorkflowSteps());

        CordaFuture val = handle.getReturnValue();
        SignedTransaction returnVal = (SignedTransaction) val.get();
        UUID newSetID = returnVal.getCoreTransaction().outputsOfType(GenesisState.class).get(0).getUuid();

        //Making the dataset available to the dataspace.
        DataSetLocation newDataset = new DataSetLocation(workflow.getResultLocation(), "Raw RADAR + AIS test data", "urn:mrn:mcp:dataspace:dummy-id", newSetID);
        dssp.addDataSet(newDataset);

        //Retrieving the data provenance information stored in the BC.
        Vault.Page<GenesisState> vaultQuery = proxy.vaultQuery(GenesisState.class);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper.writeValueAsString(vaultQuery.getStates().get(0).component1().component1().getInitialSteps());
    }

    /**
     * API Method for the second processing step (Organization B)
     *
     * @return json-encoded Data provenance information stored in the blockchain.
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws JsonProcessingException
     */
    @GetMapping(value = "/step2", produces = MediaType.APPLICATION_JSON_VALUE)
    private String processingOrgB() throws ExecutionException, InterruptedException, JsonProcessingException {
        //Search for the data set in the data space.
        DataSetLocation data = this.dssp.searchDataSet("AIS");
        FilterAndSelectionFlow workflow = new FilterAndSelectionFlow(data.getLocation());
        workflow.doProcessing(); //do the processing

        CordaRPCOps proxy = this.getNodeConnection(1);

        //Adding the Provenance Information to the Blockchain
        for (WorkflowStep step : workflow.getWorkflowSteps()) {
            FlowHandle handleT = proxy.startFlowDynamic(WorkflowStepFlow.class, step, data.getWorkflowID());
            CordaFuture valT = handleT.getReturnValue();
            Object returnValT = valT.get();
            logger.info("{}", returnValT);
        }

        //Publishing a new Version of the Dataset to the dataspace
        data.setLocation(workflow.getResultLocation());
        data.setName("Filtered AIS DataSet, only Position Data");
        data.setUrn("urn:mrn:mcp:dataspace:dummy-id-2");
        dssp.addDataSet(data);

        //Retrieving the data provenance information stored in the BC.
        Vault.Page<GenesisState> vaultQueryGenesis = proxy.vaultQuery(GenesisState.class);
        Vault.Page<WorkflowState> vaultQuerySteps = proxy.vaultQuery(WorkflowState.class);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        List<WorkflowStep> states = new ArrayList<>();
        states.addAll(vaultQueryGenesis.getStates().get(0).component1().component1().getInitialSteps());
        for (StateAndRef<WorkflowState> s : vaultQuerySteps.getStates()) {
            if (s.component1().component1().getWorkflowID().equals(data.getWorkflowID()))
                states.add(s.component1().component1().getStep());
        }
        return mapper.writeValueAsString(states);

    }

    /**
     * API Method for the third processing step (Organization C)
     *
     * @return json-encoded Data provenance information stored in the blockchain.
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws JsonProcessingException
     */
    @GetMapping(value = "/step3", produces = MediaType.APPLICATION_JSON_VALUE)
    private String processingOrgC() throws ExecutionException, InterruptedException, JsonProcessingException {
        DataSetLocation data = this.dssp.getDataSetFromURN("urn:mrn:mcp:dataspace:dummy-id-2"); //Assume Organization already knows the data set
        HeatMapFlow workflow = new HeatMapFlow(data.getLocation());
        workflow.doProcessing(); //do the processing
        CordaRPCOps proxy = this.getNodeConnection(2);

        //Adding the Provenance Information to the Blockchain
        for (WorkflowStep step : workflow.getWorkflowSteps()) {
            FlowHandle handleT = proxy.startFlowDynamic(WorkflowStepFlow.class, step, data.getWorkflowID());
            CordaFuture valT = handleT.getReturnValue();
            Object returnValT = valT.get();
            logger.info("{}", returnValT);
        }

        //Publishing a new Version of the Dataset to the dataspace
        data.setLocation(workflow.getResultLocation());
        data.setName("HeatMap Data for German Bight");
        data.setUrn("urn:mrn:mcp:dataspace:dummy-id-3");
        dssp.addDataSet(data);

        //Retrieving the data provenance information stored in the BC.
        Vault.Page<GenesisState> vaultQueryGenesis = proxy.vaultQuery(GenesisState.class);
        Vault.Page<WorkflowState> vaultQuerySteps = proxy.vaultQuery(WorkflowState.class);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        List<WorkflowStep> states = new ArrayList<>();
        states.addAll(vaultQueryGenesis.getStates().get(0).component1().component1().getInitialSteps());
        for (StateAndRef<WorkflowState> s : vaultQuerySteps.getStates()) {
            if (s.component1().component1().getWorkflowID().equals(data.getWorkflowID()))
                states.add(s.component1().component1().getStep());
        }
        return mapper.writeValueAsString(states);

    }

    /**
     * Helper method for acquiring a connection to a specific BC-node.
     *
     * @param node the node.
     * @return Connection to the node.
     */
    private CordaRPCOps getNodeConnection(int node) {
        CordaRPCOps proxy = null;
        if (node == 0) {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10006);
            CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
            CordaRPCConnection rpcConnection = rpcClient.start("user1", "test");
            proxy = rpcConnection.getProxy();
        } else if (node == 1) {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10009);
            CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
            CordaRPCConnection rpcConnection = rpcClient.start("user1", "test");
            proxy = rpcConnection.getProxy();
        } else {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10012);
            CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
            CordaRPCConnection rpcConnection = rpcClient.start("user1", "test");
            proxy = rpcConnection.getProxy();
        }
        return proxy;
    }
}