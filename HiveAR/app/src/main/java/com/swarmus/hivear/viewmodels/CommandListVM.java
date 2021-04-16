package com.swarmus.hivear.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.models.FunctionTemplate;
import com.swarmus.hivear.models.FunctionTemplateList;

public abstract class CommandListVM extends ViewModel {
    protected String listTitle;
    private FunctionTemplateList functionTemplateList;
    private final MutableLiveData<FunctionTemplateList> commands;

    public CommandListVM() {
         commands = new MutableLiveData<>(functionTemplateList);
         setList(new FunctionTemplateList());
    }

    public LiveData<FunctionTemplateList> getCommandList() {
        return commands;
    }

    public void setList(FunctionTemplateList list) {
        functionTemplateList = list;
        functionTemplateList.addObserver(((observable, o) -> {
            commands.postValue(functionTemplateList);
        }));
        commands.postValue(functionTemplateList);
    }

    public String getListTitle() {return listTitle;}

    public void addDuplicateFunction(FunctionTemplate functionTemplate) {
        functionTemplateList.addDuplicate(functionTemplate);
        commands.postValue(functionTemplateList);
    }

    public void addFunction(FunctionTemplate functionTemplate) {
        functionTemplateList.add(functionTemplate);
        commands.postValue(functionTemplateList);
    }

    public void removeFunction(FunctionTemplate functionTemplate) {
        functionTemplateList.remove(functionTemplate);
        commands.postValue(functionTemplateList);
    }
}
