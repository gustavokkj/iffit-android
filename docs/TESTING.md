# Verificação do IFFIT

## Resultado da preparação da versão 0.1.0

Verificado em 16/10/2026 com JDK 21 e emulador Android API 37:

- Builds debug e release: concluídos. O APK de release gerado é sem assinatura de distribuição.
- Testes unitários: 8 aprovados.
- Testes instrumentados: 3 aprovados, incluindo abertura dos sete layouts.
- Regras do Realtime Database emulado: 5 testes aprovados.
- Lint: 0 erros e 35 avisos, incluindo dependências antigas, plurais e gerenciamento de tokens FCM ainda não implementado.
- Abertura do app e renderização das telas: conferidas no emulador.
- Capturas: dados fictícios; tela auxiliar restrita ao build debug, ausente do release.

Continuam pendentes os testes de GPS em celular físico, autenticação completa com Google, IA e entrega de push no Firebase configurado. O isolamento entre contas foi testado no banco emulado, não com duas contas reais.

## Testes automatizados

Na raiz do repositório, com o JDK do Gradle configurado:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

O segundo comando exige um dispositivo ou emulador conectado. Use um emulador de testes: o Gradle pode reinstalar/remover o aplicativo e apagar sua sessão local ao executar testes instrumentados. Em Linux/macOS, substitua `.\gradlew.bat` por `./gradlew`.

- `AtividadeTest`: permite salvar tempo com 0 km; rejeita duração vazia e distâncias inválidas.
- `MetricasTest`: formatação de duração e ritmo, arredondamento e ausência de distância.
- `RegistroStateTest`: preserva identificação, usuário e distância ao pausar/continuar; restaura a atividade pausada; descarta o estado.
- `LayoutSmokeTest`: abre os sete layouts principais com o tema real e verifica o ícone do botão Google. Não faz login nem grava dados no Firebase.

## Regras do Firebase

Os testes usam um Realtime Database local, duas identidades fictícias e o projeto `demo-iffit`. Não exigem login no Firebase CLI.

Requisitos: Node.js 22 ou 24 e Java 21 no PATH. Dentro de `firebase/`:

```powershell
npm ci
npm test
```

São verificados: salvar e ler o próprio registro; bloquear outra conta; bloquear acesso anônimo; rejeitar campos inválidos; atualizar o próprio perfil sem apagar campos antigos.

Isso valida o arquivo local `database.rules.example.json`. Não comprova que as mesmas regras estão publicadas no seu projeto Firebase.

## Imagens do README

As imagens são renderizações dos layouts de produção no emulador, preenchidas com dados fictícios. Não são capturas de uma conta real nem comprovam integração com GPS ou Firebase.

Para regenerar:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.ifpr.androidapptemplate.LayoutSmokeTest -e generateScreenshots true com.ifpr.androidapptemplate.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.ifpr.androidapptemplate/files/screenshots docs/
```

## Roteiro manual

| Fluxo | O que conferir |
| --- | --- |
| Cadastro | Campos vazios, senhas diferentes, criação da conta e mensagem de verificação |
| Login | E-mail/senha, senha incorreta, Google e retorno com sessão existente |
| GPS físico | Permissão negada/aproximada/precisa, deslocamento real e GPS desligado |
| Registro | Iniciar, pausar, continuar, descartar e salvar; conferir tempo e distância |
| Sem distância | Salvar após dois minutos parado, confirmando o aviso de 0 km |
| Ciclo de vida | Trocar de aba, girar a tela e bloquear o aparelho; registro deve pausar |
| Histórico | Conferir detalhes, totais e atividades da semana após salvar |
| Perfil | Atualizar campos e conferir persistência ao reabrir |
| Duas contas reais | Sair de uma conta, entrar em outra e conferir histórico e perfil separados |
| Rede indisponível | Conferir mensagem de salvamento pendente; restaurar rede e aguardar confirmação |
| Assistente | Texto, imagem opcional, permissão de seleção e falha de conexão |
| Notificações | Permitir/negar notificações, enviar mensagem pelo Firebase e abrir a notificação |

O salvamento no Firebase real foi confirmado pelo autor após ajustar as regras. Os testes automatizados não substituem a validação do GPS em celular físico, do login Google, da IA e da entrega de push no projeto configurado.
