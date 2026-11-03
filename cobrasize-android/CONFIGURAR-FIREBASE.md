# Configurar Firebase e VIP do COBRASIZE

O app usa Firebase Authentication para contas e Cloud Functions para validar licenças VIP no servidor. O código VIP puro nunca é salvo no Firestore: somente o hash SHA-256.

## 1. Criar o projeto Firebase
1. Entre em https://console.firebase.google.com/ e crie um projeto.
2. Adicione um aplicativo Web e copie a configuração `firebaseConfig`.
3. Em **Authentication > Sign-in method**, habilite **E-mail/senha** e **Anônimo**.
4. Em **Firestore Database**, crie o banco de dados.

## 2. Configurar o app
No arquivo `cobrasize-android/app/src/main/assets/index.html`, procure `window.COBRASIZE_FIREBASE_CONFIG` e substitua os valores de exemplo (`COLOQUE_SUA_API_KEY`, `SEU_PROJETO` e `SEU_APP_ID`) pelos valores fornecidos pelo Firebase.

A configuração web do Firebase identifica o projeto; ela não é uma senha privada. Nunca coloque chaves de service account ou credenciais administrativas no APK ou no GitHub.

## 3. Publicar o servidor VIP
No computador, instale Node.js 20 ou superior e Firebase CLI. Na pasta raiz do repositório:

```bash
npm install -g firebase-tools
firebase login
firebase use --add
firebase deploy --only functions,firestore:rules
```

Escolha o mesmo projeto Firebase usado na configuração do app. A publicação de Cloud Functions pode exigir que o projeto esteja no plano Blaze e tenha faturamento configurado no Firebase/Google Cloud.

As funções publicadas são:
- `validateVipCode`: confere o código no servidor e vincula a licença à conta autenticada.
- `getVipStatus`: consulta o estado da licença ao abrir o app.

## 4. Criar códigos VIP
Depois de publicar, entre na pasta `functions`, instale as dependências e autentique o SDK Admin com credenciais administrativas locais (Application Default Credentials). Nunca envie essas credenciais ao app nem as publique no repositório.

```bash
cd functions
npm install
gcloud auth application-default login
```

Crie um código longo e aleatório, com pelo menos 10 caracteres. Para validade ilimitada:

```bash
GCLOUD_PROJECT=SEU_PROJECT_ID node scripts/create-license.js SEU-CODIGO-VIP never
```

Para uma licença com data de expiração:

```bash
GCLOUD_PROJECT=SEU_PROJECT_ID node scripts/create-license.js SEU-CODIGO-VIP 2027-12-31T23:59:59Z
```

Entregue o código ao usuário por um canal privado. O Firestore guarda somente o hash do código. Cada licença pode ser vinculada a uma conta Firebase; outra conta não poderá reutilizá-la. Para revogar, altere o documento correspondente em `licenses` para `active: false` pelo console ou por ferramenta administrativa confiável.

## 5. O que já está implementado
- Login por e-mail e senha.
- Login anônimo para convidados autenticados pelo Firebase.
- Sessão Firebase persistente no aparelho.
- Validação de código VIP por Cloud Function.
- Vinculação de cada licença a uma conta.
- Consulta do status VIP ao entrar no app.
- Regras Firestore que bloqueiam leitura e escrita direta das coleções de licenças.

## 6. Limites importantes
- O modo convidado local, sem Firebase, não pode validar VIP online.
- O app só libera o VIP quando o servidor confirma a licença.
- O código não deve ser considerado ativo até configurar o Firebase, publicar as funções e cadastrar uma licença.
- Antes de distribuir publicamente, configure também Firebase App Check e monitore uso/faturamento das funções.
