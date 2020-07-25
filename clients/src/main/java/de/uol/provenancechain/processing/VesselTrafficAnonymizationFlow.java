package de.uol.provenancechain.processing;

import de.uol.provenancechain.workflow.Anonymization;
import de.uol.provenancechain.workflow.DataAcquisition;
import de.uol.provenancechain.workflow.WorkflowStep;
import org.apache.commons.codec.digest.DigestUtils;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a workflow for preparing and anonymizing raw vessel traffic data for the dataspace.
 */
public class VesselTrafficAnonymizationFlow implements DataProcessingFlow {
    /** Path of the source data */
    private final Path dataSetPath;
    /** Path of the result data */
    private final Path resultPath;

    /**
     * Constructor.
     * @param dataSet source data set.
     */
    public VesselTrafficAnonymizationFlow(Path dataSet) {
        this.dataSetPath = dataSet;
        this.resultPath = Paths.get("clients/src/main/resources/ais-data1.csv");
    }

    @Override
    public List<WorkflowStep> getWorkflowSteps() {
        String dataHash = "";
        try {
            dataHash = DigestUtils.sha256Hex(Files.newInputStream(this.resultPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<WorkflowStep> steps = new ArrayList<>();
        DataAcquisition acquisition = new DataAcquisition("AIS+RADAR Sensor data from OrgA Sensor network",
                "Data contains all AIS attributes for AIS messages and position, time, velocity and track_id" +
                        " for RADAR data", dataHash, "Organization A", "NaviBox Sensors v1.0",
                LocalDateTime.of(2019, 11, 25, 10, 7), "NMEA0183->PostgreSQL DB Scheme");
        Anonymization anonymization = new Anonymization("Anonymization of MMSI", "MMSI replaces by " +
                "sha256Hex(MMSI)", dataHash, "sha256", "encoding=hex");
        steps.add(acquisition);
        steps.add(anonymization);
        return steps;

    }

    @Override
    public void doProcessing() {
        Table aisDataRaw = null;
        try {
            aisDataRaw = Table.read().csv(this.dataSetPath.toAbsolutePath().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Hashing MMSIs for Anonymization
        StringColumn mmsis = aisDataRaw.stringColumn(1);
        StringColumn hashed = StringColumn.create("hashedMMSI");
        for (String m : mmsis) {
            if (m != null)
                hashed.append(DigestUtils.sha256Hex(m));
            else
                hashed.append("n/a");
        }
        aisDataRaw.replaceColumn(1, hashed);

        try {
            aisDataRaw.write().csv(this.resultPath.toAbsolutePath().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Path getResultLocation() {
        return resultPath;
    }
}
