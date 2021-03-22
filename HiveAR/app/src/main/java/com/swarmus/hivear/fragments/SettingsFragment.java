package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
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
        TableLayout tableLayout = view.findViewById(R.id.tableLayout);
        tableLayout.setStretchAllColumns(true);
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
        builder.setTitle(getString(R.string.add_new_database_folder));

        // Set up the input
        final EditText input = new EditText(requireContext());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(getString(R.string.add), (dialog, which) -> {
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
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());

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
        TableLayout tableLayout = view.findViewById(R.id.tableLayout);
        tableLayout.removeAllViews();
        TableRow headers = getTableHeaderRow();
        tableLayout.addView(headers, new TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));

        File folder = new File(settingsViewModel.getActiveFolderAbsolutePath());
        FilenameFilter filter = (f, name) -> name.endsWith(getString(R.string.jpeg_extension));
        String[] filesInFolder = folder.list(filter);
        for (String file : filesInFolder) {
            file = file.replace(".jpg", "");
            String robotName = file.split("-")[0];
            int robotUid = Integer.valueOf(file.split("-")[1]);

            TableRow row = getRobotInfoRow(robotName, robotUid);
            tableLayout.addView(row, new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private TableRow getTableHeaderRow() {
        TextView name = new TextView(requireContext());
        name.setText(getString(R.string.robot_name));
        name.setTextSize(16.0f);
        name.setBackgroundColor(getResources().getColor(R.color.honeycomb));
        name.setTextColor(getResources().getColor(R.color.design_default_color_on_secondary));
        TextView uid = new TextView(requireContext());
        uid.setText(getString(R.string.uid));
        uid.setTextSize(16.0f);
        uid.setBackgroundColor(getResources().getColor(R.color.honeycomb));
        uid.setTextColor(getResources().getColor(R.color.design_default_color_on_secondary));

        TableRow rowHeader = new TableRow(requireContext());
        rowHeader.setPadding(0, 0, 0, 10);

        rowHeader.addView(name);
        rowHeader.addView(uid);
        return rowHeader;
    }

    private TableRow getRobotInfoRow(String name, int uid) {
        TextView nameTV = new TextView(requireContext());
        nameTV.setText(name);
        TextView uidTV = new TextView(requireContext());
        uidTV.setText(Integer.toString(uid));

        TableRow row = new TableRow(requireContext());
        row.setPadding(0, 5, 0, 5);

        row.addView(nameTV);
        row.addView(uidTV);
        return row;
    }
}