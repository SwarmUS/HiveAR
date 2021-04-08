package com.swarmus.hivear.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.library.baseAdapters.BR;

public class FunctionTemplateArgument extends BaseObservable {
    private String name;
    private String value;
    private Class argumentType;

    public FunctionTemplateArgument(String name, String value, Class valueType) {
        this.name = name;
        this.argumentType = valueType;
        this.value = value;
    }

    public FunctionTemplateArgument(FunctionTemplateArgument f) {
        this.name = f.name;
        this.value = f.value;
        this.argumentType = f.argumentType;
    }

    public Class getArgumentType() {return this.argumentType;}

    @Bindable
    public String getValue() {return value;}

    public void setValue(String value) {
        if (!this.value.equals(value)) {
            this.value = value;
            notifyPropertyChanged(BR.value);
        }
    }

    @Bindable
    public String getName() {return name;}

    public void setName(String name) {
        this.name = name;
        notifyPropertyChanged(BR.name);
    }

    public Object getValueFromType() {
        if (Integer.class.equals(argumentType)) {
            return Integer.valueOf(value);
        }
        else if (Float.class.equals(argumentType)) {
            return Float.valueOf(value);
        }
        else {
            // By default, return as string
            return value;
        }
    }

    public boolean equals(Object other) {
        if (other instanceof FunctionTemplateArgument) {
            FunctionTemplateArgument otherA = (FunctionTemplateArgument)other;
            return this.argumentType.equals(otherA.argumentType) && this.name.equals(otherA.name);
        }
        return false;
    }

}
