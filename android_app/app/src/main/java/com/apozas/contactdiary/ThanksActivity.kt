package com.apozas.contactdiary

/*
    This file is part of Contact Diary.
    Contact Diary is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    Contact Diary is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with Contact Diary. If not, see <http://www.gnu.org/licenses/>.
    Copyright 2020 by Alex Pozas-Kerstjens (apozas)
*/

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ThanksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thanks)

        val homeScreen = findViewById<TextView>(R.id.thanks_homescreen)
        homeScreen.movementMethod = LinkMovementMethod.getInstance()

        val chinese = findViewById<TextView>(R.id.thanks_chinese)
        chinese.movementMethod = LinkMovementMethod.getInstance()

        val dutch = findViewById<TextView>(R.id.thanks_dutch)
        dutch.movementMethod = LinkMovementMethod.getInstance()

        val finnish = findViewById<TextView>(R.id.thanks_finnish)
        finnish.movementMethod = LinkMovementMethod.getInstance()

        val french = findViewById<TextView>(R.id.thanks_french)
        french.movementMethod = LinkMovementMethod.getInstance()

        val german = findViewById<TextView>(R.id.thanks_german)
        german.movementMethod = LinkMovementMethod.getInstance()

        val indonesian = findViewById<TextView>(R.id.thanks_indonesian)
        indonesian.movementMethod = LinkMovementMethod.getInstance()

        val italian = findViewById<TextView>(R.id.thanks_italian)
        italian.movementMethod = LinkMovementMethod.getInstance()

        val polish = findViewById<TextView>(R.id.thanks_polish)
        polish.movementMethod = LinkMovementMethod.getInstance()

        val portuguese = findViewById<TextView>(R.id.thanks_portuguese)
        portuguese.movementMethod = LinkMovementMethod.getInstance()

        val swedish = findViewById<TextView>(R.id.thanks_swedish)
        swedish.movementMethod = LinkMovementMethod.getInstance()
    }
}