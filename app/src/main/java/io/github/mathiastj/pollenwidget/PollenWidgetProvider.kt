package io.github.mathiastj.pollenwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*


class PollenWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val remoteViews = RemoteViews(context.packageName, R.layout.pollen_widget)


        val intent = Intent(context, PollenWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        remoteViews.setOnClickPendingIntent(R.id.actionButton, pendingIntent)

        val hallo = GlobalScope.async {
            val res = getPollenData()
            Log.i("MyActivity",res)
            var grassPollen = findSpecificPollen("Græs", res)
            if (grassPollen === null) {
                grassPollen = "-"
            }
            findSpecificPollen("Bynke", res)
            remoteViews.setTextViewText(R.id.appwidget_text, grassPollen)
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            }
        }
    }

    private suspend fun getPollenData(): String {
        return URL("https://www.dmi.dk/dmidk_byvejrWS/rest/texts/forecast/pollen/Danmark").run {
            openConnection().run {
                this as HttpURLConnection
                inputStream.bufferedReader().readText()
            }
        }
    }

    private fun findSpecificPollen(typeOfPollen: String, data: String): String? {
        val pattern = "<name>$typeOfPollen<\\/name>\\s*<value>([-,\\d])<\\/value>".toRegex()
        val match = pattern.find(data)
        Log.i("MyActivity", match?.groupValues?.get(1))


        return match?.groupValues?.get(1)
    }


}