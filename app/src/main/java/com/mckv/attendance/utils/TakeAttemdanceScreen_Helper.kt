package com.mckv.attendance.utils

import android.R
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener

/*@Composable
fun DepartmentAutoComplete(
    department: String,
    onDepartmentChange: (String) -> Unit
) {
    val departments = listOf("CSE", "CSEDS", "CSEAIML", "IT", "EE", "ECE", "ME", "AUE")

    AndroidView(
        factory = { context ->
            AutoCompleteTextView(context).apply {

                // Set adapter for dropdown
                val adapter = ArrayAdapter(
                    context,
                    R.layout.simple_dropdown_item_1line,
                    departments
                )
                setAdapter(adapter)

                // When user types
                addTextChangedListener {
                    onDepartmentChange(it.toString())
                }

                // Show dropdown on typing
                threshold = 1   // suggestions appear after 1 letter

                // Always show keyboard properly
                isFocusable = true
                isFocusableInTouchMode = true
            }
        },
        update = { view ->
            if (view.text.toString() != department) {
                view.setText(department)
                view.setSelection(department.length)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}*/

/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentAutoComplete(
    department: String,
    onDepartmentChange: (String) -> Unit
) {
    val departments = listOf("CSE", "CSEDS", "CSEAIML", "IT", "EE", "ECE", "ME", "AUE")

    var internalText by remember { mutableStateOf(department) }

    // Outlined container
    OutlinedTextField(
        value = internalText,
        onValueChange = {
            internalText = it
            onDepartmentChange(it)
        },
        label = { Text("Department") },
        leadingIcon = { Icon(Icons.Default.School, null) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,   // IMPORTANT: we will show the real textfield inside it!
        textStyle = LocalTextStyle.current
    )

    // AutoCompleteTextView positioned on top of the OutlinedTextField
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)     // align with OutlinedTextField
            .offset(y = (-56).dp),           // place inside the box
        factory = { context ->
            AutoCompleteTextView(context).apply {

                val adapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_dropdown_item_1line,
                    departments
                )
                setAdapter(adapter)

                threshold = 1
                isFocusable = true
                isFocusableInTouchMode = true

                textSize = 16f
                setPadding(30, 50, 30, 20) // proper padding inside box

                addTextChangedListener {
                    val text = it.toString()
                    internalText = text
                    onDepartmentChange(text)
                }
            }
        },
        update = { view ->
            if (view.text.toString() != internalText) {
                view.setText(internalText)
                view.setSelection(internalText.length)
            }
        }
    )
}*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentAutoComplete(
    department: String,
    onDepartmentChange: (String) -> Unit
) {
    val context = LocalContext.current
    val departments = listOf("CSE", "CSEDS", "CSEAIML", "IT", "EE", "ECE", "ME", "AUE")

    var text by remember { mutableStateOf(department) }
    val viewRef = remember { arrayOfNulls<AutoCompleteTextView>(1) }

    Column(modifier = Modifier.fillMaxWidth()) {

        // --- Visible OutlinedTextField ---
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onDepartmentChange(it)
                viewRef[0]?.setText(it)          // send text to ACTV
                viewRef[0]?.showDropDown()       // always open dropdown
            },
            label = { Text("Department") },
            leadingIcon = { Icon(Icons.Default.School, null) },
            modifier = Modifier.fillMaxWidth()
        )

        // --- Invisible AutoCompleteTextView that only shows dropdown ---
        AndroidView(
            factory = {
                AutoCompleteTextView(context).apply {

                    // ACTV SHOULD NOT BE VISIBLE
                    alpha = 0f      // invisible
                    isCursorVisible = false
                    isFocusable = false
                    isFocusableInTouchMode = false

                    val adapter = ArrayAdapter(
                        context,
                        R.layout.simple_list_item_activated_1,
                        departments
                    )
                    setAdapter(adapter)
                    threshold = 1

                    // When user chooses item from suggestion
                    setOnItemClickListener { _, _, position, _ ->
                        val selected = adapter.getItem(position) ?: ""
                        text = selected
                        onDepartmentChange(selected)
                    }
                }.also { viewRef[0] = it }
            },
            update = { view ->
                if (view.text.toString() != text) {
                    view.setText(text)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)   // takes no visual space
        )
    }
}


@Composable
fun SubjectAutoComplete(
    subject: String,
    onSubjectChange: (String) -> Unit
) {
    val context = LocalContext.current
    val subjects = listOf("HM-HU101", "HM-HU501", "HM-HU604", "HM-HU702", "HM-HU291", "HM-HU591", "BS-PH101", "BS-M101", "BS-CH201", "BS-M201","BS-M303","BS-M301","BS-BIO301","BS-M404","BS-PH191","BS-CH291","ES-EE101","ES-CS201","ES-AUE301","ES-AUE302","ES-AUE401","ES-EE191","ES-ME191","ES-CS291","ES-ME292","PC-AUE301","PC-AUE302","PC-AUE401","PC-AUE402","PC-AUE403","PC-AUE404","PC-AUE501","PC-AUE502","PC-AUE503","PC-AUE504","PC-AUE601","PC-AUE602","PC-AUE701","PC-AUE391","PC-AUE491","PC-AUE591","PC-AUE592","PC-AUE691","PC-AUE692","PC-AUE693","PC-AUE791","PC-AUE881","PE-AUE601","PE-AUE601A","PE-AUE601B","PE-AUE601C","PE-AUE701","PE-AUE701A","PE-AUE701B","PE-AUE701C","PE-AUE702","PE-AUE702A","PE-AUE702B","PE-AUE702C","PE-AUE801","PE-AUE801A","PE-AUE801B","PE-AUE801C","PE-AUE801D","PE-AUE802","PE-AUE802A","PE-AUE802B","PE-AUE802C","OE-AUE701","OE-AUE701A","OE-AUE701B","OE-AUE701C","OE-AUE701D","OE-AUE801","OE-AUE801A","OE-AUE801B","OE-AUE801C","OE-AUE802","OE-AUE802A","OE-AUE802B","OE-AUE802C","OE-AUE802D","PW-AUE581","PW-AUE681","PW-AUE781","PW-AUE882","MC471","MC571","MC671","MC673","MC772")

    var text by remember { mutableStateOf(subject) }
    val viewRef = remember { arrayOfNulls<AutoCompleteTextView>(1) }

    Column(modifier = Modifier.fillMaxWidth()) {

        // --- Visible OutlinedTextField ---
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onSubjectChange(it)
                viewRef[0]?.setText(it)          // send text to ACTV
                viewRef[0]?.showDropDown()       // always open dropdown
            },
            label = { Text("Subject") },
            leadingIcon = { Icon(Icons.Default.Book, null) },
            modifier = Modifier.fillMaxWidth()
        )

        // --- Invisible AutoCompleteTextView that only shows dropdown ---
        AndroidView(
            factory = {
                AutoCompleteTextView(context).apply {

                    // ACTV SHOULD NOT BE VISIBLE
                    alpha = 0f      // invisible
                    isCursorVisible = false
                    isFocusable = false
                    isFocusableInTouchMode = false

                    val adapter = ArrayAdapter(
                        context,
                        R.layout.simple_list_item_activated_1,
                        subjects
                    )
                    setAdapter(adapter)
                    threshold = 1

                    // When user chooses item from suggestion
                    setOnItemClickListener { _, _, position, _ ->
                        val selected = adapter.getItem(position) ?: ""
                        text = selected
                        onSubjectChange(selected)
                    }
                }.also { viewRef[0] = it }
            },
            update = { view ->
                if (view.text.toString() != text) {
                    view.setText(text)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)   // takes no visual space
        )
    }
}
