package de.uol.provenancechain.processing;

import de.uol.provenancechain.workflow.Conversion;
import de.uol.provenancechain.workflow.DataTransformation;
import de.uol.provenancechain.workflow.WorkflowStep;
import org.apache.commons.codec.digest.DigestUtils;
import tech.tablesaw.api.Table;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index2D;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements the creation of a Traffic density Heatmap based on preprocessed vessel traffic data.
 */
public class HeatMapFlow implements DataProcessingFlow {
    private final Path dataSetPath;
    private Path resultPath;

    @Override
    public List<WorkflowStep> getWorkflowSteps() {
        String dataHash = "";
        try {
            dataHash = DigestUtils.sha256Hex(Files.newInputStream(resultPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataTransformation transformation = new DataTransformation("Transformation to Heatmap", "The " +
                "position data is accumulated into a grid of lats/lons. The more positions are contained in one field of " +
                "the grid, the higher the heat value assigned to this field.", "transformToHeatMap",
                "grid_size=1000*1000", dataHash, null);
        Conversion csvToNc = new Conversion("CSV to netCDF", "File conversion from csv Format to netCDF",
                dataHash, "csv", "nc");
        return Arrays.asList(transformation, csvToNc);
    }

    /**
     * Constructor
     *
     * @param dataSetPath Location of the source data set.
     */
    public HeatMapFlow(Path dataSetPath) {
        this.dataSetPath = dataSetPath;
    }

    @Override
    public void doProcessing() {
        Table positionData = null;
        try {
            positionData = Table.read().csv(this.dataSetPath.toAbsolutePath().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Creating a Grid for the netCDF datastructure
        double minLat = positionData.doubleColumn("lat").min();
        double maxLat = positionData.doubleColumn("lat").max();
        double minLon = positionData.doubleColumn("lon").min();
        double maxLon = positionData.doubleColumn("lon").max();

        double resolution = 1000;
        double deltaLat = (maxLat - minLat) / resolution;
        double deltaLon = (maxLon - minLon) / resolution;

        double[] gridLats = new double[(int) resolution + 1];
        double[] gridLons = new double[(int) resolution + 1];
        int[][] heatData = new int[gridLats.length][gridLons.length];

        for (int i = 0; i <= resolution; i++) {
            gridLats[i] = minLat + deltaLat * i;
            gridLons[i] = minLon + deltaLon * i;
        }

        //Calculate heat values.
        for (int i = 0; i < positionData.rowCount(); i++) {
            double lat = positionData.doubleColumn("lat").get(i);
            double lon = positionData.doubleColumn("lon").get(i);
            int[] indices = getIndices(lat, lon, gridLats, gridLons);
            heatData[indices[0]][indices[1]]++;
        }
        this.resultPath = Paths.get("clients/src/main/resources/heatmap.nc");
        writeNetCDF(gridLats, gridLons, heatData);

    }

    @Override
    public Path getResultLocation() {
        return this.resultPath;
    }

    /**
     * Returns the grid index of a specific position in a specific grid.
     *
     * @param lat      position latitude
     * @param lon      position longitude
     * @param gridLats latitudes of grid cells
     * @param gridLons longitudes of grid cells
     * @return the index in the grid.
     */
    private int[] getIndices(double lat, double lon, double[] gridLats, double[] gridLons) {
        int x = 0;
        int y = 0;
        while (gridLats[x] < lat)
            x++;
        while (gridLons[y] < lon)
            y++;
        return new int[]{x, y};
    }

    /**
     * Writes a Grid with heat data into a netCDF File. This file can be viewed e.g. with Panoply.
     *
     * @param gridLats the grid latitudes
     * @param gridLons the grid longitudes
     * @param heatData the heat data for the grid.
     */

    private void writeNetCDF(double[] gridLats, double[] gridLons, int[][] heatData) {
        //See: https://www.unidata.ucar.edu/software/netcdf/examples/programs/Sfc_pres_temp_wr.java
        // Create the file.
        String filename = this.resultPath.toAbsolutePath().toString();
        NetcdfFileWriter dataFile = null;

        try {
            // Create new netcdf-3 file with the given filename
            dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename);

            // In addition to the latitude and longitude dimensions, we will
            // also create latitude and longitude netCDF variables which will
            // hold the actual latitudes and longitudes. Since they hold data
            // about the coordinate system, the netCDF term for these is:
            // "coordinate variables."
            Dimension latDim = dataFile.addDimension(null, "latitude", gridLats.length);
            Dimension lonDim = dataFile.addDimension(null, "longitude", gridLons.length);
            List<Dimension> dims = new ArrayList<Dimension>();
            dims.add(latDim);
            dims.add(lonDim);

            Variable vlat = dataFile.addVariable(null, "latitude", DataType.FLOAT, "latitude");
            Variable vlon = dataFile.addVariable(null, "longitude", DataType.FLOAT, "longitude");

            Variable heat = dataFile.addVariable(null, "heat", DataType.FLOAT, dims);

            // Define units attributes for coordinate vars. This attaches a
            // text attribute to each of the coordinate variables, containing
            // the units.

            vlat.addAttribute(new Attribute("units", "degrees_dec"));
            vlon.addAttribute(new Attribute("units", "degrees_dec"));

            // Define units attributes for variables.
            heat.addAttribute(new Attribute("units", "#"));

            // Write the coordinate variable data. This will put the latitudes
            // and longitudes of our data grid into the netCDF file.
            dataFile.create();

            Array dataLat = Array.factory(DataType.FLOAT, new int[]{gridLats.length});
            Array dataLon = Array.factory(DataType.FLOAT, new int[]{gridLons.length});

            int i, j;

            for (i = 0; i < latDim.getLength(); i++) {
                dataLat.setFloat(i, (float) gridLats[i]);
            }

            for (j = 0; j < lonDim.getLength(); j++) {
                dataLon.setFloat(j, (float) gridLons[j]);
            }

            dataFile.write(vlat, dataLat);
            dataFile.write(vlon, dataLon);

            // Create the pretend data. This will write our surface pressure and
            // surface temperature data.

            int[] iDim = new int[]{latDim.getLength(), lonDim.getLength()};
            Array dataHeat = Array.factory(DataType.FLOAT, iDim);

            Index2D idx = new Index2D(iDim);

            for (i = 0; i < latDim.getLength(); i++) {
                for (j = 0; j < lonDim.getLength(); j++) {
                    idx.set(i, j);
                    dataHeat.setFloat(idx, heatData[i][j]);
                }
            }

            dataFile.write(heat, dataHeat);
        } catch (IOException e) {
            e.printStackTrace();

        } catch (InvalidRangeException e) {
            e.printStackTrace();

        } finally {
            if (null != dataFile) {
                try {
                    dataFile.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }
}
