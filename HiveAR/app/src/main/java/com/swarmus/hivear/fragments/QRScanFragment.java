package com.swarmus.hivear.fragments;

import android.Manifest;
import android.content.Context;
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

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.swarmus.hivear.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class QRScanFragment extends Fragment {
    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private TextView textView;
    private BarcodeDetector barcodeDetector;
    private Button addToDatabase;
    private Button download;
    Bitmap bitmap;
    JSONObject qrJsonInfo;

    private final static float QRCodeWorldWidth = 0.1f; // m
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

        surfaceView = view.findViewById(R.id.camera);
        textView = view.findViewById(R.id.scan_qr_code_text);
        EditText robotNameET = view.findViewById(R.id.input_name);
        EditText robotUidET = view.findViewById(R.id.input_uid);
        EditText robotDescriptionET = view.findViewById(R.id.input_description);
        addToDatabase = view.findViewById(R.id.add_qr_to_database);
        addToDatabase.setVisibility(View.INVISIBLE);
        addToDatabase.setOnClickListener(v -> {
            try{
                bitmap = textToImageEncode(qrJsonInfo.toString());
                String robotName = qrJsonInfo.getString(JSON_ROBOT_NAME);
                int robotUID = qrJsonInfo.getInt(JSON_ROBOT_UID);
                String fileTitle = robotName + "-" + robotUID;
                // Add to data base instead of save to device
                addQRToARDatabase(fileTitle, bitmap);

            }catch (WriterException | JSONException e){
                e.printStackTrace();
            }

        });

        download = view.findViewById(R.id.download);
        download.setOnClickListener(v -> {
            try{
                JSONObject robotJsonDescription = new JSONObject();
                robotJsonDescription.accumulate(JSON_ROBOT_NAME, robotNameET.getText());
                robotJsonDescription.accumulate(JSON_ROBOT_UID, robotUidET.getText());
                robotJsonDescription.accumulate(JSON_ROBOT_DESCRIPTION, robotDescriptionET.getText());
                bitmap = textToImageEncode(robotJsonDescription.toString());
                String fileTitle = robotNameET.getText() + "-" + robotUidET.getText();
                MediaStore.Images.Media.insertImage(requireActivity().getContentResolver(), bitmap, fileTitle
                        , null);

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
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

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
                        // Set filter for robot
                        // Save to gallery / database
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        textView.post(() -> textView.setText(getString(R.string.scan_qr_code)));
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
        try {
            AugmentedImageDatabase augmentedImageDatabase;
            Session arSession = new Session(requireContext());
            Config config = new Config(arSession);
            try (FileInputStream inputStream = requireContext().openFileInput(getString(R.string.tags_db))) {
                augmentedImageDatabase = AugmentedImageDatabase.deserialize(arSession, inputStream);
                config.setAugmentedImageDatabase(augmentedImageDatabase);
                arSession.configure(config);
            }
            catch (IOException e) {
                e.printStackTrace();
                augmentedImageDatabase = new AugmentedImageDatabase(arSession);
            }
            // Verify if not already added
            // Not possible to see if in database right now
            for(AugmentedImage ai : arSession.getAllTrackables(AugmentedImage.class)){
                if (ai.getName().equals(fileName)) {
                    Toast.makeText(requireContext(), "Already in database", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
            augmentedImageDatabase.addImage(fileName, qrCode, QRCodeWorldWidth);

            try (FileOutputStream fos = requireContext().openFileOutput(
                    getString(R.string.tags_db), Context.MODE_PRIVATE)) {
                augmentedImageDatabase.serialize(fos);
                Toast.makeText(requireContext(), "Added to database", Toast.LENGTH_LONG).show();
                return true;
            }
        } catch (IOException | UnavailableApkTooOldException | UnavailableDeviceNotCompatibleException | UnavailableArcoreNotInstalledException | UnavailableSdkTooOldException e) {
            e.printStackTrace();
        }
        return false;
    }
}