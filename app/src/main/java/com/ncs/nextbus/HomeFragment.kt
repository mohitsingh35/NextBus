package com.ncs.nextbus

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat.recreate
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ncs.nextbus.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale


@AndroidEntryPoint
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: FrontScreenViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        val suggestedBusStops = ArrayList<String>()
        val suggestedBusNumbers = ArrayList<String>()

        val busStopsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            suggestedBusStops
        )
        val list = ArrayList<RealtimeDB>()
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentLanguage = sharedPreferences.getString("language", "en")
        setAppLanguage(currentLanguage!!)
        binding.langcardview.setOnClickListener {
            val newLanguage = if (currentLanguage == "en") "hi" else "en"
            sharedPreferences.edit().putString("language", newLanguage).apply()
            setAppLanguage(newLanguage)
            recreate(requireActivity())
        }
            viewModel = ViewModelProvider(this).get(FrontScreenViewModel::class.java)
            viewModel.res.observe(viewLifecycleOwner, Observer { locationState ->
                if (locationState.item.isNotEmpty()) {
                    for (i in 0 until locationState.item.size) {
                        suggestedBusStops.add(locationState.item[i].item?.destination!!)
                        suggestedBusStops.add(locationState.item[i].item?.start!!)
                    }
                }
            })
            suggestedBusStops.distinct()
            viewModel.res.observe(viewLifecycleOwner, Observer { locationState ->
                if (locationState.item.isNotEmpty()) {
                    for (i in 0 until locationState.item.size) {
                        suggestedBusNumbers.add(locationState.item[i].item?.busNum!!)
                    }
                }
            })
            suggestedBusStops.distinct()

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
            val busdetailsvianum = mutableStateOf<RealtimeDB?>(null)


            binding.destination.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val departure = binding.start.text.toString()
                    val destination = binding.destination.text.toString()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.putExtra("departure", departure)
                    intent.putExtra("destination", destination)
                    requireContext().startActivity(intent)
                    return@setOnEditorActionListener true
                }
                false
            }

            binding.swap.setOnClickListener {
                val temp = binding.start.text
                binding.start.text = binding.destination.text
                binding.destination.text = temp
            }

            binding.searchByNum.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val busNum = binding.searchByNum.text.toString()
                    viewModel.res.observe(viewLifecycleOwner, Observer { locationState ->
                        for (i in 0 until locationState.item.size) {
                            if (locationState.item[i].item?.busNum == busNum) {
                                busdetailsvianum.value = locationState.item[i]
                            }
                        }
                    })
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.putExtra("busdetails", busdetailsvianum.value)
                    intent.putExtra("busnum", busNum)
                    requireContext().startActivity(intent)

                }
                false
            }
            return binding.root
        }
    private fun setAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
