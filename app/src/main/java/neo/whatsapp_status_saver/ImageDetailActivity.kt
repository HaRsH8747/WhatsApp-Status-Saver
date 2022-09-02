package neo.whatsapp_status_saver

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import neo.whatsapp_status_saver.model.IVModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neo.whatsapp_status_saver.databinding.ActivityImageDetailBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import java.util.regex.Pattern

class ImageDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDetailBinding
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this,
        R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this,
        R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this,
        R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this,
        R.anim.to_bottom_anim) }
    private var clicked = false
    private lateinit var currentPhoto: IVModel
    //    private var isReadPermissionGranted = false
    private var isWritePermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var shareFile: String = ""
    private lateinit var bitmap: Bitmap
    private lateinit var progressDialog: Dialog
    lateinit var destpath: String
    lateinit var file: String
    lateinit var uri: String
    lateinit var filename: String
    private lateinit var appPref: AppPref
    private var favToggle: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_WhatsApp_Status_Saver)
        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressDialog = Dialog(this)
        progressDialog.setContentView(R.layout.progress_circular)
        progressDialog.setCancelable(false)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        appPref = AppPref(this)
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
        destpath = intent.getStringExtra("DEST_PATH")!!
        file = intent.getStringExtra("FILE")!!
        uri = intent.getStringExtra("URI")!!
        filename = intent.getStringExtra("FILENAME")!!

        favToggle = if (checkFavourite(filename)){
            binding.fabFav.setImageResource(R.drawable.filled_fav)
            true
        }else{
            binding.fabFav.setImageResource(R.drawable.unfilled_fav)
            false
        }

        binding.fabMain.setOnClickListener {
            setVisibility()
            setAnimation()
            clicked = !clicked
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
            deletePhotoFromInternalStorage(shareFile)
        }

        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object: CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    bitmap = resource
//                    binding.lavImageLoading.pauseAnimation()
//                    binding.lavImageLoading.visibility = View.GONE
                    binding.ivWallpaper.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
//        Glide.with(applicationContext).load(uri).into(binding.ivWallpaper)

        binding.fabDownload.setOnClickListener {
            if(sdkCheck()){
                saveImageToExternalStorage(bitmap)
            }else{
                if (!isWritePermissionGranted){
                    Log.d("CLEAR","No Write")
                    requestPermission()
                }else{
                    Log.d("CLEAR","Is Write")
                    saveImageToExternalStorage(bitmap)
                }
            }
        }

        binding.fabFav.setOnClickListener {
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
//            val fileList = "$currentFileList$filename:"
            Log.d("CLEAR","favList: ${newFav}")
            appPref.setString(AppPref.FAVOURITE_ITEMS,newFav)
            if (favToggle){
                val favImage = Utils.filesList.filter { it.fileName == filename }
                Utils.favouriteList.add(favImage[0])
                binding.fabFav.setImageResource(R.drawable.filled_fav)
            }else{
                val favImage = Utils.filesList.filter { it.fileName == filename }
                Utils.favouriteList.remove(favImage[0])
                binding.fabFav.setImageResource(R.drawable.unfilled_fav)
            }
        }

        binding.fabShare.setOnClickListener {
            progressDialog.show()
            lifecycleScope.launch{
                shareImage()
            }
        }
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            deletePhotoFromInternalStorage(file)
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

    override fun onDestroy() {
        super.onDestroy()
        if (file.isNotEmpty()){
            try {
                deletePhotoFromInternalStorage(file)
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadPhotoFromInternalStorage(fileName: String){
        try {
            withContext(Dispatchers.IO){
                val files = filesDir.listFiles()
                files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") && it.name.equals("$fileName.jpg") }?.map {
                    val photoUri = FileProvider.getUriForFile(this@ImageDetailActivity, "${BuildConfig.APPLICATION_ID}.provider",it)
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "image/jpg"
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    intent.putExtra(Intent.EXTRA_STREAM, photoUri)
//                    intent.putExtra(Intent.EXTRA_TEXT,"Found some amazing Wallpapers from this platform\n\nYou can also give it a try by downloading it from here\nWallstick Application:\nhttps://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                    progressDialog.dismiss()
                    resultLauncher.launch(Intent.createChooser(intent,"Share Image"))
                    file = "$fileName.jpg"
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
            progressDialog.dismiss()
        }
    }


    private suspend fun savePhotoToInternalStorage(filename: String, bmp: Bitmap) {
        withContext(Dispatchers.IO){
            try {
                openFileOutput("$filename.jpg", MODE_PRIVATE).use { stream ->
                    if (!bmp.compress(Bitmap.CompressFormat.PNG,100,stream)){
                        progressDialog.dismiss()
                        throw IOException("Couldn't save Image")
                    }
                    loadPhotoFromInternalStorage(filename)
                }
            }catch (e: Exception){
                e.printStackTrace()
                progressDialog.dismiss()
                Log.d("CLEAR","msg: ${e.message}")
            }
        }
    }

    private suspend fun shareImage() {
        withContext(Dispatchers.IO){
            val date = Date()
            val format: String = DateFormat.format("MM-dd-yyyy_hh:mm:ss", date).toString()
            savePhotoToInternalStorage(format,bitmap)
        }
    }

    private fun deletePhotoFromInternalStorage(filename: String): Boolean {
        return try {
            val dir: File = filesDir
            val file = File(dir, filename)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
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

    private fun saveImageToExternalStorage(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver?.also { resolver ->
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                Toast.makeText(this , "Saved to Pictures" , Toast.LENGTH_SHORT).show()
            }
        }catch (e: IOException){
            e.printStackTrace()
            Toast.makeText(this , "Download Failed" , Toast.LENGTH_SHORT).show()
        }
    }

    private fun setVisibility() {
        if (!clicked){
            binding.fabFav.visibility = View.VISIBLE
            binding.fabShare.visibility = View.VISIBLE
            binding.fabDownload.visibility = View.VISIBLE
        }else{
            binding.fabFav.visibility = View.GONE
            binding.fabShare.visibility = View.GONE
            binding.fabDownload.visibility = View.GONE
        }
    }

    private fun setAnimation() {
        if (!clicked){
            binding.fabFav.startAnimation(fromBottom)
            binding.fabShare.startAnimation(fromBottom)
            binding.fabDownload.startAnimation(fromBottom)
            binding.fabMain.startAnimation(rotateOpen)
        }else{
            binding.fabFav.startAnimation(toBottom)
            binding.fabShare.startAnimation(toBottom)
            binding.fabDownload.startAnimation(toBottom)
            binding.fabMain.startAnimation(rotateClose)
        }
    }
}