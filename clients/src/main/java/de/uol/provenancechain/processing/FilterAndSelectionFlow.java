package de.uol.provenancechain.processing;

import de.uol.provenancechain.workflow.DataTransformation;
import de.uol.provenancechain.workflow.WorkflowStep;
import org.apache.commons.codec.digest.DigestUtils;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.TextColumn;
import tech.tablesaw.selection.Selection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the filtering and Selection workflow for vessel traffic data.
 */
public class FilterAndSelectionFlow implements DataProcessingFlow {
    private final Path dataSetPath;
    private Path resultLocation;

    @Override
    public List<WorkflowStep> getWorkflowSteps() {
        List<WorkflowStep> steps = new ArrayList<>();
        String dataHash = "";
        try {
            dataHash = DigestUtils.sha256Hex(Files.newInputStream(this.resultLocation));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataTransformation filterAttributes = new DataTransformation("Filter out unnecessary RADAR Data",
                "Remove rows only containing RADAR data", "filterRADARData", "theme=RADAR",
                dataHash, Collections.emptyList());
        steps.add(filterAttributes);
        DataTransformation filterCols = new DataTransformation("Filter out unnecessary data Columns",
                "Remove all columns except position data", "filterCols", "keep=lat,lon",
                dataHash, Collections.emptyList());
        steps.add(filterCols);
        return steps;
    }

    /**
     * Constructor.
     * @param dataSetPath Path to the source data for this workflow.
     */
    public FilterAndSelectionFlow(Path dataSetPath) {
        this.dataSetPath = dataSetPath;

    }

    @Override
    public void doProcessing() {
        Table aisDataRaw = null;
        try {
            aisDataRaw = Table.read().csv(this.dataSetPath.toAbsolutePath().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Filter out unnecessary data:
        StringColumn type = aisDataRaw.stringColumn("theme");
        Selection radar = type.isEqualTo("RADAR");
        aisDataRaw.dropWhere(radar);

        TextColumn position = aisDataRaw.textColumn("position");
        Selection staticMessages = position.isEqualTo("(0,0)");
        System.out.println(aisDataRaw.rowCount());
        aisDataRaw = aisDataRaw.dropWhere(staticMessages);
        System.out.println(aisDataRaw.rowCount());

        //Convert to lat,lon columns
        DoubleColumn lat = DoubleColumn.create("lat");
        DoubleColumn lon = DoubleColumn.create("lon");

        for (String s : aisDataRaw.textColumn("position")) {
            String[] pos = s.replace("(", "").replace(")", "").split(",");
            lat.append(Double.valueOf(pos[0]));
            lon.append(Double.valueOf(pos[1]));
        }

        aisDataRaw = aisDataRaw.removeColumns(position);
        aisDataRaw = aisDataRaw.addColumns(lat, lon);

        //These values are out of range of the sensor network, so they must be invalid
        Selection invalidLatValues = lat.isBetweenInclusive(40, 60).flip(0, lat.size());
        Selection invalidLonValues = lon.isBetweenInclusive(0, 15).flip(0, lon.size());

        aisDataRaw = aisDataRaw.dropWhere(invalidLatValues);
        aisDataRaw = aisDataRaw.dropWhere(invalidLonValues);

        Table newDataSet = Table.create(aisDataRaw.column("lat"), aisDataRaw.column("lon"));


        String newDataSetPath = "clients/src/main/resources/ais-data2.csv";
        this.resultLocation = Paths.get(newDataSetPath);
        try {
            newDataSet.write().csv(newDataSetPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Path getResultLocation() {
        return this.resultLocation;
    }
}
