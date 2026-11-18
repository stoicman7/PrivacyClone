# Import PrivacyClone into your GitHub repository

1. Create an empty GitHub repository named `PrivacyClone`.
2. Upload `PrivacyClone-source.zip` to the repository root using **Add file → Upload files**.
3. Open the repository in GitHub Codespaces.
4. In the Codespaces terminal, run:

```bash
unzip PrivacyClone-source.zip -d .
rm PrivacyClone-source.zip

git add .
git commit -m "Add PrivacyClone Android prototype"
git push
```

5. Open the repository's **Actions** tab. The workflow named **Build PrivacyClone APK** should start after the push.
6. Download the `PrivacyClone-debug-apk` artifact from the successful workflow run.

The zip is structured so its contents extract directly into the repository root.
