package com.swarmus.hivear.models;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swarmus.hivear.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsViewModel extends ViewModel {
    private MutableLiveData<String> activeDatabaseFolder;
    private MutableLiveData<List<String>> allDatabases;
    private File rootFolder;

    public MutableLiveData<String> getActiveDatabaseFolder()
    {
        activeDatabaseFolder  = activeDatabaseFolder == null ? new MutableLiveData<>() : activeDatabaseFolder;
        return activeDatabaseFolder;
    }

    public MutableLiveData<List<String>> getAllDatabases()
    {
        allDatabases = allDatabases == null ? new MutableLiveData<>() : allDatabases;
        return allDatabases;
    }

    public void updateDatabaseDirs(Context context) {
        List<String> databaseFolders = new ArrayList<>();
        if (rootFolder == null) {
            rootFolder = context.getDir(context.getString(R.string.ar_database_dir), Context.MODE_PRIVATE);
        }
        File[] childFolders = rootFolder.listFiles();
        for (File child : childFolders) {
            if (child.isDirectory()) { databaseFolders.add(child.getName()); }
        }
        getAllDatabases().setValue(databaseFolders);
    }

    public String getActiveFolderAbsolutePath() {
        if (rootFolder == null) { return null; }
        File newFile = new File(rootFolder, getActiveDatabaseFolder().getValue());
        return newFile.getAbsolutePath();
    }

    public File getRootFolder() {return rootFolder; }
}
