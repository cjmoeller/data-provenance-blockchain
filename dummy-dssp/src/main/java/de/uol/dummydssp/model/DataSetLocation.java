package de.uol.dummydssp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Represents a workflow-data location on the data space.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSetLocation {
    /**
     * @return The workflow id of the workflow
     */
    public UUID getWorkflowID() {
        return workflowID;
    }

    private final UUID workflowID;

    /**
     * @return the location of the acutal data
     */
    public Path getLocation() {
        return location;
    }

    /**
     * @param location the location of the acutal data
     */
    public void setLocation(Path location) {
        this.location = location;
    }

    /**
     * @return the name of the data set
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name of the data set
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DataSetLocation{" +
                "location=" + location +
                ", name='" + name + '\'' +
                ", urn='" + urn + '\'' +
                '}';
    }

    /**
     * @return the URN for identifying the data set.
     */
    public String getUrn() {
        return urn;
    }

    /**
     * @param urn the URN for identifying the data set.
     */
    public void setUrn(String urn) {
        this.urn = urn;
    }

    private Path location;
    private String name;
    private String urn;

    /**
     * Constructor
     *
     * @param location   the location of the data set
     * @param name       the name of the data set
     * @param urn        the URN of the data set
     * @param workflowID the corresponding workflow
     */
    public DataSetLocation(Path location, String name, String urn, UUID workflowID) {
        this.location = location;
        this.name = name;
        this.urn = urn;
        this.workflowID = workflowID;
    }
}
