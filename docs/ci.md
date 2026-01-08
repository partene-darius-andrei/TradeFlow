# GitHub Actions CI/CD

**Status:** ✅ Active
**Last Build:** #30 SUCCESS  
**Workflow File:** `.github/workflows/build.yml` + `.github/workflows/update-docs.yml`

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
2. ✅ Builds debug APK with embedded credentials (includes PEM key escape handling)
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

**Enhanced Build Process (v1.4.0):**
1. GitHub Actions reads secrets from repository settings
2. Sets environment variables for Gradle process
3. `app/build.gradle.kts` reads env vars and handles PEM key escaping:
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
    # Get git diff and all documentation files
    # Call Claude API to analyze changes
    # Update CLAUDE.md and docs/ files
    # Commit back with [skip ci]
```

### Claude Code Usage

**After pushing changes:**
```bash
# Wait for Actions to complete (~3-5 minutes)
git pull

# Check build status
cat .build-status
# Output: SUCCESS or FAILURE

# If failed, read error details
cat build-log.txt
# Output: Last 200 lines of build output
```

## Troubleshooting

### Common Build Issues

**1. PEM Key Format Errors:**
```
Error: Failed to parse PEM private key
```

**Solution:** Verify GitHub secret `COINBASE_API_SECRET` contains:
- Complete PEM headers (`-----BEGIN EC PRIVATE KEY-----`)
- Actual newline characters (not `\n` literals)
- No extra whitespace or formatting

**2. Gradle Build Escaping:**
```
Error: Unterminated string literal
```

**Solution:** The build script now automatically escapes PEM keys. If this error persists, check for unusual characters in the PEM key.

**3. BouncyCastle Dependency Issues:**
```
Error: Could not resolve org.bouncycastle:bcprov-jdk18on:1.78
```

**Solution:** BouncyCastle dependencies are correctly configured in `exchange/coinbase/build.gradle.kts`. This should not occur.

### Version History

**v1.4.0 Changes:**
- Enhanced PEM key escaping for environment variable injection
- Improved error handling for credential parsing
- Updated app version to reflect live portfolio data integration
- Added comprehensive PEM format support (EC and PKCS8)

**Previous versions:**
- v1.3.0: Live portfolio data integration
- v1.2.0: Adaptive app icon
- v1.1.0: Basic CI/CD setup
