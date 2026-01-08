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
2. ✅ Builds debug APK with embedded credentials
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
| `COINBASE_API_SECRET` | Your EC private key | Full PEM format with headers |

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

**Build Process:**
1. GitHub Actions reads secrets from repository settings
2. Sets environment variables for Gradle process
3. `app/build.gradle.kts` reads env vars and injects into BuildConfig:
   ```kotlin
   val coinbaseApiKey = System.getenv("COINBASE_API_KEY")
       ?: props.getProperty("coinbase.api.key", "")
   buildConfigField("String", "COINBASE_API_KEY", "\"$coinbaseApiKey\"")
   ```
4. `CredentialsModule` provides credentials to app via Hilt DI
5. Built APK contains embedded credentials (encrypted in APK)

### Local Development Fallback

If environment variables aren't set (local development), build falls back to `local.properties`:

```properties
# local.properties (NOT committed to git)
coinbase.api.key=organizations/your-org/apiKeys/your-key
coinbase.api.secret=-----BEGIN EC PRIVATE KEY-----...
```

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

**1. Commit-back pattern** (steps 44-63 in build workflow):
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

**2. Continue on error** (step 31-34):
```yaml
- name: Build debug APK
  continue-on-error: true  # Don't fail workflow, just record outcome
```

**Why:** Allows workflow to always commit status, even when build fails.

**3. Credential injection for CI/CD:**
```yaml
- name: Build debug APK
  env:
    COINBASE_API_KEY: ${{ secrets.COINBASE_API_KEY }}
    COINBASE_API_SECRET: ${{ secrets.COINBASE_API_SECRET }}
  run: ./gradlew assembleDebug
```

**4. Automated testing flow:**
```
Claude implements → Push → Actions build with credentials → Firebase distribution → Test on phone
```

No local Gradle execution needed. Claude Code Mobile can make changes remotely and immediately test on device with real credentials.

**5. Auto-documentation update:**
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

# Documentation is automatically updated
git log --oneline -5
# Shows: "Update documentation based on code changes [skip ci]"
```

**Fix-iterate loop:**
```bash
# 1. Push changes
git push

# 2. Wait for Actions (check GitHub UI or use gh CLI)
gh run watch

# 3. Pull result (build status + doc updates)
git pull

# 4. Check status
cat .build-status

# 5. If FAILURE, read logs and fix
cat build-log.txt
# Make fixes, repeat from step 1
```

## Recent Build History

### Build #30 - SUCCESS ✅

**Status:** SUCCESS
**Changes:** Implemented static credential injection system
**APK:** Built successfully with embedded credentials
**Distribution:** Uploaded to Firebase App Distribution

**Key Improvements:**
- Removed login screen dependency
- Simplified app navigation (direct to Dashboard)
- Build-time credential embedding
- Environment variable priority system

### Previous Issues (Resolved)

**Build #29 - Kotlin Compatibility Failure (RESOLVED)**
- **Problem:** Kotlin metadata version mismatch (2.1.0 vs 2.3.0)
- **Solution:** Updated to Kotlin 2.3.0 to match Compose BOM
- **Status:** ✅ Fixed in current build

## Required Secrets Setup

### GitHub Repository Secrets

Navigate to: **GitHub repo → Settings → Secrets and variables → Actions**

Add these repository secrets:

| Name | Value | Description |
|------|-------|-------------|
| `COINBASE_API_KEY` | `organizations/.../apiKeys/...` | Your Coinbase API key ID |
| `COINBASE_API_SECRET` | `-----BEGIN EC PRIVATE KEY-----...` | Your EC private key PEM |
| `ANTHROPIC_API_KEY` | `sk-ant-...` | Claude API key for doc updates |

### Coinbase API Key Format

**API Key ID:**
```
organizations/{your-org-id}/apiKeys/{your-key-id}
```

**Private Key PEM:**
```
-----BEGIN EC PRIVATE KEY-----
MHcCAQEEIBEhExkuoT4RX7bP...
...more base64 content...
-----END EC PRIVATE KEY-----
```

**Important:** Use the EXACT private key provided by Coinbase when creating API credentials. Do not generate your own.

### Security Notes

- Secrets are encrypted at rest in GitHub
- Only accessible to workflow runs in this repository
- Injected as environment variables during build
- APK contains credentials but is encrypted within APK structure
- Credentials never appear in build logs

## Troubleshooting

### Build Failures

**Common Issues:**

1. **Missing Secrets:**
   ```
   Error: COINBASE_API_KEY environment variable not set
   ```
   **Fix:** Add missing secret in GitHub repo settings

2. **Invalid Credential Format:**
   ```
   Error: Invalid API key format
   ```
   **Fix:** Ensure API key starts with `organizations/`

3. **PEM Parsing Error:**
   ```
   Error: Failed to parse EC private key
   ```
   **Fix:** Ensure PEM includes headers and is properly formatted

### Credential Testing

**Verify secrets are set:**
```bash
# In GitHub Actions workflow (add temporarily for debugging)
- name: Debug credentials
  run: |
    echo "API Key length: ${#COINBASE_API_KEY}"
    echo "API Secret length: ${#COINBASE_API_SECRET}"
    echo "API Key prefix: ${COINBASE_API_KEY:0:13}"  # Should show "organizations"
```

**Test locally:**
```bash
# Set environment variables
export COINBASE_API_KEY="organizations/..."
export COINBASE_API_SECRET="-----BEGIN EC PRIVATE KEY-----..."

# Build
./gradlew assembleDebug

# Check BuildConfig generated correctly
cat app/build/generated/source/buildConfig/debug/com/dpart/tradeflow/BuildConfig.java
```

### Workflow Permissions

Ensure repository has correct permissions:
- **Settings → Actions → General → Workflow permissions**
- Select "Read and write permissions"
- Required for commit-back functionality

## Monitoring

**Firebase App Distribution:**
- Builds automatically uploaded to partene.darius@gmail.com
- Install from Firebase console or email notifications
- Track app versions and crash reports

**GitHub Actions:**
- View build history: GitHub repo → Actions
- Download APK artifacts (7-day retention)
- Monitor build times and success rates

**Build Status:**
- Check `.build-status` file after each build
- Read `build-log.txt` for failure details
- Documentation auto-updated on successful changes
