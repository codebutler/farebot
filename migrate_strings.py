#!/usr/bin/env python3
"""
Mechanical migration script: Replace getStringBlocking/getPluralStringBlocking/stringResource.getString
with FormattedString equivalents throughout the codebase.

This handles:
1. getStringBlocking(... ) -> FormattedString(... )
2. getPluralStringBlocking(... ) -> FormattedString.plural(... )
3. stringResource.getString(... ) -> FormattedString(... )
4. Import replacements
5. Property type fixups: override val cardName: String -> override val cardName: FormattedString
6. Remove stringResource constructor parameters and fields
7. Update override val signatures for Trip/Subscription/TransitInfo properties
"""

import os
import re
import sys

WORKTREE = os.path.dirname(os.path.abspath(__file__))

# Files to skip (already manually updated or infrastructure files to delete)
SKIP_FILES = {
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/ui/ListItem.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/ui/HeaderListItem.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/ui/ListItemInterface.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/ui/FareBotUiTree.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/ui/UiTreeBuilder.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/util/FormattedString.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/util/DefaultStringResource.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/util/StringResource.kt',
    'base/src/androidMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'base/src/iosMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'base/src/jvmMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'base/src/wasmJsMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'transit/src/commonMain/kotlin/com/codebutler/farebot/transit/TransitInfo.kt',
    'transit/src/commonMain/kotlin/com/codebutler/farebot/transit/TransitIdentity.kt',
    'transit/src/commonMain/kotlin/com/codebutler/farebot/transit/Trip.kt',
    'transit/src/commonMain/kotlin/com/codebutler/farebot/transit/Subscription.kt',
    'transit/src/commonMain/kotlin/com/codebutler/farebot/transit/Station.kt',
    'app/src/commonTest/kotlin/com/codebutler/farebot/test/TestStringResource.kt',
    # Skip non-Kotlin files
    'CLAUDE.md',
    'migrate_strings.py',
}

# Properties that changed from String to FormattedString
FORMATTEDSTRING_OVERRIDE_PROPS = [
    'cardName',
    'routeName',
    'agencyName',
    'shortAgencyName',
    'fareString',
    'subscriptionName',
    'saleAgencyName',
    'warning',
    'emptyStateMessage',
]

def should_skip(filepath):
    rel = os.path.relpath(filepath, WORKTREE)
    return rel in SKIP_FILES

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    original = content
    modified = False

    # 1. Replace getStringBlocking( -> FormattedString(
    if 'getStringBlocking(' in content:
        content = content.replace('getStringBlocking(', 'FormattedString(')
        modified = True

    # 2. Replace getPluralStringBlocking( -> FormattedString.plural(
    if 'getPluralStringBlocking(' in content:
        content = content.replace('getPluralStringBlocking(', 'FormattedString.plural(')
        modified = True

    # 3. Replace stringResource.getString( -> FormattedString(
    if 'stringResource.getString(' in content:
        content = content.replace('stringResource.getString(', 'FormattedString(')
        modified = True

    # 4. Fix override val properties that changed type from String to FormattedString
    for prop in FORMATTEDSTRING_OVERRIDE_PROPS:
        # override val propName: String  ->  override val propName: FormattedString
        pattern = rf'(override\s+val\s+{prop}\s*:\s*)String(\b)'
        replacement = rf'\1FormattedString\2'
        new_content = re.sub(pattern, replacement, content)
        if new_content != content:
            content = new_content
            modified = True

    # 5. Fix "val description: String get() = getStringBlocking" -> already handled by step 1
    #    But need to fix the return type
    # PaymentMethod.description is handled in Subscription.kt (manually updated)

    # 6. Update imports
    if modified:
        # Remove old imports
        old_imports = [
            'import com.codebutler.farebot.base.util.getStringBlocking',
            'import com.codebutler.farebot.base.util.getPluralStringBlocking',
        ]
        for old_import in old_imports:
            if old_import in content:
                content = content.replace(old_import + '\n', '')

        # Add FormattedString import if not already present and file uses it
        if 'FormattedString(' in content or 'FormattedString.plural(' in content:
            fs_import = 'import com.codebutler.farebot.base.util.FormattedString'
            if fs_import not in content:
                # Find the last import line and add after it
                lines = content.split('\n')
                last_import_idx = -1
                for i, line in enumerate(lines):
                    if line.startswith('import '):
                        last_import_idx = i
                if last_import_idx >= 0:
                    lines.insert(last_import_idx + 1, fs_import)
                    content = '\n'.join(lines)

    if content != original:
        with open(filepath, 'w') as f:
            f.write(content)
        return True
    return False

def main():
    count = 0
    for root, dirs, files in os.walk(WORKTREE):
        # Skip build directories, .git, metrodroid, etc.
        dirs[:] = [d for d in dirs if d not in {
            'build', '.git', '.gradle', 'metrodroid', 'kotlin-js-store',
            'docs', '.devcontainer', 'config', '.claude', '.idea',
        }]
        for fname in files:
            if not fname.endswith('.kt'):
                continue
            filepath = os.path.join(root, fname)
            if should_skip(filepath):
                continue
            if process_file(filepath):
                rel = os.path.relpath(filepath, WORKTREE)
                print(f'  Modified: {rel}')
                count += 1
    print(f'\nTotal files modified: {count}')

if __name__ == '__main__':
    main()
