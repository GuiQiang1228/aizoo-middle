package aizoo.viewObject.object;


public class ApplicationResultVO extends BaseVO{
    private String name;

    private String description;

    private String inputFile;

    private String path;

    private String url;

    private String applicationName;

    private VisualContainerVO visualContainerVO;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public VisualContainerVO getVisualContainerVO() {
        return visualContainerVO;
    }

    public void setVisualContainerVO(VisualContainerVO visualContainerVO) {
        this.visualContainerVO = visualContainerVO;
    }
}
