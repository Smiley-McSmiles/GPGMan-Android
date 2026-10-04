package com.example.data

/**
 * Configure your donation buttons and clipboard text here.
 * Changes to labels and clipboard values in this file take effect directly.
 * Users cannot edit these in the app.
 */
object DonationConfig {

    // === 1. BUTTON LABELS (Text displayed on the buttons) ===
    const val OPTION_1_LABEL = "BTC"
    const val OPTION_2_LABEL = "XMR"
    const val OPTION_3_LABEL = "CashApp"

    // === 2. CLIPBOARD TEXT (Text copied to clipboard when clicked) ===
    // Option-1: e.g. Bitcoin / Monero address, custom link, or hash
    const val OPTION_1_CLIPBOARD_TEXT = "bc1qy2gtdhnfxp9dcs6v9jda748npmsjx3jgwp99mx"

    // Option-2: e.g. Ethereum address, custom link, or hash
    const val OPTION_2_CLIPBOARD_TEXT = "82xtMVSmesuLjPtgHfBCEhM5Fpqh1SLLNf9pzHRRNPqQZsvrnmoM1ZGC7AiLyPfsufdyrMWHrWYV2hsC8jc5rEBVLMHWTLy"

    // Option-3: e.g. Sponsor link, custom URL, or hash
    const val OPTION_3_CLIPBOARD_TEXT = "\$SmileyMcSmiles"
}
