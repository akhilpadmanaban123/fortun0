package app.paisapilot.feature.safety

data class SafetyTip(val title: String, val howItWorks: String, val whatToDo: List<String>)

/** The frauds people in India are most often hit by, and how to shut each one down. */
val safetyTips = listOf(
    SafetyTip(
        title = "\"Share the OTP\" calls",
        howItWorks = "Someone claiming to be from your bank, a courier or customer care says they need an OTP to verify you, unblock your card or cancel an order.",
        whatToDo = listOf(
            "An OTP is a signature. Nobody genuine will ever ask you to read it out.",
            "Hang up and call the number printed on the back of your card.",
            "Read the OTP message fully: it says what the OTP is actually for.",
        ),
    ),
    SafetyTip(
        title = "UPI \"collect\" and QR code tricks",
        howItWorks = "A buyer or \"refund agent\" sends a collect request or a QR code and says you must approve it or scan it to receive money.",
        whatToDo = listOf(
            "You never enter your UPI PIN to receive money. PIN means money is leaving.",
            "Scanning a QR code is for paying, not for getting paid.",
            "Decline collect requests from anyone you did not expect.",
        ),
    ),
    SafetyTip(
        title = "\"Digital arrest\" and fake police calls",
        howItWorks = "A video call from someone in uniform says a parcel, SIM or bank account in your name is linked to a crime, and that you must stay on the call and transfer money \"for verification\".",
        whatToDo = listOf(
            "There is no such thing as a digital arrest. Police do not interrogate or take money over video calls.",
            "Disconnect, tell a family member, and call 1930.",
            "Never move money to a \"safe account\" someone on a call gives you.",
        ),
    ),
    SafetyTip(
        title = "Screen-sharing and remote access apps",
        howItWorks = "A caller helping with KYC, a refund or a stuck payment asks you to install an app like AnyDesk, TeamViewer or a \"support\" APK. They then watch your screen and read your OTPs.",
        whatToDo = listOf(
            "Never install an app because a caller asked you to.",
            "Never share your screen while a banking or UPI app is open.",
            "Install apps only from the Play Store, not from links or WhatsApp files.",
        ),
    ),
    SafetyTip(
        title = "KYC, electricity and SIM \"will be blocked today\" messages",
        howItWorks = "An SMS or WhatsApp message says your account, power connection or SIM will be cut tonight unless you call a number or tap a link.",
        whatToDo = listOf(
            "Urgency is the trick. Real providers give notice in writing and inside their own app.",
            "Do not tap the link or call the number in the message.",
            "Check in the official app or at a branch.",
        ),
    ),
    SafetyTip(
        title = "Instant loan apps",
        howItWorks = "Apps offering loans in minutes with no paperwork take access to your contacts and photos, charge huge hidden fees, then harass you and your contacts.",
        whatToDo = listOf(
            "Borrow only from RBI-regulated banks and NBFCs. Check the lender's name on the RBI website.",
            "Refuse apps that demand access to contacts, gallery or SMS for a loan.",
            "Never pay an upfront \"processing fee\" to get a loan.",
        ),
    ),
    SafetyTip(
        title = "Investment and task scams",
        howItWorks = "A Telegram or WhatsApp group promises guaranteed daily returns, or pays you small amounts for liking videos, then asks for bigger \"deposits\" you can never withdraw.",
        whatToDo = listOf(
            "Guaranteed high returns do not exist. That promise alone is the red flag.",
            "Invest only through SEBI-registered brokers and well-known apps.",
            "If you must pay to withdraw your own money, it is a scam. Stop paying.",
        ),
    ),
    SafetyTip(
        title = "Fake customer care numbers",
        howItWorks = "You search online for a helpline and the top result is a fraudster's number, who then asks for card details or sends a payment link.",
        whatToDo = listOf(
            "Take helpline numbers only from the official app, website or the back of your card.",
            "Customer care never needs your PIN, CVV, full card number or OTP.",
        ),
    ),
    SafetyTip(
        title = "Everyday habits that protect you",
        howItWorks = "Most fraud succeeds because of one rushed moment. A few settings make that moment far less costly.",
        whatToDo = listOf(
            "Set low daily limits for UPI and cards, and raise them only when needed.",
            "Turn off international and contactless use on cards you do not use that way.",
            "Use a screen lock, and lock your SIM with a PIN.",
            "Turn on SMS and email alerts for every transaction.",
            "Check your credit report once a year for loans you did not take.",
        ),
    ),
)
