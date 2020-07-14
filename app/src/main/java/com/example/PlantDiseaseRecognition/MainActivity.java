package com.example.PlantDiseaseRecognition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.LocaleDisplayNames;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


import static android.os.Environment.getExternalStoragePublicDirectory;
import com.github.clans.fab.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements ExampleBottomSheetDialog.BottomSheetListener{
    int checkCam = 0;//0 = nothing, 1 = camera, 2 = storage
    String pathToFile=null;
    EditText imgPath;
    ImageView imageView;
    final int SELECT_IMAGES = 1;
    String selectedImagePath;
    String currentImagePath = "";
    //ArrayList<String> selectedImagesPaths = new ArrayList<>(); // Paths of the image(s) selected by the user.
    boolean imagesSelected = false;
    private static final Pattern IP_ADDRESS
            = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");
    private static final boolean DEBUG = false;
    public static final String DOCUMENTS_DIR = "documents";
    LinearLayout result_box;
    TextView responseText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT>=23){
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        responseText = findViewById(R.id.responseText);
        result_box = findViewById(R.id.result_box);
        result_box.setVisibility(View.GONE);
        imgPath = findViewById(R.id.imgPath);
        imgPath.setCursorVisible(false);
        imgPath.setFocusable(false);
        imgPath.setFocusableInTouchMode(false);
        imageView  = findViewById(R.id.image);

        Button buttonOpenBottomSheet = findViewById(R.id.button_open_bottom_sheet);
        buttonOpenBottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExampleBottomSheetDialog bottomSheet = new ExampleBottomSheetDialog();
                bottomSheet.show(getSupportFragmentManager(), "exampleBottomSheet");
            }
        });


    }

    @Override
    public void onButtonClicked(int img_src) {
        Log.d("Button","eeeeeeee");
        if(img_src == 1){ // camera
            dispatchPictureTakerAction();
        }
        if(img_src == 2){ // storage
            selectImage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(getApplicationContext(), "Access to Storage Permission Granted. Thanks.", Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(getApplicationContext(), "Access to Storage Permission Denied.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(getApplicationContext(), "Access to Internet Permission Granted. Thanks.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Access to Internet Permission Denied.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public void selectImage() {
        checkCam = 2;
        Intent intent = new Intent();
        intent.setType("*/*");
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        Log.d("check","Select img");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGES);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            try {
                if (requestCode == SELECT_IMAGES  && null != data) {
                    if (data.getData() != null) {
                        // data.getData() != null means a single image is selected.
                        Uri uri = data.getData();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            currentImagePath = getPath(getApplicationContext(), uri);
                        }
                        Log.d("ImageDetails", "Single Image URI : " + uri);
                        Log.d("ImageDetails", "Single Image Path : " + currentImagePath);
                        //selectedImagesPaths.add(currentImagePath);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            selectedImagePath = getPath(getApplicationContext(), uri);
                        }

                        imgPath.setText(selectedImagePath.substring(selectedImagePath.lastIndexOf("/")+1));
                        imagesSelected = true;

                    } else {
                        // When multiple images are selected.
                        // Thanks tp Laith Mihyar for this Stackoverflow answer : https://stackoverflow.com/a/34047251/5426539
                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                ClipData.Item item = clipData.getItemAt(i);
                                Uri uri = item.getUri();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    currentImagePath = getPath(getApplicationContext(), uri);
                                }
                                //selectedImagesPaths.add(currentImagePath);
                                Log.d("ImageDetails", "Image URI " + i + " = " + uri);
                                Log.d("ImageDetails", "Image Path " + i + " = " + currentImagePath);
                                imagesSelected = true;
                                //numSelectedImages.setText("Number of Selected Images : " + selectedImagesPaths.size());
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "You haven't Picked any Image.", Toast.LENGTH_LONG).show();
                }
                //selectedImagesPaths.add(pathToFile);
                Toast.makeText(getApplicationContext(), "已選擇圖片", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Something Went Wrong.", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            if(requestCode == 1){
                if(checkCam == 1) {
                    Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);
                    imageView.setImageBitmap(bitmap);
                    Log.d("CHECK", "By camera: "+ pathToFile);

                }
                if(checkCam == 2) {
                    Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath);
                    imageView.setImageBitmap(bitmap);
                    Log.d("CHECK", "By storage: "+ currentImagePath);
                }

            }
            responseText.setText("");
        }

        //super.onActivityResult(requestCode, resultCode, data);
    }

    public void dispatchPictureTakerAction() {
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePic.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
            photoFile = createPhotoFile();
            if(photoFile!=null) {
                pathToFile = photoFile.getAbsolutePath();
                Uri photoUri = FileProvider.getUriForFile(MainActivity.this, "com.example.myapplication", photoFile);
                takePic.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                Log.d("cccc",pathToFile);
                //selectedImagesPaths.add(pathToFile);
                currentImagePath = pathToFile;
                EditText imgPath = findViewById(R.id.imgPath);
                imgPath.setText(pathToFile.substring(pathToFile.lastIndexOf("/")+1));
                imagesSelected = true;
                checkCam = 1;
                startActivityForResult(takePic, 1);
            }
        }
    }

    private File createPhotoFile() {
        String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(name, ".jpg", storageDir);
        } catch (IOException e){
            Log.d("mylog", "Except : "+ e.toString());
        }
        return image;
    }


    public void connectServer(View v) {
        TextView responseText = findViewById(R.id.responseText);
        if (imagesSelected == false) { // This means no image is selected and thus nothing to upload.
            responseText.setText("尚未選擇圖片，請重試一次。");
            return;
        }
        responseText.setText("圖片上傳中...");
        String ipv4Address = "134.208.3.54";
        String portNumber = "7789";
        Matcher matcher = IP_ADDRESS.matcher(ipv4Address);
        if (!matcher.matches()) {
            responseText.setText("Invalid IPv4 Address. Please Check Your Inputs.");
            return;
        }

        String postUrl = "http://" + ipv4Address + ":" + portNumber + "/";

        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);


        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        String filePath = currentImagePath;
        String fileName = filePath.substring(filePath.lastIndexOf("/")+1);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            // Read BitMap by file path.
            Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath, options);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        }catch(Exception e){
            Log.e("EEEE",e.toString());
            responseText.setText("檔案格式錯誤，請確認檔案是否為圖片。");
            return;
        }
        byte[] byteArray = stream.toByteArray();

        multipartBodyBuilder.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("image/*"), byteArray));

        /*
        for (int i = 0; i < selectedImagesPaths.size(); i++) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            String filePath = selectedImagesPaths.get(i);
            String fileName = filePath.substring(filePath.lastIndexOf("/")+1);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                // Read BitMap by file path.
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImagesPaths.get(i), options);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            }catch(Exception e){
                responseText.setText("檔案格式錯誤，請確認檔案是否為圖片。");
                return;
            }
            byte[] byteArray = stream.toByteArray();

            multipartBodyBuilder.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("image/*"), byteArray));
        }*/

        RequestBody postBodyImage = multipartBodyBuilder.build();

        Log.d("test", postBodyImage.toString());
        postRequest(postUrl, postBodyImage);
    }

    void postRequest(String postUrl, RequestBody postBody) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                // Cancel the post on failure.
                call.cancel();
                Log.d("FAIL", e.getMessage());

                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.responseText);
                        responseText.setText("Failed to Connect to Server. Please Try Again.");
                    }
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, final okhttp3.Response response) throws IOException {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                //String str = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            String result = response.body().string();
                            //r[0]
                            String r[] = result.split(",");
                            /*
                             * r[0] 辨識結果:油胞病
                             * r[1] 黑點病: 0.0%
                             * r[2] 缺鎂:   0.0%
                             * r[3] 潛葉蛾: 0.1609%
                             * r[4] 油胞病: 99.8391%
                             * r[5] 健康:   0.0%
                             * */
                            result_box.setVisibility(View.VISIBLE);
                            String s1[] = r[1].split(":");
                            String s2[] = r[2].split(":");
                            String s3[] = r[3].split(":");
                            String s4[] = r[4].split(":");
                            String s5[] = r[5].split(":");
                            TextView pred_res = findViewById(R.id.pred_res);
                            TextView black = findViewById(R.id.black);
                            TextView black_res = findViewById(R.id.black_v);
                            TextView mg = findViewById(R.id.mg);
                            TextView mg_res = findViewById(R.id.mg_v);
                            TextView moth = findViewById(R.id.moth);
                            TextView moth_res = findViewById(R.id.moth_v);
                            TextView oil = findViewById(R.id.oil);
                            TextView oil_res = findViewById(R.id.oil_v);
                            TextView heal = findViewById(R.id.health);
                            TextView heal_res = findViewById(R.id.health_v);
                            responseText.setText("上傳成功！");
                            pred_res.setText(r[0]+"\n");
                            black.setText("     " + s1[0] + ":");
                            black_res.setText(s1[1]);
                            mg.setText("     " + s2[0] + ":");
                            mg_res.setText(s2[1]);
                            moth.setText("     " + s3[0] + ":");
                            moth_res.setText(s3[1]);
                            oil.setText("     " + s4[0] + ":");
                            oil_res.setText(s4[1]);
                            heal.setText("     " + s5[0] + ":");
                            heal_res.setText(s5[1]);


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {
        Log.d("CHECK", "Original URI:"+uri.toString());
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {

                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Log.d("DEBUG",type);
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                Log.d("CHECK1", "in download!!!!!");
                final String id = DocumentsContract.getDocumentId(uri);
                Log.d("CHECK id", id);
                if (id != null && id.startsWith("raw:")) {
                    return id.substring(4);
                }

                String[] contentUriPrefixesToTry = new String[]{
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads",
                        "content://downloads/all_downloads"
                };

                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                    String path = "";
                    Log.d("eeeee","cUri:"+ contentUri);
                    try {
                        path = getDataColumn(context, contentUri, null, null);
                        if (path != null) {
                            Log.d("eeeee","path:"+ path);
                            return path;
                        }
                    } catch (Exception e) {

                    }
                }

                String fileName = getFileName(context, uri);
                File cacheDir = getDocumentCacheDir(context);
                File file = generateFileName(fileName, cacheDir);
                String destinationPath = null;
                if (file != null) {
                    destinationPath = file.getAbsolutePath();
                    saveFileFromUri(context, uri, destinationPath);
                }

                return destinationPath;


                /*
                Log.d("CHECK1", "in download");
                final String id = DocumentsContract.getDocumentId(uri);


                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                Log.d("Check2", "Change URI:"+contentUri);
                return getDataColumn(context, uri, null, null);
                /*
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }*/
                /*
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);*/
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Log.d("CHECK1", "storage");
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static String getFileName(@NonNull Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        String filename = null;

        if (mimeType == null && context != null) {
            String path = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                path = getPath(context, uri);
            }
            if (path == null) {
                filename = getName(uri.toString());
            } else {
                File file = new File(path);
                filename = file.getName();
            }
        } else {
            Cursor returnCursor = context.getContentResolver().query(uri, null,
                    null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }
        }

        return filename;
    }

    public static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf('/');
        return filename.substring(index + 1);
    }

    public static File getDocumentCacheDir(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), DOCUMENTS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        logDir(context.getCacheDir());
        logDir(dir);

        return dir;
    }

    @Nullable
    public static File generateFileName(@Nullable String name, File directory) {
        if (name == null) {
            return null;
        }

        File file = new File(directory, name);

        if (file.exists()) {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);
            }

            int index = 0;

            while (file.exists()) {
                index++;
                name = fileName + '(' + index + ')' + extension;
                file = new File(directory, name);
            }
        }

        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            Log.w("eE", e);
            return null;
        }

        logDir(directory);

        return file;
    }

    private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            is.read(buf);
            do {
                bos.write(buf);
            } while (is.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void logDir(File dir) {
        if(!DEBUG) return;
        Log.d("", "Dir=" + dir);
        File[] files = dir.listFiles();
        for (File file : files) {
            Log.d("", "File=" + file.getPath());
        }
    }
}
