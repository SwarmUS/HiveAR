package com.swarmus.hivear.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.swarmus.hivear.R;
import com.swarmus.hivear.models.SettingsViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private List<String> databaseFolders;
    private SettingsViewModel settingsViewModel;
    private File root;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateDatabaseDirs();
        // TODO SHOULD INIT AT ACTIVITY FOR AR
        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button folderSelection = view.findViewById(R.id.folder_selection);
        folderSelection.setOnClickListener(v -> {
            PopupWindow popupFolders = popupFolders();
            if (popupFolders != null){ popupFolders.showAsDropDown(v, -5, 0); };
        });
        // TODO SET DEFAULT FOLDER

        view.findViewById(R.id.add_folder).setOnClickListener(v -> {
            // TODO INPUT POPUP
            File newFolder = new File(root, "test");
            newFolder.mkdir();

            settingsViewModel.getActiveDatabaseFolder().setValue(newFolder.getAbsolutePath());
            folderSelection.setText(newFolder.getName());
            updateDatabaseDirs();
        });

        view.findViewById(R.id.delete_folder).setOnClickListener(v -> {
            File folderToDelete = new File(root, folderSelection.getText().toString());
            deleteRecursive(folderToDelete);
            folderSelection.setText(getString(R.string.select_folder));
            settingsViewModel.getActiveDatabaseFolder().setValue(null);
        });

        view.findViewById(R.id.scan_qr_code).setOnClickListener(v -> {
            NavDirections action = SettingsFragmentDirections.actionSettingsFragmentToQRScanFragment();

            Navigation.findNavController(view).navigate(action);
        });
        return view;
    }

    private void updateDatabaseDirs() {
        databaseFolders = new ArrayList<>();
        root = requireContext().getDir(getString(R.string.ar_database_dir), Context.MODE_PRIVATE);
        File[] childFolders = root.listFiles();
        for (File child : childFolders) {
            if (child.isDirectory()) { databaseFolders.add(child.getName()); }
        }
    }

    private PopupWindow popupFolders() {

        updateDatabaseDirs();
        if (databaseFolders.isEmpty()) { return null; }

        // initialize a pop up window type
        PopupWindow popupWindow = new PopupWindow(requireContext());

        // the drop down list is a list view
        ListView listView = new ListView(requireContext());

        // set our adapter and pass our pop up window contents
        listView.setAdapter(foldersAdapter(databaseFolders.toArray(new String[0])));

        // set the item click listener
        listView.setOnItemClickListener((adapterView, view, i, l) -> {

            // add some animation when a list item was clicked
            Animation fadeInAnimation = AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_in);
            fadeInAnimation.setDuration(10);
            view.startAnimation(fadeInAnimation);

            // dismiss the pop up
            popupWindow.dismiss();

            // get the text and set it as the button text
            String selectedItemText = ((TextView) view).getText().toString();
            settingsViewModel.getActiveDatabaseFolder().setValue(selectedItemText);

        });

        // some other visual settings
        popupWindow.setFocusable(true);
        popupWindow.setWidth(250);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        // set the list view as pop up window content
        popupWindow.setContentView(listView);

        return popupWindow;
    }

    private ArrayAdapter<String> foldersAdapter(String folders[]) {

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, folders) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                // setting the ID and text for every items in the list
                String item = getItem(position);

                // visual settings for the list item
                TextView listItem = new TextView(requireContext());

                listItem.setText(item);
                listItem.setTextSize(22);
                listItem.setPadding(10, 10, 10, 10);
                listItem.setTextColor(Color.WHITE);

                return listItem;
            }
        };

        return adapter;
    }

    private void deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}