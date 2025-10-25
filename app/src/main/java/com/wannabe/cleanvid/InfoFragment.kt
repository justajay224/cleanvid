package com.wannabe.cleanvid

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController

class InfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Atur aksi saat panah kembali ditekan
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Hubungkan semua view dari layout
        val question1 = view.findViewById<TextView>(R.id.question1)
        val answer1 = view.findViewById<TextView>(R.id.answer1)
        val question2 = view.findViewById<TextView>(R.id.question2)
        val answer2 = view.findViewById<TextView>(R.id.answer2)
        val question3 = view.findViewById<TextView>(R.id.question3)
        val answer3 = view.findViewById<TextView>(R.id.answer3)

        // Atur OnClickListener untuk setiap pertanyaan
        question1.setOnClickListener {
            toggleAnswerVisibility(answer1, question1)
        }
        question2.setOnClickListener {
            toggleAnswerVisibility(answer2, question2)
        }
        question3.setOnClickListener {
            toggleAnswerVisibility(answer3, question3)
        }
    }

    private fun toggleAnswerVisibility(answerView: TextView, questionView: TextView) {
        if (answerView.visibility == View.VISIBLE) {
            answerView.visibility = View.GONE
            questionView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_add, 0)
        } else {
            answerView.visibility = View.VISIBLE
            questionView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0)
        }
    }
}