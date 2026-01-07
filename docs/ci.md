# GitHub Actions CI/CD

**Status:** ✅ Active
**Last Build:** #27 (Failed)
**Workflow File:** `.github/workflows/build.yml`

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

1. ✅ Builds debug APK
2. ✅ Uploads to Firebase App Distribution (partene.darius@gmail.com)
3. ✅ Commits build status back to branch
4. ✅ Uploads APK artifact (7-day retention)

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
│ 4. Builds APK       │
│ 5. Commits result:  │
│    - .build-status  │
│    - build-log.txt  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Claude Code         │
│                     │
│ 6. git pull         │
│ 7. cat .build-      │
│    status           │
│ 8. Fix if needed    │
└─────────────────────┘
```

### Key Features for Claude Integration

**1. Commit-back pattern** (steps 44-63 in workflow):
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

**3. Automated testing flow:**
```
Claude implements → Push → Actions build → Firebase distribution → Test on phone
```

No local Gradle execution needed. Claude Code Mobile can make changes remotely and immediately test on device.

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

**Fix-iterate loop:**
```bash
# 1. Push changes
git push

# 2. Wait for Actions (check GitHub UI or use gh CLI)
gh run watch

# 3. Pull result
git pull

# 4. Check status
cat .build-status

# 5. If FAILURE, read logs and fix
cat build-log.txt
# Make fixes, repeat from step 1
```

### Benefits

✅ **Remote development** - Claude Code Mobile can develop from anywhere
✅ **No local builds** - Save device resources, battery, time
✅ **Automated testing** - APK delivered to phone via Firebase
✅ **Build verification** - Claude verifies changes compile before user tests
✅ **Fast iteration** - Push → build → test in 3-5 minutes

### Limitations

⚠️ **Latency** - 3-5 minute feedback loop (vs instant local builds)
⚠️ **GitHub Actions minutes** - Free tier: 2000 min/month (~400-666 builds)
⚠️ **Network required** - Can't work offline
⚠️ **Build logs truncated** - Only last 200 lines on failure
⚠️ **Mobile limitations** - No MCP servers (Notion/Coinbase), limited context depth

### Mobile vs Desktop

**Claude Code Mobile:**
- ✅ Good for small tweaks, bug fixes, simple refactors
- ❌ No Notion MCP (can't read tickets/docs)
- ❌ No Coinbase MCP (can't search API docs)
- ❌ No IDE diagnostics
- ⚠️ Limited codebase context

**Claude Code Desktop + IDE:**
- ✅ Full MCP access (Notion tickets, Coinbase API docs)
- ✅ IDE integration (live errors, diagnostics)
- ✅ Deep codebase exploration
- ✅ Best for complex features, architecture changes, initial planning

**Recommendation:** Use Mobile for quick fixes after detailed tickets are written. Use Desktop for complex work requiring API docs or deep context.

## Triggers

```yaml
on:
  push:
    branches: [ "claude/*", "main" ]
  workflow_dispatch:  # Manual trigger via GitHub UI
```

**Runs on:**
- Every push to `main`
- Every push to `claude/*` pattern branches
- Manual dispatch from GitHub Actions tab

## Build Flow

```
┌─────────────────────┐
│ Push to branch      │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ Checkout code       │
│ Setup Java 17       │
│ Setup Gradle        │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ Create Firebase     │
│ service account     │
│ (from secret)       │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ ./gradlew           │
│ assembleDebug       │
│ (continue on error) │
└──────────┬──────────┘
           │
      ┌────┴────┐
      │         │
   SUCCESS   FAILURE
      │         │
┌─────▼─────┐   │
│ Upload to │   │
│ Firebase  │   │
│ App Dist  │   │
└─────┬─────┘   │
      │         │
      └────┬────┘
           │
┌──────────▼──────────┐
│ Cleanup credentials │
│ (always runs)       │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ Commit build status │
│ - SUCCESS → write   │
│   .build-status     │
│ - FAILURE → write   │
│   .build-status +   │
│   build-log.txt     │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ Upload APK artifact │
│ (success only)      │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ Fail workflow if    │
│ build failed        │
└─────────────────────┘
```

## Required Secrets

Configure in GitHub repo settings → Secrets and variables → Actions:

| Secret Name | Description | Format |
|------------|-------------|--------|
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase service account credentials | JSON file content |

**Firebase service account setup:**
1. Firebase Console → Project Settings → Service Accounts
2. Generate new private key
3. Copy entire JSON content to GitHub secret

## Build Artifacts

### 1. APK Artifact (GitHub Actions)
- **Name:** `debug-apk`
- **Path:** `app/build/outputs/apk/debug/*.apk`
- **Retention:** 7 days
- **Access:** GitHub Actions → Workflow run → Artifacts

### 2. Firebase App Distribution
- **Email:** partene.darius@gmail.com
- **Triggered:** Only on successful builds
- **Command:** `./gradlew appDistributionUploadDebug`

### 3. Build Status Files (Committed to Branch)

**`.build-status`** - Always created:
```
SUCCESS
```
or
```
FAILURE
```

**`build-log.txt`** - Only on failure:
```
[Last 200 lines of build output]
```

**Commit message pattern:**
```
Build #27: success
Build #28: failure
```

## Build Environment

```yaml
runs-on: ubuntu-latest

Java:
  distribution: temurin
  version: 17

Gradle:
  uses: gradle/actions/setup-gradle@v4
  # Automatic caching enabled
```

## Key Features

### ✅ Continue on Error
```yaml
- name: Build debug APK
  run: ./gradlew assembleDebug
  continue-on-error: true  # Don't stop workflow on build failure
```

**Why:** Allows workflow to commit build status even when build fails.

### ✅ Conditional Steps
```yaml
- name: Upload to Firebase App Distribution
  if: steps.build.outcome == 'success'
  run: ./gradlew appDistributionUploadDebug
```

**Conditions used:**
- `if: steps.build.outcome == 'success'` - Only on successful build
- `if: always()` - Always run (cleanup, status commit)
- `if: steps.build.outcome != 'success'` - Final failure step

### ✅ Credential Security
```yaml
- name: Create Firebase service account
  run: echo '${{ secrets.FIREBASE_SERVICE_ACCOUNT_JSON }}' > app/tradeflow.json

- name: Cleanup credentials
  if: always()
  run: rm -f app/tradeflow.json
```

**Security:**
- Service account created at runtime
- Deleted after build (success or failure)
- Never committed to repository

### ✅ Build Status Tracking
```yaml
- name: Commit build result
  if: always()
  run: |
    git config user.name "GitHub Actions"
    git config user.email "actions@github.com"

    if [ "${{ steps.build.outcome }}" = "success" ]; then
      echo "SUCCESS" > .build-status
      rm -f build-log.txt  # Clean up old failure logs
    else
      echo "FAILURE" > .build-status
      ./gradlew assembleDebug 2>&1 | tail -200 > build-log.txt
    fi

    git commit -m "Build #${{ github.run_number }}: ${{ steps.build.outcome }}"
```

**Benefits:**
- Build status visible in repository
- Failure logs committed for debugging
- Build number tracking via `${{ github.run_number }}`

## Current Issues

### Build #27 Failed
**Status:** ❌ Failure
**Likely cause:** Dependency/configuration issues (check `build-log.txt` if exists)

**Debug steps:**
```bash
# Check build status file
cat .build-status

# Check failure log (if exists)
cat build-log.txt

# Reproduce locally
./gradlew assembleDebug --stacktrace

# Check GitHub Actions logs
gh run view --log
```

## Local Development

**Test build before pushing:**
```bash
# Clean build
./gradlew clean assembleDebug

# Verify APK created
ls -lh app/build/outputs/apk/debug/

# Test Firebase upload (requires google-services.json)
./gradlew appDistributionUploadDebug
```

## Firebase App Distribution Configuration

**Location:** `app/build.gradle.kts`

```kotlin
firebaseAppDistribution {
    serviceCredentialsFile = "app/tradeflow.json"
    releaseNotes = "Automated build from CI/CD"
    groups = "testers"  // Or specific tester emails
}
```

**Note:** `app/tradeflow.json` is created by CI from secrets, not in repository.

## Permissions

```yaml
permissions:
  contents: write  # Required to commit build status
```

**Why write access:**
- Commit `.build-status` file
- Commit `build-log.txt` on failure
- Push changes back to branch

## Workflow Improvements (Future)

**Potential enhancements:**

1. **Release builds:**
```yaml
- name: Build release APK
  if: github.ref == 'refs/heads/main'
  run: ./gradlew assembleRelease
```

2. **Test execution:**
```yaml
- name: Run unit tests
  run: ./gradlew testDebugUnitTest

- name: Upload test results
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: app/build/test-results/
```

3. **Code quality checks:**
```yaml
- name: Run Detekt
  run: ./gradlew detekt
  continue-on-error: true
```

4. **Slack/Discord notifications:**
```yaml
- name: Notify on failure
  if: steps.build.outcome != 'success'
  uses: slackapi/slack-github-action@v1
```

5. **Versioning:**
```yaml
- name: Tag release
  if: github.ref == 'refs/heads/main' && steps.build.outcome == 'success'
  run: |
    git tag "v1.0.${{ github.run_number }}"
    git push origin "v1.0.${{ github.run_number }}"
```

## Troubleshooting

### Build fails in CI but works locally

**Check:**
1. Java version mismatch (CI uses Java 17)
2. Missing `google-services.json` (should be in repo)
3. Gradle wrapper version
4. Environment-specific configurations

**Fix:**
```bash
# Test with Java 17 locally
java -version

# Run with same Gradle version
./gradlew --version
```

### Firebase upload fails

**Check:**
1. `FIREBASE_SERVICE_ACCOUNT_JSON` secret configured
2. Firebase project permissions for service account
3. `google-services.json` in `app/` directory
4. `firebaseAppDistribution` block in `app/build.gradle.kts`

### Build status not committed

**Check:**
1. `contents: write` permission in workflow
2. Branch protection rules (may prevent force push)
3. Workflow logs for git errors

## Cost & Limits

**GitHub Actions (Free tier):**
- 2,000 minutes/month for private repos
- Unlimited for public repos

**Current usage per build:**
- ~3-5 minutes per run
- ~400-666 builds/month on free tier

**Firebase App Distribution:**
- Free tier: Unlimited testers
- No build quota limits

## Manual Workflow Dispatch

**Via GitHub UI:**
1. GitHub repo → Actions tab
2. Select "Build Android" workflow
3. Click "Run workflow"
4. Select branch
5. Click "Run workflow" button

**Via GitHub CLI:**
```bash
gh workflow run build.yml --ref main
gh workflow run build.yml --ref claude/new-feature
```

## Related Files

```
.github/workflows/build.yml    # This workflow
app/google-services.json       # Firebase config (in repo)
app/tradeflow.json            # Service account (CI-generated, not in repo)
.build-status                 # Build result (auto-generated)
build-log.txt                 # Failure log (auto-generated on failure)
app/build.gradle.kts          # Firebase App Distribution config
```

## Critical Rules

1. **Never commit `app/tradeflow.json`** - Service account credentials
2. **Check `.build-status` before merging** - Ensure builds pass
3. **Review `build-log.txt` on failures** - Debugging info
4. **Test locally before pushing** - Save CI minutes
5. **Don't disable `continue-on-error`** - Breaks status tracking

## Quick Status Check

```bash
# Check current build status
cat .build-status

# View last 5 build commits
git log --oneline --grep="Build #" -5

# Count recent failures
git log --oneline --grep="Build #.*failure" --since="1 week ago" | wc -l
```