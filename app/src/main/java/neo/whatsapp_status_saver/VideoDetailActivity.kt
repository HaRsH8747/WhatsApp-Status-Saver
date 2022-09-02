package neo.whatsapp_status_saver

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neo.whatsapp_status_saver.databinding.ActivityVideoDetailBinding
import java.io.*
import java.util.regex.Pattern


class VideoDetailActivity : AppCompatActivity() {

    private lateinit var appPref: AppPref
    private lateinit var binding: ActivityVideoDetailBinding
    private var favToggle: Boolean = false
    private var file: String? = ""
    private lateinit var destpath2: File
    private var uri: String? = ""
    private var fileSaveName = ""
    private lateinit var progressDialog: Dialog
    private var isWritePermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_WhatsApp_Status_Saver)
        binding = ActivityVideoDetailBinding.inflate(layoutInflater)
//        supportActionBar!!.title = "Video"
        setContentView(binding.root)
        appPref = AppPref(this)
        progressDialog = Dialog(this)
        progressDialog.setContentView(R.layout.progress_circular)
        progressDialog.setCancelable(false)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        isWritePermissionGranted = isWritePermission || sdkCheck()
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ permissions ->
//            isReadPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: isReadPermissionGranted
            if (permissions){
                isWritePermissionGranted = true
            }else{
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                    requestPermission()
                } else {
                    //display error dialog
                    val snackbar = Snackbar.make(binding.root,
                        "Storage Permission is required to store Image to the gallery",
                        Snackbar.LENGTH_LONG)
                    snackbar.setAction("Permission Snackbar") {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", this.packageName, null)
                        intent.data = uri
                        this.startActivity(intent)
                    }
                    snackbar.show()
                }
            }
//            isWritePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: isWritePermissionGranted
        }

        val intent = intent
        val destpath = intent.getStringExtra("DEST_PATH_VIDEO")
        file = intent.getStringExtra("FILE_VIDEO")
        uri = intent.getStringExtra("URI_VIDEO")
        val filename = intent.getStringExtra("FILENAME_VIDEO")

        destpath2 = File(destpath!!)
        val file1 = File(file!!)

//        mparticularvideo = findViewById<VideoView>(R.id.particularvideo)
//        share = findViewById<ImageView>(R.id.share)
//        download = findViewById<ImageView>(R.id.download)
//        ssmychatapp = findViewById<ImageView>(R.id.ssmychatapp)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        if (checkFavourite(filename!!)){
            favToggle = true
            binding.heart.setImageResource(R.drawable.filled_fav)
        }else{
            favToggle = false
            binding.heart.setImageResource(R.drawable.unfilled_fav)
        }

        binding.heart.setOnClickListener {
            favToggle = !favToggle
            val currentFileList = appPref.getString(AppPref.FAVOURITE_ITEMS).toString()
            val pattern = Pattern.compile(":")
            val items = pattern.split(currentFileList)
            var newFav = ""
            if (checkFavourite(filename)){
                for (item in items){
                    if (item != filename){
                        newFav = "$newFav$item:"
                    }
                }
            }else{
                newFav = "$currentFileList$filename:"
//                for (item in items){
//                    if (item == filename){
//                    }
//                }
            }
            appPref.setString(AppPref.FAVOURITE_ITEMS,newFav)
            if (favToggle){
                val favImage = Utils.filesList.filter { it.fileName == filename }
                Utils.favouriteList.add(favImage[0])
                binding.heart.setImageResource(R.drawable.filled_fav)
            }else{
                val favImage = Utils.filesList.filter { it.fileName == filename }
                Utils.favouriteList.remove(favImage[0])
                binding.heart.setImageResource(R.drawable.unfilled_fav)
            }
        }

        binding.share.setOnClickListener {
            lifecycleScope.launch {
                progressDialog.show()
                saveVideoToInternalStorage()
            }
        }


        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.particularvideo)
        val uri1 = Uri.parse(uri)
        binding.particularvideo.setMediaController(mediaController)
        binding.particularvideo.setVideoURI(uri1)
        binding.particularvideo.requestFocus()
        binding.particularvideo.start()

        //Glide.with(getApplicationContext()).load(uri).into(mparticularimage);

        //Glide.with(getApplicationContext()).load(uri).into(mparticularimage);
        binding.download.setOnClickListener {
            if (sdkCheck()) {
                saveVideoToExternalStorage()
            } else {
                if (!isWritePermissionGranted) {
                    Log.d("CLEAR", "No Write")
                    requestPermission()
                } else {
                    Log.d("CLEAR", "Is Write")
                    saveVideoToExternalStorage()
                }
            }
//            try {
//                FileUtils.copyFileToDirectory(File(uri), destpath2)
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//            MediaScannerConnection.scanFile(applicationContext,
//                arrayOf(destpath + filename),
//                arrayOf("*/*"),
//                object : MediaScannerConnectionClient {
//                    override fun onMediaScannerConnected() {}
//                    override fun onScanCompleted(path: String, uri: Uri) {}
//                })
//            val dialog = Dialog(this)
//            dialog.setContentView(R.layout.custom_dialog)
//            dialog.show()
//            val button = dialog.findViewById<Button>(R.id.okbutton)
//            button.setOnClickListener {
//                val intent1 = Intent(this@Video, MainActivity::class.java)
//                startActivity(intent1)
//                finish()
//            }
//            Toast.makeText(this,"Downloaded Successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermission(){
//        val isReadPermission = ContextCompat.checkSelfPermission(
//            requireContext(),
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        ) == PackageManager.PERMISSION_DENIED

        val isWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

//        isReadPermissionGranted = isReadPermission
        isWritePermissionGranted = isWritePermission || sdkCheck()

        val permissionRequest = mutableListOf<String>()
        if (!isWritePermissionGranted){
            permissionRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
//        if (!isReadPermissionGranted){
//            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
//        }
        if (permissionRequest.isNotEmpty()){
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun sdkCheck(): Boolean{
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    private fun saveVideoToExternalStorage() {
//        try {
//            val newfile: File
//            val videoAsset = contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")
//            val `in`: FileInputStream = videoAsset!!.createInputStream()
//            val filepath: File = filesDir
//            val dir = File(filepath.absolutePath)
//            if (!dir.exists()) {
//                dir.mkdirs()
//            }
//            newfile = File(dir, "${System.currentTimeMillis()}.mp4")
//            if (newfile.exists()) newfile.delete()
//            val out: OutputStream = FileOutputStream(newfile)
//
//            // Copy the bits from instream to outstream
//            val buf = ByteArray(1024)
//            var len: Int
//            while (`in`.read(buf).also { len = it } > 0) {
//                out.write(buf, 0, len)
//            }
//            `in`.close()
//            out.close()
//            Log.v("", "Copy file successful.")
//        } catch (e: Exception) {
//            progressDialog.dismiss()
//            e.printStackTrace()
//        }
        val filename = "${System.currentTimeMillis()}.mp4"
        var fos: OutputStream? = null
        try {
            val videoAsset = contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")
            val inputStream: FileInputStream = videoAsset!!.createInputStream()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver?.also { resolver ->
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/*")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val imageUri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }
            fos?.use { out ->
                inputStream.use {
                    val buf = ByteArray(1024)
                    var len: Int
                    while (inputStream.read(buf).also { len = it } > 0) {
                        out.write(buf, 0, len)
                    }
                }
                Toast.makeText(this , "Saved to Downloads" , Toast.LENGTH_SHORT).show()
            }
        }catch (e: IOException){
            e.printStackTrace()
            Toast.makeText(this , "Download Failed" , Toast.LENGTH_SHORT).show()
        }
    }


    override fun onResume() {
        super.onResume()
        if (fileSaveName.isNotEmpty()){
            deletePhotoFromInternalStorage()
        }
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            deletePhotoFromInternalStorage()
        }
    }

    private fun deletePhotoFromInternalStorage(): Boolean {
        return try {
            val dir: File = filesDir
            val file = File(dir, fileSaveName)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun loadVideoFromInternalStorage(){
        try {
            withContext(Dispatchers.IO){
                val files = filesDir.listFiles()
                files?.filter { it.name.equals(fileSaveName) }?.map {
                    val photoUri = FileProvider.getUriForFile(this@VideoDetailActivity, "${BuildConfig.APPLICATION_ID}.provider",it)
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "video/*"
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    intent.putExtra(Intent.EXTRA_STREAM, photoUri)
                    progressDialog.dismiss()
                    resultLauncher.launch(Intent.createChooser(intent,"Share Video"))
                }
            }
        }catch (e: Exception){
            progressDialog.dismiss()
            e.printStackTrace()
        }
    }

    private suspend fun saveVideoToInternalStorage(){
        try {
            val newfile: File
            val videoAsset = contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")
            val `in`: FileInputStream = videoAsset!!.createInputStream()
            val filepath: File = filesDir
            val dir = File(filepath.absolutePath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            fileSaveName = "video${System.currentTimeMillis()}.mp4"
            newfile = File(dir, fileSaveName)
            if (newfile.exists()) newfile.delete()
            val out: OutputStream = FileOutputStream(newfile)

            // Copy the bits from instream to outstream
            val buf = ByteArray(1024)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            `in`.close()
            out.close()
            loadVideoFromInternalStorage()
            Log.v("", "Copy file successful.")
        } catch (e: Exception) {
            progressDialog.dismiss()
            e.printStackTrace()
        }
    }

    private fun checkFavourite(fileName: String): Boolean{
        val currentFileList = appPref.getString(AppPref.FAVOURITE_ITEMS).toString()
        val pattern = Pattern.compile(":")
        val items = pattern.split(currentFileList)
        if (items.isNotEmpty()){
            for (item in items){
                if (item == fileName){
                    return true
                }
            }
        }else{
            return false
        }
        return false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}