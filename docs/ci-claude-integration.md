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
  -d '{"model": "claude-sonnet-4-5", "messages": [...]}'

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
- Pipeline runs normally
- If still fails → Manual intervention message
- Prevents infinite retry loops

**Example:**
```
[claude-fix] Build failed - Claude analysis attached

Exit code: 1

See fix-instructions.txt for analysis and recommended fixes.
Full log: full-build-log.txt
Error excerpt: error-log.txt
```

### `[skip ci]`

**Purpose:** Prevents pipeline from retriggering

**Used for:**
- Version bump commits
- Build status commits
- Documentation updates (non-code)

**Example:**
```
🔖 Bump version to 1.6.1 [skip ci]

Automated version bump by Claude based on recent commits.

See docs/releases/current.md for release notes.
```

---

## Example Scenarios

### Scenario 1: Compilation Error

```
1. You push code with a typo in Kotlin file
2. Pipeline runs: testDebugUnitTest + assembleDebug
3. Compilation fails
4. Claude API analyzes error:
   "Missing closing brace in RiskManager.kt:45"
5. Commits fix-instructions.txt with analysis
6. Pipeline retriggers
7. You (or Claude Code) apply the fix locally
8. Push again → Build succeeds
```

### Scenario 2: Test Failure

```
1. You push code that breaks a test
2. Pipeline runs tests
3. Test fails: "Expected 0.05, got 0.10"
4. Claude API analyzes:
   "RiskManager.calculatePositionSize() uses wrong config value"
5. Commits detailed fix instructions
6. Pipeline retriggers
7. You fix the test
8. Push → Tests pass → Version bumped → APK uploaded
```

### Scenario 3: Successful Build

```
1. You push new feature (3 commits)
2. Tests pass, build succeeds
3. Claude analyzes commits:
   - "Add PortfolioRepositoryImpl"
   - "Fix RiskManagerTest"
   - "Update CI pipeline"
4. Determines: patch bump (1.6.0 → 1.6.1)
5. Updates version files
6. Commits with [skip ci]
7. Builds APK
8. Uploads to Firebase App Distribution
```

---

## Troubleshooting

### Build Fails After Claude Fix

**Check:**
1. Read `fix-instructions.txt` for Claude's analysis
2. Check `error-log.txt` for error context
3. Review `full-build-log.txt` for complete output
4. Apply fixes manually if needed

**Common issues:**
- API rate limits (Claude API)
- Missing dependencies
- Environment-specific errors (local vs CI)

### Version Not Bumping

**Possible causes:**
1. Claude determined `"bump": "none"` (check `version-response.json`)
2. Commits are too minor (docs, refactors)
3. API error (check GitHub Actions logs)

**Manual bump:**
```bash
# Edit app/build.gradle.kts
versionName = "1.6.1"
versionCode = 112

# Commit with [skip ci] to avoid triggering version logic again
git commit -m "🔖 Manual version bump [skip ci]"
```

### API Key Issues

**Symptoms:**
- Pipeline fails at Claude API step
- "Unauthorized" or "Invalid API key"

**Fix:**
1. Check secret is set: GitHub → Settings → Secrets → Actions
2. Verify key format: `sk-ant-api03-...`
3. Test locally: `curl -H "x-api-key: $KEY" https://api.anthropic.com/v1/messages ...`

---

## Local Testing

### Test Claude API Integration

```bash
# 1. Export API key
export ANTHROPIC_API_KEY="sk-ant-api03-..."

# 2. Simulate build failure
./gradlew testDebugUnitTest 2>&1 | tail -500 > error-log.txt

# 3. Call Claude API
curl -s https://api.anthropic.com/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -d '{
    "model": "claude-sonnet-4-5-20250929",
    "max_tokens": 4096,
    "messages": [{
      "role": "user",
      "content": "Analyze this build error: $(cat error-log.txt)"
    }]
  }' | jq '.content[0].text'
```

### Test Version Bump Logic

```bash
# 1. Get recent commits
git log -10 --pretty=format:"%h %s" > recent-commits.txt

# 2. Read current version
CURRENT_VERSION=$(grep "versionName" app/build.gradle.kts | sed -E 's/.*"(.*)".*/\1/')
CURRENT_CODE=$(grep "versionCode" app/build.gradle.kts | sed -E 's/.*= ([0-9]+).*/\1/')

# 3. Call Claude API
curl -s https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -d '{
    "model": "claude-sonnet-4-5-20250929",
    "messages": [{
      "content": "Current: '"$CURRENT_VERSION"'. Commits: $(cat recent-commits.txt). Determine bump."
    }]
  }' | jq -r '.content[0].text'
```

---

## Future Improvements

### Potential Enhancements

1. **Auto-apply fixes** (not just analysis)
   - Parse Claude's response
   - Apply code changes automatically
   - Create PR if on main branch

2. **Test coverage reporting**
   - Send coverage to Claude
   - Get suggestions for missing tests

3. **Performance regression detection**
   - Compare build times
   - Alert if significantly slower

4. **Dependency updates**
   - Claude analyzes changelogs
   - Determines safe upgrades

5. **Smarter version bumping**
   - Analyze PR labels (breaking/feature/fix)
   - Look at file changes (domain/ui/docs)
   - Consider semantic commit messages

---

## Cost Estimation

**Claude API Usage:**

| Event | Tokens (avg) | Cost @ $3/1M input | Per Month (30 builds) |
|-------|--------------|-------------------|----------------------|
| Build failure analysis | ~2,000 | $0.006 | $0.18 |
| Version bump analysis | ~500 | $0.0015 | $0.045 |
| **Total per build** | ~2,500 | ~$0.0075 | **~$0.225** |

**Extremely low cost** for the value provided.

---

## Summary

This CI/CD pipeline provides:

✅ **Intelligent fixing** - Claude analyzes and provides fixes for build/test failures
✅ **Automated versioning** - Semantic version bumps based on commit analysis
✅ **Auto-generated release notes** - Clear changelog from commits
✅ **Loop prevention** - Avoids infinite retry cycles
✅ **Full logging** - Complete error context for debugging
✅ **Firebase integration** - Automatic APK distribution on success

**Result:** Faster development, fewer manual steps, better release management.
