# Auto-Documentation Workflow

Automatic documentation updates using GitHub Actions and Claude API.

**Status:** ✅ Active
**Workflow File:** `.github/workflows/update-docs.yml`

## Overview

Documentation automatically updates when you push code changes. No manual doc edits needed.

## How It Works

```
┌──────────────────┐
│ Push code        │
│ to claude/*      │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ GitHub Actions   │
│ - Get diff       │
│ - Read all docs  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Claude API       │
│ - Analyze code   │
│ - Update docs    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Commit updates   │
│ back to branch   │
└──────────────────┘
```

## Triggers

**Push to branch:**
- Any `claude/*` branch
- Example: `claude/add-coinbase-api`

**Pull requests:**
- PRs targeting `main`
- Updates run on every PR sync

## What Gets Updated

### CLAUDE.md
- **Current Project State** section
  - New files/directories added
  - Features implemented
- **Tech Stack** section
  - Dependency changes
  - Library status (❌ → ⚠️ → ✅)
- **Missing Dependencies** section
  - Move to Tech Stack when added

### docs/github_actions.md
- Workflow changes
- New build steps
- Secret/environment changes
- Trigger pattern updates

### docs/plan.md
- Roadmap checkboxes ([ ] → [x])
- Phase status updates
- Implementation notes

### Any docs/*.md file
- All documentation files scanned
- Updates applied where relevant

## Example Workflow

### Scenario: Add Coinbase API Client

**1. Code changes:**
```bash
# Create new files
app/src/main/kotlin/data/remote/
├── CoinbaseApi.kt
├── dto/
│   ├── CandleDto.kt
│   └── ProductDto.kt
```

**2. Push to branch:**
```bash
git add app/src/main/kotlin/data/remote/
git commit -m "Add Coinbase API client"
git push origin claude/coinbase-api
```

**3. GitHub Actions runs:**
```
📊 Code changes detected:
   - app/src/main/kotlin/data/remote/CoinbaseApi.kt
   - app/src/main/kotlin/data/remote/dto/CandleDto.kt
   - app/src/main/kotlin/data/remote/dto/ProductDto.kt

🤖 Calling Claude API...
✅ Documentation updated:
   - CLAUDE.md (added data/remote/ to "What Exists")
   - docs/plan.md (marked Phase 0 checkbox)
```

**4. Automatic commit:**
```
commit abc123
Author: GitHub Actions <actions@github.com>

    Update documentation based on code changes

    Automated update by update-docs workflow.

    Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

**5. Pull latest:**
```bash
git pull  # Get the doc updates
```

## Configuration

### Required Secret

**`ANTHROPIC_API_KEY`** - Set in GitHub repo settings

**Setup:**
1. GitHub repo → Settings → Secrets and variables → Actions
2. New repository secret
3. Name: `ANTHROPIC_API_KEY`
4. Value: Your Anthropic API key
5. Save

### Workflow Settings

**Location:** `.github/workflows/update-docs.yml`

**Key settings:**
```yaml
# Triggers
on:
  push:
    branches: ["claude/*"]
  pull_request:
    branches: ["main"]

# Permissions
permissions:
  contents: write  # Required to commit back

# Model
ANTHROPIC_MODEL: claude-sonnet-4-20250514
```

## How Claude Analyzes Changes

**Input to Claude:**
1. **Changed files list** - What files were modified
2. **Git diff** - Actual code changes (up to 8000 lines)
3. **All documentation files** - Current state of CLAUDE.md, docs/*.md

**Claude's task:**
- Review code changes
- Identify what documentation is affected
- Update only relevant sections
- Preserve existing structure and formatting
- Mark status changes (❌ → ✅)
- Update checkboxes in roadmap

**Output format:**
```
### FILE: CLAUDE.md
[complete updated file content]
### END FILE

### FILE: docs/plan.md
[complete updated file content]
### END FILE
```

Workflow parses this and writes files.

## Benefits

### ✅ Works with Mobile Claude Code
No local setup needed. Push from anywhere, docs update automatically.

### ✅ Never Out of Sync
Documentation updates in same commit as code changes (via Actions commit-back).

### ✅ No Manual Work
Claude reads the diff and updates docs. You just code.

### ✅ Consistent with Build Workflow
Same pattern as `.github/workflows/build.yml` - commit results back to branch.

## Limitations

### ⚠️ Diff Size Limit
- Max 8000 lines analyzed
- Large refactors may need manual doc updates
- Check Actions logs if updates seem incomplete

### ⚠️ API Rate Limits
- Anthropic API has rate limits
- Multiple rapid pushes may be throttled
- Wait a few seconds between pushes if hitting limits

### ⚠️ Update Latency
- ~30-60 seconds after push
- Pull to get updates before continuing work

### ⚠️ Cost
- Uses Anthropic API credits
- ~$0.01-0.05 per update (Sonnet 4 pricing)
- Monitor usage in Anthropic Console

## Troubleshooting

### Workflow not running

**Check triggers:**
```bash
# Workflow only runs on claude/* branches
git branch --show-current
# Should output: claude/something

# Or on PRs to main
```

**Check Actions tab:**
- GitHub repo → Actions → "Update Documentation"
- View recent runs and logs

### Docs not updated

**Possible causes:**
1. Only doc files changed (no code changes detected)
2. API key not configured
3. Claude determined no updates needed
4. Diff parsing failed

**Debug:**
```bash
# View workflow logs
gh run list --workflow=update-docs.yml
gh run view <run-id> --log

# Check API key is set
# GitHub repo → Settings → Secrets → ANTHROPIC_API_KEY should exist
```

### Wrong updates applied

**Claude misunderstood changes:**
1. Review the commit from Actions
2. Fix manually if needed
3. Report pattern to improve prompt

**Too aggressive updates:**
- Claude is conservative by default
- Should only update affected sections
- File issue if overwriting unrelated content

### Commit conflicts

**If you push while Actions is running:**
```bash
# Pull to get Actions commit
git pull --rebase

# Resolve conflicts if any
git add .
git rebase --continue

# Push again
git push
```

## Disabling Auto-Docs

### Temporarily (one push)

Skip by pushing to non-matching branch:
```bash
# Workflow only runs on claude/* branches
git push origin HEAD:feature/no-auto-doc
```

### Permanently

Delete or disable workflow:
```bash
# Delete workflow file
rm .github/workflows/update-docs.yml

# Or disable in GitHub UI
# Repo → Actions → Update Documentation → ⋮ → Disable workflow
```

## Why GitHub Actions (Not Local Hooks)

This project uses GitHub Actions instead of local git hooks for documentation updates.

**Reasons:**
- ✅ Works with Mobile Claude Code (no local setup)
- ✅ Consistent with build workflow pattern
- ✅ No local dependencies (no `claude` CLI needed)
- ✅ Updates happen server-side (works from any device)
- ✅ API key stored as GitHub secret (not in local env)

## Workflow Improvements (Future)

**Potential enhancements:**

1. **Smart batching** - Group rapid commits, one doc update
2. **PR comments** - Comment on PR with doc changes summary
3. **Diff summaries** - Auto-generate changelog from commits
4. **Selective updates** - Only update specific docs based on file patterns
5. **Rollback detection** - Restore old doc state if commit reverted

## Cost Tracking

**Typical usage:**

| Scenario | API Calls | Est. Cost |
|----------|-----------|-----------|
| Small change (1-2 files) | 1 | $0.01-0.02 |
| Medium feature (5-10 files) | 1 | $0.03-0.05 |
| Large refactor (20+ files) | 1 | $0.05-0.10 |

**Monthly estimate:**
- ~20 pushes/month = $0.40-1.00
- ~50 pushes/month = $1.00-2.50
- Negligible compared to development time saved

**Monitor usage:**
- Anthropic Console → Usage
- Check monthly API spend
- Set budget alerts if concerned

## Related Files

```
.github/workflows/update-docs.yml    # Main workflow
CLAUDE.md                             # Main doc (gets updated)
docs/*.md                             # All docs (get updated)
```

## Critical Rules

1. **Always pull after push** - Get doc updates before next commit
2. **Review Actions commits** - Claude is smart but not perfect
3. **Don't edit docs manually** - Let automation handle it (or disable workflow)
4. **Monitor API usage** - Keep eye on Anthropic credits
5. **Check workflow logs** - Debug issues via Actions tab

## Quick Reference

```bash
# Standard workflow
git add app/src/NewFeature.kt
git commit -m "Add feature"
git push origin claude/feature

# Wait 30-60 seconds, then:
git pull  # Get doc updates

# Check what was updated
git log -1  # See Actions commit
git diff HEAD~1 CLAUDE.md docs/
```
