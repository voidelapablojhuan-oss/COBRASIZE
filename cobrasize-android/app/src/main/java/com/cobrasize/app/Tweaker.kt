package com.cobrasize.app

import android.content.Context

/**
 * O COBRASIZE não altera configurações protegidas do Android nem toca nos dados
 * ou na memória do Free Fire. Os perfis são guardados localmente pela interface.
 */
object Tweaker {
    fun apply(c: Context, sensi: Int, gpu: String, fps: Int): String {
        return "✅ Perfil salvo no COBRASIZE! Para mudar sensibilidade e gráficos, use as opções do próprio Free Fire. Nenhuma permissão ADB é necessária."
    }

    fun restore(c: Context) {
        // Não há alterações privilegiadas de sistema para restaurar.
    }
}
