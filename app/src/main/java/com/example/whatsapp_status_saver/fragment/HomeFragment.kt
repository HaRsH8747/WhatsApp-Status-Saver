package com.example.whatsapp_status_saver.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
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
import androidx.core.graphics.PathUtils
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.whatsapp_status_saver.AppPref
import com.example.whatsapp_status_saver.R
import com.example.whatsapp_status_saver.Utils
import com.example.whatsapp_status_saver.adapters.HomeAdapter
import com.example.whatsapp_status_saver.databinding.FragmentHomeBinding
import com.example.whatsapp_status_saver.model.IVModel
import com.google.android.material.snackbar.Snackbar
import java.io.File

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    lateinit var files: Array<File>
    private lateinit var adapter: HomeAdapter
    private lateinit var appPref: AppPref
    private lateinit var dialog: Dialog
    private lateinit var btnFolderPermission: Button
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var isReadPermissionGranted = false
    private var isWritePermissionGranted = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHomeBinding.inflate(inflater,container,false)
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
                    snackbar.setAction("Permission Snackbar",
                        View.OnClickListener {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", requireContext().packageName, null)
                            intent.data = uri
                            this.startActivity(intent)
                        })
                    snackbar.show()
                }
            }
//            isWritePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: isWritePermissionGranted
        }

        requestPermission()
//        fetchStatus()

        binding.srlHome.setOnRefreshListener {
//            fetchStatus()
//            setUpLayout()
//            adapter.updateList(getData())
            requestPermission()
            binding.srlHome.isRefreshing = false
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
//        if (!sdkCheck()){
//            val isWritePermission = ContextCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            ) == PackageManager.PERMISSION_GRANTED
//        }
        fetchStatus()
    }

    private fun requestPermission(){
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


    private fun fetchStatus(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            val result = readDataFromPrefs()
            if (result){
                val uriPath = appPref.getString(AppPref.PATH)
                requireContext().contentResolver.takePersistableUriPermission(Uri.parse(uriPath), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d("CLEAR","result")
                Utils.filesList.clear()
                if (uriPath != null){
                    val fileDoc = DocumentFile.fromTreeUri(requireContext().applicationContext, Uri.parse(uriPath))
                    for (file: DocumentFile in fileDoc!!.listFiles()){
                        if (!file.name!!.endsWith(".nomedia")){
                            val ivModel = IVModel("", file.name!!,file.uri,file.lastModified())
                            Utils.filesList.add(ivModel)
                        }
                    }
                    Utils.filesList.sortByDescending { it.lastModified }
                    if (Utils.filesList.size == 0){
                        binding.tvEmptyHome.visibility = View.VISIBLE
                    }else{
                        binding.tvEmptyHome.visibility = View.INVISIBLE
                        setUpLayout()
                    }
                }
            }else{
                getFolderPermission()
            }
//            getStatusAccess()
        }else{
            Log.d("CLEAR","Q")
            Utils.filesList.clear()
            Utils.filesList = getData()
            if (Utils.filesList.size == 0){
                binding.tvEmptyHome.visibility = View.VISIBLE
            }else{
                binding.tvEmptyHome.visibility = View.INVISIBLE
                setUpLayout()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK && requestCode == 1234){
            dialog.dismiss()
            val treeUri = data?.data
            Log.d("CLEAR","path: ${treeUri.toString()}")
            appPref.setString(AppPref.PATH,treeUri.toString())
            if (treeUri != null){
                requireContext().contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val fileDoc = DocumentFile.fromTreeUri(requireContext().applicationContext, treeUri)
                Utils.filesList.clear()
                for (file:DocumentFile in fileDoc!!.listFiles()){
                    if (!file.name!!.endsWith(".nomedia")){
                        val ivModel = IVModel("", file.name!!,file.uri,file.lastModified())
                        Utils.filesList.add(ivModel)
                    }
                }
                if (Utils.filesList.size == 0){
                    binding.tvEmptyHome.visibility = View.VISIBLE
                }else{
                    binding.tvEmptyHome.visibility = View.INVISIBLE
                    setUpLayout()
                }
            }
        }
        dialog.dismiss()
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
            Utils.filesList.clear()
            for (i in files.indices) {
                val file: File = files[i]
                val ivModel = IVModel(files[i].absolutePath,file.name,Uri.fromFile(file),file.lastModified())
                if (!ivModel.uri.toString().endsWith(".nomedia")) {
                    Utils.filesList.add(ivModel)
                }
            }
            Utils.filesList.sortByDescending { it.lastModified }

            Utils.filesList
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setUpLayout() {
        binding.rvHome.setHasFixedSize(true)
        val staggeredGridLayoutManager =
            StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        binding.rvHome.layoutManager = staggeredGridLayoutManager
        adapter = HomeAdapter(requireContext(), Utils.filesList)
        binding.rvHome.adapter = adapter
        adapter.notifyDataSetChanged()
    }
}