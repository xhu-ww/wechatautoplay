package com.example.wechatservice

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.PendingIntent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.view.KeyEvent.KEYCODE_BACK
import java.io.IOException


class WeChatAccessibilityService : AccessibilityService() {
    private var hasNotification = false

    override fun onInterrupt() {

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {// 通知栏事件
                hasNotification = openWeChatByNotification(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                if (hasNotification && event.className.toString() == WE_CHAT_LAUNCHER_UI) {
                    logAccessibilityNodeInfo(rootInActiveWindow)
                    hasNotification = false

                    if (setReplayContent(rootInActiveWindow, "你好我在工作")) {
                        sendMessage(rootInActiveWindow)
                        pressBackButton()
                        backToHome()
                    }
                }
        }
    }

    private fun openWeChatByNotification(event: AccessibilityEvent): Boolean {
        val notification = event.parcelableData as? Notification ?: return false
        try {
            notification.contentIntent.send()
            return true
        } catch (e: PendingIntent.CanceledException) {
            e.printStackTrace()
        }
        return false
    }

    private fun logAccessibilityNodeInfo(nodeInfo: AccessibilityNodeInfo) {
        for (i in 0 until nodeInfo.childCount) {
            val childInfo = nodeInfo.getChild(i)
            log("${childInfo.text}----${childInfo.className}----${childInfo.viewIdResourceName}")
            logAccessibilityNodeInfo(childInfo)
        }
    }

    private fun setReplayContent(nodeInfo: AccessibilityNodeInfo, replayContent: String): Boolean {
        nodeInfo.findAccessibilityNodeInfosByViewId(WE_CHAT_EDIT_TEXT_ID)
            .filter { it.className == EditText::class.java.name }
            .forEach {
                setText(it, replayContent)
                return true
            }
        return false
    }

    private fun setText(nodeInfo: AccessibilityNodeInfo, content: String) {
        val arguments = Bundle().apply {
            putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
            )
            putBoolean(
                AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                true
            )
        }

        nodeInfo.performAction(
            AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
            arguments
        )
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)  // 获取焦点
        val clip = ClipData.newPlainText("label", content)
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip = clip
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE) // 执行粘贴
    }

    private fun sendMessage(nodeInfo: AccessibilityNodeInfo) {
        nodeInfo.findAccessibilityNodeInfosByViewId(WE_CHAT_EDIT_SEND_BUTTON_ID)
            .filter { it.className == Button::class.java.name && it.isClickable }
            .forEach {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
    }

    private fun pressBackButton() {
        try {
            Runtime.getRuntime().exec("input keyevent $KEYCODE_BACK")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 回到系统桌面
     */
    private fun backToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_HOME)
        }
        startActivity(homeIntent)
    }

    private fun log(content: String) {
        Log.i("TAG", "---- $content")
    }

    companion object {

        const val WE_CHAT_EDIT_TEXT_ID = "com.tencent.mm:id/ao9"
        const val WE_CHAT_EDIT_SEND_BUTTON_ID = "com.tencent.mm:id/aof"

        const val WE_CHAT_MESSAGE_TEXT_VIEW_ID = "com.tencent.mm:id/oy"
        const val WE_CHAT_MESSAGE_ITEM_ID = "com.tencent.mm:id/a_"
        const val WE_CHAT_LAUNCHER_UI = "com.tencent.mm.ui.LauncherUI"
    }
}
