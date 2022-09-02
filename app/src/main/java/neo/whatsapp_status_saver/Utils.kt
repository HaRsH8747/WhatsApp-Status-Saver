package neo.whatsapp_status_saver

import neo.whatsapp_status_saver.model.IVModel

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