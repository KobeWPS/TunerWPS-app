package com.wps.tuner

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.MenuItem
import android.view.View
import android.widget.TextView

class HelpActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Strangely disabled by default for API level 14+
        actionBar!!.setHomeButtonEnabled(true)
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.help)
        var tv: TextView
        val linkMovement = LinkMovementMethod.getInstance()
        for (i in HAS_LINKS.indices) {
            tv = findViewById<View>(HAS_LINKS[i]) as TextView
            tv.movementMethod = linkMovement
            tv.autoLinkMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private val HAS_LINKS = intArrayOf(
            R.id.changelog, R.id.faq, R.id.contact,
            R.id.open_source,  /*R.id.donate, */R.id.contact
        )
    }
}