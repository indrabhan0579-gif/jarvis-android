# Build JARVIS using only an Android phone

This project does not include a ready-to-install APK. The workflow in `.github/workflows/build-apk.yml` lets GitHub's hosted runner compile it, so you do not need a PC. You need a GitHub account and an internet connection.

## Steps
1. Extract this ZIP on your phone using Files by Google or another archive app.
2. In Chrome, open https://github.com/new and create a **Public** or **Private** repository named `jarvis-android-assistant`. Keep it empty (do not add a README or license).
3. Open the new repository, choose **Add file → Upload files**. In your phone's file picker, select the extracted project contents (including the hidden `.github` folder if your picker shows it). If selecting folders is not supported, use GitHub's browser in Desktop site mode and upload the project files/folders. Commit to `main`.
4. Open the repository's **Actions** tab. Select **Build JARVIS APK**, tap **Run workflow**, and run it on `main`. The first build may take several minutes.
5. Open the completed workflow run. Under **Artifacts**, download `jarvis-debug-apk`. Extract that downloaded ZIP; it contains `app-debug.apk`.
6. Open `app-debug.apk`. If Android blocks it, go to Settings → Apps → Special app access → Install unknown apps, allow your browser or file manager, then retry. Only install APKs you built and trust.
7. Launch JARVIS and grant only the permissions for features you want to use. Add your Gemini API key in the app's Settings if you want AI replies.

## Important notes
- The debug package uses application ID `com.jarvis.assistant.debug`.
- Do not put your Gemini API key in a public repository. The app's Settings supports entering it on-device.
- This workflow only compiles the project; it does not guarantee that every runtime feature or Gemini model configuration works. The source currently defaults to model name `gemini-3.8-flash`, which may need to be changed in the app's settings to a model currently supported by your Gemini API account.
- GitHub's mobile upload interface can be awkward for folders and hidden files. If the `.github/workflows/build-apk.yml` file is not uploaded, the Actions workflow will not appear.
