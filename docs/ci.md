# GitHub Actions CI/CD

**Status:** ✅ Active
**Last Build:** #30 (SUCCESS)
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

## Triggers

```yaml
# Build workflow
on:
  push:
    branches: [ "claude/*", "main" ]
  workflow_dispatch:  # Manual trigger via GitHub UI

# Documentation workflow
on:
  push:
    branches: [ "claude/*" ]
  pull_request:
    branches: [ "main" ]
```

**Build runs on:**
- Every push to `main`
- Every push to `claude/*` pattern branches
- Manual dispatch from GitHub Actions tab

**Documentation runs on:**
- Every push to `claude/*` pattern branches
- Every pull request to `main`

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
│ Build debug APK     │
│ (continue-on-error) │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ Upload to Firebase  │
│ App Distribution    │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ Commit build status │
│ (.build-status file)│
│ Push back to branch │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ Auto-update docs    │
│ (via Claude API)    │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ Commit doc updates  │
│ Push back [skip ci] │
└─────────────────────┘
```

## Auto-Documentation Workflow

**File:** `.github/workflows/update-docs.yml`

### How It Works

1. **Triggered on push** to `claude/*` branches or PR to `main`
2. **Analyzes changes** - Gets git diff (up to 8000 lines)
3. **Reads current docs** - CLAUDE.md and all files in docs/
4. **Calls Claude API** - Analyzes changes and updates relevant documentation
5. **Commits back** - Updates files and pushes with `[skip ci]` to avoid infinite loops

### What Gets Updated

- **CLAUDE.md** - Current project state, tech stack, dependencies
- **docs/ci.md** - This file (workflow changes)
- **docs/reference.md** - Implementation guide updates
- **Any docs/*.md file** - Based on code changes

### Configuration

**Required Secret:** `ANTHROPIC_API_KEY` in GitHub repo settings

**Model:** `claude-3-5-sonnet-20241022`

**Commit Format:**
```
Update documentation based on code changes [skip ci]

Automated update by update-docs workflow.

Co-Authored-By: Claude Sonnet 3.5 <noreply@anthropic.com>
```

### Benefits

✅ **Never out of sync** - Documentation updates with every code change
✅ **Zero manual work** - Claude reads the diff and updates docs automatically
✅ **Works with Mobile** - No local setup needed for doc maintenance
✅ **Preserves context** - Maintains existing structure and formatting

## Security

**Secrets used:**
- `ANTHROPIC_API_KEY` - For documentation updates
- Firebase service account (automatic via Firebase CLI)

**Branch protection:**
- No secrets exposed to public
- Only runs on authenticated pushes
- `[skip ci]` prevents infinite loops

## Troubleshooting

### Build Failures

1. Check `.build-status` file after `git pull`
2. Read `build-log.txt` for specific errors
3. Common issues:
   - Gradle sync problems
   - Missing dependencies
   - Kotlin compilation errors
   - Resource conflicts

### Documentation Not Updating

1. Check if `ANTHROPIC_API_KEY` secret is set
2. Verify branch matches `claude/*` pattern
3. Look for workflow errors in GitHub Actions tab
4. Check if changes actually affect documented areas

### Firebase Distribution Issues

1. Ensure Firebase project is configured
2. Check `google-services.json` is present
3. Verify Firebase CLI authentication
4. Check Firebase App Distribution limits

**Build History:** All builds logged in GitHub Actions with artifacts and Firebase distribution links.

