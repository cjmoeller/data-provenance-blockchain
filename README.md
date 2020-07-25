# Permissioned Blockchain for Scientific Data Management Implementation

This is the corresponding implementation for the paper "Permissioned Blockchain for Scientific Data Management" by Julius
Möller, Sibylle Fröschle and Axel Hahn.

Note: This is an experimental implementation for demonstrating the proposed concept of the paper. Also please note, that 
we cannot provide public access to our internal data space infrastructure. For this reason, all data space-specific 
features are replaced by a mock-up implementation. This does not alter the functionality of the actual work.

# Pre-Requisites

Set-up the project according to https://docs.corda.net/getting-set-up.html and https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp .

# Usage
1. Start with building the project, including the corda nodes: ``gradle clean build deploy-nodes``
2. Start the nodes by running _build/nodes/runnodes_
3. Start the DSSP by running _dummy-dssp/src/main/java/de/uol/dummydssp/DummyDsspApplication.java_
4. Start the node-client by running _clients/src/main/java/de/uol/provenancechain/webserver/Starter.java_
5. Open a web-browser and open (in this order):
    - http://localhost:8082/regKeys - to register the nodes' identities at the DSSP
    - http://localhost:8080/step1 - to launch the data processing steps from Organization A (see figure 7 in the paper)
    - http://localhost:8080/step2 - to launch the data processing steps from Organization B
    - http://localhost:8080/step2 - to launch the data processing steps from Organization C
    
You should always receive a json object which represents the data provenance information stored in the blockchain as a response.

# Overview
``clients`` - contains the code for connecting to the blockchain nodes and interacting with them. Also includes the data processing logic.

``contracts`` - includes the smart contracts which where used to implement the consensus method and storage of data provenance information

``dummy-dssp`` - contains a mock-up implementation of a data-space support platform with a REST-interface.

``workflows`` - includes code which is deployed on the blockchain nodes, which specifies the communication between the nodes.

This implementation is mainly based on corda (https://github.com/corda/corda) and the Spring framework (https://spring.io/).