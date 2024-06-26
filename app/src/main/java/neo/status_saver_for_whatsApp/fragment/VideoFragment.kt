package neo.status_saver_for_whatsApp.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import neo.status_saver_for_whatsApp.AppPref
import neo.status_saver_for_whatsApp.Utils
import neo.status_saver_for_whatsApp.adapters.VideoAdapter
import neo.status_saver_for_whatsApp.model.IVModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import neo.status_saver_for_whatsApp.R
import neo.status_saver_for_whatsApp.databinding.FragmentVideoBinding
import java.io.File

class VideoFragment : Fragment() {

    private lateinit var binding: FragmentVideoBinding
    lateinit var files: Array<File>
    private lateinit var adapter: VideoAdapter
    private lateinit var appPref: AppPref
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var isReadPermissionGranted = false
    private var isWritePermissionGranted = false
    private lateinit var dialog: Dialog
    private lateinit var btnFolderPermission: Button
    private lateinit var job: Job

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoBinding.inflate(inflater, container, false)
        appPref = AppPref(requireContext())
        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.folder_permission_dialog)
        dialog.setCancelable(false)
        btnFolderPermission = dialog.findViewById<Button>(R.id.btnOk)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ permissions ->
//            isReadPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: isReadPermissionGranted
            if (permissions){
                isWritePermissionGranted = true
            }else{
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
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
                        val uri = Uri.fromParts("package", requireContext().packageName, null)
                        intent.data = uri
                        this.startActivity(intent)
                    }
                    snackbar.show()
                }
            }
//            isWritePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: isWritePermissionGranted
        }

        job = GlobalScope.launch(Dispatchers.IO){
            requestPermission()
        }

        binding.srlVideo.setOnRefreshListener {
//            val images = getData()
//            images.filter { it.uri.toString().endsWith(".jpg") }
            Log.d("CLEAR","Video job: ${job.isActive}")
            if (!job.isActive){
                job = GlobalScope.launch(Dispatchers.IO){
                    requestPermission()
                }
            }
//            requestPermission()
//            adapter.updateList(images)
            binding.srlVideo.isRefreshing = false
        }

        binding.btnRate.setOnClickListener {
            val str = "android.intent.action.VIEW"
            val sb2 = StringBuilder()
            sb2.append("http://play.google.com/store/apps/details?id=")
            sb2.append(requireContext().packageName)
            requireContext().startActivity(Intent(str, Uri.parse(sb2.toString())))
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
//        requestPermission()
    }

    override fun onPause() {
        super.onPause()
        if (job.isActive){
            job.cancel()
            Log.d("CLEAR","Video job canceled")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun requestPermission(){
//        val isReadPermission = ContextCompat.checkSelfPermission(
//            requireContext(),
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        ) == PackageManager.PERMISSION_DENIED

        val isWritePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

//        isReadPermissionGranted = isReadPermission
        isWritePermissionGranted = isWritePermission || sdkCheck()
        if (isWritePermissionGranted){
            withContext(Dispatchers.IO){
                fetchStatus()
            }
            return
        }

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
//        else{
//            fetchStatus()
//        }
    }

    private fun sdkCheck(): Boolean{
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    private suspend fun fetchStatus(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            val result = readDataFromPrefs()
            if (result){
                val uriPath = appPref.getString(AppPref.PATH)
                requireContext().contentResolver.takePersistableUriPermission(Uri.parse(uriPath), Intent.FLAG_GRANT_READ_URI_PERMISSION)

                Utils.videoList.clear()
                if (uriPath != null){
                    val fileDoc = DocumentFile.fromTreeUri(requireContext().applicationContext, Uri.parse(uriPath))
                    Utils.videoList.clear()
                    for (file: DocumentFile in fileDoc!!.listFiles()){
                        if (!file.name!!.endsWith(".nomedia")){
                            val ivModel = IVModel("", file.name!!,file.uri,file.lastModified())
                            Utils.videoList.add(ivModel)
                        }
                    }
                    Utils.videoList = Utils.videoList.filter {
                        it.uri.toString().endsWith(".mp4")
                    }.toMutableList()
                    Utils.videoList.sortByDescending { it.lastModified }
                    if (Utils.videoList.size == 0){
                        withContext(Dispatchers.Main){
                            binding.tvEmptyVideo.visibility = View.VISIBLE
                        }
                    }else{
                        withContext(Dispatchers.Main){
                            binding.tvEmptyVideo.visibility = View.INVISIBLE
                        }
                        setUpLayout()
                    }
                }
            }
            else{
//                getFolderPermission()
                openDirectory()
            }
//            getStatusAccess()
        }else{
            Utils.videoList.clear()
            Utils.videoList = getData()
            Utils.videoList = Utils.videoList.filter {
                it.uri.toString().endsWith(".mp4")
            }.toMutableList()
            if (Utils.videoList.size == 0){
                withContext(Dispatchers.Main){
                    binding.tvEmptyVideo.visibility = View.VISIBLE
                }
            }else{
                withContext(Dispatchers.Main){
                    binding.tvEmptyVideo.visibility = View.INVISIBLE
                }
                setUpLayout()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        dialog.dismiss()
        if (resultCode == AppCompatActivity.RESULT_OK && resultCode == 1234){
            val treeUri = data?.data
            Log.d("CLEAR","path: ${treeUri.toString()}")
            appPref.setString(AppPref.PATH,treeUri.toString())
            if (treeUri != null){
                requireContext().contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val fileDoc = DocumentFile.fromTreeUri(requireContext().applicationContext, treeUri)
                Utils.videoList.clear()
                for (file: DocumentFile in fileDoc!!.listFiles()){
                    if (!file.name!!.endsWith(".nomedia")){
                        val ivModel = IVModel("", file.name!!,file.uri,file.lastModified())
                        Utils.videoList.add(ivModel)
                    }
                }
                if (Utils.videoList.size == 0){
                    lifecycleScope.launch {
                        binding.tvEmptyVideo.visibility = View.VISIBLE
                    }
                }else{
                    lifecycleScope.launch {
                        binding.tvEmptyVideo.visibility = View.INVISIBLE
                        setUpLayout()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun openDirectory() {
        withContext(Dispatchers.Main){
            dialog.show()
        }
        val path = Environment.getExternalStorageDirectory()
            .toString() + "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
        val file = File(path)
        var secondDir: String
        val finalDirPath: String
        val startDir: String = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
//        if (file.exists()) {
//        }
        val sm = requireContext().getSystemService(Context.STORAGE_SERVICE) as StorageManager?
        val intent = sm!!.primaryStorageVolume.createOpenDocumentTreeIntent()
        var uri = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI")
        var scheme = uri.toString()
        Log.d("TAG", "INITIAL_URI scheme: $scheme")
        scheme = scheme.replace("/root/", "/document/")
        finalDirPath = "$scheme%3A$startDir"
        uri = Uri.parse(finalDirPath)
        intent.putExtra("android.provider.extra.INITIAL_URI", uri)
        Log.d("TAG", "uri: $uri")
        try {
            btnFolderPermission.setOnClickListener {
                startActivityForResult(intent,1234)
            }
        } catch (ignored: ActivityNotFoundException) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getFolderPermission() {
        dialog.show()
        val storageManager = requireContext().getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
        val targetDirectory = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
        var uri = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI") as Uri
        var scheme = uri.toString()
        scheme = scheme.replace("/root/","/tree/")
        scheme += "%3A$targetDirectory"
        uri = Uri.parse(scheme)
        intent.putExtra("android.provider.extra.INITIAL_URI",uri)
        intent.putExtra("android.content.extra.SHOW_ADVANCED",true)
        btnFolderPermission.setOnClickListener {
            startActivityForResult(intent,1234)
        }
    }

    private fun readDataFromPrefs(): Boolean{
        val uriPath = appPref.getString(AppPref.PATH)
        val targetDirectory = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
        if (uriPath != null){
            if (uriPath.isEmpty() || !uriPath.contains(targetDirectory)){
                return false
            }
        }
        return true
    }

    private fun getData(): MutableList<IVModel> {
        val targetpath =
            Environment.getExternalStorageDirectory().absolutePath + Utils.FOLDER_NAME + "Media/.Statuses"
        val targerdir = File(targetpath)
        return if (targerdir.listFiles() == null){
            emptyList<IVModel>().toMutableList()
        }else{
            files = targerdir.listFiles()
            Utils.videoList.clear()
            for (i in files.indices) {
                val file: File = files[i]
                val ivModel = IVModel(files[i].absolutePath,file.name,Uri.fromFile(file),file.lastModified())
                if (!ivModel.uri.toString().endsWith(".nomedia")) {
                    Utils.videoList.add(ivModel)
                }
            }
            Utils.videoList.sortByDescending { it.lastModified }

            Utils.videoList
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun setUpLayout() {
        withContext(Dispatchers.Main){
            binding.rvVideo.setHasFixedSize(true)
            val staggeredGridLayoutManager =
                StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
            binding.rvVideo.layoutManager = staggeredGridLayoutManager
            val videoList: MutableList<IVModel> = mutableListOf()
            videoList.addAll(Utils.videoList)
            adapter = VideoAdapter(requireContext(), videoList)
            binding.rvVideo.adapter = adapter
            Log.d("CLEAR","image size: ${Utils.videoList.size}")
            if (job.isActive){
                adapter.notifyDataSetChanged()
            }
        }
    }
}