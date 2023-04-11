package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.calDAVHelper
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.helpers.OTHER_EVENT
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.dialog_event_type.view.*

class EditEventTypeDialog(val activity: Activity, var eventType: EventType? = null, val callback: (eventType: EventType) -> Unit) {
    private var isNewEvent = eventType == null

    init {
        if (eventType == null) {
            eventType = EventType(null, "", activity.getProperPrimaryColor())
        }

        val view = activity.layoutInflater.inflate(R.layout.dialog_event_type, null).apply {
            setupColor(type_color)
            type_title.setText(eventType!!.title)
            type_color.setOnClickListener {
                if (eventType?.caldavCalendarId == 0) {
                    ColorPickerDialog(activity, eventType!!.color) { wasPositivePressed, color ->
                        if (wasPositivePressed) {
                            eventType!!.color = color
                            setupColor(type_color)
                        }
                    }
                } else {
                    val currentColor = eventType!!.color
                    val colors = activity.calDAVHelper.getAvailableCalDAVCalendarColors(eventType!!).keys.toIntArray()
                    SelectEventTypeColorDialog(activity, colors = colors, currentColor = currentColor) {
                        eventType!!.color = it
                        setupColor(type_color)
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, if (isNewEvent) R.string.add_new_type else R.string.edit_type) { alertDialog ->
                    alertDialog.showKeyboard(view.type_title)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        ensureBackgroundThread {
                            eventTypeConfirmed(view.type_title.value, alertDialog)
                        }
                    }
                }
            }
    }

    private fun setupColor(view: ImageView) {
        view.setFillWithStroke(eventType!!.color, activity.getProperBackgroundColor())
    }

    private fun eventTypeConfirmed(title: String, dialog: AlertDialog) {
        val eventTypeClass = eventType?.type ?: OTHER_EVENT
        val eventTypeId = if (eventTypeClass == OTHER_EVENT) {
            activity.eventsHelper.getEventTypeIdWithTitle(title)
        } else {
            activity.eventsHelper.getEventTypeIdWithClass(eventTypeClass)
        }

        var isEventTypeTitleTaken = isNewEvent && eventTypeId != -1L
        if (!isEventTypeTitleTaken) {
            isEventTypeTitleTaken = !isNewEvent && eventType!!.id != eventTypeId && eventTypeId != -1L
        }

        if (title.isEmpty()) {
            activity.toast(R.string.title_empty)
            return
        } else if (isEventTypeTitleTaken) {
            activity.toast(R.string.type_already_exists)
            return
        }

        eventType!!.title = title
        if (eventType!!.caldavCalendarId != 0) {
            eventType!!.caldavDisplayName = title
        }

        eventType!!.id = activity.eventsHelper.insertOrUpdateEventTypeSync(eventType!!)

        if (eventType!!.id != -1L) {
            activity.runOnUiThread {
                dialog.dismiss()
                callback(eventType!!)
            }
        } else {
            activity.toast(R.string.editing_calendar_failed)
        }
    }
}
