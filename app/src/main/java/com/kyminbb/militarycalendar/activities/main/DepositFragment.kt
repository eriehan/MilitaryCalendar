package com.kyminbb.militarycalendar.activities.main


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.kyminbb.militarycalendar.R
import com.kyminbb.militarycalendar.utils.*
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import com.tsongkha.spinnerdatepicker.DatePickerDialog
import com.tsongkha.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.add_deposit.*
import kotlinx.android.synthetic.main.fragment_deposit.*
import org.jetbrains.anko.find
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.matchParent
import org.threeten.bp.LocalDate
import java.lang.Double.parseDouble
import java.text.DecimalFormat

class DepositFragment : Fragment() {

    companion object {
        private const val animStyle = R.style.Animation_AppCompat_DropDownUp
        private const val BANK_PREFS_NAME = "com.kyminbb.militarycalendar.activities.main.DepositFragment"
        private const val BANK_PREF_PREFIX_KEY = "BANK_NAME_"
        private const val ADD_BUTTON_INFO = 99
    }

    private var userInfo = User()
    private val today = LocalDate.now()
    private val decimalFormat = DecimalFormat("#,###")
    private var temp = ""
    //private var arrayBankList = loadBankData(this.context!!)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_deposit, container, false)
    }

    /**
     * onViewCreated is divided into 4 parts of code -> to be restructured later.
     * 1. Implementing Monthly Deposit(적금) information
     * 2. Implementing Monthly Graph
     * 3. Implementing current well-being(적금이 잘 이루어지는지에 대한 평가)
     * 4. Implementing addButton on the top-right corner
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()

        /** Implement Monthly Deposit Info using Recycler View */
        loadBankData(activity!!.applicationContext)
        updateRecyclerView(activity!!.applicationContext, bankRecyclerView)

        /** Implement Monthly Graph **/
        /** Implement Current Well-Being **/

        /** Implement addButton on the top-right corner */
        // Add new deposit information
        depositButtonAdd.setOnClickListener {
            showBigPopUp(ADD_BUTTON_INFO)
        }
    }

    // set datePicker functionality for buttons
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
            .defaultDate(today.year, today.monthValue - 1, today.dayOfMonth)
        dialog.build().show()
    }


    // convert date into format that are SQLite readable
    private fun date2String(year: Int, month: Int, dayOfMonth: Int): String {
        return "$year-${"%02d".format(month)}-${"%02d".format(dayOfMonth)}"
    }

    // load userInfo data
    private fun loadData() {
        // Get context from the parent activity.
        val prefs = this.context!!.getSharedPreferences("prefs", AppCompatActivity.MODE_PRIVATE)
        userInfo = Gson().fromJson(prefs.getString("userInfo", ""), User::class.java)
    }

    // show big pop-up for adding/editing bank information
    private fun showBigPopUp(position: Int) {
        // Show pop-up when add button is clicked
        val popupView = layoutInflater.inflate(R.layout.add_deposit, null)
        val popup = PopupWindow(popupView)
        popup.isFocusable = true
        popup.showAtLocation(view, Gravity.CENTER, 0, 0)
        popup.animationStyle =animStyle
        popup.update(
            view,
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        )

        // enable buttons as views
        val popupTitle = popupView.find<TextView>(R.id.popUpBankTitle)
        val bankNameButton = popupView.find<Button>(R.id.bankNameButton)
        val bankStartDateButton = popupView.find<Button>(R.id.bankStartDateButton)
        val bankEndDateButton = popupView.find<Button>(R.id.bankEndDateButton)
        val bankDepositAmountButton = popupView.find<Button>(R.id.bankDepositAmountButton)
        val bankInterestButton = popupView.find<Button>(R.id.bankInterestButton)
        val bankInitButton = popupView.find<Button>(R.id.bankInitButton)
        val bankCancelButton = popupView.find<Button>(R.id.bankCancelButton)
        val bankRegisterButton = popupView.find<Button>(R.id.bankRegisterButton)


        /* if mode is unique, then just show the end date as 전역일
        *  if mode is edit, then show the data stored */
        if (position in 0 until 13) {
            popupTitle.text = "은행 정보 수정하기"
            val bankInfo = loadBankData(this.context!!)[position]
            bankNameButton.text = bankInfo.bankName
            bankStartDateButton.text = DateCalc.localDateToString(bankInfo.startDate)
            bankEndDateButton.text = DateCalc.localDateToString(bankInfo.endDate)
            bankDepositAmountButton.text = bankInfo.monthDeposit
        }

        /* Add functionality to buttons */
        // set end date default as the 전역일(end date)
        bankEndDateButton.text = DateCalc.localDateToString(userInfo.promotionDates[Dates.END.ordinal])
        val bankArr = resources.getStringArray(R.array.bank_string)
        val bankArrButtons = arrayOf(
            R.id.bankSuhyeob, R.id.bankShinhan, R.id.bankWoori, R.id.bankHana,
            R.id.bankKookmin, R.id.bankNonghyeob, R.id.bankIndustrial, R.id.bankDaegu,
            R.id.bankBusan, R.id.bankGyeongnam, R.id.bankGwangju,
            R.id.bankJeonbook, R.id.bankJeju, R.id.bankPostOffice
        )


        // input information
        // bank name button is only activated when addButton is clicked
        if (position == ADD_BUTTON_INFO){
            bankNameButton.setOnClickListener {
                val popupMenu = PopupMenu(activity, bankNameButton)
                popupMenu.menuInflater.inflate(R.menu.bank_name_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener {
                    bankNameButton.text = bankArr[bankArrButtons.indexOf(it.itemId)]
                    true
                }
                popupMenu.show()
            }
        }
        // input the date from spinnerDatePicker, default: today
        bankStartDateButton.setOnClickListener { setDate(bankStartDateButton) }
        // input the endDate from spinnerDatePicker, default: userInfo.promotion.endDate
        bankEndDateButton.setOnClickListener { setDate(bankEndDateButton) }
        // input the deposit amount, using another pop-up view
        bankDepositAmountButton.setOnClickListener {
            // enable deposit Popup
            val popupDepositView = layoutInflater.inflate(R.layout.add_deposit_amount, null)
            val popupDeposit = PopupWindow(popupDepositView)
            popupDeposit.animationStyle = animStyle
            popupDeposit.isFocusable = true
            popupDeposit.showAtLocation(view, Gravity.CENTER, 0, 0)
            popupDeposit.update(
                view,
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels
            )

            // enable buttons, editTexts
            val depositPopUpCancel = popupDepositView.find<Button>(R.id.depositPopUpCancel)
            val depositPopUpSave = popupDepositView.find<Button>(R.id.depositPopUpSave)
            val depositAmountEditText = popupDepositView.find<EditText>(R.id.depositAmountEditText)

            // format editText into Korean Currency
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    if (!TextUtils.isEmpty(charSequence.toString()) && charSequence.toString() != temp) {
                        temp = decimalFormat.format(
                            parseDouble(
                                charSequence.toString().replace(",".toRegex(), "")
                            )
                        )
                        depositAmountEditText.setText(temp)
                        depositAmountEditText.setSelection(temp.length)
                    }
                }
                override fun afterTextChanged(editable: Editable) {}
            }
            depositAmountEditText.addTextChangedListener(watcher)

            // cancel input
            depositPopUpCancel.setOnClickListener { popupDeposit.dismiss() }
            // save input
            depositPopUpSave.setOnClickListener {
                bankDepositAmountButton.text = "${depositAmountEditText.text}원"
                popupDeposit.dismiss()
            }
        }
        bankInterestButton.setOnClickListener {}


        /* make decision on init, cancel, registering bank information */
        // create an arrayList of info-buttons for convenience
        val buttonArr = arrayOf(
            bankNameButton, bankStartDateButton, bankEndDateButton,
            bankDepositAmountButton
            //bankInterestButton
        )
        // init
        bankInitButton.setOnClickListener {
            when(position) {
                ADD_BUTTON_INFO ->
                    for (infoButton in buttonArr)
                        infoButton.text = ""
                else -> {
                    val temp = bankNameButton.text
                    for (infoButton in buttonArr)
                        infoButton.text = ""
                    bankNameButton.text = temp
                }
            }
        }
        // cancel
        bankCancelButton.setOnClickListener {
            for (infoButton in buttonArr)
                infoButton.text = ""
            popup.dismiss()
        }
        // register
        bankRegisterButton.setOnClickListener {
            var complete = true
            for (infoButton in buttonArr) { complete = complete && infoButton.text != "" }
            if (!complete) {
                DynamicToast.makeError(activity!!.applicationContext, "정보입력이 완료되지 않았습니다!").show()
            }
            else {
                val bankToBeSaved = Bank(
                    bankNameButton.text.toString(), //LocalDate.now(), LocalDate.now(),
                    LocalDate.parse(bankStartDateButton.text),
                    LocalDate.parse(bankEndDateButton.text),
                    bankDepositAmountButton.text.toString(),
                    0.0
                    //parseDouble(bankInterestButton.text.toString())
                )

                saveBankData(activity!!.applicationContext, bankToBeSaved, this.view!!, bankRecyclerView)
                //loadBankData(activity!!.applicationContext)
                updateRecyclerView(activity!!.applicationContext, bankRecyclerView)
                for (infoButton in buttonArr)
                    infoButton.text = ""
                popup.dismiss()
            }
        }
    }


    private fun saveBankData(context: Context, inputBank: Bank, view: View, recyclerView: RecyclerView) {
        val prefs = context.getSharedPreferences(BANK_PREFS_NAME, MODE_PRIVATE)
        val prefsEditor = prefs.edit()
        val jsonString = Gson().toJson(inputBank)

        // when the bank type is already contained
        // show pop-up whether to update or not
        if(prefs.all.containsKey(BANK_PREF_PREFIX_KEY + inputBank.bankName.hashCode())){
            val popupHasBankView = context.layoutInflater.inflate(R.layout.popup_has_bank, null)
            val popupHasBank = PopupWindow(popupHasBankView)
            popupHasBank.isFocusable = true
            popupHasBank.animationStyle = animStyle
            popupHasBank.showAtLocation(view, Gravity.CENTER, 0,0)
            popupHasBank.update(
                view,
                view.resources.displayMetrics.widthPixels,
                view.resources.displayMetrics.heightPixels)

            val popupHasBankCancel = popupHasBankView.find<Button>(R.id.popupHasBankCancel)
            val popupHasBankRegister = popupHasBankView.find<Button>(R.id.popupHasBankRegister)

            popupHasBankCancel.setOnClickListener { popupHasBank.dismiss() }
            popupHasBankRegister.setOnClickListener {
                prefsEditor.putString(BANK_PREF_PREFIX_KEY + inputBank.bankName.hashCode(), jsonString)
                prefsEditor.apply()
                //loadBankData(context)
                updateRecyclerView(context, recyclerView)
                popupHasBank.dismiss()
            }

        } else {
            prefsEditor.putString(BANK_PREF_PREFIX_KEY + inputBank.bankName.hashCode(), jsonString)
            prefsEditor.apply()
        }

    }

    private fun loadBankData(context: Context): ArrayList<Bank> {
        val prefs = context.getSharedPreferences(BANK_PREFS_NAME, MODE_PRIVATE)
        val arrayList = ArrayList<Bank>()

        // retrieve a map containing all data saved, save values as arrayList
        val allEntries = prefs.all
        for(keys in allEntries.keys){
            val bankInfo = Gson().fromJson(prefs.getString(keys, ""), Bank::class.java)
            arrayList.add(bankInfo)
        }
        return arrayList
    }

    private fun deleteBankData(context: Context, inputBank: Bank) {
        val prefs = context.getSharedPreferences(BANK_PREFS_NAME, MODE_PRIVATE).edit()
        prefs.remove(BANK_PREF_PREFIX_KEY + inputBank.bankName.hashCode())
        prefs.apply()
    }

    private fun updateRecyclerView(context:Context, view: RecyclerView) {
        // add recyclerView
        val arrayBankList = loadBankData(context)
        val adapter = BankRvAdapter(context, arrayBankList)
        view.adapter = adapter

        // add layoutManager
        val lm = LinearLayoutManager(context)
        view.layoutManager = lm
        view.setHasFixedSize(true)

        adapter.setOnItemClickListener(object :BankRvAdapter.OnItemClickListener {
            override fun onItemClick(v: View, position: Int) {
                val popupEditMenu = PopupMenu(context, v)
                popupEditMenu.menuInflater.inflate(R.menu.bank_recycler_edit_menu, popupEditMenu.menu)
                popupEditMenu.setOnMenuItemClickListener {
                    when ((it.itemId)) {
                        R.id.bankRecyclerEdit -> showBigPopUp(position)
                        else -> {
                            deleteBankData(context, arrayBankList[position])
                            updateRecyclerView(context, view)
                        }
                    }
                    true
                }
                popupEditMenu.show()
            }
        })
    }
}

