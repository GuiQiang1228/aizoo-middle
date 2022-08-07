package aizoo.domain;

import aizoo.common.ParameterIOType;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
public class ComponentOutputParameter extends BaseDomain {
    @Embedded
    private Parameter parameter;

    @JsonBackReference
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Component component;

    @Enumerated(EnumType.STRING)//枚举字符串
    private ParameterIOType parameterIoType = ParameterIOType.OUTPUT;

    private boolean isSelf;

    public ComponentOutputParameter() {
    }

    public ComponentOutputParameter(Parameter parameter, Component component, ParameterIOType parameterIoType) {
        this.parameter = parameter;
        this.component = component;
        this.parameterIoType = parameterIoType;
    }
    public ComponentOutputParameter(Parameter parameter, Component component, ParameterIOType parameterIoType, boolean isSelf) {
        this.parameter = parameter;
        this.component = component;
        this.parameterIoType = parameterIoType;
        this.isSelf = isSelf;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
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
