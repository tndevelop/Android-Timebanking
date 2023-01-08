package it.polito.timebanking.database

import androidx.room.TypeConverter
import java.util.*

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }

    @TypeConverter
    fun dateToCalendar(date: Date?): Calendar?{
        val calendar = Calendar.getInstance()
        calendar.time = date!!
        return calendar
    }

    @TypeConverter
    //Convert Calendar to Date
    fun calendarToDate(calendar: Calendar): Date? {
        return calendar.time
    }

}