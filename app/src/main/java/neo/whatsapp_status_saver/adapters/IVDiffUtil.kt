package neo.whatsapp_status_saver.adapters

import androidx.recyclerview.widget.DiffUtil
import neo.whatsapp_status_saver.model.IVModel

class IVDiffUtil(
    private val oldList: List<IVModel>,
    private val newList: List<IVModel>
): DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].fileName == newList[newItemPosition].fileName
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return when{
            oldList[oldItemPosition].path != newList[newItemPosition].path -> {
                false
            }
            oldList[oldItemPosition].fileName != newList[newItemPosition].fileName -> {
                false
            }
            oldList[oldItemPosition].uri != newList[newItemPosition].uri -> {
                false
            }
            else -> true
        }
    }
}