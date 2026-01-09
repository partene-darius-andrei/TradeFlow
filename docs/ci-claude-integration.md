# CI/CD with Claude API Integration

**Last Updated:** 2026-01-09

## Overview

TradeFlow's CI/CD pipeline uses **Claude AI** for intelligent build fixing and automated version management. When tests or builds fail, Claude analyzes the error and provides fixes. When they succeed, Claude manages version bumps and release notes.

---

## Pipeline Flow

```
┌─────────────────────────────────────────────────────────────┐
│ PUSH TO claude/* or main                                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ Check: Is this a [claude-fix] retry?                       │
│ → Yes: Continue (no infinite loops)                         │
│ → No: First attempt                                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ RUN: ./gradlew testDebugUnitTest assembleDebug             │
│ → Captures all output to full-build-log.txt                │
│ → Detects compilation errors + test failures                │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ├─────────────FAIL──────────────┐
                       │                                │
                       │                                ▼
                       │                  ┌─────────────────────────────┐
                       │                  │ Claude API: Analyze Error   │
                       │                  │ → Last 500 lines of log     │
                       │                  │ → Identify root cause       │
                       │                  │ → Provide fix instructions  │
                       │                  └─────────────┬───────────────┘
                       │                                │
                       │                                ▼
                       │                  ┌─────────────────────────────┐
                       │                  │ Commit Analysis Files       │
                       │                  │ → fix-instructions.txt      │
                       │                  │ → error-log.txt             │
                       │                  │ → full-build-log.txt        │
                       │                  │ → Marker: [claude-fix]      │
                       │                  └─────────────┬───────────────┘
                       │                                │
                       │                                ▼
                       │                  ┌─────────────────────────────┐
                       │                  │ Push → Retrigger Pipeline   │
                       │                  │ EXIT 1 (stops here)         │
                       │                  └─────────────────────────────┘
                       │
                       └─────────────SUCCESS───────────┐
                                                        │
                                                        ▼
                                          ┌──────────────────────────────┐
                                          │ Claude API: Version Mgmt     │
                                          │ → Analyze recent commits     │
                                          │ → Determine semver bump      │
                                          │ → Generate release notes     │
                                          └──────────────┬───────────────┘
                                                        │
                                                        ▼
                                          ┌──────────────────────────────┐
                                          │ Update Version Files         │
                                          │ → app/build.gradle.kts       │
                                          │   (versionName/versionCode)  │
                                          │ → docs/releases/current.md   │
                                          └──────────────┬───────────────┘
                                                        │
                                                        ▼
                                          ┌──────────────────────────────┐
                                          │ Commit [skip ci]             │
                                          │ → No pipeline retrigger      │
                                          └──────────────┬───────────────┘
                                                        │
                                                        ▼
                                          ┌──────────────────────────────┐
                                          │ Build APK & Upload           │
                                          │ → assembleDebug              │
                                          │ → Firebase App Distribution  │
                                          └──────────────────────────────┘
```

---

## Features

### 1. Intelligent Build Fixing

**What it does:**
- Runs tests and build
- On failure, calls Claude API with error logs
- Gets analysis and fix recommendations
- Commits fix instructions with `[claude-fix]` marker
- Automatically retriggers pipeline

**How it works:**
```bash
# Step 1: Run tests (always logs output)
./gradlew testDebugUnitTest assembleDebug 2>&1 | tee full-build-log.txt

# Step 2: On failure, extract error context
tail -500 full-build-log.txt > error-log.txt

# Step 3: Call Claude API
curl https://api.anthropic.com/v1/messages \
  -d '{"model": "claude-sonnet-4-5-20250929", "messages": [...]}'

# Step 4: Commit analysis
git add fix-instructions.txt error-log.txt full-build-log.txt
git commit -m "[claude-fix] Build failed - Claude analysis attached"
git push  # Retriggers pipeline
```

**Infinite loop prevention:**
- Detects `[claude-fix]` marker in commit message
- If found, allows one retry
- If still fails, manual intervention required

**Output files:**
- `fix-instructions.txt`: Claude's analysis and recommended fixes
- `error-log.txt`: Last 500 lines of error output
- `full-build-log.txt`: Complete build log

### 2. Automated Version Management

**What it does:**
- Analyzes last 10 commits
- Determines if version bump needed (major/minor/patch)
- Updates `versionName`, `versionCode` in `app/build.gradle.kts`
- Generates release notes
- Commits with `[skip ci]` to avoid retriggering

**How it works:**
```bash
# Step 1: Get recent commits
git log -10 --pretty=format:"%h %s" > recent-commits.txt

# Step 2: Read current version
CURRENT_VERSION=$(grep "versionName" app/build.gradle.kts | ...)
CURRENT_CODE=$(grep "versionCode" app/build.gradle.kts | ...)

# Step 3: Call Claude API
curl https://api.anthropic.com/v1/messages \
  -d '{"messages": [{"content": "Analyze commits and bump version..."}]}'

# Step 4: Parse JSON response
{
  "bump": "patch",
  "new_version": "1.6.1",
  "new_code": 112,
  "release_notes": ["- Fix ...", "- Add ..."]
}

# Step 5: Update files
sed -i 's/versionName = ".*"/versionName = "1.6.1"/' app/build.gradle.kts
sed -i 's/versionCode = [0-9]*/versionCode = 112/' app/build.gradle.kts

# Step 6: Commit (no retrigger)
git commit -m "🔖 Bump version to 1.6.1 [skip ci]"
git push
```

**Bump logic:**
- **Major (X.0.0)**: Breaking changes, API redesign
- **Minor (x.Y.0)**: New features, enhancements
- **Patch (x.y.Z)**: Bug fixes, small improvements
- **None**: Documentation, CI changes, refactors

**Output files:**
- `docs/releases/current.md`: Generated release notes
- `app/build.gradle.kts`: Updated version numbers

---

## Required Secrets

Add these to **GitHub Settings → Secrets and variables → Actions**:

| Secret | Purpose | Example |
|--------|---------|---------|
| `ANTHROPIC_API_KEY` | Claude API access | `sk-ant-api03-...` |
| `COINBASE_API_KEY` | Embedded in APK | Your Coinbase key |
| `COINBASE_API_SECRET` | Embedded in APK | Your PEM private key |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | App Distribution upload | `{"type": "service_account", ...}` |

---

## Commit Message Markers

### `[claude-fix]`

**Purpose:** Marks commits that contain Claude's fix analysis

**Behavior:**
- Pipeline detects this marker
- Allows one retry after fix attempt
- If build still fails, requires manual intervention
- Prevents infinite loop of failed fixes

**Example:**
```
[claude-fix] Build failed - Claude analysis attached

Exit code: 1

See fix-instructions.txt for analysis and recommended fixes.
Full log: full-build-log.txt
Error excerpt: error-log.txt
```

### `[skip ci]`

**Purpose:** Prevents pipeline from running (standard GitHub convention)

**Behavior:**
- Used for version bump commits
- Used for documentation-only changes
- Prevents unnecessary builds

**Example:**
```
🔖 Bump version to 1.10.0 [skip ci]

Automated version bump by Claude based on recent commits.

See docs/releases/current.md for release notes.
```

---

## Example Workflows

### Successful Build Flow

```bash
# 1. Developer pushes code
git push origin claude/new-feature

# 2. Pipeline runs
- Tests pass ✅
- Build succeeds ✅
- Claude analyzes commits
- Version bumped 1.9.0 → 1.10.0
- Release notes generated
- APK uploaded to Firebase

# 3. Developer gets updated code
git pull  # Gets version bump commit
```

### Failed Build Flow

```bash
# 1. Developer pushes code with compilation error
git push origin claude/new-feature

# 2. Pipeline detects failure
- Tests/build fail ❌
- Claude analyzes error log
- Creates fix-instructions.txt
- Commits analysis with [claude-fix] marker
- Retriggers pipeline

# 3. Developer reviews Claude's analysis
git pull
cat fix-instructions.txt
# Apply Claude's recommendations
git commit -m "Fix compilation error based on Claude analysis"
git push
```

---

## Troubleshooting

### Build Still Failing After Claude Fix

**Symptoms:**
- `[claude-fix]` commit exists
- Build still fails
- Pipeline shows "Manual intervention required"

**Solutions:**
1. Read `fix-instructions.txt` carefully
2. Apply Claude's recommendations
3. Test locally: `./gradlew testDebugUnitTest assembleDebug`
4. Push fix without `[claude-fix]` marker

### Version Not Bumping

**Possible causes:**
- Commits are documentation-only
- Changes too minor for version bump
- Claude determined no bump needed

**Check:**
- Look for "No version bump needed" in build log
- Review `version-update.json` file
- Ensure commits represent meaningful changes

### Claude API Rate Limits

**Symptoms:**
- API calls failing
- "Rate limited" errors in logs

**Solutions:**
- Wait for rate limit reset
- Check `ANTHROPIC_API_KEY` secret is valid
- Reduce pipeline frequency if needed

---

## Benefits

### ✅ Mobile Development Friendly

Perfect for Claude Code mobile development:
- No local build required
- Push changes from anywhere
- Get instant feedback via CI/CD
- Claude fixes issues automatically

### ✅ Automated Quality Control

- Catches build failures immediately
- Provides specific fix guidance
- Maintains version history automatically
- Generates professional release notes

### ✅ Zero-Maintenance Version Management

- Semantic versioning applied consistently
- Release notes match actual changes
- Version numbers never forgotten
- Professional release documentation

### ✅ Developer Experience

- Focus on coding, not pipeline maintenance
- Clear error analysis when things break
- Automatic version bumps remove manual work
- Professional release process

