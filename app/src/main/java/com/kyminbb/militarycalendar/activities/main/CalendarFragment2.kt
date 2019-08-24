package com.kyminbb.militarycalendar.activities.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils.isEmpty
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.kyminbb.militarycalendar.R
import com.kyminbb.militarycalendar.database.CalendarDB
import com.kyminbb.militarycalendar.database.Schedule
import com.tsongkha.spinnerdatepicker.DatePickerDialog
import com.tsongkha.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import kotlinx.android.synthetic.main.fragment_calendar2.*
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColorResource
import org.threeten.bp.LocalDate


class CalendarFragment2 : Fragment() {

    private var adding = true
    private var cal = LocalDate.now()
    private val today = LocalDate.now()

    private var daySelected = -1

    private var slots: Array<Button> = arrayOf()
    private var textSlots: Array<TextView> = arrayOf()
    private var startSlot = 0
    private var eventTextViewNum = 0
    private var eventsinMonth: MutableList<TextView> = mutableListOf<TextView>()
    var memoTyped = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_calendar2, container, false)
    }

    // Update UI after views are created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = CalendarDB(context!!)

        slots = arrayOf(
            day1, day2, day3, day4, day5, day6, day7, day8, day9, day10, day11, day12, day13, day14,
            day15, day16, day17, day18, day19, day20, day21, day22, day23, day24, day25, day26,
            day27, day28, day29, day30, day31, day32, day33, day34, day35, day36, day37, day38,
            day39, day40, day41, day42
        )
        textSlots = arrayOf(
            dayText1, dayText2, dayText3, dayText4, dayText5, dayText6, dayText7, dayText8, dayText9,
            dayText10, dayText11, dayText12, dayText13, dayText14, dayText15, dayText16, dayText17,
            dayText18, dayText19, dayText20, dayText21, dayText22, dayText23, dayText24, dayText25,
            dayText26, dayText27, dayText28, dayText29, dayText30, dayText31, dayText32, dayText33,
            dayText34, dayText35, dayText36, dayText37, dayText38, dayText39, dayText40, dayText41,
            dayText42
        )

        updateCalendar(db)

        changeMonth(db)

        // Show the menu for adding a schedule.
        buttonAdd.setOnClickListener {
            addTab.visibility = if (adding) View.VISIBLE else View.GONE
            adding = !adding
        }

        addLeave.setOnClickListener { addLeaveMenu(db) }
        addDuty.setOnClickListener { addSchedule(db, "당직") }
        addExercise.setOnClickListener { addSchedule(db, "훈련") }
        addPersonal.setOnClickListener { addSchedule(db, "개인") }

        buttonMenu.setOnClickListener {
            val wrapper = ContextThemeWrapper(this.context!!, R.style.calendarPopUp)
            val popupMenu = PopupMenu(wrapper, it)
            popupMenu.menuInflater.inflate(R.menu.calendar_setting_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.searchItem -> Toast.makeText(
                        this.context!!,
                        "1",
                        Toast.LENGTH_SHORT
                    ).show()
                    else -> {
                        it.isChecked = !it.isChecked
                    }
                }
                false
            }
            popupMenu.show()
        }
    }

    /************************************************************************
    Manage calendar
     ************************************************************************/

    // Change to the previous or to the next month.
    private fun changeMonth(db: CalendarDB) {
        val prevNext = arrayOf(buttonLeft, Button(context), buttonRight)
        for (i in 0 until prevNext.size step 2) {
            prevNext[i].setOnClickListener {
                cal = cal.plusMonths(i.toLong() - 1).withDayOfMonth(1)
                updateCalendar(db)
            }
        }
    }

    /************************************************************************
    Manage schedules
     ************************************************************************/

    private fun addLeaveMenu(db: CalendarDB) {
        val addLeaveArray = resources.getStringArray(R.array.addLeave_string)
        val addLeaveButtons = arrayOf(
            R.id.LeaveSelect,
            R.id.PassSelect,
            R.id.offPostSelect
        )
        val popupMenu = PopupMenu(this.context, addLeave)
        popupMenu.menuInflater.inflate(R.menu.pass_leave_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener {
            addSchedule(db, addLeaveArray[addLeaveButtons.indexOf(it.itemId)])
            true
        }
        popupMenu.show()
    }

    @SuppressLint("InflateParams")
    private fun addSchedule(db: CalendarDB, type: String) {
        val popupView = when (type) {
            "휴가" -> layoutInflater.inflate(R.layout.add_leave, null)
            "외출" -> layoutInflater.inflate(R.layout.add_offpost, null)
            else -> layoutInflater.inflate(R.layout.add_pass, null)
        }
        val popup = PopupWindow(popupView)
        popup.isFocusable = true
        popup.showAtLocation(view, Gravity.CENTER, 0, 0)
        popup.update(
            view,
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        )

        // Initialize buttons of the popupView.
        val startSchedule = popupView.find<Button>(R.id.startSchedule)
        val endSchedule = popupView.find<Button>(R.id.endSchedule)
        val titleInput = popupView.find<EditText>(R.id.nameEdit)
        val buttonRegister = popupView.find<Button>(R.id.RegisterBtn)
        val buttonCancel = popupView.find<Button>(R.id.CancelBtn)
        val buttonInit = popupView.find<Button>(R.id.InitBtn)
        val buttonMemo = popupView.find<Button>(R.id.memoButton)

        startSchedule.setOnClickListener { setDate(startSchedule) }
        endSchedule.setOnClickListener { setDate(endSchedule) }

        buttonRegister.setOnClickListener {
            if (type == "휴가" || type == "외박") {
                if (startSchedule.text.isNotEmpty() && endSchedule.text.isEmpty()) {
                    toast("복귀 안 할거에요?")
                    return@setOnClickListener
                }
                if (startSchedule.text.isNotEmpty() && endSchedule.text.isNotEmpty() &&
                    string2Date(startSchedule.text.toString()).isAfter(string2Date(endSchedule.text.toString()))
                ) {
                    toast("복귀일이 시작일보다 앞이려면 \n 시간여행을 해야해요!")
                    return@setOnClickListener
                }
            }
            db.writeDB(
                startSchedule.text.toString(),
                endSchedule.text.toString(),
                type,
                titleInput.text.toString(),
                ""
            )
            updateCalendar(db)
            popup.dismiss()
        }

        buttonCancel.setOnClickListener { popup.dismiss() }

        buttonInit.setOnClickListener {
            startSchedule.text = ""
            endSchedule.text = ""
            titleInput.setText("")
        }
    }

    /************************************************************************
    Visuals
     ************************************************************************/

    private fun initCalendar() {
        for (i in 0..41) {
            textSlots[i].text = ""
            textSlots[i].setBackgroundResource(0)
            textSlots[i].setBackgroundResource(0)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCalendar(db: CalendarDB) {
        // Clear up the calendar.
        initCalendar()

        // Load schedules of the current month.
        val monthSchedules = db.readDB(date2String(cal), "Month")
        var scheduleIdx = 0

        textYear.text = "${cal.year}"
        textMonth.text = "${cal.monthValue}월"

        // Display dates and schedules of the current month.
        var position = cal.withDayOfMonth(1).dayOfWeek.value % 7
        for (i in 1 until cal.lengthOfMonth() + 1) {
            textSlots[position].text = i.toString()
            // Set colors for weekends.
            if (position % 7 == 0) textSlots[position].textColorResource =
                R.color.horizontalProgressbarRed
            if (position % 7 == 6) textSlots[position].textColorResource =
                R.color.horizontalProgressbarBlue
            // Circle on today.
            // https://stackoverflow.com/questions/25203501/android-creating-a-circular-textview
            if (cal.year == today.year && cal.monthValue == today.monthValue && i == today.dayOfMonth) {
                textSlots[position].setBackgroundResource(R.drawable.rounded_textview)
            }

            // Display schedules starting from the current date.
            while (scheduleIdx < monthSchedules.size &&
                monthSchedules[scheduleIdx].startDate.endsWith("%02d".format(i))) {
                displaySchedule(monthSchedules[scheduleIdx])
                scheduleIdx += 1
            }
            position += 1
        }
    }

    private fun displaySchedule(schedule: Schedule) {

    }

    private fun addEventinCalendar(
        startDate: LocalDate,
        endDate: LocalDate,
        type: String,
        text: String
    ) {
        var startPosition = startDate.dayOfMonth + startSlot - 1
        var endPosition = endDate.dayOfMonth + startSlot - 1
        if (cal.year != startDate.year || cal.monthValue != startDate.monthValue) {
            if (cal.year != endDate.year || cal.monthValue != endDate.monthValue) {
                return
            } else {
                startPosition = startSlot
            }
        } else {
            if (endDate.monthValue != startDate.monthValue) {
                endPosition =
                    startDate.withDayOfMonth(1).plusMonths(1).minusDays(1).dayOfMonth + startSlot - 1
            }
        }

        if ((startPosition) / 7 == (endPosition) / 7) {
            drawEventTextView(startPosition, endPosition, type, text)
        } else {
            drawEventTextView(startPosition, (startPosition / 7) * 7 + 6, type, text)
            for (i in (startPosition / 7) + 1..(endPosition / 7) - 1) {
                drawEventTextView(i * 7, i * 7 + 6, type, text)
            }
            drawEventTextView((endPosition / 7) * 7, endPosition, type, text)
        }
    }

    private fun drawEventTextView(startPos: Int, endPos: Int, type: String, text: String) {
        val constraintSet = ConstraintSet()
        eventsinMonth.add(TextView(this.context))
        eventsinMonth[eventTextViewNum].id = View.generateViewId()
        constraintSet.clone(calendarLayout)
        constraintSet.connect(
            eventsinMonth[eventTextViewNum].id,
            ConstraintSet.START,
            slots[startPos].id,
            ConstraintSet.START
        )
        constraintSet.connect(
            eventsinMonth[eventTextViewNum].id,
            ConstraintSet.END,
            slots[endPos].id,
            ConstraintSet.END
        )
        constraintSet.connect(
            eventsinMonth[eventTextViewNum].id,
            ConstraintSet.BOTTOM,
            slots[startPos].id,
            ConstraintSet.BOTTOM
        )
        constraintSet.connect(
            eventsinMonth[eventTextViewNum].id,
            ConstraintSet.TOP,
            textSlots[endPos].id,
            ConstraintSet.BOTTOM
        )
        constraintSet.constrainWidth(
            eventsinMonth[eventTextViewNum].id,
            ConstraintSet.MATCH_CONSTRAINT
        )
        constraintSet.constrainHeight(
            eventsinMonth[eventTextViewNum].id,
            ConstraintSet.WRAP_CONTENT
        )
        constraintSet.setMargin(eventsinMonth[eventTextViewNum].id, ConstraintSet.START, 4)
        constraintSet.setMargin(eventsinMonth[eventTextViewNum].id, ConstraintSet.END, 4)
        when (type) {
            "휴가" -> {
                constraintSet.setVerticalBias(eventsinMonth[eventTextViewNum].id, 0.05f)
                eventsinMonth[eventTextViewNum].setBackgroundResource(R.drawable.leave_textview_both_rounded)
            }
            "당직" -> {
                constraintSet.setVerticalBias(eventsinMonth[eventTextViewNum].id, 0.30f)
                eventsinMonth[eventTextViewNum].setBackgroundResource(R.drawable.duty_textview_both_rounded)
            }
            "훈련" -> {
                constraintSet.setVerticalBias(eventsinMonth[eventTextViewNum].id, 0.55f)
                eventsinMonth[eventTextViewNum].setBackgroundResource(R.drawable.exercise_textview_both_rounded)
            }
            else -> {
                constraintSet.setVerticalBias(eventsinMonth[eventTextViewNum].id, 0.80f)
                eventsinMonth[eventTextViewNum].setBackgroundResource(R.drawable.personal_textview_both_rounded)
            }
        }
        eventsinMonth[eventTextViewNum].text = text
        eventsinMonth[eventTextViewNum].textSize = 8.0f
        calendarLayout.addView(eventsinMonth[eventTextViewNum])
        constraintSet.applyTo(calendarLayout)
        eventTextViewNum++
    }

    /************************************************************************
    Utils
     ************************************************************************/

    private fun setDate(button: Button) {
        // Use SpinnerDatePicker to select date.
        // https://github.com/drawers/SpinnerDatePicker
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            button.text = date2String(year, month + 1, day)
        }
        val dialog = SpinnerDatePickerDialogBuilder()
            .context(context)
            .callback(dateSetListener)
            .spinnerTheme(R.style.NumberPickerStyle)
            .showTitle(true)
            .showDaySpinner(true)
            .maxDate(today.year + 4, 11, 31)
            .minDate(today.year - 5, 0, 1)
        if (isEmpty(button.text)) {
            dialog.defaultDate(today.year, today.monthValue - 1, today.dayOfMonth)
        } else {
            val curDate = string2Date(button.text.toString())
            dialog.defaultDate(curDate.year, curDate.monthValue - 1, curDate.dayOfMonth)
        }
        dialog.build().show()
    }

    private fun date2String(year: Int, month: Int, dayOfMonth: Int): String {
        return "$year-${"%02d".format(month)}-${"%02d".format(dayOfMonth)}"
    }

    private fun date2String(date: LocalDate): String {
        return "${date.year}-${"%02d".format(date.monthValue)}"
    }

    private fun month2String(date: LocalDate): String {
        return "${date.year}-${"%02d".format(date.monthValue)}-${"%02d".format(date.dayOfMonth)}"
    }

    @SuppressLint("SimpleDateFormat")
    private fun string2Date(date: String): LocalDate {
        return LocalDate.parse(date)
    }
}
