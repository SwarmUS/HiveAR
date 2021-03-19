package com.swarmus.hivear.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.swarmus.hivear.R;
import com.swarmus.hivear.models.SettingsViewModel;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private SettingsViewModel settingsViewModel;
    private Spinner folderSelection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        folderSelection = view.findViewById(R.id.folder_selection);
        updateAdapter();
        updateDatabaseContent(view);
        folderSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                settingsViewModel.getActiveDatabaseFolder().setValue(adapterView.getItemAtPosition(i).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        settingsViewModel.getActiveDatabaseFolder().observe(getViewLifecycleOwner(), s -> {
            updateDatabaseContent(view);
        });

        view.findViewById(R.id.add_folder).setOnClickListener(v -> {
            addNewFolder();
        });

        view.findViewById(R.id.delete_folder).setOnClickListener(v -> {
            String currentDatabase = folderSelection.getSelectedItem().toString();
            if (currentDatabase != null && !currentDatabase.isEmpty()) {
                File folderToDelete = new File(settingsViewModel.getRootFolder(), currentDatabase);
                settingsViewModel.getActiveDatabaseFolder().setValue(null);
                deleteRecursive(folderToDelete);
                settingsViewModel.updateDatabaseDirs(requireContext()); // Update cache list of folders
                updateAdapter(); // Update UI from new updated list
            }
        });

        view.findViewById(R.id.scan_qr_code).setOnClickListener(v -> {
            NavDirections action = SettingsFragmentDirections.actionSettingsFragmentToQRScanFragment();
            Navigation.findNavController(view).navigate(action);
        });
        return view;
    }

    private void deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void addNewFolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add new Database Folder");

        // Set up the input
        final EditText input = new EditText(requireContext());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String foldername = input.getText().toString();
                File newFolder = new File(settingsViewModel.getRootFolder(), foldername);
                try {
                    newFolder.getCanonicalPath();
                    newFolder.mkdir();

                    settingsViewModel.getActiveDatabaseFolder().setValue(newFolder.getName());
                    settingsViewModel.updateDatabaseDirs(requireContext());
                    updateAdapter();
                }
                catch (IOException e) {
                    Toast.makeText(requireContext(), "Folder couldn't be created", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void updateAdapter() {
        List<String> databaseFolders = settingsViewModel.getAllDatabases().getValue();
        if (databaseFolders == null || databaseFolders.isEmpty()) { databaseFolders = new ArrayList<>(); }
        ArrayAdapter<String> foldersAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1,
                databaseFolders.toArray(new String[0]));
        int currentIndex = databaseFolders.indexOf(settingsViewModel.getActiveDatabaseFolder().getValue());
        folderSelection.setAdapter(foldersAdapter);
        if (currentIndex >= 0) { folderSelection.setSelection(currentIndex); }
    }

    private void updateDatabaseContent(View view) {
        TextView databaseContent = view.findViewById(R.id.database_content);
        File folder = new File(settingsViewModel.getActiveFolderAbsolutePath());
        FilenameFilter filter = (f, name) -> name.endsWith(".jpg");
        String[] filesInFolder = folder.list(filter);
        String tvText = "";
        for (String file : filesInFolder) {
            tvText += file;
            tvText += "\n";
        }
        databaseContent.setText(tvText);
    }
}