package com.bioquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioquest.ui.BioQuestViewModel
import com.bioquest.ui.navigation.BioQuestApp
import com.bioquest.ui.theme.BioQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BioQuestTheme {
                val vm: BioQuestViewModel = viewModel(factory = BioQuestViewModel.Factory)
                BioQuestApp(vm)
            }
        }
    }
}
