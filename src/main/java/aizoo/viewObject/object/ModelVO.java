package aizoo.viewObject.object;

import aizoo.common.GraphType;

public class ModelVO extends BaseVO{
    private String name;

    private long graphId;

    private String description;

    private String username;

    private GraphType graphType;

    private long sourceId;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public long getGraphId() { return graphId; }

    public void setGraphId(long graphId) { this.graphId = graphId; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public GraphType getGraphType() { return graphType; }

    public void setGraphType(GraphType graphType) { this.graphType = graphType; }

    public long getSourceId() { return sourceId; }

    public void setSourceId(long sourceId) { this.sourceId = sourceId; }
}
