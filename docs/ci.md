# GitHub Actions CI/CD

**Status:** ✅ Active
**Last Build:** #30 (FAILURE - Kotlin compatibility)
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

1. ✅ Builds debug APK
2. ✅ Uploads to Firebase App Distribution (partene.darius@gmail.com)
3. ✅ Commits build status back to branch
4. ✅ Uploads APK artifact (7-day retention)

**Auto-documentation pipeline** that updates docs when code changes:

1. ✅ Analyzes git diff on push
2. ✅ Calls Claude API to update documentation
3. ✅ Commits updated CLAUDE.md and docs/ back to branch

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
│ 5. Updates docs     │
│ 6. Commits result:  │
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
│ 7. git pull         │
│ 8. cat .build-      │
│    status           │
│ 9. Fix if needed    │
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

**3. Automated testing flow:**
```
Claude implements → Push → Actions build → Firebase distribution → Test on phone
```

No local Gradle execution needed. Claude Code Mobile can make changes remotely and immediately test on device.

**4. Auto-documentation update:**
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

## Current Build Issues

### Build #30 - Kotlin Compatibility Failure

**Status:** FAILURE
**Root Cause:** Kotlin metadata version mismatch

**Error Details:**
```
> Task :core:ui:compileDebugKotlin FAILED

e: Module was compiled with an incompatible version of Kotlin. 
   The binary version of its metadata is 2.3.0, expected version is 2.1.0.

Affected libraries:
- compose-2.4.0-api.jar
- core-2.4.0-api.jar  
- compose-m3-2.4.0-api.jar
```

**Problem:** 
- Project using Kotlin 2.1.0
- Compose BOM 2025.12.01 libraries compiled with Kotlin 2.3.0
- Binary metadata incompatible

**Solutions:**

**Option 1: Update Kotlin (Recommended)**
```kotlin
// build.gradle.kts (Project level)
plugins {
    kotlin("android") version "2.3.0"  // Update from 2.1.0
    kotlin("plugin.compose") version "2.3.0"
}
```

**Option 2: Downgrade Compose BOM**
```kotlin
// gradle/libs.versions.toml
[versions]
composeBom = "2024.09.00"  # Compatible with Kotlin 2.1.0
```

**Impact:** UI theme system implemented but can't compile until resolved.

### Benefits

✅ **Remote development** - Claude Code Mobile can develop from anywhere
✅ **No local builds** - Save device resources, battery, time
✅ **Automated testing** - APK delivered to phone via Firebase
✅ **Build verification** - Claude verifies changes compile before user tests
✅ **Fast iteration** - Push → build → test in 3-5 minutes
✅ **Always-current docs** - Documentation updates automatically with code changes

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

## Troubleshooting

### Common Build Failures

**1. Kotlin Compatibility (Current Issue)**
- **Symptom:** "Module was compiled with an incompatible version of Kotlin"
- **Fix:** Update Kotlin version or downgrade dependencies
- **Prevention:** Keep Kotlin and library versions aligned

**2. Dependency Conflicts**
- **Symptom:** "Duplicate class" or "Resolution failed"
- **Fix:** Check gradle/libs.versions.toml for version mismatches
- **Prevention:** Use BOM dependencies for version alignment

**3. Memory Issues**
- **Symptom:** "OutOfMemoryError" during build
- **Fix:** Increase Gradle memory in gradle.properties
- **Prevention:** Monitor build performance, clean regularly

### Recovery Steps

**If build continues to fail:**
1. Check build-log.txt for specific errors
2. Verify all dependencies are compatible
3. Clean and rebuild: `./gradlew clean assembleDebug`
4. Update documentation to reflect current status
5. Create issue for persistent problems

**Emergency rollback:**
1. Revert to last known good commit
2. Update .build-status to SUCCESS
3. Commit with message explaining rollback
4. Investigate issue separately
