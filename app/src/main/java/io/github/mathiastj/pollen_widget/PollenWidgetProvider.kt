package io.github.mathiastj.pollen_widget

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
import java.lang.Exception
import java.time.LocalDate
import java.util.*

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
        remoteViews.setOnClickPendingIntent(R.id.pollen_container, pendingIntent)

        val scope = CoroutineScope(context = Dispatchers.Main)

        scope.launch {
            val pollenData = try {
                withContext(Dispatchers.IO) {
                    getPollenData()
                }
            } catch (err: Exception) {
                // Set data to null if request fails
                null
            }

            var grassPollen = "-"
            var artemisiaPollen = "-"
            if (pollenData !== null) {
                Log.i("PollenWidget pollen data:", pollenData)
                val pollenDate = findPollenDataDate((pollenData))
                val now = LocalDate.now()
                // Allow data to be max 2 days old
                if (pollenDate < now.minusDays(2)) {
                    Log.i("PollenWidget:", "Pollen data is outdated")
                } else {
                    grassPollen = findSpecificPollen("Græs", pollenData)
                    artemisiaPollen = findSpecificPollen("Bynke", pollenData)
                }
            } else {
                Log.i("PollenWidget:", "Found no pollen data")
            }

            remoteViews.setTextViewText(R.id.pollen_widget_grass_amount, grassPollen)
            remoteViews.setTextViewText(R.id.pollen_widget_artemisia_amount, artemisiaPollen)
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            }
        }
    }


    private fun getPollenData(): String {
        return URL("https://www.dmi.dk/dmidk_byvejrWS/rest/texts/forecast/pollen/Danmark").run {
            openConnection().run {
                this as HttpURLConnection
                inputStream.bufferedReader().readText()
            }
        }
    }

    // Get the date from when the pollen data was issued
    private fun findPollenDataDate(data: String): LocalDate {
        val datePattern = "<day>(\\d+)<\\/day>\\s*<month>(\\d+)<\\/month>\\s*<year>(\\d+)<\\/year>\\s*".toRegex()
        val match = datePattern.find(data)
        var day = match?.groupValues?.get(1)
        var month = match?.groupValues?.get(2)
        val year = match?.groupValues?.get(3)
        if (day !== null && month !== null && year !== null) {
            // The month seems to be a single digit, i.e. August is just 8, format it to 08
            if (month.length == 1){
                month = "0$month"
            }
            // Day is also a single digit now, format it like the month above
            if (day.length == 1) {
                day = "0$day"
            }
            Log.i("Pollenwidget date", "$year $month $day")
            return LocalDate.parse("$year-$month-$day")
        }
        // If we can't find a date, assume that it is now
        return LocalDate.now()
    }

    // Instead of figuring out Kotlin XML parsing; uses regex to find the first occurrence (which is Copenhagen) of the specific pollen and looks at the subsequent value
    private fun findSpecificPollen(typeOfPollen: String, data: String): String {
        val pattern = "<name>$typeOfPollen<\\/name>\\s*<value>([-,\\d]+)<\\/value>".toRegex()
        val match = pattern.find(data)
        val pollenValue = match?.groupValues?.get(1)
        if (pollenValue !== null) {
            Log.i("PollenWidget pollen value:", pollenValue)
        } else {
            Log.i("PollenWidget", "Regex failed" + data)
        }

        return "-"
    }
}