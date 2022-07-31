package com.db.view.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

//view model不放到这里写入，应该并不每个activity都需要view model
abstract class BaseBindingActivity<VB : ViewBinding> : AppCompatActivity() {
    protected lateinit var viewBinding: VB
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ViewBindingUtil.inflateWithGeneric(this, layoutInflater)
        setContentView(viewBinding.root)
    }
}
