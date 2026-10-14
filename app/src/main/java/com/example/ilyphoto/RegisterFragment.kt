package com.example.ilyphoto

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ilyphoto.data.ServiceLocator
import com.example.ilyphoto.data.toUserMessage
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firstNameInput = view.findViewById<EditText>(R.id.firstNameInput)
        val surnameInput = view.findViewById<EditText>(R.id.surnameInput)
        val emailInput = view.findViewById<EditText>(R.id.registerEmailInput)
        val passwordInput = view.findViewById<EditText>(R.id.registerPasswordInput)
        val signUpButton = view.findViewById<TextView>(R.id.signUpButton)
        val loginTab = view.findViewById<TextView>(R.id.loginTab)
        val backToLogin = view.findViewById<TextView>(R.id.backToLogin)

        loginTab?.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        backToLogin?.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        signUpButton?.setOnClickListener {
            val firstName = firstNameInput?.text?.toString()?.trim().orEmpty()
            val surname = surnameInput?.text?.toString()?.trim().orEmpty()
            val email = emailInput?.text?.toString()?.trim().orEmpty()
            val password = passwordInput?.text?.toString().orEmpty().ifEmpty { "Password123" }

            if (firstName.isEmpty() || surname.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your first name and surname", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fullName = "$firstName $surname"
            signUpButton.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                val result = ServiceLocator.auth.register(
                    displayName = fullName,
                    email = email,
                    password = password,
                    username = email.substringBefore('@'),
                    phone = "",
                    isPhotographer = false
                )
                signUpButton.isEnabled = true
                result.onSuccess {
                    Toast.makeText(requireContext(), "Account created successfully for $fullName!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                }.onFailure { err ->
                    Toast.makeText(requireContext(), err.toUserMessage(), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
