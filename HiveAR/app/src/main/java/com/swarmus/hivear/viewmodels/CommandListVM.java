package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.models.FunctionTemplate;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandListVM extends ViewModel {
    protected String listTitle;
    MutableLiveData<List<FunctionTemplate>> commands = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<FunctionTemplate>> getCommandList() {
        return commands;
    }

    public void setList(List<FunctionTemplate> list) {
        commands.setValue(list);
    }

    public String getListTitle() {return listTitle;}

    public void addFunction(FunctionTemplate functionTemplate) {
        ArrayList<FunctionTemplate> l = new ArrayList<>(getCommandList().getValue());
        if (!l.contains(functionTemplate)) {
            l.add(new FunctionTemplate(functionTemplate));
            commands.setValue(l);
        }
    }

    public void removeFunction(FunctionTemplate functionTemplate) {
        ArrayList<FunctionTemplate> l = new ArrayList<>(getCommandList().getValue());
        if (l.contains(functionTemplate)) {
            l.remove(functionTemplate);
            commands.setValue(l);
        }
    }
}
