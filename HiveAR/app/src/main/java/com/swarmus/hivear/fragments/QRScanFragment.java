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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.swarmus.hivear.R;

import java.io.IOException;

public class QRScanFragment extends Fragment {
    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private TextView textView;
    private EditText editText;
    private BarcodeDetector barcodeDetector;
    private Button addToDatabase;
    private Button download;
    private final static int QRCodeWidth = 500;
    Bitmap bitmap;
    String qrText;

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
        editText = view.findViewById(R.id.input_text);
        addToDatabase = view.findViewById(R.id.add_qr_to_database);
        addToDatabase.setVisibility(View.INVISIBLE);
        addToDatabase.setOnClickListener(v -> {
            try{
                bitmap = textToImageEncode(qrText);
                MediaStore.Images.Media.insertImage(requireActivity().getContentResolver(), bitmap, "code_scanner"
                        , null);

            }catch (WriterException e){
                e.printStackTrace();
            }

        });

        download = view.findViewById(R.id.download);
        download.setOnClickListener(v -> {
            try{
                bitmap = textToImageEncode(editText.getText().toString());
                MediaStore.Images.Media.insertImage(requireActivity().getContentResolver(), bitmap, "code_scanner"
                        , null);

            }catch (WriterException e){
                e.printStackTrace();
            }

        });

        barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(getContext(), barcodeDetector)
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
                    qrText = qrcode.valueAt(0).displayValue;
                    textView.post(() -> textView.setText(qrText));
                    addToDatabase.setVisibility(View.VISIBLE);
                    // Set filter for robot
                    // Save to gallery / database
                }
                else {
                    textView.post(() -> textView.setText(getString(R.string.scan_qr_code)));
                    addToDatabase.setVisibility(View.INVISIBLE);
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
}