package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.models.FunctionTemplate;

import java.util.List;

public abstract class CommandListVM extends ViewModel {
    protected String listTitle;
    MutableLiveData<List<FunctionTemplate>> commands;

    public MutableLiveData<List<FunctionTemplate>> getCommandList() {
        if (commands == null) { commands = new MutableLiveData<>(); }
        return commands;
    }

    public String getListTitle() {return listTitle;}
}
