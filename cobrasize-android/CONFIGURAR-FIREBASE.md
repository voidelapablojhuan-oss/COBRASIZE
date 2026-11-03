# Configurar login do COBRASIZE

O projeto recebeu a tela de login, criação de conta, sessão persistente e modo convidado.

## 1. Criar o projeto Firebase
1. Entre em https://console.firebase.google.com/ e crie um projeto.
2. Adicione um aplicativo Web e copie a configuração `firebaseConfig`.
3. Em **Authentication > Sign-in method**, habilite **E-mail/senha** e **Anônimo**.
4. Em **Authentication > Settings > Authorized domains**, confira os domínios usados caso publique uma versão web.

## 2. Colar a configuração
No arquivo `app/src/main/assets/index.html`, procure `window.COBRASIZE_FIREBASE_CONFIG` e substitua os valores de exemplo (`COLOQUE_SUA_API_KEY`, `SEU_PROJETO` e `SEU_APP_ID`) pelos valores fornecidos pelo Firebase.

A configuração web do Firebase identifica o projeto; ela não é uma senha privada. Nunca coloque chaves de service account ou credenciais administrativas no APK.

## 3. O que funciona
- Criar conta por e-mail e senha.
- Entrar em uma conta existente.
- Sessão Firebase persistente no aparelho.
- Entrar anonimamente pelo Firebase quando configurado.
- Modo convidado local para testar antes de configurar o Firebase.

## 4. VIP
A validação do código VIP local foi removida porque um código embutido no APK pode ser extraído. A caixa VIP informa que a validação online ainda precisa de um endpoint seguro (por exemplo, Firebase Cloud Function) e uma base de licenças. Não habilite VIP apenas com uma comparação feita no JavaScript do aparelho.

## 5. Observações
O modo convidado sem Firebase salva apenas a escolha neste aparelho e não sincroniza dados. Para contas reais, configure o Firebase antes de distribuir o APK. Revise também as regras de segurança do banco de dados antes de armazenar configurações dos usuários.
