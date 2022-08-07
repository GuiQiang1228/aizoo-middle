package aizoo.domain;

import aizoo.common.ParameterIOType;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
public class ServiceOutputParameter extends BaseDomain {
    @Embedded
    private Parameter parameter;

    @JsonBackReference
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Service service;

    @Enumerated(EnumType.STRING)//枚举字符串
    private ParameterIOType parameterIoType = ParameterIOType.OUTPUT;

    private boolean isSelf;

    public ServiceOutputParameter(Parameter parameter, Service targetService, ParameterIOType parameterIoType, boolean isSelf) {
        this.parameter = parameter;
        this.service = targetService;
        this.parameterIoType = parameterIoType;
        this.isSelf = isSelf;
    }

    public ServiceOutputParameter() {
    }

    ;

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public ParameterIOType getParameterIoType() {
        return parameterIoType;
    }

    public void setParameterIoType(ParameterIOType parameterIoType) {
        this.parameterIoType = parameterIoType;
    }

    public boolean getIsSelf() {
        return isSelf;
    }

    public void setIsSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }
}
