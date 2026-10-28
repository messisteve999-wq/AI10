# AI10 — assistant de messagerie Android

Application Android Kotlin avec interface noire et accents or, icône de couronne, configuration Gemini, fenêtre flottante de suggestions (SugesZap) et réponse automatique (AutoZap) par actions de réponse des notifications.

## Limites importantes
- L’accès aux notifications et l’autorisation d’affichage par-dessus les autres applications doivent être activés manuellement dans Android.
- La réponse automatique ne fonctionne que si l’application de messagerie expose une action `RemoteInput` compatible dans la notification.
- AI10 ne peut pas accepter les demandes d’amis ni envoyer un message dans une application qui ne fournit pas cette action. Les contacts sont autorisés à partir des notifications reçues.
- Maximum de quatre contacts autorisés par identifiant d’application. AutoZap est désactivé par défaut pour chaque contact.
- Pour utiliser Gemini, configure une clé API depuis Google AI Studio et ne la partage pas.

## Compiler
Avec Java 17, Android SDK 35 et Gradle 8.9 :

```bash
gradle --no-daemon assembleDebug
```

APK attendu : `app/build/outputs/apk/debug/app-debug.apk`.
Le workflow GitHub Actions construit et publie l’APK en tant qu’artefact après chaque push sur `main` ou lancement manuel.
