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
- Pipeline recognizes as retry attempt
- No infinite loops (max 1 retry)
- Manual intervention required if still fails

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

**Behavior:**
- Used for version bumps
- Used for documentation updates
- Prevents infinite commit loops

**Example:**
```
🔖 Bump version to 1.6.1 [skip ci]

Automated version bump by Claude based on recent commits.

See docs/releases/current.md for release notes.
```

---

## Workflow Files

### `.github/workflows/build.yml`

**Triggers:**
- Push to `main` branch
- Push to `claude/*` branches
- Manual workflow dispatch

**Key steps:**
1. **Environment setup** - Java 17, credentials injection
2. **Build and test** - `testDebugUnitTest` + `assembleDebug`
3. **Claude error analysis** - On failure, analyze with AI
4. **Claude version management** - On success, manage versions
5. **APK upload** - Firebase App Distribution
6. **Status commit** - Always commits build result

**Enhanced features:**
- Full git history (`fetch-depth: 0`) for Claude analysis
- Comprehensive error capture and analysis
- Intelligent version bumping based on commit patterns
- Automated release note generation

### Example Build Execution

```yaml
# Smart credential handling with enhanced escaping
- name: Run unit tests (catches compilation errors)
  env:
    COINBASE_API_KEY: ${{ secrets.COINBASE_API_KEY }}
    COINBASE_API_SECRET: ${{ secrets.COINBASE_API_SECRET }}
  run: |
    set +e
    ./gradlew testDebugUnitTest 2>&1 | tee full-build-log.txt
    BUILD_EXIT_CODE=${PIPESTATUS[0]}
    echo "exit_code=$BUILD_EXIT_CODE" >> $GITHUB_OUTPUT
    exit $BUILD_EXIT_CODE
  continue-on-error: true

# Claude API integration for error analysis
- name: Analyze and fix failures with Claude
  if: steps.test_build.outcome == 'failure' && steps.check_fix.outputs.is_fix_attempt == 'false'
  env:
    ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
  run: |
    # Extract error context
    tail -500 full-build-log.txt > error-log.txt
    
    # Call Claude API with structured prompt
    RESPONSE=$(curl -s https://api.anthropic.com/v1/messages \
      -H "x-api-key: $ANTHROPIC_API_KEY" \
      -d '{
        "model": "claude-sonnet-4-5-20250929",
        "max_tokens": 4096,
        "messages": [{"role": "user", "content": "..."}]
      }')
    
    # Commit analysis for developer review
    git add fix-instructions.txt error-log.txt full-build-log.txt
    git commit -m "[claude-fix] Build failed - Claude analysis attached"
    git push
```

---

## Benefits

### For Claude Code Development

**1. Remote Build Capability**
- No local Gradle required
- Build with real credentials on GitHub infrastructure  
- Immediate APK available for testing via Firebase

**2. Intelligent Error Recovery**
- AI analyzes build failures
- Provides specific fix recommendations
- Reduces debugging time

**3. Automated Project Management**
- Version numbers managed automatically
- Release notes generated from commits
- Professional release workflow

**4. Mobile-Friendly Workflow**
- Push from anywhere → Build → Test on device
- No heavy local toolchain required
- Fast iteration cycle

### For Project Quality

**1. Consistent Releases**
- Semantic versioning enforced
- Documentation always up-to-date
- Build status always visible

**2. Error Documentation**
- All build failures logged and analyzed
- Historical troubleshooting knowledge
- Improved code quality over time

**3. CI/CD Best Practices**
- Comprehensive build validation
- Automated APK distribution
- Professional development workflow

---

## Troubleshooting

### Build Still Failing After Claude Fix

1. **Check fix-instructions.txt** - Review Claude's analysis
2. **Apply recommended changes** manually
3. **Push with descriptive commit** message
4. **Monitor build logs** for remaining issues

### Version Not Bumping

1. **Check commit messages** - Must indicate meaningful changes
2. **Verify ANTHROPIC_API_KEY** is valid
3. **Review recent-commits.txt** in build artifacts
4. **Manual version bump** if needed

### APK Not Uploading

1. **Verify FIREBASE_SERVICE_ACCOUNT_JSON** is valid
2. **Check Firebase project** configuration
3. **Ensure proper permissions** for App Distribution
4. **Download from GitHub artifacts** as fallback

### Claude API Limits

1. **Rate limit reached** - Wait and retry
2. **API key invalid** - Check ANTHROPIC_API_KEY secret
3. **Response parsing failed** - Check build logs for details
4. **Manual intervention** when AI analysis fails

---

## Advanced Configuration

### Custom Version Bump Rules

Add patterns to influence version bumping:

```yaml
# Commit patterns that trigger specific bumps
- feat: -> minor bump
- fix: -> patch bump
- BREAKING CHANGE: -> major bump
- docs: -> no bump
- refactor: -> no bump
```

### Enhanced Error Analysis

Customize Claude prompts for specific error types:

```yaml
# Different prompts for different failure types
- Compilation errors -> Focus on syntax/imports
- Test failures -> Focus on logic/mocking
- Build errors -> Focus on configuration/dependencies
```

### Custom Release Notes

Influence release note generation:

```yaml
# Templates for different change types
- New features: "✨ Added [feature]"
- Bug fixes: "🐛 Fixed [issue]"  
- Performance: "⚡ Improved [aspect]"
```

---

## Security Considerations

### API Key Management

- **Environment variables only** - Never commit API keys
- **Minimal permissions** - Coinbase keys trade-only
- **Regular rotation** - Update keys periodically
- **Audit trail** - Monitor API usage

### Build Security

- **Trusted secrets only** - Verify all GitHub secrets
- **Code review** - Review all workflow changes
- **Artifact retention** - 7-day APK retention only
- **Access control** - Limit repository access

### Claude API Usage

- **Content filtering** - No sensitive data in prompts
- **Usage monitoring** - Track API consumption
- **Error handling** - Graceful failures
- **Privacy** - Build logs may contain sensitive info
