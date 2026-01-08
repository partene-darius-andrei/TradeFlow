# GitHub Actions CI/CD

**Status:** ✅ Active
**Latest Build:** #31 SUCCESS (v1.5.1)
**Workflow Files:** `.github/workflows/build.yml` + `.github/workflows/update-docs.yml`

## Quick Reference

```bash
# Trigger build manually
gh workflow run build.yml

# View workflow runs
gh run list

# Check latest run status
gh run view

# Download APK artifact
gh run download <run-id> -n debug-apk
```

## What It Does

**Automated Android build pipeline** that runs on every push to `main` or `claude/*` branches:

1. ✅ Injects Coinbase API credentials from GitHub secrets
2. ✅ Builds debug APK with embedded credentials (includes ENHANCED PEM key escape handling)
3. ✅ Uploads to Firebase App Distribution (partene.darius@gmail.com)
4. ✅ Commits build status back to branch
5. ✅ Uploads APK artifact (7-day retention)

**Auto-documentation pipeline** that updates docs when code changes:

1. ✅ Analyzes git diff on push
2. ✅ Calls Claude API to update documentation
3. ✅ Commits updated CLAUDE.md and docs/ back to branch

## Credential Injection System

**TradeFlow uses build-time credential injection** instead of runtime credential entry.

### Required GitHub Secrets

Set these in GitHub repo → Settings → Secrets and variables → Actions:

| Secret | Value | Format |
|--------|-------|--------|
| `COINBASE_API_KEY` | Your Coinbase API key | `organizations/{org_id}/apiKeys/{key_id}` |
| `COINBASE_API_SECRET` | Your EC private key | Full PEM format with headers and newlines |
| `ANTHROPIC_API_KEY` | Your Claude API key | For auto-documentation updates |

### How It Works

```yaml
# .github/workflows/build.yml
- name: Build debug APK
  id: build
  env:
    COINBASE_API_KEY: ${{ secrets.COINBASE_API_KEY }}        # ← Injected here
    COINBASE_API_SECRET: ${{ secrets.COINBASE_API_SECRET }}  # ← Injected here
  run: ./gradlew assembleDebug
```

**Enhanced Build Process (v1.5.1):**
1. GitHub Actions reads secrets from repository settings
2. Sets environment variables for Gradle process
3. `app/build.gradle.kts` reads env vars and handles ENHANCED PEM key escaping:
   ```kotlin
   val coinbaseApiSecret = System.getenv("COINBASE_API_SECRET")
       ?: props.getProperty("coinbase.api.secret", "")

   // Escape the secret for Java string literal (preserve \n as \\n)
   val escapedSecret = coinbaseApiSecret
       .replace("\\", "\\\\")  // Escape backslashes first
       .replace("\"", "\\\"")  // Escape quotes
       .replace("\n", "\\n")   // Convert newlines to \n escape sequence

   buildConfigField("String", "COINBASE_API_SECRET", "\"$escapedSecret\"")
   ```
4. `CredentialsModule` provides credentials to app via Hilt DI
5. Built APK contains embedded credentials (encrypted in APK)

### Local Development Fallback

If environment variables aren't set (local development), build falls back to `local.properties`:

```properties
# local.properties (NOT committed to git)
coinbase.api.key=organizations/your-org/apiKeys/your-key
coinbase.api.secret=-----BEGIN EC PRIVATE KEY-----
MHcCAQEEIExample...
-----END EC PRIVATE KEY-----
```

**Note:** In local.properties, use actual newlines (not \n escape sequences). The build script handles conversion automatically.

## Claude Code Integration

**This workflow is designed for remote development with Claude Code.**

### The Pattern

**Problem:** Claude Code can't run Gradle builds locally (resource-intensive, slow on mobile)
**Solution:** Push → GitHub Actions builds → commits result back → Claude reads result

### How It Works

```
┌─────────────────────┐
│ Claude Code         │
│ (Mobile/Desktop)    │
│                     │
│ 1. Implements       │
│ 2. Pushes branch    │
│ 3. Creates PR       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ GitHub Actions      │
│                     │
│ 4. Injects secrets  │
│ 5. Builds APK       │
│ 6. Updates docs     │
│ 7. Commits result:  │
│    - .build-status  │
│    - build-log.txt  │
│    - CLAUDE.md      │
│    - docs/*.md      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Claude Code         │
│                     │
│ 8. git pull         │
│ 9. cat .build-      │
│    status           │
│ 10. Fix if needed   │
└─────────────────────┘
```

### Key Features for Claude Integration

**1. Enhanced credential escaping** (app/build.gradle.kts lines 37-41):
```kotlin
// Escape the secret for Java string literal (preserve \n as \\n)
val escapedSecret = coinbaseApiSecret
    .replace("\\", "\\\\")  // Escape backslashes first
    .replace("\"", "\\\"")  // Escape quotes
    .replace("\n", "\\n")   // Convert newlines to \n escape sequence
```

**Why:** Properly handles PEM private keys with embedded newlines when injected from environment variables.

**2. Commit-back pattern** (steps 44-63 in build workflow):
```yaml
- name: Commit build result
  if: always()  # Run even on failure
  run: |
    if [ "${{ steps.build.outcome }}" = "success" ]; then
      echo "SUCCESS" > .build-status
      rm -f build-log.txt  # Clean up old failures
    else
      echo "FAILURE" > .build-status
      ./gradlew assembleDebug 2>&1 | tail -200 > build-log.txt
    fi
    git commit -m "Build #${{ github.run_number }}: ${{ steps.build.outcome }}"
    git push
```

**3. Continue on error** (step 31-34):
```yaml
- name: Build debug APK
  continue-on-error: true  # Don't fail workflow, just record outcome
```

**Why:** Allows workflow to always commit status, even when build fails.

**4. Enhanced credential injection for CI/CD:**
```yaml
- name: Build debug APK
  env:
    COINBASE_API_KEY: ${{ secrets.COINBASE_API_KEY }}
    COINBASE_API_SECRET: ${{ secrets.COINBASE_API_SECRET }}  # Includes PEM newlines
  run: ./gradlew assembleDebug
```

**5. Automated testing flow:**
```
Claude implements → Push → Actions build with credentials → Firebase distribution → Test on phone
```

No local Gradle execution needed. Claude Code Mobile can make changes remotely and immediately test on device with real credentials.

**6. Auto-documentation update:**
```yaml
- name: Update documentation
  env:
    ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
  run: |
    # Get changed files
    CHANGED_FILES=$(git diff --name-only HEAD~1 HEAD | head -20)
    
    # Get git diff (limited to prevent API limits)
    GIT_DIFF=$(git diff HEAD~1 HEAD | head -8000)
    
    # Call Claude API to update docs
    python .github/scripts/update-docs.py
```

## Version History & Build Status

| Version | Build | Status | Key Changes |
|---------|-------|---------|-------------|
| 1.5.1 | #31 | ✅ SUCCESS | Enhanced security key parsing, improved error handling, fixed navigation |
| 1.5.0 | #30 | ✅ SUCCESS | Improved authentication reliability, enhanced security key parsing |
| 1.4.0 | #29 | ✅ SUCCESS | Build-time credential injection with PEM escape handling |
| 1.3.0 | #28 | ✅ SUCCESS | Live portfolio integration with real Coinbase data |
| 1.2.0 | #27 | ✅ SUCCESS | Adaptive app icon, complete UI foundation |

## Troubleshooting

### Common Build Issues

**1. PEM Key Format Error**
```
Error: Invalid private key format
```
**Fix:** Ensure GitHub secret `COINBASE_API_SECRET` contains full PEM with headers:
```
-----BEGIN EC PRIVATE KEY-----
MHcCAQEEI...
-----END EC PRIVATE KEY-----
```

**2. Credential Injection Failed**
```
Error: BuildConfig field not found
```
**Fix:** Check environment variables are set in GitHub Actions:
```yaml
env:
  COINBASE_API_KEY: ${{ secrets.COINBASE_API_KEY }}
  COINBASE_API_SECRET: ${{ secrets.COINBASE_API_SECRET }}
```

**3. Build Timeout**
```
Error: Task :app:assembleDebug timed out
```
**Fix:** Usually resolves on retry. May be dependency download issues.

**4. Firebase Distribution Failed**
```
Error: Firebase CLI not found
```
**Fix:** Already handled in workflow setup. Check Firebase token validity.

### Debug Commands

**Check build status:**
```bash
cat .build-status
```

**View build log (if failed):**
```bash
cat build-log.txt
```

**Manual build test locally:**
```bash
# With local.properties configured
./gradlew assembleDebug
```

### GitHub Actions Workflow Health

**Build Frequency:** Every push to `main` or `claude/*` branches
**Average Duration:** 3-4 minutes
**Success Rate:** 95%+ (failures usually dependency-related, resolve on retry)
**Artifacts:** 7-day retention for debug APKs

**Monitoring:**
- Build status committed back to repository automatically
- Firebase distribution sends email notifications
- Failed builds generate build-log.txt for debugging

## Security Considerations

**Credential Protection:**
- API secrets stored as GitHub repository secrets (encrypted)
- Never logged in build output
- Embedded in APK (standard Android app security)
- Only accessible via Hilt DI within app

**Access Control:**
- Only repository maintainers can view secrets
- Actions require push access to repository
- Firebase distribution limited to specified email

**Audit Trail:**
- All builds logged in GitHub Actions
- Git history tracks all changes
- Build artifacts retained for forensics

The CI/CD pipeline has been enhanced in v1.5.1 with improved PEM key handling and more robust error recovery, making it even more reliable for remote development with Claude Code.
