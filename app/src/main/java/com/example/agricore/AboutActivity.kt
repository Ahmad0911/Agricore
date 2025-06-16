package com.example.agricore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class AboutActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AboutActivity"
        private const val SUPPORT_EMAIL = "support@agricore.com"
        private const val SUPPORT_PHONE = "+234-800-AGRICORE"
        private const val WEBSITE_URL = "https://www.agricore.com"
        private const val FACEBOOK_URL = "https://facebook.com/agricore"
        private const val TWITTER_URL = "https://twitter.com/agricore"
        private const val INSTAGRAM_URL = "https://instagram.com/agricore"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        try {
            setupToolbar()
            setupViews()
            setupClickListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showError("Failed to initialize about page")
        }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "About AgriCore"
        }
    }

    private fun setupViews() {
        // App info
        val appVersionText: TextView = findViewById(R.id.tv_app_version)
        val appDescriptionText: TextView = findViewById(R.id.tv_app_description)

        // Set app version (you can get this dynamically)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            appVersionText.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            appVersionText.text = "Version 1.0.0"
        }

        // Set app description
        appDescriptionText.text = """
            AgriCore is your comprehensive agricultural companion, designed to empower farmers and agricultural enthusiasts with the tools and knowledge they need to succeed.
            
            Our platform provides real-time weather updates, expert planting tips, and access to quality agricultural products - all in one convenient location.
            
            Whether you're a seasoned farmer or just starting your agricultural journey, AgriCore provides the insights and resources you need to make informed decisions and maximize your harvest.
        """.trimIndent()

        // Team info
        setupTeamInfo()
    }

    private fun setupTeamInfo() {
        val teamDescriptionText: TextView = findViewById(R.id.tv_team_description)
        teamDescriptionText.text = """
            AgriCore is developed by a passionate team of agricultural experts, software developers, and data scientists committed to revolutionizing farming through technology.
            
            Our mission is to make modern agricultural knowledge and tools accessible to farmers everywhere, regardless of their location or resources.
            
            We believe that by combining traditional farming wisdom with cutting-edge technology, we can help create a more sustainable and productive agricultural future.
        """.trimIndent()
    }

    private fun setupClickListeners() {
        // Contact buttons
        val emailButton: Button = findViewById(R.id.btn_email)
        val phoneButton: Button = findViewById(R.id.btn_phone)
        val websiteButton: Button = findViewById(R.id.btn_website)

        // Social media buttons
        val facebookButton: Button = findViewById(R.id.btn_facebook)
        val twitterButton: Button = findViewById(R.id.btn_twitter)
        val instagramButton: Button = findViewById(R.id.btn_instagram)

        // Contact click listeners
        emailButton.setOnClickListener {
            openEmail()
        }

        phoneButton.setOnClickListener {
            openPhoneDialer()
        }

        websiteButton.setOnClickListener {
            openWebsite()
        }

        // Social media click listeners
        facebookButton.setOnClickListener {
            openUrl(FACEBOOK_URL)
        }

        twitterButton.setOnClickListener {
            openUrl(TWITTER_URL)
        }

        instagramButton.setOnClickListener {
            openUrl(INSTAGRAM_URL)
        }

        // Additional info cards
        setupInfoCardListeners()
    }

    private fun setupInfoCardListeners() {
        val privacyCard: CardView = findViewById(R.id.card_privacy)
        val termsCard: CardView = findViewById(R.id.card_terms)
        val supportCard: CardView = findViewById(R.id.card_support)

        privacyCard.setOnClickListener {
            Toast.makeText(this, "Privacy Policy - Coming Soon", Toast.LENGTH_SHORT).show()
            // You can add navigation to privacy policy activity here
        }

        termsCard.setOnClickListener {
            Toast.makeText(this, "Terms of Service - Coming Soon", Toast.LENGTH_SHORT).show()
            // You can add navigation to terms activity here
        }

        supportCard.setOnClickListener {
            openEmail()
        }
    }

    private fun openEmail() {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$SUPPORT_EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, "AgriCore Support Request")
                putExtra(Intent.EXTRA_TEXT, "Hello AgriCore Team,\n\nI need assistance with...")
            }

            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(emailIntent)
            } else {
                showError("No email app found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening email", e)
            showError("Failed to open email")
        }
    }

    private fun openPhoneDialer() {
        try {
            val phoneIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$SUPPORT_PHONE")
            }

            if (phoneIntent.resolveActivity(packageManager) != null) {
                startActivity(phoneIntent)
            } else {
                showError("No phone app found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening phone dialer", e)
            showError("Failed to open phone dialer")
        }
    }

    private fun openWebsite() {
        openUrl(WEBSITE_URL)
    }

    private fun openUrl(url: String) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

            if (webIntent.resolveActivity(packageManager) != null) {
                startActivity(webIntent)
            } else {
                showError("No browser app found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL: $url", e)
            showError("Failed to open link")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}