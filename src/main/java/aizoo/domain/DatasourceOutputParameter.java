package aizoo.domain;

import aizoo.common.ParameterIOType;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

@Entity
public class DatasourceOutputParameter extends BaseDomain {
    @Embedded
    private Parameter parameter;

    @JsonBackReference
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Datasource datasource;

    @Enumerated(EnumType.STRING)//枚举字符串
    private ParameterIOType parameterIoType = ParameterIOType.OUTPUT;

    public DatasourceOutputParameter(){

    }

    public DatasourceOutputParameter(Parameter parameter, Datasource datasource, ParameterIOType parameterIoType) {
        this.parameter = parameter;
        this.datasource = datasource;
        this.parameterIoType = parameterIoType;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }

    public ParameterIOType getParameterIoType() {
        return parameterIoType;
    }

    public void setParameterIoType(ParameterIOType parameterIoType) {
        this.parameterIoType = parameterIoType;
    }
}
