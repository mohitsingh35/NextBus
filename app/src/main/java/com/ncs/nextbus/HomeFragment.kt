package com.ncs.nextbus

import android.R
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.ncs.nextbus.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val suggestedBusStops = arrayOf("Labour Chowk", "Noida", "Delhi", "Sector 62")
        val suggestedBusNumbers = arrayOf("UP13BQ0001", "UP13BM0002", "UP13CQ1233")

        val busStopsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            suggestedBusStops
        )

        val busNumbersAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            suggestedBusNumbers
        )

        binding.start.setAdapter(busStopsAdapter)
        binding.start.threshold = 1

        binding.destination.setAdapter(busStopsAdapter)
        binding.destination.threshold = 1

        binding.searchByNum.setAdapter(busNumbersAdapter)
        binding.searchByNum.threshold = 1

        binding.imageView4.setOnClickListener {
            requireContext().startActivity(Intent(requireContext(), MainActivity::class.java))
        }
        binding.start.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.destination.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        binding.destination.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val departure=binding.start.text.toString()
                val destination=binding.destination.text.toString()
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.putExtra("departure", departure)
                intent.putExtra("destination", destination)
                requireContext().startActivity(intent)
                return@setOnEditorActionListener true
            }
            false
        }

        binding.swap.setOnClickListener{
            val temp=binding.start.text
            binding.start.text=binding.destination.text
            binding.destination.text=temp
        }
        binding.searchByNum.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val busNum=binding.searchByNum.text.toString()
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.putExtra("busNum", busNum)
                requireContext().startActivity(intent)
                return@setOnEditorActionListener true
            }
            false
        }

        return binding.root
    }
}
