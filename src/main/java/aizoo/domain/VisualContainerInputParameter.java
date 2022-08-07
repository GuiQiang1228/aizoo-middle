package aizoo.domain;

import aizoo.common.ParameterIOType;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
public class VisualContainerInputParameter extends BaseDomain{
    @JsonBackReference
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private VisualContainer container;

    @Enumerated(EnumType.STRING)//枚举字符串
    private ParameterIOType parameterIoType = ParameterIOType.INPUT;

    @Embedded
    private Parameter parameter;

    public VisualContainer getContainer() {
        return container;
    }

    public void setContainer(VisualContainer container) {
        this.container = container;
    }

    public ParameterIOType getParameterIoType() {
        return parameterIoType;
    }

    public void setParameterIoType(ParameterIOType parameterIoType) {
        this.parameterIoType = parameterIoType;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }
}
