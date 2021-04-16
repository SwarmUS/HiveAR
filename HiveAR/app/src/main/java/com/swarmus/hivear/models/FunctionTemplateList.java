package com.swarmus.hivear.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;

public class FunctionTemplateList extends Observable {
    private List<FunctionTemplate> functionTemplateList = new ArrayList<>();

    public void set(List<FunctionTemplate> list) {
        functionTemplateList = new ArrayList<>(list);
        functionTemplateList.sort(Comparator.comparing(FunctionTemplate::getName));
        setChanged();
        notifyObservers();
    }

    public void addDuplicate(FunctionTemplate functionTemplate) {
        functionTemplateList.add(new FunctionTemplate(functionTemplate)); // Create new to unlink reference
        functionTemplateList.sort(Comparator.comparing(FunctionTemplate::getName));
        setChanged();
        notifyObservers();
    }

    public void add(FunctionTemplate functionTemplate) {
        if (!functionTemplateList.contains(functionTemplate)) {
            functionTemplateList.add(new FunctionTemplate(functionTemplate));
            functionTemplateList.sort(Comparator.comparing(FunctionTemplate::getName));
            setChanged();
            notifyObservers();
        }
    }

    public void remove(FunctionTemplate functionTemplate) {
        if (functionTemplateList.contains(functionTemplate)) {
            functionTemplateList.remove(functionTemplate);
            setChanged();
            notifyObservers();
        }
    }

    public void clear() {
        functionTemplateList = new ArrayList<>();
        setChanged();
        notifyObservers();
    }

    public FunctionTemplate at(int index) {
        return functionTemplateList.get(index);
    }

    public int size() {
        return functionTemplateList.size();
    }
}
