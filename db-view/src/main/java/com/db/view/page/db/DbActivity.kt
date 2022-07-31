package com.db.view.page.db

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.db.view.R
import com.db.view.base.BaseBindingActivity
import com.db.view.databinding.LayoutActivityDbBinding
import com.db.view.extfuns.visible
import com.db.view.model.DBFile
import com.db.view.page.table.TableActivity

class DbActivity : BaseBindingActivity<LayoutActivityDbBinding>() {
    companion object {
        private const val TAG = "DbActivity"
    }

    private val dbViewModel by viewModels<DBViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding.titleText.text = baseContext.applicationInfo.loadLabel(packageManager)
        val dbList = ArrayList<DBFile>()
        val adapter = DBAdapter(dbList) { pos, db ->
            Log.i(TAG, "click db:$db with pos: $pos")
            TableActivity.actionOpenDatabase(this, db.name)
        }
        val layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.adapter = adapter
        viewBinding.recyclerView.layoutManager = layoutManager
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                // When new item inserted by DiffUtil in adapter, we always scroll to the top to show the lasted db file to user.
                if (savedInstanceState == null) {
                    // We only scroll to the top when savedInstanceState is null.
                    // This can avoid scrolling to top every time when device rotates.
                    viewBinding.recyclerView.scrollToPosition(0)
                }
            }
        })
        dbViewModel.urlServer.observe(this) {
            viewBinding.appServer.text = it
        }
        dbViewModel.dbListLiveData.observe(this) { newDBList ->
            val diffResult = DiffUtil.calculateDiff(DBDiffCallback(dbList, newDBList))
            dbList.clear()
            dbList.addAll(newDBList)
            diffResult.dispatchUpdatesTo(adapter)
            val title = if (adapter.itemCount <= 1) {
                "${adapter.itemCount} ${getString(R.string.database_database_found)}"
            } else {
                "${adapter.itemCount} ${getString(R.string.database_databases_found)}"
            }
            viewBinding.titleText.text = title
            val hasDbFile = adapter.itemCount > 0
            viewBinding.noDbTextView.visible = !hasDbFile
            viewBinding.recyclerView.visible = hasDbFile
        }
        dbViewModel.progressLiveData.observe(this) {
            viewBinding.progressBar.visible = it
        }
    }

    override fun onResume() {
        super.onResume()
        if (dbViewModel.dbListLiveData.value == null) { // When there's no data on ui, we load and refresh db files.
            dbViewModel.loadAndRefreshDBFiles()
        } else { // Otherwise, we only refresh db files to show the latest data.
            dbViewModel.refreshDBFiles()
        }
    }
}
