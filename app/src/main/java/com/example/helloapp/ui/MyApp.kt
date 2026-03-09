package com.example.helloapp

import android.app.Application
import android.util.Log
import com.example.helloapp.BuildConfig
import com.iflytek.sparkchain.core.SparkChain
import com.iflytek.sparkchain.core.SparkChainConfig

class MyApp : Application() {

    companion object {
        private const val TAG = "MyApp"
    }

    override fun onCreate() {
        super.onCreate()

        try {
            val config = SparkChainConfig.builder()
                .appID(BuildConfig.XF_APP_ID)
                .apiKey(BuildConfig.XF_API_KEY)
                .apiSecret(BuildConfig.XF_API_SECRET)

            val initRet = SparkChain.getInst().init(applicationContext, config)
            if (initRet == 0) {
                Log.d(TAG, "SparkChain 初始化成功")
            } else {
                Log.e(TAG, "SparkChain 初始化失败，错误码：$initRet")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SparkChain 初始化异常：${e.message}", e)
        }
    }
}
