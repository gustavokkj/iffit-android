package com.ifpr.androidapptemplate.ui.usuario

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.ifpr.androidapptemplate.R


class CadastroUsuarioActivity  : AppCompatActivity() {
    private lateinit var registerNameEditText: EditText
    private lateinit var registerEmailEditText: EditText
    private lateinit var registerPasswordEditText: EditText
    private lateinit var registerConfirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var sairButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_usuario)

        // Inicializa o Firebase Auth
        auth = FirebaseAuth.getInstance()

        registerNameEditText = findViewById(R.id.registerNameEditText)
        registerEmailEditText = findViewById(R.id.registerEmailEditText)
        registerPasswordEditText = findViewById(R.id.registerPasswordEditText)
        registerConfirmPasswordEditText = findViewById(R.id.registerConfirmPasswordEditText)
        registerButton = findViewById(R.id.salvarButton)
        sairButton = findViewById(R.id.sairButton)

        registerButton.setOnClickListener {
            createAccount()
        }

        sairButton.setOnClickListener {
            finish()
        }
    }



    private fun createAccount() {
        val name = registerNameEditText.text.toString().trim()
        val email = registerEmailEditText.text.toString().trim()
        val password = registerPasswordEditText.text.toString()
        val confirmPassword = registerConfirmPasswordEditText.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.usuario_por_favor_preencha_todos_os_campos), Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, getString(R.string.usuario_a_senha_deve_ter_no_minimo_6), Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.usuario_as_senhas_nao_coincidem), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            getString(R.string.usuario_novo_usuario_cadastrado_com_sucesso),
                            Toast.LENGTH_SHORT
                        ).show()
                        val user = auth.currentUser
                        updateProfile(user, name)
                        sendEmailVerification(user)
                    } else {
                        val errorMessage = task.exception?.message ?: getString(R.string.usuario_erro_desconhecido)
                        Log.e("FirebaseAuth", "Erro ao cadastrar usuário: $errorMessage")
                        Toast.makeText(
                            this,
                            getString(R.string.signup_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirebaseAuth", getString(R.string.usuario_erro_ao_conectar_com_o_firebase), ex)
            Toast.makeText(
                this,
                getString(R.string.signup_failed),
                Toast.LENGTH_LONG
            ).show()
        }


    }

    private fun sendEmailVerification(user: FirebaseUser?) {
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(baseContext, getString(R.string.verification_sent, user.email.orEmpty()),
                        Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(baseContext, getString(R.string.verification_failed),
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateProfile(user: FirebaseUser?, displayName: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(baseContext, getString(R.string.profile_name_updated),
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(baseContext, getString(R.string.profile_name_failed),
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}
