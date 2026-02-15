#!/usr/bin/env python3
"""
Remove stringResource: StringResource parameter/field from all transit factories,
transit info classes, trip classes, subscription classes, card classes, and DI graphs.

This handles:
1. Constructor/function parameter: `stringResource: StringResource` (with various modifiers)
2. Constructor argument passing: `stringResource = stringResource,` or `stringResource,`
3. Import removal: StringResource, DefaultStringResource
4. Property declarations: `val stringResource = DefaultStringResource()`
5. getAdvancedUi(stringResource: StringResource) -> getAdvancedUi()
6. getAdvancedUi(stringResource) -> getAdvancedUi()
7. Refill.getAgencyName(stringResource) patterns
"""

import os
import re
import sys

WORKTREE = os.path.dirname(os.path.abspath(__file__))

# Files to skip
SKIP_FILES = {
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/util/FormattedString.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/util/StringResource.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/util/DefaultStringResource.kt',
    'base/src/commonMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'base/src/androidMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'base/src/iosMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'base/src/jvmMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'base/src/wasmJsMain/kotlin/com/codebutler/farebot/base/util/GetStringBlocking.kt',
    'CLAUDE.md',
    'migrate_strings.py',
    'remove_string_resource.py',
}

def should_skip(filepath):
    rel = os.path.relpath(filepath, WORKTREE)
    return rel in SKIP_FILES

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    original = content

    # 1. Remove imports
    imports_to_remove = [
        'import com.codebutler.farebot.base.util.StringResource',
        'import com.codebutler.farebot.base.util.DefaultStringResource',
        'import com.codebutler.farebot.base.util.getStringBlocking',
        'import com.codebutler.farebot.base.util.getPluralStringBlocking',
    ]
    for imp in imports_to_remove:
        content = content.replace(imp + '\n', '')

    # 2. Remove stringResource parameter lines from constructors/functions
    # Patterns like:
    #   private val stringResource: StringResource,
    #   val stringResource: StringResource,
    #   override val stringResource: StringResource,
    #   stringResource: StringResource,
    #   stringResource: StringResource = DefaultStringResource(),
    #   private val stringResource: StringResource = DefaultStringResource(),
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        # Match stringResource parameter/field declarations
        if re.match(r'^(private\s+val\s+|val\s+|override\s+val\s+)?stringResource\s*:\s*StringResource(\s*=\s*DefaultStringResource\(\))?\s*,?\s*$', stripped):
            # If the line ends with comma, just remove the line
            # If not, check if previous line ends with comma that needs removing
            if not stripped.endswith(',') and new_lines:
                # Remove trailing comma from previous non-blank line
                for j in range(len(new_lines) - 1, -1, -1):
                    if new_lines[j].strip():
                        new_lines[j] = new_lines[j].rstrip().rstrip(',')
                        break
            i += 1
            continue

        new_lines.append(line)
        i += 1

    content = '\n'.join(new_lines)

    # 3. Remove stringResource being passed as constructor argument
    # Patterns:
    #   stringResource = stringResource,
    #   stringResource = this.stringResource,
    #   stringResource = stringResource
    #   stringResource,  (standalone on a line, as positional arg)
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        # Match: stringResource = stringResource, or stringResource = this.stringResource,
        if re.match(r'^stringResource\s*=\s*(this\.)?stringResource\s*,?\s*$', stripped):
            if not stripped.endswith(',') and new_lines:
                for j in range(len(new_lines) - 1, -1, -1):
                    if new_lines[j].strip():
                        new_lines[j] = new_lines[j].rstrip().rstrip(',')
                        break
            i += 1
            continue

        # Match standalone: stringResource = DefaultStringResource(), (as arg)
        if re.match(r'^stringResource\s*=\s*DefaultStringResource\(\)\s*,?\s*$', stripped):
            if not stripped.endswith(',') and new_lines:
                for j in range(len(new_lines) - 1, -1, -1):
                    if new_lines[j].strip():
                        new_lines[j] = new_lines[j].rstrip().rstrip(',')
                        break
            i += 1
            continue

        new_lines.append(line)
        i += 1

    content = '\n'.join(new_lines)

    # 4. Remove inline stringResource parameter from function signatures
    # e.g., fun getAdvancedUi(stringResource: StringResource): ...
    # -> fun getAdvancedUi(): ...
    content = re.sub(
        r'(\bgetAdvancedUi\s*)\(\s*stringResource\s*:\s*StringResource\s*\)',
        r'\1()',
        content
    )

    # 5. Remove stringResource argument from getAdvancedUi calls
    # e.g., getAdvancedUi(stringResource) -> getAdvancedUi()
    # e.g., getAdvancedUi(this.stringResource) -> getAdvancedUi()
    content = re.sub(
        r'(\bgetAdvancedUi\s*)\(\s*(this\.)?stringResource\s*\)',
        r'\1()',
        content
    )

    # 6. Remove stringResource from inline constructor params (single line)
    # e.g., SuicaTrip(stringResource, ...) -> SuicaTrip(...)
    # e.g., Foo(bar, stringResource, baz) -> Foo(bar, baz)
    # e.g., Foo(stringResource = stringResource, bar = baz) -> Foo(bar = baz)

    # Pattern: remove "stringResource, " or ", stringResource" from argument lists
    # Be careful not to match inside strings
    # Handle named arg: stringResource = stringResource,
    content = re.sub(r'stringResource\s*=\s*(this\.)?stringResource\s*,\s*', '', content)
    # Handle named arg at end: , stringResource = stringResource)
    content = re.sub(r',\s*stringResource\s*=\s*(this\.)?stringResource\s*(?=\))', '', content)
    # Handle named arg as only arg: (stringResource = stringResource)
    content = re.sub(r'\(\s*stringResource\s*=\s*(this\.)?stringResource\s*\)', '()', content)

    # Handle positional: stringResource, (at start of args)
    content = re.sub(r'(?<=\()(\s*)stringResource\s*,\s*', r'\1', content)
    # Handle positional: , stringResource (at end of args)
    content = re.sub(r',\s*stringResource\s*(?=\s*\))', '', content)
    # Handle positional as only arg: (stringResource)  but NOT (stringResource: Type)
    content = re.sub(r'\(\s*stringResource\s*\)(?!\s*[:{])', '()', content)

    # 7. Remove stringResource = DefaultStringResource() as arg
    content = re.sub(r'stringResource\s*=\s*DefaultStringResource\(\)\s*,\s*', '', content)
    content = re.sub(r',\s*stringResource\s*=\s*DefaultStringResource\(\)\s*(?=\))', '', content)
    content = re.sub(r'\(\s*stringResource\s*=\s*DefaultStringResource\(\)\s*\)', '()', content)

    # 8. Remove standalone property assignments
    # val stringResource = DefaultStringResource()
    # private val stringResource = DefaultStringResource()
    content = re.sub(r'\n\s*(private\s+)?val\s+stringResource\s*=\s*DefaultStringResource\(\)\s*\n', '\n', content)
    content = re.sub(r'\n\s*(private\s+)?val\s+stringResource\s*:\s*StringResource\s*=\s*DefaultStringResource\(\)\s*\n', '\n', content)

    # 9. Handle Refill abstract methods that take stringResource
    # fun getAgencyName(stringResource: StringResource): String? -> fun getAgencyName(): FormattedString?
    # These should have been handled by the mechanical migration, but just in case
    content = re.sub(
        r'(\bgetAgencyName\s*)\(\s*stringResource\s*:\s*StringResource\s*\)',
        r'\1()',
        content
    )
    content = re.sub(
        r'(\bgetShortAgencyName\s*)\(\s*stringResource\s*:\s*StringResource\s*\)',
        r'\1()',
        content
    )
    content = re.sub(
        r'(\bgetAmountString\s*)\(\s*stringResource\s*:\s*StringResource\s*\)',
        r'\1()',
        content
    )

    # Also fix call sites
    content = re.sub(r'(\bgetAgencyName\s*)\(\s*(this\.)?stringResource\s*\)', r'\1()', content)
    content = re.sub(r'(\bgetShortAgencyName\s*)\(\s*(this\.)?stringResource\s*\)', r'\1()', content)
    content = re.sub(r'(\bgetAmountString\s*)\(\s*(this\.)?stringResource\s*\)', r'\1()', content)

    # 10. Remove stringResource from inline function params
    # e.g., fun foo(stringResource: StringResource, bar: Bar) -> fun foo(bar: Bar)
    content = re.sub(r'stringResource\s*:\s*StringResource\s*,\s*', '', content)
    content = re.sub(r',\s*stringResource\s*:\s*StringResource\s*(?=\))', '', content)
    content = re.sub(r'\(\s*stringResource\s*:\s*StringResource\s*\)', '()', content)

    # Also with default value
    content = re.sub(r'stringResource\s*:\s*StringResource\s*=\s*DefaultStringResource\(\)\s*,\s*', '', content)
    content = re.sub(r',\s*stringResource\s*:\s*StringResource\s*=\s*DefaultStringResource\(\)\s*(?=\))', '', content)
    content = re.sub(r'\(\s*stringResource\s*:\s*StringResource\s*=\s*DefaultStringResource\(\)\s*\)', '()', content)

    # 11. Clean up double blank lines that may have been created
    while '\n\n\n' in content:
        content = content.replace('\n\n\n', '\n\n')

    if content != original:
        with open(filepath, 'w') as f:
            f.write(content)
        return True
    return False

def main():
    count = 0
    for root, dirs, files in os.walk(WORKTREE):
        dirs[:] = [d for d in dirs if d not in {
            'build', '.git', '.gradle', 'metrodroid', 'kotlin-js-store',
            'docs', '.devcontainer', 'config', '.claude', '.idea', 'worktrees',
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
