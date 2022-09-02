package neo.whatsapp_status_saver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import neo.whatsapp_status_saver.fragment.ViewPagerAdapter
import com.google.android.material.snackbar.Snackbar
import neo.whatsapp_status_saver.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE = 999
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var isReadPermissionGranted = false
    private lateinit var appPref: AppPref

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_WhatsApp_Status_Saver)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.menu.getItem(2).isEnabled = false
        binding.viewpager2.adapter = ViewPagerAdapter(supportFragmentManager,lifecycle)
//        binding.viewpager2.currentItem = 2
        binding.viewpager2.isUserInputEnabled = false
        appPref = AppPref(this)
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ permissions ->
//            isReadPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: isReadPermissionGranted
            if (permissions){
                isReadPermissionGranted = true
            }else{
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
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
//        checkPermission()
        requestPermission()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//            val result = readDataFromPrefs()
//            if (result){
//                val uriPath = appPref.getString(AppPref.PATH)
//                contentResolver.takePersistableUriPermission(Uri.parse(uriPath), Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//                if (uriPath != null){
//                    val fileDoc = DocumentFile.fromTreeUri(applicationContext, Uri.parse(uriPath))
//                    Utils.filesList.clear()
//                    for (file:DocumentFile in fileDoc!!.listFiles()){
//                        if (!file.name!!.endsWith(".nomedia")){
//                            val ivModel = IVModel("", file.name!!,file.uri)
//                            Utils.filesList.add(ivModel)
//                        }
//                    }
//                }
//            }else{
//                getFolderPermission()
//            }
////            getStatusAccess()
//        }
        binding.homeFab.setOnClickListener {
            binding.viewpager2.currentItem = 0
            binding.bottomNavigationView.selectedItemId = R.id.placeholder
        }


        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.image -> {
                    binding.viewpager2.currentItem = 1
                    return@setOnItemSelectedListener true
                }
                R.id.video -> {
                    binding.viewpager2.currentItem = 2
                    return@setOnItemSelectedListener true
                }
                R.id.heart -> {
                    binding.viewpager2.currentItem = 3
                    return@setOnItemSelectedListener true
                }
                R.id.share -> {
                    shareApp()
                    return@setOnItemSelectedListener true
                }
                else -> {
                    binding.viewpager2.currentItem = 0
                    return@setOnItemSelectedListener true
                }
            }
        }

//        binding.viewpager2.currentItem = 2
        binding.bottomNavigationView.selectedItemId = R.id.placeholder

    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == RESULT_OK){
//            val treeUri = data?.data
//            appPref.setString(AppPref.PATH,treeUri.toString())
//            if (treeUri != null){
//                contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                val fileDoc = DocumentFile.fromTreeUri(applicationContext, treeUri)
//                Utils.filesList.clear()
//                for (file:DocumentFile in fileDoc!!.listFiles()){
//                    if (!file.name!!.endsWith(".nomedia")){
//                        val ivModel = IVModel("", file.name!!,file.uri)
//                        Utils.filesList.add(ivModel)
//                    }
//                }
//            }
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun getFolderPermission() {
//        val storageManager = application.getSystemService(Context.STORAGE_SERVICE) as StorageManager
//        val intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
//        val targetDirectory = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
//        var uri = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI") as Uri
//        var scheme = uri.toString()
//        scheme = scheme.replace("/root/","/tree/")
//        scheme += "%3A$targetDirectory"
//        uri = Uri.parse(scheme)
//        intent.putExtra("android.provider.extra.INITIAL_URI",uri)
//        intent.putExtra("android.content.extra.SHOW_ADVANCED",true)
//        startActivityForResult(intent,1234)
//    }
//
//    private fun readDataFromPrefs(): Boolean{
//        val uriPath = appPref.getString(AppPref.PATH)
//        if (uriPath != null){
//            if (uriPath.isEmpty()){
//                return false
//            }
//        }
//        return true
//    }

    private fun shareApp() {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT,
            "Hey check out this amazing Whatsapp Status Saver: https://play.google.com/store/apps/details?id=$packageName")
        sendIntent.type = "text/plain"
        startActivity(sendIntent)
    }

    private fun checkPermission(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
            if (Build.VERSION.SDK_INT > 23) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    //main code
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE)
                }
            } else {
                Toast.makeText(applicationContext, "Already", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermission(){
//        val isReadPermission = ContextCompat.checkSelfPermission(
//            requireContext(),
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        ) == PackageManager.PERMISSION_DENIED

        val isWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

//        isReadPermissionGranted = isReadPermission
        isReadPermissionGranted = isWritePermission || sdkCheck()

        val permissionRequest = mutableListOf<String>()
        if (!isReadPermissionGranted){
            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
//        if (!isReadPermissionGranted){
//            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
//        }
        if (permissionRequest.isNotEmpty()){
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun sdkCheck(): Boolean{
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun getStatusAccess(){
//        // Choose a directory using the system's file picker.
//        // Choose a directory using the system's file picker.
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
//        // Optionally, specify a URI for the directory that should be opened in
//        // the system file picker when it loads.
//
//        // Optionally, specify a URI for the directory that should be opened in
//        // the system file picker when it loads.
//        val wa_status_uri =
//            Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia/document/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses")
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, wa_status_uri)
//        startActivityForResult(intent, 10001)
//    }

//    private fun loadFragment(fragment: Fragment) {
//        val transaction = supportFragmentManager.beginTransaction()
//        transaction.replace(R.id.container, fragment)
//        transaction.commit()
//    }
}