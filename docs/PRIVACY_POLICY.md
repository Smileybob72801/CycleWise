# Privacy Policy

**Effective Date:** February 28, 2026

RhythmWise ("the app") is a privacy-first menstrual cycle tracker. This policy
explains what data the app handles and how it protects your information.

---

## 1. Data We Collect

RhythmWise stores the following data that **you** enter:

- Menstrual cycle start and end dates
- Flow intensity, color, and consistency
- Mood, energy, and libido ratings
- Symptoms and symptom severity
- Medications and dosage notes
- Water intake
- Custom tags and freeform notes

The app does **not** collect your name, email address, location, device
identifiers, or any other personal information beyond what you explicitly enter.

## 2. How Your Data Is Stored

All data is stored **locally on your device only**. The app uses SQLCipher to
encrypt the database with AES-256-GCM. Your passphrase is processed through
Argon2id key derivation (64 MB memory cost, 3 iterations) to produce the
encryption key. The encrypted database never leaves your device.

## 3. No Network Access

RhythmWise requests **zero internet permissions**. The app cannot connect to the
internet. There is no cloud sync, no remote backup, no telemetry, no analytics,
no crash reporting, and no third-party SDKs that transmit data.

## 4. Your Passphrase

Your passphrase never leaves your device and is never stored in plain text. It
is used solely to derive the encryption key in memory, and the key is zeroized
when you lock the app or the session ends.

**There is no passphrase recovery mechanism.** If you forget your passphrase,
your data cannot be recovered. This is by design — the same protection that
keeps others out means nobody (including the app developers) can unlock your
data without the correct passphrase.

## 5. Data Retention

Your data persists on your device until you choose to delete it. You can:

- Delete individual daily log entries from within the app
- Delete entire period records from within the app
- Uninstall the app to remove all data

The app does not automatically delete or expire your data.

## 6. Your Rights

Because all data is local to your device, you have complete control:

- **Access:** You can view all data you have entered at any time while the app
  is unlocked.
- **Deletion:** You can delete individual entries, entire period records, or all
  data by uninstalling the app.
- **Portability:** Your data is not transmitted anywhere, so there is no remote
  copy to request.

## 7. Children's Privacy

RhythmWise does not knowingly collect data from children under 13. The app does
not collect any identifying information from any user.

## 8. Medical Disclaimer

This app is not a medical device and does not provide medical advice, diagnosis,
or treatment. Cycle predictions and insights are based on statistical patterns
in your self-reported data and may not be accurate. Always consult a qualified
healthcare provider for medical decisions. Do not rely on this app for
contraception, fertility planning, or diagnosing any medical condition.

## 9. Changes to This Policy

If this policy is updated, the new version will be included in the app update.
The effective date at the top of this document will be revised accordingly.

## 10. Contact

RhythmWise is an open-source project. If you have questions about this privacy
policy, please open an issue on the project's GitHub repository.
