package aizoo.common;

public enum DownloadStatus {
    NOT_BEGINNING("未开始"),
    DOWNLOADING("正在下载"),
    DOWNLOAD_ERROR("实验下载出错"),
    COMPLETED("已完成");

    DownloadStatus(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
