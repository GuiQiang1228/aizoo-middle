package aizoo.domain;

import aizoo.common.ParameterIOType;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
public class ServiceInputParameter extends BaseDomain {
    @Embedded
    private Parameter parameter;

    @JsonBackReference
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Service service;

    @Enumerated(EnumType.STRING)//枚举字符串
    private ParameterIOType parameterIoType = ParameterIOType.INPUT;

    public ServiceInputParameter() {
    }

    public ServiceInputParameter(Parameter parameter, Service service, ParameterIOType parameterIoType) {
        this.parameter = parameter;
        this.service = service;
        this.parameterIoType = parameterIoType;
    }

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
}
