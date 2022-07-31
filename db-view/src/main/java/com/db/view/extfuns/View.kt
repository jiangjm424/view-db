package com.db.view.extfuns

import android.view.View
import com.db.view.widget.TableCellView

/**
 * View extension methods.
 * @author guolin
 * @since 2021/5/29
 */

/**
 * Register a callback to be invoked when this view is double clicked. If this view is not
 * clickable, it becomes clickable.
 *
 * @param listener The callback that will run
 */
fun TableCellView.setOnDoubleClickListener(listener: View.OnClickListener) {
    setOnClickListener {
        val clickTimeStamp = System.currentTimeMillis()
        if (clickTimeStamp - firstClickTimeStamp <= 300) {
            // This triggers double click event.
            listener.onClick(this)
        } else {
            firstClickTimeStamp = clickTimeStamp;
        }
    }
}

var View.visible: Boolean
    set(value) {
        if (value) visibility = View.VISIBLE else visibility = View.GONE
    }
    get() {
        return visibility == View.VISIBLE
    }
