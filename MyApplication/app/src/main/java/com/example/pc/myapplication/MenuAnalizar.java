package com.example.pc.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MenuAnalizar extends AppCompatActivity {

    //private final String[] tipoAnalisis= {"objeto", "texto", "codigo", "cara"};
    private int indiceAnalisis;

    //Elementos de la interfaz
    private ImageView imageView;
    private TextView textViewResultados;

    //Ruta y Uri del archivo de cada imagen
    private String infoImagen;
    private String currentPhotoPath;
    Uri file;

    //variables para el analisis de la imagen
    private Bitmap bitmap;          //Para pasar la imagen a un mapa de bits, y poder usar los algoritmos de analisis
    private FirebaseVisionImage imageAnalyzed;

    //variables para el analisis de texto
    private FirebaseVisionTextRecognizer textRecognizer;

    //variables para el analisis de objetos
    private FirebaseVisionLabelDetectorOptions optionsLabel;
    private FirebaseVisionLabelDetector detectorLabel;

    //variables para el analisis de codigos de barra
    private FirebaseVisionBarcodeDetectorOptions optionsBarcode;
    private FirebaseVisionBarcodeDetector detectorBarcode;

    //variables para el analisis de Caras
    private FirebaseVisionFaceDetectorOptions optionsFace;
    private FirebaseVisionFaceDetector detectorFace;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_analizar);
        if(Build.VERSION.SDK_INT >= 23){
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        //Antes de usar las funciones de firebase, debe ser inicializado
        FirebaseApp.initializeApp(this);
        imageView = (ImageView) findViewById(R.id.imageView);
        textViewResultados = (TextView) findViewById(R.id.textViewResultados);
        infoImagen = "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){

            if(requestCode == 100){
                Log.d("STATE", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+ currentPhotoPath);
                //File file = new File(currentPhotoPath);
                bitmap = BitmapFactory.decodeFile(currentPhotoPath);

                if(bitmap != null){
                    imageView.setImageBitmap(bitmap);
                    switch (indiceAnalisis){
                        case 0: analizarObjeto();
                            break;
                        case 1: analizarTexto();
                            break;
                        case 2: analizarCodigo();
                            break;
                        case 3: analizarCara();
                            break;
                    }
                }else{
                    Log.e("STATE","->>>>>>>>>>>>>>>>>>>>> imagen nula");
                }
            }

        }else{
            Toast.makeText(this, "Error al tomar la foto", Toast.LENGTH_LONG).show();
        }
    }

    private FirebaseVisionLabelDetectorOptions inicializarOpcionesLabelDetector(){
        return
                new FirebaseVisionLabelDetectorOptions.Builder()
                        .setConfidenceThreshold(0.8f)
                        .build();
    }

    // High-accuracy landmark detection and face classification
    private FirebaseVisionFaceDetectorOptions inicializarOpcionesFaceDetector(){
        return
                new FirebaseVisionFaceDetectorOptions.Builder().
                setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build();
    }

    private FirebaseVisionBarcodeDetectorOptions inicializarOpcionesBarcode(){
        return
                new FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(
                                FirebaseVisionBarcode.FORMAT_QR_CODE,
                                FirebaseVisionBarcode.FORMAT_AZTEC)
                        .build();
    }

    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
    }

    /**
     * Método que obtiene una imagen por medio de Inten.getExtras (la cual solo nos devuelve una foto de 250*150
     */
    private void tomarFoto(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = Uri.fromFile(getOutputMediaFile());
        currentPhotoPath = file.getPath();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
        startActivityForResult(intent, 100);
    }

    public void handlerBotonAnalizarObjeto(View view){
        this.indiceAnalisis = 0;
        tomarFoto();
    }

    public void handlerBotonAnalizarTexto(View view){
        this.indiceAnalisis = 1;
        tomarFoto();
    }

    public void handlerBotonAnalizarCodigo(View view){
        this.indiceAnalisis = 2;
        tomarFoto();
    }

    public void handlerBotonAnalizarCara(View view){
        this.indiceAnalisis = 3;
        tomarFoto();
    }

    private void analizarObjeto(){
        imageAnalyzed = FirebaseVisionImage.fromBitmap(bitmap);
        optionsLabel = inicializarOpcionesLabelDetector();
        detectorLabel = FirebaseVision.getInstance().getVisionLabelDetector();

        Task<List<FirebaseVisionLabel>> result =
                detectorLabel.detectInImage(imageAnalyzed)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionLabel>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionLabel> labels) {
                                        String mensajeObjetosResultado = "";
                                        for (FirebaseVisionLabel label: labels) {
                                            String text = label.getLabel();
                                            String entityId = label.getEntityId();
                                            float confidence = label.getConfidence();
                                            mensajeObjetosResultado += "-->> Text: "+text+", Confidence: "+confidence+"\n";
                                        }
                                        textViewResultados.setText(mensajeObjetosResultado);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
    }

    private void analizarTexto(){
        imageAnalyzed = FirebaseVisionImage.fromBitmap(bitmap);
        textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        textRecognizer.processImage(imageAnalyzed).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText result) {
                //Análisis de texto correcto
                String resultText = result.getText();
                for (FirebaseVisionText.TextBlock block: result.getTextBlocks()) {
                    String blockText = block.getText();
                    Float blockConfidence = block.getConfidence();
                    List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
                    Point[] blockCornerPoints = block.getCornerPoints();
                    Rect blockFrame = block.getBoundingBox();
                    for (FirebaseVisionText.Line line: block.getLines()) {
                        String lineText = line.getText();
                        Float lineConfidence = line.getConfidence();
                        List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                        Point[] lineCornerPoints = line.getCornerPoints();
                        Rect lineFrame = line.getBoundingBox();
                        for (FirebaseVisionText.Element element: line.getElements()) {
                            String elementText = element.getText();
                            Float elementConfidence = element.getConfidence();
                            List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                            Point[] elementCornerPoints = element.getCornerPoints();
                            Rect elementFrame = element.getBoundingBox();
                        }
                    }
                }
                Toast.makeText(MenuAnalizar.this, "Texto obtenido:\n"+resultText, Toast.LENGTH_LONG).show();
                textViewResultados.setText(resultText);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Error de Análisis de texto
            }
        });
    }

    private void analizarCodigo(){
        imageAnalyzed = FirebaseVisionImage.fromBitmap(bitmap);
        optionsBarcode = inicializarOpcionesBarcode();
        detectorBarcode = FirebaseVision.getInstance().getVisionBarcodeDetector();

        //obteniendo resultados
        Task<List<FirebaseVisionBarcode>> result = detectorBarcode.detectInImage(imageAnalyzed)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
                        for (FirebaseVisionBarcode barcode: barcodes) {
                            Rect bounds = barcode.getBoundingBox();
                            Point[] corners = barcode.getCornerPoints();

                            String rawValue = barcode.getRawValue();

                            int valueType = barcode.getValueType();
                            // See API reference for complete list of supported types
                            switch (valueType) {
                                case FirebaseVisionBarcode.TYPE_WIFI:
                                    String ssid = barcode.getWifi().getSsid();
                                    String password = barcode.getWifi().getPassword();
                                    int type = barcode.getWifi().getEncryptionType();
                                    textViewResultados.setText("Codigo Wifi: "+ type);
                                    break;
                                case FirebaseVisionBarcode.TYPE_URL:
                                    String title = barcode.getUrl().getTitle();
                                    String url = barcode.getUrl().getUrl();
                                    textViewResultados.setText("Codigo Url: "+ url);
                                    break;
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });
    }

    private void analizarCara(){
        imageAnalyzed = FirebaseVisionImage.fromBitmap(bitmap);
        optionsFace = inicializarOpcionesFaceDetector();
        detectorFace = FirebaseVision.getInstance().getVisionFaceDetector(optionsFace);

        //obteniendo resultados
        Task<List<FirebaseVisionFace>> result =
                detectorFace.detectInImage(imageAnalyzed)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        for (FirebaseVisionFace face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            // If classification was enabled:
                                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                                                float smileProb = face.getSmilingProbability();
                                                String smileProbabilityMessage = "Probabilidad: "+(smileProb*100)+"%";
                                                textViewResultados.setText(smileProbabilityMessage);
                                                Toast.makeText(MenuAnalizar.this, smileProbabilityMessage, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
    }


}
