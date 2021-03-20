package com.swarmus.hivear.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.swarmus.hivear.R;
import com.swarmus.hivear.models.SettingsViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class QRScanFragment extends Fragment {
    private CameraSource cameraSource;
    private TextView textView;
    private Button addToDatabase;
    private JSONObject qrJsonInfo;
    private BarcodeDetector barcodeDetector;

    private final static int QRCodeWidth = 500; // Pixels
    private final static String JSON_ROBOT_NAME = "name";
    private final static String JSON_ROBOT_UID = "uid";
    private final static String JSON_ROBOT_DESCRIPTION = "description";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_q_r_scan, container, false);

        SurfaceView surfaceView = view.findViewById(R.id.camera);
        textView = view.findViewById(R.id.scan_qr_code_text);
        EditText robotNameET = view.findViewById(R.id.input_name);
        EditText robotUidET = view.findViewById(R.id.input_uid);
        EditText robotDescriptionET = view.findViewById(R.id.input_description);
        addToDatabase = view.findViewById(R.id.add_qr_to_database);
        addToDatabase.setVisibility(View.INVISIBLE);
        addToDatabase.setOnClickListener(v -> {
            try{
                Bitmap bitmap = textToImageEncode(qrJsonInfo.toString());
                String robotName = qrJsonInfo.getString(JSON_ROBOT_NAME);
                int robotUID = qrJsonInfo.getInt(JSON_ROBOT_UID);
                String fileTitle = robotName + "-" + robotUID;
                // Add to data base instead of save to device
                SettingsViewModel settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
                String wasAddedMsg = addQRToARDatabase(fileTitle, bitmap) ?
                    "Saved to " + settingsViewModel.getActiveDatabaseFolder().getValue() :
                        "Couldn't save to AR Database.";

                Toast.makeText(requireContext(), wasAddedMsg, Toast.LENGTH_LONG).show();

            }catch (WriterException | JSONException e){
                Toast.makeText(requireContext(), "Error while adding QR to database", Toast.LENGTH_LONG).show();
            }

        });

        Button download = view.findViewById(R.id.download);
        download.setOnClickListener(v -> {
            try{
                JSONObject robotJsonDescription = new JSONObject();
                robotJsonDescription.accumulate(JSON_ROBOT_NAME, robotNameET.getText());
                robotJsonDescription.accumulate(JSON_ROBOT_UID, robotUidET.getText());
                robotJsonDescription.accumulate(JSON_ROBOT_DESCRIPTION, robotDescriptionET.getText());
                Bitmap bitmap = textToImageEncode(robotJsonDescription.toString());
                String fileTitle = robotNameET.getText() + "-" + robotUidET.getText();
                MediaStore.Images.Media.insertImage(requireActivity().getContentResolver(), bitmap,
                        fileTitle, null);
                // Also automatically save to database
                SettingsViewModel settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
                String wasAddedMsg = addQRToARDatabase(fileTitle, bitmap) ?
                        "Downloaded to storage and saved to " + settingsViewModel.getActiveDatabaseFolder().getValue() :
                        "Couldn't save to AR Database.";

                Toast.makeText(requireContext(), wasAddedMsg, Toast.LENGTH_LONG).show();

            }catch (WriterException | JSONException e){
                e.printStackTrace();
            }

        });

        barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(getContext(), barcodeDetector)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(640, 480).build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                try {
                    cameraSource.start(surfaceHolder);
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {}

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrcode = detections.getDetectedItems();
                if (qrcode.size() != 0) {
                    String qrText = qrcode.valueAt(0).displayValue;
                    try {
                        textView.post(() -> textView.setText(qrText));
                        qrJsonInfo = new JSONObject(qrText);
                        if (qrJsonInfo.has(JSON_ROBOT_NAME) && qrJsonInfo.has(JSON_ROBOT_UID))
                        {
                            addToDatabase.post(() -> addToDatabase.setVisibility(View.VISIBLE));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        String defaultText = getString(R.string.scan_qr_code);
                        textView.post(() -> textView.setText(defaultText));
                        addToDatabase.post(() -> addToDatabase.setVisibility(View.INVISIBLE));
                    }
                    catch(IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        barcodeDetector.release();
    }

    private Bitmap textToImageEncode(String value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(value,
                    BarcodeFormat.QR_CODE, QRCodeWidth, QRCodeWidth, null);
        } catch (IllegalArgumentException e) {
            return null;
        }

        int bitMatrixWidth = bitMatrix.getWidth();
        int bitMatrixHeight = bitMatrix.getHeight();
        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offSet = y * bitMatrixWidth;
            for (int x = 0; x < bitMatrixWidth; x++) {
                pixels[offSet + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.black) : getResources().getColor(R.color.white);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);
        bitmap.setPixels(pixels, 0, 500, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }

    private boolean addQRToARDatabase(String fileName, Bitmap qrCode)
    {
        SettingsViewModel settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
        File activeDatabase = new File(settingsViewModel.getActiveFolderAbsolutePath());
        File file = new File(activeDatabase, fileName + getString(R.string.jpeg_extension));
        try {
            FileOutputStream fos = new FileOutputStream(file, false);
            qrCode.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            return true;
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}