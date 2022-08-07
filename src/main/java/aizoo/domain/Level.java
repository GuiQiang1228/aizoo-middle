package aizoo.domain;

import aizoo.common.LevelType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;

@Entity
public class Level extends BaseDomain{

    @Enumerated(EnumType.STRING)
    private LevelType name;

    private String title;

    @Lob
    private String description;

    private int GPU;

    private int CPU;

    private double memory;

    private double disk;

    private int experimentTotalNum;

    private int serviceTotalNum;

    private int appTotalNum;

    private int mirrorTotalNum;

    private int experimentMaxRunningNum;

    private int serviceMaxRunningNum;

    private int appMaxRunningNum;

    private int mirrorMaxRunningNum;

    public LevelType getName() {
        return name;
    }

    public void setName(LevelType name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getGPU() {
        return GPU;
    }

    public void setGPU(int GPU) {
        this.GPU = GPU;
    }

    public int getCPU() {
        return CPU;
    }

    public void setCPU(int CPU) {
        this.CPU = CPU;
    }

    public double getMemory() {
        return memory;
    }

    public void setMemory(double memory) {
        this.memory = memory;
    }

    public double getDisk() {
        return disk;
    }

    public void setDisk(double disk) {
        this.disk = disk;
    }

    public int getExperimentTotalNum() {
        return experimentTotalNum;
    }

    public void setExperimentTotalNum(int experimentTotalNum) {
        this.experimentTotalNum = experimentTotalNum;
    }

    public int getServiceTotalNum() { return serviceTotalNum; }

    public void setServiceTotalNum(int serviceTotalNum) {
        this.serviceTotalNum = serviceTotalNum;
    }

    public int getAppTotalNum() {
        return appTotalNum;
    }

    public void setAppTotalNum(int appTotalNum) {
        this.appTotalNum = appTotalNum;
    }

    public int getMirrorTotalNum() { return mirrorTotalNum; }

    public void setMirrorTotalNum(int mirrorTotalNum) { this.mirrorTotalNum = mirrorTotalNum; }

    public int getExperimentMaxRunningNum() {
        return experimentMaxRunningNum;
    }

    public void setExperimentMaxRunningNum(int experimentMaxRunningNum) {
        this.experimentMaxRunningNum = experimentMaxRunningNum;
    }

    public int getServiceMaxRunningNum() {
        return serviceMaxRunningNum;
    }

    public void setServiceMaxRunningNum(int serviceMaxRunningNum) {
        this.serviceMaxRunningNum = serviceMaxRunningNum;
    }

    public int getAppMaxRunningNum() {
        return appMaxRunningNum;
    }

    public void setAppMaxRunningNum(int appMaxRunningNum) {
        this.appMaxRunningNum = appMaxRunningNum;
    }

    public int getMirrorMaxRunningNum() {
        return mirrorMaxRunningNum;
    }

    public void setMirrorMaxRunningNum(int mirrorMaxRunningNum) {
        this.mirrorMaxRunningNum = mirrorMaxRunningNum;
    }
}
