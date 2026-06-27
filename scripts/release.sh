#!/usr/bin/env bash
set -euo pipefail

# SkyMetron Release Script
#
# Creates a new release tag, builds, and publishes.
#
# Usage:
#   ./scripts/release.sh v0.1.0-beta       # dry run (shows what would happen)
#   ./scripts/release.sh v0.1.0-beta --go   # actually do it
#
# Prerequisites:
#   - GitHub CLI (gh) installed and authenticated
#   - Git tag permissions
#   - Docker login to ghcr.io
#   - npm login to GitHub Packages (for desktop publish)

VERSION="${1:?Usage: $0 <version> [--go]}"
CONFIRM="${2:-}"

if [ "$CONFIRM" != "--go" ]; then
  echo "╔══════════════════════════════════════════════════════════╗"
  echo "║  DRY RUN — Add --go to execute                          ║"
  echo "╚══════════════════════════════════════════════════════════╝"
  echo ""
  echo "Would execute:"
  echo "  1. Update version in sky-monolith/pom.xml to $VERSION"
  echo "  2. Update version in sky-desktop/package.json to $VERSION"
  echo "  3. git commit -m 'Release $VERSION'"
  echo "  4. git tag -a '$VERSION' -m 'Release $VERSION'"
  echo "  5. git push origin main --tags"
  echo "  6. GitHub Actions builds and publishes:"
  echo "     - sky-core JAR"
  echo "     - Desktop installers (Linux/macOS/Windows)"
  echo "     - Docker images (ghcr.io)"
  echo "     - GitHub Release with changelog"
  echo ""
  echo "Current state:"
  git log --oneline -5
  exit 0
fi

echo "→ Releasing SkyMetron $VERSION ..."

# 1. Update versions
echo "  → Updating pom.xml version ..."
mvn -q versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false -f sky-monolith/pom.xml

echo "  → Updating package.json version ..."
if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i '' "s/\"version\": \".*\"/\"version\": \"$VERSION\"/" sky-desktop/package.json
else
  sed -i "s/\"version\": \".*\"/\"version\": \"$VERSION\"/" sky-desktop/package.json
fi

# 2. Commit and tag
echo "  → Committing and tagging ..."
git add -A
git commit -m "Release $VERSION"
git tag -a "$VERSION" -m "Release $VERSION"

# 3. Push
echo "  → Pushing to remote ..."
git push origin main --tags

echo ""
echo "✓ Release $VERSION pushed!"
echo "  Monitor: https://github.com/anomalyco/SkyMetron/actions"
echo "  Releases: https://github.com/anomalyco/SkyMetron/releases"
echo ""
echo "Post-release checklist:"
echo "  [ ] Verify CI: https://github.com/anomalyco/SkyMetron/actions"
echo "  [ ] Check Docker images: ghcr.io/anomalyco/skymetron"
echo "  [ ] Test desktop auto-update"
echo "  [ ] Verify health: curl http://localhost:8080/actuator/health"
echo "  [ ] Run benchmark: curl http://localhost:8080/api/admin/benchmark"
echo "  [ ] Update version back to SNAPSHOT: mvn versions:set -DnewVersion=0.2.0-SNAPSHOT"
