package neo.status_saver_for_whatsApp

import neo.status_saver_for_whatsApp.model.IVModel

class Utils {

    companion object{
        val FOLDER_NAME = "/WhatsApp/"
        val SAVE_FOLDER_NAME = "/WhatsappStatusSaver/"
//        var files = mutableListOf<File>()
        var filesList = mutableListOf<IVModel>()
        var imageList = mutableListOf<IVModel>()
        var videoList = mutableListOf<IVModel>()
        var favouriteList: MutableList<IVModel> = mutableListOf()
    }
}