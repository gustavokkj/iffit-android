package com.ifpr.androidapptemplate.ui.ai

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.databinding.FragmentAiLogicBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiLogicFragment : Fragment(R.layout.fragment_ai_logic) {
    private var binding: FragmentAiLogicBinding? = null
    private var selected: Uri? = null
    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selected = uri
        binding?.let { b ->
            b.bitmapImageView.visibility = if (uri == null) View.GONE else View.VISIBLE
            Glide.with(this).load(uri).into(b.bitmapImageView)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentAiLogicBinding.bind(view)
        binding = b
        b.btnBack.setOnClickListener { requireActivity().finish() }
        b.btnSelectImage.setOnClickListener { picker.launch("image/*") }
        b.btnGenerate.setOnClickListener {
            val question = b.promptInput.text.toString().trim()
            if (question.isBlank()) { b.promptInput.error = getString(R.string.ai_escreva_uma_pergunta); return@setOnClickListener }
            val uri = selected
            val appContext = requireContext().applicationContext
            b.btnGenerate.isEnabled = false
            b.btnSelectImage.isEnabled = false
            b.resultText.text = getString(R.string.ai_aguardando_resposta)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val bitmap: Bitmap? = if (uri == null) null else withContext(Dispatchers.IO) {
                        val request = Glide.with(appContext).asBitmap().load(uri).submit(1024, 1024)
                        try { request.get().copy(Bitmap.Config.ARGB_8888, false) }
                        finally { Glide.with(appContext).clear(request) }
                    }
                    val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                        modelName = getString(R.string.ai_model),
                        systemInstruction = content {
                            text(getString(R.string.ai_voce_e_o_assistente_esportivo_do_iffit))
                        }
                    )
                    try {
                        val response = model.generateContent(content {
                            bitmap?.let { image(it) }
                            text(question)
                        })
                        b.resultText.text = response.text ?: getString(R.string.ai_nenhuma_resposta_recebida)
                    } finally { bitmap?.recycle() }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) {
                    b.resultText.text = getString(R.string.ai_o_assistente_esta_indisponivel_confira_a_conexao)
                } finally {
                    b.btnGenerate.isEnabled = true
                    b.btnSelectImage.isEnabled = true
                }
            }
        }
    }
    override fun onDestroyView() {
        binding = null
        selected = null
        super.onDestroyView()
    }
}
