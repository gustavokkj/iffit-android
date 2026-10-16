package com.ifpr.androidapptemplate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.Intent
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Inflates the production layouts without signing out or changing any Firebase data. */
@RunWith(AndroidJUnit4::class)
class LayoutSmokeTest {
    @Test fun allScreensInflateAndOptionalDocumentationImagesRender() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val capture = InstrumentationRegistry.getArguments().getString("generateScreenshots") == "true"
        val scenario = if (capture) ActivityScenario.launch<ScreenshotActivity>(
            Intent(instrumentation.targetContext, ScreenshotActivity::class.java)
        ) else null
        try {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_IFFIT)
            val screens = listOf(
                "login" to R.layout.activity_login,
                "cadastro" to R.layout.activity_cadastro_usuario,
                "inicio" to R.layout.fragment_home,
                "atividades" to R.layout.fragment_dashboard,
                "perfil" to R.layout.fragment_perfil_usuario,
                "notificacoes" to R.layout.fragment_notifications,
                "assistente" to R.layout.fragment_ai_logic
            )
            for ((name, layout) in screens) {
                lateinit var view: View
                instrumentation.runOnMainSync {
                    view = LayoutInflater.from(context).inflate(layout, null, false)
                    assertNotNull(view)
                    if (name == "login") assertNotNull(view.findViewById<MaterialButton>(R.id.btnGoogleSignIn).icon)
                    if (!capture) return@runOnMainSync
                    fun label(id: Int, value: String) { view.findViewById<TextView>(id)?.text = value }
                    when (name) {
                        "inicio" -> {
                            view.findViewById<Spinner>(R.id.modalidade).adapter = ArrayAdapter(
                                context, android.R.layout.simple_spinner_dropdown_item,
                                listOf("Corrida", "Caminhada", "Ciclismo")
                            )
                            label(R.id.txt_tempo, "00:28:40")
                            label(R.id.txt_distancia, "5,00 km")
                            label(R.id.txt_ritmo, "5:44 min/km")
                            label(R.id.txt_status, "Atividade pausada")
                            label(R.id.btn_iniciar, "Continuar")
                        }
                        "atividades" -> {
                            label(R.id.resumo, "Esta semana: 3 atividades • 12,40 km\nTotal: 8 atividades • 34,20 km")
                            val list = view.findViewById<LinearLayout>(R.id.lista)
                            for (item in listOf("Corrida • 15/10/2026\n5,00 km • 00:28:40", "Caminhada • 13/10/2026\n3,20 km • 00:40:00", "Corrida • 12/10/2026\n4,20 km • 00:26:10")) {
                                list.addView(MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                                    text = item
                                    isAllCaps = false
                                })
                            }
                        }
                        "perfil" -> {
                            label(R.id.nome, "Atleta IFFIT")
                            label(R.id.email, "atleta@example.com")
                            label(R.id.cidade, "Curitiba, PR")
                            label(R.id.modalidade, "Corrida e caminhada")
                            label(R.id.bio, "Um passo de cada vez.")
                            label(R.id.estatisticas, "8 atividades\n34,20 km percorridos\n03:48:20 em atividade")
                            view.findViewById<ImageView>(R.id.foto).setImageResource(R.drawable.ic_profile_black_24dp)
                        }
                        "notificacoes" -> label(R.id.conquistas, "Primeira atividade concluída!\n\n8 atividades registradas\n34,20 km acumulados\n\nMaior distância: 7,00 km")
                }
                }
                if (!capture) continue
                scenario!!.onActivity { it.setContentView(view) }
                instrumentation.waitForIdleSync()
                // Material fields animate floating labels after receiving fixture text.
                SystemClock.sleep(300)
                instrumentation.runOnMainSync {
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    view.draw(canvas)
                    val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots")
                    check(directory.mkdirs() || directory.isDirectory)
                    File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
        } finally { scenario?.close() }
    }
}
