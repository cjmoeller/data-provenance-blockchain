package de.uol.provenancechain.util;

import de.uol.dummydssp.model.DataSetLocation;
import de.uol.provenancechain.flows.GenesisFlow;
import de.uol.provenancechain.flows.WorkflowStepFlow;
import de.uol.provenancechain.states.GenesisState;
import de.uol.provenancechain.webserver.CertUtil;
import de.uol.provenancechain.webserver.DSSPConnector;
import de.uol.provenancechain.workflow.Validation;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.lang.model.util.ElementScanner6;

public class PerformanceTest {
    private static DSSPConnector connector;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /*RestTemplateBuilder builder = new RestTemplateBuilder();
        //connector = new DSSPConnector(builder);
        List<CordaRPCOps> proxies = connectToNodes();
        for (CordaRPCOps p : proxies) {
            //connector.addCert(CertUtil.createCertificate(p.nodeInfo().getLegalIdentities().get(0).getOwningKey(), "TestNode"));
        }
        List<CordaRPCOps> toAssign = new ArrayList<>(proxies);
        while (toAssign.size() >= 3) {
            int numNewWorkflowNodes = ThreadLocalRandom.current().nextInt(3, 10);
            List<CordaRPCOps> newWorkflowNodes = new ArrayList<>();
            for (int i = 0; i < numNewWorkflowNodes; i++) {
                if (toAssign.size() > 0) {
                    CordaRPCOps p = toAssign.remove(ThreadLocalRandom.current().nextInt(toAssign.size()));
                    newWorkflowNodes.add(p);
                }
            }
            createGensisFlow(newWorkflowNodes);
        }
        int transactionSize = 50;
        long time = System.currentTimeMillis();
        for (int i = 0; i < transactionSize; i++) {
            UUID randomFlow = (UUID) workflows.keySet().toArray()[ThreadLocalRandom.current().nextInt(workflows.keySet().toArray().length)];
            createWorkflowStep(randomFlow, workflows.get(randomFlow));
        }
        System.out.println("Transactions per second (TPS):" + 1000d / ((float) (System.currentTimeMillis() - time) / (float) transactionSize)); */
        test5Nodes6Validator();
        System.out.println("1 WFs");
    }

    private static List<CordaRPCOps> connectToNodes() {
        List<CordaRPCOps> proxies = new ArrayList<>();
        proxies.addAll(Arrays.asList(getNodeConnection(0), getNodeConnection(2), getNodeConnection(3)));//, getNodeConnection(3), getNodeConnection(4), getNodeConnection(5), getNodeConnection(6)));
        return proxies;
    }

    private static void test10Nodes1Validator() throws ExecutionException, InterruptedException {
        createConnections(9);
        CordaRPCOps dataOwner = connections.get(0);
        CordaRPCOps validator = connections.get(1);
        connections.remove(dataOwner);
        connections.remove(validator);
        UUID setId = createGensisFlow(Arrays.asList(validator), dataOwner, connections);
        int transactionSize = 20;
        long time = System.currentTimeMillis();

        for (int i = 0; i < transactionSize; i++) {
            createWorkflowStep(setId, connections.get(ThreadLocalRandom.current().nextInt(connections.size())));
        }
        System.out.println("Transactions time avg:" + ((float) (System.currentTimeMillis() - time) / (float) transactionSize));


    }

    private static void test10Nodes2Validator() throws ExecutionException, InterruptedException {
        createConnections(9);
        CordaRPCOps dataOwner = connections.get(0);
        CordaRPCOps validator1 = connections.get(1);
        CordaRPCOps validator2 = connections.get(2);

        connections.remove(dataOwner);
        connections.remove(validator1);
        connections.remove(validator2);
        UUID setId = createGensisFlow(Arrays.asList(validator1, validator2), dataOwner, connections);
        int transactionSize = 20;
        long time = System.currentTimeMillis();

        for (int i = 0; i < transactionSize; i++) {
            createWorkflowStep(setId, connections.get(ThreadLocalRandom.current().nextInt(connections.size())));
        }
        System.out.println("Transactions time avg:" + ((float) (System.currentTimeMillis() - time) / (float) transactionSize));


    }

    private static void test10Nodes2Wfs() throws ExecutionException, InterruptedException {
        createConnections(9);
        CordaRPCOps dataOwnerA = connections.get(0);
        CordaRPCOps validatorA1 = connections.get(1);
        CordaRPCOps validatorA2 = connections.get(2);
        CordaRPCOps dataOwnerB = connections.get(3);
        CordaRPCOps validatorB1 = connections.get(4);
        CordaRPCOps validatorB2 = connections.get(5);

        connections.removeAll(Arrays.asList(dataOwnerA, validatorA1,validatorA2,validatorB1,validatorB2, dataOwnerB));
        List<CordaRPCOps> uA = Arrays.asList(connections.remove(0), connections.remove(1));
        UUID setIdA = createGensisFlow(Arrays.asList(validatorA1, validatorA2), dataOwnerA, uA);
        UUID setIdB = createGensisFlow(Arrays.asList(validatorB1, validatorB2), dataOwnerB, connections);

        int transactionSize = 20;
        long time = System.currentTimeMillis();

        for (int i = 0; i < transactionSize; i++) {
            if(i%2 == 0)
                createWorkflowStep(setIdA, uA.get(ThreadLocalRandom.current().nextInt(uA.size())));
            else
                createWorkflowStep(setIdB, connections.get(ThreadLocalRandom.current().nextInt(connections.size())));

        }
        System.out.println("Transactions time avg:" + ((float) (System.currentTimeMillis() - time) / (float) transactionSize));


    }

    private static void test10Nodes3Wfs() throws ExecutionException, InterruptedException {
        createConnections(9);
        CordaRPCOps dataOwnerA = connections.get(0);
        CordaRPCOps validatorA = connections.get(1);
        CordaRPCOps userA = connections.get(2);
        CordaRPCOps dataOwnerB = connections.get(3);
        CordaRPCOps validatorB = connections.get(4);
        CordaRPCOps userB = connections.get(5);
        CordaRPCOps dataOwnerC = connections.get(3);
        CordaRPCOps validatorC = connections.get(4);
        CordaRPCOps userC = connections.get(5);

        UUID setIdA = createGensisFlow(Arrays.asList(validatorA), dataOwnerA, Arrays.asList(userA));
        UUID setIdB = createGensisFlow(Arrays.asList(validatorB), dataOwnerB, Arrays.asList(userB));
        UUID setIdC = createGensisFlow(Arrays.asList(validatorC), dataOwnerC, Arrays.asList(userC));

        int transactionSize = 20;
        long time = System.currentTimeMillis();

        for (int i = 0; i < transactionSize; i++) {
            if (i%3 == 0)
                createWorkflowStep(setIdA, userA);
            else if (i%3 == 1)
                createWorkflowStep(setIdB, userB);
            else
                createWorkflowStep(setIdB, userC);


        }
        System.out.println("Transactions time avg:" + ((float) (System.currentTimeMillis() - time) / (float) transactionSize));


    }

    private static void test10Nodes4Validator() throws ExecutionException, InterruptedException {
        createConnections(9);
        CordaRPCOps dataOwner = connections.get(0);
        CordaRPCOps validator1 = connections.get(1);
        CordaRPCOps validator2 = connections.get(2);
        CordaRPCOps validator3 = connections.get(3);
        CordaRPCOps validator4 = connections.get(4);

        connections.remove(dataOwner);
        connections.remove(validator1);
        connections.remove(validator2);
        connections.remove(validator3);
        connections.remove(validator4);
        UUID setId = createGensisFlow(Arrays.asList(validator1, validator2, validator3, validator4), dataOwner, connections);
        int transactionSize = 20;
        long time = System.currentTimeMillis();

        for (int i = 0; i < transactionSize; i++) {
            createWorkflowStep(setId, connections.get(ThreadLocalRandom.current().nextInt(connections.size())));
        }
        System.out.println("Transactions time avg:" + ((float) (System.currentTimeMillis() - time) / (float) transactionSize));


    }

    private static void test10Nodes6Validator() throws ExecutionException, InterruptedException {
        createConnections(9);
        CordaRPCOps dataOwner = connections.get(0);
        CordaRPCOps validator1 = connections.get(1);
        CordaRPCOps validator2 = connections.get(2);
        CordaRPCOps validator3 = connections.get(3);
        CordaRPCOps validator4 = connections.get(4);
        CordaRPCOps validator5 = connections.get(5);
        CordaRPCOps validator6 = connections.get(6);

        connections.remove(dataOwner);
        connections.remove(validator1);
        connections.remove(validator2);
        connections.remove(validator3);
        connections.remove(validator4);
        connections.remove(validator5);
        connections.remove(validator6);
        UUID setId = createGensisFlow(Arrays.asList(validator1, validator2, validator3, validator4, validator5, validator6), dataOwner, connections);
        int transactionSize = 20;
        long time = System.currentTimeMillis();

        for (int i = 0; i < transactionSize; i++) {
            createWorkflowStep(setId, connections.get(ThreadLocalRandom.current().nextInt(connections.size())));
        }
        System.out.println("Transactions time avg:" + ((float) (System.currentTimeMillis() - time) / (float) transactionSize));


    }

    private static void test5Nodes6Validator() throws ExecutionException, InterruptedException {
        createConnections(4);
        CordaRPCOps dataOwner = connections.get(0);
        CordaRPCOps validator1 = connections.get(1);
        CordaRPCOps validator2 = connections.get(2);

        connections.remove(dataOwner);
        connections.remove(validator1);
        connections.remove(validator2);

        UUID setId = createGensisFlow(Arrays.asList(validator1, validator2), dataOwner, connections);
        int transactionSize = 20;
        long time = System.currentTimeMillis();

        for (int i = 0; i < transactionSize; i++) {
            createWorkflowStep(setId, connections.get(ThreadLocalRandom.current().nextInt(connections.size())));
        }
        System.out.println("Transactions time avg:" + ((float) (System.currentTimeMillis() - time) / (float) transactionSize));


    }

    private static HashMap<UUID, CordaRPCOps> workflows = new HashMap<>();

    private static UUID createGensisFlow(List<CordaRPCOps> validators, CordaRPCOps dataOwner, List<CordaRPCOps> users) throws ExecutionException, InterruptedException {
        //Create a Genesis Block
        String id = UUID.randomUUID().toString();
        List<AbstractParty> validatorParties = validators.stream().map(CordaRPCOps::nodeInfo)
                .map(NodeInfo::getLegalIdentities).map(index -> index.get(0)).collect(Collectors.toList());
        List<AbstractParty> permissionedParties = users.stream().map(CordaRPCOps::nodeInfo)
                .map(NodeInfo::getLegalIdentities).map(index -> index.get(0)).collect(Collectors.toList());
        FlowHandle handle = dataOwner.startFlowDynamic(GenesisFlow.class, validatorParties, permissionedParties, "urn:mrn:mcp:dataspace:" + id, Collections.emptyList());

        CordaFuture val = handle.getReturnValue();
        SignedTransaction returnVal = (SignedTransaction) val.get();
        UUID newSetID = returnVal.getCoreTransaction().outputsOfType(GenesisState.class).get(0).getUuid();

        //Making the dataset available to the dataspace.
        //DataSetLocation newDataset = new DataSetLocation(Paths.get("/dummy"), "Test data", "urn:mrn:mcp:dataspace:" + id, newSetID);
        workflows.put(newSetID, dataOwner);
        //connector.addDataSet(newDataset);
        return newSetID;

    }

    private static void createWorkflowStep(UUID workflowId, CordaRPCOps node) throws ExecutionException, InterruptedException {
        Validation dummyStep = new Validation("Dummy entry", "none", "1234", "test", "success");
        FlowHandle handleT = node.startFlowDynamic(WorkflowStepFlow.class, dummyStep, workflowId);
        CordaFuture valT = handleT.getReturnValue();
        Object returnValT = valT.get();
    }

    private static List<CordaRPCOps> connections = new ArrayList<>();

    private static void createConnections(int amount) {
        for (int i = 0; i < amount; i++) {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10006 + i);
            CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
            System.out.println("Connecting to:" + (10006 + i));
            CordaRPCConnection rpcConnection = rpcClient.start("user1", "test");
            CordaRPCOps proxy = rpcConnection.getProxy();
            connections.add(proxy);
        }
    }

    private static CordaRPCOps getNodeConnection(int node) {
        CordaRPCOps proxy = null;
        if (node == 0) {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10006);
            CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
            CordaRPCConnection rpcConnection = rpcClient.start("user1", "test");
            proxy = rpcConnection.getProxy();
        } else if (node == 1) {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10003);
            CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
            CordaRPCConnection rpcConnection = rpcClient.start("user1", "test");
            proxy = rpcConnection.getProxy();
        } else if (node == 2) {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10009);
            CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
            CordaRPCConnection rpcConnection = rpcClient.start("user1", "test");
            proxy = rpcConnection.getProxy();
        } else if (node == 3) {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10012);
            CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
            CordaRPCConnection rpcConnection = rpcClient.start("user1", "test");
            proxy = rpcConnection.getProxy();
        } else if (node == 4) {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10312);
            CordaRPCClient rpcClient = new CordaRPCClient(rpcAddress);
            CordaRPCConnection rpcConnection = rpcClient.start("user1", "test");
            proxy = rpcConnection.getProxy();
        } else if (node == 5) {
            NetworkHostAndPort rpcAddress = new NetworkHostAndPort("localhost", 10412);
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
