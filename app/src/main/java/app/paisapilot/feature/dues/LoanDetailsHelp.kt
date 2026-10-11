package app.paisapilot.feature.dues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The prompt a user pastes into an AI app along with screenshots of their loan statement.
 * It asks for exactly the boxes on the loan form, in the same order, and tells the AI not
 * to guess, because a made-up interest rate is worse than a blank.
 */
internal val LOAN_DETAILS_PROMPT = """
I am attaching screenshots of my loan statement. Read them carefully and give me the details below, one per line, in exactly this order. If something is not shown in the screenshots, write "not shown" rather than guessing. Do not include my loan account number or any other personal details in your reply.

1. Lender (bank or finance company) name
2. Type of loan (home, personal, vehicle, education, gold, consumer durable, other)
3. Original loan amount sanctioned
4. Interest rate, % per year
5. Is the rate fixed or floating?
6. Total tenure in months
7. EMI amount
8. Date of the first EMI
9. Number of EMIs paid so far
10. Principal outstanding, and the date it is as of
11. Day of the month the EMI is debited
12. Is part-payment (paying extra towards principal) allowed? Any conditions, such as a minimum amount or a lock-in period?
13. Part-payment or prepayment charge, as a %
14. Foreclosure (closing the loan early) charge, as a %
15. Any overdue amount, penalty or bounce charges shown

After the list, add one line saying whether the EMIs paid so far and the outstanding principal are consistent with the loan amount, rate and tenure.
""".trim()

/** Walks someone who is unsure of their loan's details through getting them in a few minutes. */
@Composable
fun LoanDetailsHelpDialog(onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Get your loan details in about 5 minutes") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Five minutes now gives me a clear view of this loan, so I can show exactly what is left, warn you before every EMI, and find ways for you to finish it sooner.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Step(1, "Open your latest loan statement, from your lender's app or the monthly email.")
                Step(2, "Take screenshots of its pages. Statements are usually password-protected PDFs, and screenshots get around that.")
                Step(3, "Open an AI app you trust (such as ChatGPT, Gemini or Claude), attach the screenshots, and paste the prompt below.")
                Step(4, "Come back here and fill in the boxes from its answer. They are in the same order.")
                Button(onClick = {
                    clipboard.setText(AnnotatedString(LOAN_DETAILS_PROMPT))
                    copied = true
                }) { Text(if (copied) "Prompt copied" else "Copy the prompt") }
                Text(
                    "Before you share: crop or cover your loan account number. The screenshots go to the AI app you choose, not to Fortun0. " +
                        "AI can misread figures, so check the important ones (rate, EMI, amount owed) against the statement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun Step(number: Int, text: String) {
    Row {
        Text("$number.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}
