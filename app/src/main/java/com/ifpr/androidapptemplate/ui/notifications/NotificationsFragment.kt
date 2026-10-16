package com.ifpr.androidapptemplate.ui.notifications

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.ifpr.androidapptemplate.R
import com.ifpr.androidapptemplate.baseclasses.Metricas
import com.ifpr.androidapptemplate.data.AtividadesRepository
import com.ifpr.androidapptemplate.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {
    private var binding: FragmentNotificationsBinding? = null
    private var cancel: (() -> Unit)? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { renderPermission() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentNotificationsBinding.bind(view)
        binding!!.permitir.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) &&
                !requireContext().getSharedPreferences("iffit", 0).getBoolean("notificationAsked", false)) {
                requireContext().getSharedPreferences("iffit", 0).edit().putBoolean("notificationAsked", true).apply()
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                val settings = if (Build.VERSION.SDK_INT >= 26) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${requireContext().packageName}"))
                }
                startActivity(settings)
            }
        }
    }
    override fun onResume() { super.onResume(); renderPermission() }
    private fun renderPermission() {
        val enabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        binding?.permissao?.text = if (enabled) getString(R.string.notifications_notificacoes_do_iffit_ativadas) else getString(R.string.notifications_notificacoes_desativadas_neste_dispositivo)
        binding?.permitir?.text = if (enabled) getString(R.string.notifications_configurar_notificacoes) else getString(R.string.notifications_ativar_notificacoes)
    }
    override fun onStart() {
        super.onStart()
        cancel = AtividadesRepository(requireContext()).observar { items, error ->
            val activities = items.orEmpty()
            if (binding == null) return@observar
            binding?.conquistas?.text = error ?: if (activities.isEmpty())
                getString(R.string.notifications_seu_primeiro_marco_comeca_com_uma_atividade)
            else getString(R.string.milestones, activities.size,
                Metricas.distancia(activities.sumOf { it.distanciaMetros }),
                Metricas.distancia(activities.maxOf { it.distanciaMetros }))
        }
    }
    override fun onStop() { cancel?.invoke(); cancel = null; super.onStop() }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
